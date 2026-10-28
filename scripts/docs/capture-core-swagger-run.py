#!/usr/bin/env python3
"""Capture runner for the User Guide evidence: Swagger UI of the real core resources.

What it does (one host-lease invocation; it re-executes itself under the lease): extracts the Swagger UI
assets from the swagger-ui webjar, starts the opt-in fixture CoreSwaggerCaptureStackTest through Maven, waits
for ready.url, runs scripts/docs/capture-swagger.mjs against a Chrome YOU already started (CDP port argument),
and writes docs/user-guide/images/swagger/ (PNG files + capture.json).

Honest label (written into the Swagger capture.json): the Swagger capture shows the REAL core JAX-RS resources
served through a TEST-ONLY OpenAPI harness, because no core-service main code (appbase-core, appbase-service-*)
registers an OpenApiResource (internal work item 2548; the configuration wizard's own web server does).

Guards: every volume involved (the checkout, the evidence directory, the Maven repository) must start with
>= 12 GiB free; the run aborts (kills its own process groups) when any of them drops below 8 GiB free, reports
a free-space drop above 5 GiB, and aborts if free space cannot be measured repeatedly. It refuses to run unless
--sha is a full 40-hex SHA equal to the HEAD of this checkout and the working tree has no modified or untracked
files (the evidence directory is excluded; the images directory too when --overwrite is given); that check is
repeated just before the build and again after the build, before the manifest is written. The git calls ignore the
caller's GIT_* environment and every user and system git configuration, and the check also refuses a checkout in
which git is told to look away: a file marked assume-unchanged or skip-work-tree, an ignored path under a source
tree (a `src` directory), or an ignored file outside a `target` directory. It never starts or stops a browser. Maven may download
dependencies; the runner itself connects only to 127.0.0.1 (the DevTools port and the fixture) and reads local listener metadata with lsof. It deletes nothing: it only writes (a) its own evidence
directory and (b) the images directory named above, and refuses to write into a non-empty images directory
unless --overwrite is given. --evidence-dir may lie inside the checkout only below a directory named `target`. Every Swagger file a failed or interrupted capture wrote (and the manifest that no
longer matches them), and any output file that contains this checkout's path or the home directory, is moved
into the evidence directory (quarantine), not left in the docs. The path-leak scan covers json and text; PNG
pixels are NOT scanned (a path drawn into an image is invisible to it); the needles cover each path in its NFC and
NFD forms, lower-cased, percent-encoded, html-escaped and json-escaped. Every catchable signal (all but SIGKILL and
SIGSTOP, plus the ones that do not end a process or are an interpreter crash, see HANDLED_SIGNALS) ends the run
through the cleanup; SIGKILL cannot be caught and leaves the outputs unscanned. The Swagger manifest records the
swagger-ui webjar name, version and sha256.

Chrome's debugging port is unauthenticated: start it with a DEDICATED profile (--user-data-dir <an empty temp dir>, no
real sessions) and --remote-debugging-address=127.0.0.1. The runner proves the DevTools listener loopback-only by inspecting the port's
listening sockets with lsof (macOS and Linux; required) and refuses when it cannot prove it. capture-swagger.mjs
refuses a DevTools WebSocket URL that is not ws://127.0.0.1:<that port>. While the fixture runs (up to 600 s) any local
process can GET its read operations. The MAVEN_ARGS check only validates that the variable parses: its content has the
trust of running Maven directly. --inner is internal (the lease re-exec sets APPBASE_CAPTURE_LEASED=1; advisory guard).

Nothing here builds or runs automatically; it is run by hand:

  python3 scripts/docs/capture-core-swagger-run.py --lease <path to the lease script> \\
      --chrome-port 9227 --sha $(git rev-parse HEAD) [--m2 <absolute maven repo path, no spaces>]

@author Navaneeth Mysore G
@author Claude
"""
import argparse
import datetime
import hashlib
import html
import json
import os
import re
import shlex
import shutil
import signal
import subprocess
import sys
import threading
import time
import unicodedata
import urllib.parse
import urllib.request
import zipfile

EXAMPLE = '''Swagger capture, run from the repository root with a Chrome you started
(google-chrome --remote-debugging-port=9227 --remote-debugging-address=127.0.0.1 --user-data-dir="$(mktemp -d)" ...):

  python3 scripts/docs/capture-core-swagger-run.py --lease <path to the lease script> --chrome-port 9227 \\
      --sha "$(git rev-parse HEAD)" [--m2 <absolute maven repo>]

--evidence-dir must be outside docs/user-guide, and inside the checkout only below a directory named target. The leak scan also looks for '/<user name>/'; for an account
whose name is a common path word (api, test, user, app) it can match legitimate text and the run then fails
closed (no escape hatch): run it under another account.'''
GIB = 1048576  # KiB per GiB
START_FLOOR = 12 * GIB
ABORT_FLOOR = 8 * GIB
DROP_REPORT = 5 * GIB
LABEL = ('Real Appbase core JAX-RS resources, served through a test-only OpenAPI harness because no core-service '
         'main code (appbase-core, appbase-service-*) registers an OpenApiResource (internal work item 2548; the configuration '
         "wizard's own web server does). Operations are not invoked in the Swagger UI.")
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
GUIDE_IMAGES = os.path.join(REPO, 'docs', 'user-guide', 'images')


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def parse():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=EXAMPLE)
    ap.add_argument('--lease', default=os.environ.get('APPBASE_BUILD_LEASE'),
                    help='path to the lease script (or env APPBASE_BUILD_LEASE); required')
    ap.add_argument('--lease-project', default='appbase')
    ap.add_argument('--chrome-port', type=int, required=True, help='CDP port of a Chrome you already started')
    ap.add_argument('--sha', required=True, help='full 40-hex SHA; must equal HEAD of this checkout')
    ap.add_argument('--m2', default=os.environ.get('APPBASE_M2_REPO'),
                    help='Maven local repository (default: the user default, ~/.m2/repository)')
    ap.add_argument('--mvn', default=os.environ.get('MVN', 'mvn'))
    ap.add_argument('--evidence-dir', help='fresh directory for logs and intermediate files '
                    '(default: a new directory under appbase-stacktest/target)')
    ap.add_argument('--overwrite', action='store_true', help='allow writing into a non-empty images directory')
    ap.add_argument('--hold-seconds', type=int, default=600)
    ap.add_argument('--inner', action='store_true', help=argparse.SUPPRESS)
    return ap.parse_args()


def listener_lines(port, which=shutil.which, run=subprocess.run):
    """The 'n' lines of lsof for the LISTEN sockets of port, or a string naming why lsof gave no answer."""
    if not which('lsof'):
        return 'lsof is not installed'
    try:
        r = run(['lsof', '-nP', '-iTCP:%d' % port, '-sTCP:LISTEN', '-Fn'], stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL, universal_newlines=True, timeout=5)
    except (OSError, subprocess.TimeoutExpired) as e:
        return 'lsof %s' % ('timed out' if isinstance(e, subprocess.TimeoutExpired) else 'did not run')
    if r.returncode != 0:
        return 'lsof exited %d' % r.returncode
    return [x[1:] for x in r.stdout.splitlines() if x.startswith('n')]


def listener_problem(lines):
    """None when every listening socket is numeric loopback; else the refusal text. lines: listener_lines()."""
    if isinstance(lines, str) or not lines:
        return ('cannot prove the DevTools listener is loopback-only (%s): install/enable lsof, or start Chrome with '
                '--remote-debugging-address=127.0.0.1 and a dedicated profile' % (lines or 'lsof found no listening socket'))
    bad = [x for x in lines if x.rsplit(':', 1)[0] not in ('127.0.0.1', '[::1]')]
    return 'the DevTools listener is not loopback-only (%s): start Chrome with --remote-debugging-address=127.0.0.1' % ', '.join(bad) if bad else None


def free_kib(path=REPO):
    line = subprocess.check_output(['df', '-Pk', path], universal_newlines=True).splitlines()[-1].split()
    return int(line[3])


def nearest_existing(path):
    path = os.path.abspath(path)
    while not os.path.exists(path):
        path = os.path.dirname(path)
    return path


def volumes(paths):
    """One representative existing path per distinct volume (by st_dev)."""
    seen = {}
    for p in paths:
        e = nearest_existing(p)
        seen.setdefault(os.stat(e).st_dev, e)
    return list(seen.values())


def m2_root(a):
    return a.m2 or os.path.join(os.path.expanduser('~'), '.m2', 'repository')


def default_evidence():
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    return os.path.join(REPO, 'appbase-stacktest', 'target', 'capture-evidence-' + stamp)


def fold(path):
    """Case-fold a path on macOS, whose default APFS volume is case-insensitive (Docs/User-Guide is docs/user-guide)."""
    return path.lower() if sys.platform == 'darwin' else path


def inside(path, d):
    """True when `path` is the directory `d` or lies inside it. `path` itself and each existing ancestor of it
    are compared with os.path.samefile (device and inode), so a wrong-case spelling on a case-insensitive volume,
    or a symlink or hard-link alias, is recognised; on macOS a case-folded string comparison backs it up."""
    p, d = os.path.realpath(path), os.path.realpath(d)
    cur = p
    while True:
        if os.path.exists(cur):
            try:
                if os.path.samefile(cur, d):
                    return True
            except OSError:
                pass
        parent = os.path.dirname(cur)
        if parent == cur:
            break
        cur = parent
    return fold(p) == fold(d) or fold(p).startswith(fold(d) + os.sep)


def evidence_in_docs(ev):
    """True when the evidence directory is the guide tree or the images directory, or inside one: a quarantine
    there would leave the leaking file in the docs tree."""
    return any(inside(ev, d) for d in (os.path.join(REPO, 'docs', 'user-guide'), GUIDE_IMAGES))


def repo_relative(path):
    """`path` relative to the checkout (case-folded comparison on macOS), or None when it is outside."""
    p, r = os.path.realpath(path), os.path.realpath(REPO)
    if fold(p).startswith(fold(r) + os.sep):
        return p[len(r) + 1:]
    return None


# Not handled: SIGKILL and SIGSTOP cannot be caught; SIGCHLD, SIGWINCH, SIGCONT, SIGURG, SIGPIPE, SIGINFO and SIGIO
# do not end a process (and Python itself ignores SIGPIPE); SIGTSTP, SIGTTIN and SIGTTOU only stop it; SIGSEGV,
# SIGBUS, SIGILL, SIGFPE, SIGABRT and SIGTRAP are an interpreter crash, where a Python handler could only re-fault;
# the real-time signals SIGRTMIN and SIGRTMAX belong to the threading library.
NOT_HANDLED = ('SIGKILL', 'SIGSTOP', 'SIGCHLD', 'SIGCLD', 'SIGWINCH', 'SIGCONT', 'SIGURG', 'SIGPIPE', 'SIGINFO',
               'SIGIO', 'SIGPOLL', 'SIGTSTP', 'SIGTTIN', 'SIGTTOU', 'SIGSEGV', 'SIGBUS', 'SIGILL', 'SIGFPE',
               'SIGABRT', 'SIGIOT', 'SIGTRAP', 'SIGEMT', 'SIGSYS', 'SIGRTMIN', 'SIGRTMAX')


def catchable_signals():
    """Every signal this platform lets the process handle and whose default action ends it (the rest is NOT_HANDLED)."""
    skip = set(getattr(signal, n) for n in NOT_HANDLED if hasattr(signal, n))
    out = []
    for s in signal.Signals:
        if s in skip or s.name.startswith('SIGRT'):
            continue
        try:
            signal.getsignal(s)
        except (OSError, ValueError):
            continue
        out.append(s)
    return tuple(out)


HANDLED_SIGNALS = catchable_signals()


def _swallow(signum, frame):
    pass


def set_handler(handler):
    """Install `handler` for every handled signal; one the platform refuses is skipped, never fatal."""
    for s in HANDLED_SIGNALS:
        try:
            signal.signal(s, handler)
        except (OSError, ValueError):
            pass


def ignore_signals():
    """Make every signal the runner handles a no-op. This is a Python-level no-op handler, not SIG_IGN: a second
    signal that has already arrived (its flag is set, its handler not yet run) is then dispatched to the no-op,
    whereas with SIG_IGN CPython 3.14 reports it as an unraisable 'ignored due to race condition' error on stderr."""
    set_handler(_swallow)


def install_term_handlers():
    """Every signal in HANDLED_SIGNALS (SIGTERM, SIGHUP, SIGINT, SIGQUIT, SIGUSR1 and the rest that would end the
    process) ends the run through SystemExit, so the finally block (children, leak check, result) runs. The handler
    first ignores all of them and only then raises: a second, different signal that arrives back to back (HUP then
    TERM) must not run a second handler and abort the cleanup before the finally block has even started. SIGKILL
    cannot be handled and leaves outputs unscanned: it is the one signal this cannot cover."""
    def stop(signum, frame):
        ignore_signals()
        raise SystemExit(128 + signum)
    set_handler(stop)


def scan_leaks(paths, needles):
    """Return 'file: needle' for every file in `paths` whose bytes contain one of the needles. This is a byte
    scan: it reads json, text and html source. PNG pixels are NOT scanned (the image data is compressed), so a
    path drawn into a screenshot is not caught by this scan, and nothing else here covers it."""
    needles = [n.encode('utf-8') for n in needles if n and len(n) > 1]
    hits = []
    for p in paths:
        with open(p, 'rb') as f:
            data = f.read()
        for n in needles:
            if n in data:
                hits.append('%s: %s' % (os.path.basename(p), n.decode('utf-8', 'replace')[:4] + '...'))
    return hits


def leak_needles(extra=()):
    """Absolute local paths that must not appear in an output: the checkout, the home directory, the user's
    directory name as a path component, the temp directory of the process and any `extra` paths (the evidence
    directory), each also in its NFC and NFD forms (macOS file names are NFD), lower-cased, percent-encoded,
    json-escaped and html-escaped."""
    home = os.path.expanduser('~')
    user = os.path.basename(home.rstrip('/'))
    out = [REPO, os.path.realpath(REPO), home, os.path.realpath(home)]
    if len(user) > 2:
        out.append('/' + user + '/')
    for p in [os.environ.get('TMPDIR')] + list(extra):
        if p and len(p.rstrip('/')) > 5:
            out += [p.rstrip('/'), os.path.realpath(p).rstrip('/')]
    out += [unicodedata.normalize('NFC', n) for n in out]  # NFC form
    out += [unicodedata.normalize('NFD', n) for n in out]  # NFD form
    out += [urllib.parse.quote(n, safe='/') for n in out]  # percent-encoded form (a file: URL)
    out += [json.dumps(n)[1:-1] for n in out]  # json-escaped form (an escaped non-ASCII letter, doubled backslashes)
    out += [n.lower() for n in out]  # lower-cased form
    out += [html.escape(n) for n in out if html.escape(n) != n]  # html-escaped form (a path in html text)
    return sorted(set(n for n in out if n and n != '/'))


def git_env(environ):
    """The environment of every git call: all GIT_* variables of the caller are dropped (the repository, the index,
    the object directory, injected configuration such as GIT_CONFIG_COUNT), and the user and system configuration
    and replace refs are switched off, so nothing outside this checkout can change what git reports."""
    env = dict((k, v) for k, v in environ.items() if not k.startswith('GIT_'))
    env.update(GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL='/dev/null', GIT_NO_REPLACE_OBJECTS='1')
    return env


def git(*args):
    """git in this checkout, never in whatever repository or configuration the caller's environment points at."""
    return subprocess.check_output(('git',) + args, cwd=REPO, env=git_env(os.environ), universal_newlines=True).strip()


def refuse(msg):
    print('REFUSED: ' + msg, file=sys.stderr)
    raise SystemExit(2)


def validate_m2(m2):
    if m2 is None:
        return
    if not re.match(r'^/[A-Za-z0-9_.+@:,=/-]+$', m2) or '..' in m2.split('/'):
        refuse('--m2 must be an absolute path made of letters, digits and _ . + @ : , = / - only '
               '(no spaces, quotes or leading dash), so it cannot split or extend the Maven arguments')
    if not os.path.isdir(m2):
        refuse('--m2 is not an existing directory')


def validate_maven_args_env():
    raw = os.environ.get('MAVEN_ARGS', '')
    try:
        shlex.split(raw)
    except ValueError as e:
        refuse('MAVEN_ARGS in the environment cannot be parsed: %s' % e)


def status_lines(a, images_too):
    """`git status` lines of the checkout, ignored paths included (the evidence directory is excluded; the images
    directory too when `images_too`). The excludes file of every configuration is switched off, so only the
    .gitignore files of the tree itself (and .git/info/exclude, which tree_hidden refuses) can hide a path."""
    spec = ['.']
    rel = repo_relative(a.evidence_dir)
    if rel:
        spec.append(':(exclude,literal)' + rel)
    if images_too:
        spec.append(':(exclude,literal)docs/user-guide/images/swagger')
    return git('-c', 'core.excludesFile=', '-c', 'core.fsmonitor=false', 'status', '--porcelain',
               '--untracked-files=normal', '--ignored=matching', '--', *spec).splitlines()


def tree_dirty(a, images_too):
    """Modified, staged or untracked files in the checkout (the evidence directory is excluded; the images
    directory too when `images_too`). Returns the porcelain text; ignored paths are not in it (see tree_hidden)."""
    return '\n'.join(l for l in status_lines(a, images_too) if not l.startswith('!! '))


def ignored_problem(path):
    """Why an ignored path cannot be trusted ('' when it is harmless): under a source tree (any `src` directory
    that is not inside a build output) or anywhere outside a `target` directory. Build output below target is fine."""
    segs = path.rstrip('/').split('/')
    if 'target' in segs:
        segs = segs[:segs.index('target')]
    elif 'src' not in segs:
        return 'an ignored file outside target'
    return 'an ignored path under a source tree' if 'src' in segs else ''


def tree_hidden(a, images_too):
    """What makes `git status` look clean while the tree is not what HEAD names: a tracked file marked
    assume-unchanged or skip-work-tree (`git ls-files -v` shows a lowercase tag or S), an ignored path under a source
    tree, or an ignored file outside target. Returns a message with counts only, never a path; '' when none."""
    found = []
    tags = [l[0] for l in git('ls-files', '-v').splitlines() if l]
    hidden = sum(1 for t in tags if t.islower() or t == 'S')
    if hidden:
        found.append('%d tracked file(s) marked assume-unchanged or skip-work-tree' % hidden)
    kinds = {}
    for l in status_lines(a, images_too):
        if l.startswith('!! '):
            why = ignored_problem(l[3:])
            if why:
                kinds[why] = kinds.get(why, 0) + 1
    for why in sorted(kinds):
        found.append('%d path(s): %s' % (kinds[why], why))
    return '; '.join(found)



def status_codes(porcelain):
    """Only the two status-code characters of the first `git status --porcelain` line: never a path."""
    lines = porcelain.splitlines()
    return re.sub(r'[^ MADRCUT?!]', '', lines[0][:2]).strip() if lines else ''


def preflight(a):
    if not a.evidence_dir:
        a.evidence_dir = default_evidence()
    a.evidence_dir = os.path.abspath(a.evidence_dir)  # the test JVM forks in another directory: never relative
    if evidence_in_docs(a.evidence_dir):
        refuse('--evidence-dir must be outside docs/user-guide (a quarantine there would stay in the docs tree)')
    rel = repo_relative(a.evidence_dir)
    if rel is not None and 'target' not in rel.split('/'):
        refuse('--evidence-dir inside the checkout must lie below a directory named target (build output): the '
               'clean-tree check excludes it, so anywhere else it could hide files')
    if not re.match(r'^[0-9a-fA-F]{40}$', a.sha):
        refuse('--sha must be a full 40-hex SHA')
    head = git('rev-parse', 'HEAD')
    if head.lower() != a.sha.lower():
        refuse('--sha %s does not match HEAD %s of this checkout' % (a.sha, head))
    if tree_dirty(a, a.overwrite):
        refuse('the working tree has modified or untracked files; commit or remove them first so the captured '
               'SHA is what ran (the images directory is exempt only with --overwrite)')
    hidden = tree_hidden(a, a.overwrite)
    if hidden:
        refuse('git is not showing everything in the working tree (%s); run `git ls-files -v` and `git status '
               '--ignored` and undo it so the captured SHA is what ran' % hidden)
    validate_m2(a.m2)
    validate_maven_args_env()
    if not a.lease or not os.path.isfile(a.lease):
        refuse('--lease (or APPBASE_BUILD_LEASE) must name the lease script')
    if not (1 <= a.hold_seconds <= 600):
        refuse('--hold-seconds must be 1..600')
    floor = START_FLOOR
    for v in volumes([REPO, a.evidence_dir, m2_root(a)]):
        try:
            free = free_kib(v)
        except Exception:
            refuse('cannot measure free disk on a volume used by this run (df failed)')
        if free < floor:
            refuse('free disk %.1f GiB on a volume used by this run is below the %d GiB start floor'
                   % (free / GIB, floor // GIB))
    return head.lower()


def check_images_dir(path, overwrite):
    if os.path.isdir(path) and os.listdir(path) and not overwrite:
        refuse('%s is not empty; pass --overwrite to replace same-named files' % path)


class Run(object):
    def __init__(self, a, head):
        self.a, self.head = a, head
        self.evidence = a.evidence_dir or default_evidence()
        os.makedirs(self.evidence)  # fails if it already exists: a fresh directory every run
        self.procs, self.stop, self.aborted = [], threading.Event(), threading.Event()
        self.vols = volumes([REPO, self.evidence, m2_root(a)])
        self.start_free_by = dict((v, free_kib(v)) for v in self.vols)
        self.start_free = min(self.start_free_by.values())
        self.samples = [self.start_free]
        self.max_drop = 0
        self.drop_reported = False
        self.touched = set()  # the images directory this run wrote into
        self.swagger_started = False
        self.swagger_before = {}  # name -> (size, mtime_ns) of the swagger images dir before the capture started
        self.webjar_info = None
        self.result = {'start': now(), 'sha': head, 'steps': {}}

    def note(self, msg):
        """Log a line to stdout and run.log. Never raises: a closed or broken stdout (BrokenPipeError) or an
        unwritable log must not abort a cleanup step that is only reporting what it did."""
        line = now() + ' ' + msg
        try:
            print(line, flush=True)
        except (OSError, ValueError):
            try:  # silence stdout for good, including the flush at interpreter exit (which would set rc 120)
                os.dup2(os.open(os.devnull, os.O_WRONLY), sys.stdout.fileno())
            except Exception:
                pass
        try:
            with open(os.path.join(self.evidence, 'run.log'), 'a') as f:
                f.write(line + '\n')
        except OSError:
            pass

    def abort(self, why):
        self.note('ABORT ' + why + '; terminating owned process groups')
        print('ABORT ' + why, file=sys.stderr, flush=True)
        self.aborted.set()
        for p in list(self.procs):
            terminate(p)

    def watch(self):
        failures = 0
        while not self.stop.wait(2):
            try:
                frees = [(v, free_kib(v)) for v in self.vols]
                failures = 0
            except Exception as e:  # df failed: never go silent
                failures += 1
                self.note('DISK-WATCH could not measure free disk (%d in a row): %s' % (failures, type(e).__name__))
                if failures >= 3:
                    self.abort('free disk cannot be measured (fail closed)')
                    return
                continue
            self.samples.append(min(f for _, f in frees))
            for v, free in frees:
                drop = self.start_free_by[v] - free
                self.max_drop = max(self.max_drop, drop)
                if drop > DROP_REPORT and not self.drop_reported:
                    self.note('DISK-DROP free space fell by more than 5 GiB since start on a volume used by this run')
                    self.drop_reported = True
                if free < ABORT_FLOOR:
                    self.abort('free disk below 8 GiB on a volume used by this run')
                    return

    def spawn(self, cmd, log, **kw):
        fh = open(os.path.join(self.evidence, log), 'w')
        p = subprocess.Popen(cmd, stdout=fh, stderr=subprocess.STDOUT, start_new_session=True, **kw)
        self.procs.append(p)
        return p

    def check_alive(self):
        if self.aborted.is_set():
            raise RuntimeError('aborted: free disk fell below 8 GiB')

    def chrome_ready(self):
        try:
            with urllib.request.urlopen('http://127.0.0.1:%d/json/version' % self.a.chrome_port, timeout=3) as r:
                json.load(r)
        except Exception as e:
            raise RuntimeError('no Chrome answering on CDP port %d (start one yourself): %s' % (self.a.chrome_port, e))
        problem = listener_problem(listener_lines(self.a.chrome_port))
        if problem:
            raise RuntimeError(problem)

    def recheck_tree(self):
        """HEAD and the working tree are what --sha named. The images directory is always excluded here (earlier
        steps of this run wrote images), and the message carries the status codes and counts only, never a path."""
        if git('rev-parse', 'HEAD').lower() != self.head:
            raise RuntimeError('HEAD moved since the run started')
        dirty = tree_dirty(self.a, True)
        if dirty:
            raise RuntimeError('the working tree changed since the run started (status %s)' % (status_codes(dirty) or '?'))
        hidden = tree_hidden(self.a, True)
        if hidden:
            raise RuntimeError('git is not showing everything in the working tree (%s)' % hidden)

    # -- Swagger --------------------------------------------------------------------------------------
    def webjar_version(self):
        with open(os.path.join(REPO, 'pom.xml')) as f:
            m = re.search(r'<swagger-ui\.version>\s*([^<\s]+)\s*</swagger-ui\.version>', f.read())
        if not m:
            raise RuntimeError('swagger-ui.version not found in the root pom')
        return m.group(1)

    def webjar_assets(self):
        """The webjar of exactly the version the repo pins (no sorting, no fallback to another repository)."""
        version = self.webjar_version()
        d = os.path.join(m2_root(self.a), 'org', 'webjars', 'swagger-ui', version)
        jar = os.path.join(d, 'swagger-ui-%s.jar' % version)
        if not os.path.isfile(jar):
            raise RuntimeError('swagger-ui webjar %s not found in the Maven repository (resolve the '
                               'appbase-config-wizard dependencies once with this --m2, or pass --m2)' % version)
        dest = os.path.join(self.evidence, 'swagger-assets')
        os.makedirs(dest)
        with zipfile.ZipFile(jar) as z:
            for name in ('swagger-ui-bundle.js', 'swagger-ui.css'):
                hits = [n for n in z.namelist() if n.startswith('META-INF/resources/webjars/swagger-ui/')
                        and n.endswith('/' + name)]
                if len(hits) != 1:
                    raise RuntimeError('%s: expected exactly one match in the webjar, found %d' % (name, len(hits)))
                with open(os.path.join(dest, name), 'wb') as f:
                    f.write(z.read(hits[0]))
        h = hashlib.sha256()
        with open(jar, 'rb') as f:
            for chunk in iter(lambda: f.read(1 << 20), b''):
                h.update(chunk)
        self.webjar_info = {'name': os.path.basename(jar), 'version': version, 'sha256': h.hexdigest()}
        self.result['swagger_ui_webjar'] = os.path.basename(jar)
        self.note('using swagger-ui webjar ' + os.path.basename(jar))
        return dest

    def swagger(self):
        images = os.path.join(GUIDE_IMAGES, 'swagger')
        check_images_dir(images, self.a.overwrite)
        self.chrome_ready()
        assets = self.webjar_assets()
        fixture = os.path.join(self.evidence, 'fixture')
        cmd = [self.a.mvn, '-B', '-q', '-pl', 'appbase-stacktest', '-am', '-Dsurefire.failIfNoSpecifiedTests=false',
               '-Dappbase-stacktest.skipTests=false', '-DskipTests=false', '-Dtest=CoreSwaggerCaptureStackTest',
               '-Dstacktest.coreswagger.capture=true', '-Dstacktest.coreswagger.assets=' + assets,
               '-Dstacktest.coreswagger.evidence=' + fixture,
               '-Dstacktest.coreswagger.holdSeconds=%d' % self.a.hold_seconds, 'test']
        if self.a.m2:
            cmd.insert(-1, '-Dmaven.repo.local=' + self.a.m2)  # its own argv item: nothing to split or inject
        self.recheck_tree()  # what Maven compiles is what --sha names
        mvn = self.spawn(cmd, 'maven.log', cwd=REPO)
        ready = os.path.join(fixture, 'ready.url')
        deadline = time.time() + 1200
        while not os.path.exists(ready):
            self.check_alive()
            if mvn.poll() is not None:
                raise RuntimeError('fixture exited before ready, rc=%s (see maven.log)' % mvn.returncode)
            if time.time() > deadline:
                raise RuntimeError('fixture startup timeout')
            time.sleep(1)
        with open(ready) as f:
            url = f.read().strip()
        self.note('fixture ready ' + url)
        os.makedirs(images, exist_ok=True)
        self.touched.add(images)
        self.swagger_before = dict((n, snapshot(os.path.join(images, n))) for n in os.listdir(images))
        self.swagger_started = True
        cap = self.spawn(['node', os.path.join(HERE, 'capture-swagger.mjs'), url, images,
                          str(self.a.chrome_port), self.head], 'capture-swagger.log')
        rc = cap.wait(timeout=240)
        if rc != 0:
            raise RuntimeError('capture-swagger.mjs failed rc=%d (see capture-swagger.log)' % rc)
        with open(os.path.join(fixture, 'capture.done'), 'w') as f:
            f.write(now())
        if mvn.wait(timeout=120) != 0:
            raise RuntimeError('fixture test failed (see maven.log)')
        self.recheck_tree()  # the build and the run changed no source: the manifest still names what ran
        harness = {}
        hp = os.path.join(fixture, 'harness.json')
        if os.path.exists(hp):
            with open(hp) as f:
                harness = json.load(f)
        self.annotate(os.path.join(images, 'capture.json'), LABEL, self.swagger_extra(harness))
        self.result['steps']['swagger'] = {'images': sorted(os.listdir(images)), 'url': url}
        self.note('swagger capture done')
        self.swagger_started = False  # complete: a later failure must not move a good capture.json

    def swagger_extra(self, harness):
        """What the runner adds to the Swagger capture.json: the harness, and which swagger-ui webjar rendered it."""
        return {'harness': 'appbase-stacktest CoreSwaggerCaptureStackTest (opt-in, test-only OpenApiResource)',
                'resources': harness, 'try_it_out': 'disabled', 'swagger_ui_webjar': self.webjar_info}

    def annotate(self, path, label, extra):
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        data['label'] = label
        data.update(extra)
        data['runner'] = 'scripts/docs/capture-core-swagger-run.py'
        with open(path, 'w', encoding='utf-8') as f:  # not ASCII-escaped: the leak scan reads the bytes
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')

    # -- outputs must not carry local paths ----------------------------------------------------------------
    def quarantine(self, path, tag):
        """Move (never delete) a file out of the docs tree into the evidence directory."""
        q = os.path.join(self.evidence, 'quarantine')
        os.makedirs(q, exist_ok=True)
        dest = os.path.join(q, '%s-%s' % (tag, os.path.basename(path)))
        shutil.move(path, dest)  # cross-volume safe (the evidence directory may be on another filesystem)
        self.note('QUARANTINED %s (moved out of the docs tree into the evidence directory)' % os.path.basename(path))

    def leak_check(self):
        """Leave no local path (checkout, home directory, temp or evidence directory) in anything this run wrote:
        every file of the images directory touched (json, text, images) is scanned. Runs on every exit path."""
        needles = leak_needles([self.evidence])
        bad, stuck = [], []
        for d in sorted(self.touched):
            if not os.path.isdir(d):
                continue
            files = [os.path.join(d, n) for n in sorted(os.listdir(d)) if os.path.isfile(os.path.join(d, n))]
            for f in files:  # one file that cannot be scanned or moved must not stop the others
                try:
                    if scan_leaks([f], needles):
                        bad.append(os.path.basename(f))
                        self.quarantine(f, os.path.basename(d))
                except Exception as e:
                    stuck.append('%s (%s)' % (os.path.basename(f), type(e).__name__))
        if bad or stuck:
            raise RuntimeError('a local path was found in: ' + ', '.join(bad or ['(not scannable)'])
                               + ('; could NOT be moved or scanned: ' + ', '.join(stuck) if stuck else
                                  '; offending files were moved to the evidence quarantine'))

    def quarantine_partial(self):
        """A failed or interrupted Swagger capture leaves partial output in the docs tree: every file it wrote or
        changed (new or modified since the capture started), and then the manifest too, which no longer matches
        the images beside it. Files that were there before and are untouched stay."""
        if not self.swagger_started:
            return
        images = os.path.join(GUIDE_IMAGES, 'swagger')
        if not os.path.isdir(images):
            return
        moved, stuck = False, []
        for n in sorted(os.listdir(images)):
            f = os.path.join(images, n)
            try:  # one file that cannot be moved must not leave the others in the docs
                if os.path.isfile(f) and self.swagger_before.get(n) != snapshot(f):
                    self.quarantine(f, 'failed-swagger')
                    moved = True
            except Exception as e:
                stuck.append('%s (%s)' % (n, type(e).__name__))
        cj = os.path.join(images, 'capture.json')
        try:
            if moved and os.path.isfile(cj):
                self.quarantine(cj, 'failed-swagger')
        except Exception as e:
            stuck.append('capture.json (%s)' % type(e).__name__)
        if stuck:
            raise RuntimeError('could not move out of the docs: ' + ', '.join(stuck))

    # -- driver ---------------------------------------------------------------------------------------
    def execute(self):
        rc = 1
        install_term_handlers()
        threading.Thread(target=self.watch, daemon=True).start()
        self.note('START freeGiB=%.1f sha=%s' % (self.start_free / GIB, self.head))
        try:
            self.swagger()
            self.leak_check()
            rc = 0
            self.note('ALL REQUESTED STEPS PASSED')
        except Exception as e:
            self.result['error'] = str(e)
            self.note('FAILED ' + str(e))  # message only, never a traceback (it would carry local paths)
        finally:
            # From here on a further SIGTERM, SIGHUP or Ctrl-C must not cut the cleanup short (the handler has
            # already ignored them when it ran; this covers a failure that never went through the handler).
            ignore_signals()
            for p in reversed(self.procs):
                try:
                    terminate(p)
                    try:
                        p.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        try:
                            os.killpg(p.pid, signal.SIGKILL)
                        except ProcessLookupError:
                            pass
                except Exception as c:  # one stuck child must not stop the others or the leak check
                    self.note('could not stop a child process: ' + type(c).__name__)
            try:  # a failed or interrupted Swagger capture (any exit path) must not leave partial files in the docs
                self.quarantine_partial()
            except Exception as q:
                self.note('could not quarantine the partial Swagger capture: ' + str(q))
            try:  # every exit path (success, failure, Ctrl-C, SIGTERM, SIGHUP): partial outputs are scanned once the children are gone
                self.leak_check()
            except BaseException as q:
                if rc == 0:
                    rc = 1
                prev = self.result.get('error')
                if str(q) not in (prev or ''):  # the success-path leak check may already have recorded this message
                    self.result['error'] = (prev + ' | ' if prev else '') + str(q)  # keep the original failure
                self.note('LEAK-CHECK ' + str(q))
            try:
                self.stop.set()
                self.samples.append(min(free_kib(v) for v in self.vols))
            except BaseException:
                self.note('DISK-WATCH final free-space measurement failed')
            try:
                self.result.update(end=now(), rc=rc, disk_abort=self.aborted.is_set(),
                                   min_free_gib=round(min(self.samples) / GIB, 3),
                                   max_drop_gib=round(self.max_drop / GIB, 3))
                with open(os.path.join(self.evidence, 'result.json'), 'w') as f:
                    json.dump(self.result, f, indent=2)
                self.note('DONE rc=%d minFreeGiB=%.3f maxDropGiB=%.3f evidence=%s' % (
                    rc, self.result['min_free_gib'], self.result['max_drop_gib'], self.evidence))
            except BaseException as w:
                rc = 1
                self.note('could not write result.json: ' + type(w).__name__)
        return rc


def snapshot(path):
    st = os.stat(path)
    return (st.st_size, st.st_mtime_ns)


def terminate(p):
    if p.poll() is None:
        try:
            os.killpg(p.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass


def lease_argv(a, inner):
    # The lease CLI is `run [--project P ...] --label X -- cmd`: the subcommand comes FIRST.
    return ['bash', a.lease, 'run', '--project', a.lease_project, '--label', 'core-swagger-capture', '--'] + inner


def main():
    a = parse()
    if a.inner and os.environ.get('APPBASE_CAPTURE_LEASED') != '1':
        refuse('--inner is set only by the lease re-exec (an advisory guard against accidental use, not a security boundary)')
    head = preflight(a)
    if not a.inner:
        # One lease invocation for the whole run: this script re-executes itself under the lease.
        env = dict(os.environ, APPBASE_CAPTURE_LEASED='1')
        inner = [sys.executable, os.path.abspath(__file__), '--inner'] + sys.argv[1:]
        cmd = lease_argv(a, inner)
        os.execvpe(cmd[0], cmd, env)
    return Run(a, head).execute()


if __name__ == '__main__':
    sys.exit(main())
