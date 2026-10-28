#!/usr/bin/env python3
"""Verify Javadoc completeness and prose diversity for the internal work item 1401 leaf."""
import argparse, collections, json, re, sys
from pathlib import Path

COVERED_PACKAGES = (
    "com.viveworks.appbase.client.core",
    "com.viveworks.appbase.client.core.accessor",
    "com.viveworks.appbase.client.core.clientkey",
    "com.viveworks.appbase.client.core.clientkey.clusterid",
)
BANNED = (
    "null arguments are not supported unless",
    "the returned value represents the state visible when this method is invoked",
    "creates a x with the collaborators and initial state supplied by the caller",
    "the instance is not ready for use until",
    "resolves <x> from the current service state",
)

def java_code(line,state):
    """Strip comments and literals while reporting the end of a Javadoc block."""
    out=[];i=0;closed=False
    while i<len(line):
        if state:
            end=line.find('*/',i)
            if end<0:return '',state,closed
            closed |= state=='javadoc';state=None;i=end+2;continue
        if line.startswith('//',i):break
        if line.startswith('/**',i):state='javadoc';i+=3;continue
        if line.startswith('/*',i):state='block';i+=2;continue
        if line[i] in "\"'":
            quote=line[i];out.append(quote+quote);i+=1
            while i<len(line) and line[i]!=quote:i += 2 if line[i]=='\\' else 1
            i+=1;continue
        out.append(line[i]);i+=1
    return ''.join(out),state,closed

def prose(paths):
    result=[]
    for path in paths:
        for block in re.findall(r"/\*\*(.*?)\*/",path.read_text(),re.S):
            text=re.sub(r"\{@\w+\s+([^}]*)}",r"\1",block)
            text=re.sub(r"<[^>]+>"," ",text)
            lines=[re.sub(r"^\s*\*\s?","",x).strip() for x in text.splitlines()]
            # Authorship/version tags are metadata, not API prose. Contract tags remain in
            # the normalized block so copied @param/@return text is checked too.
            text=" ".join(x for x in lines if not x.startswith(('@author ','@since ')))
            value=re.sub(r"[^a-z0-9]+"," ",text.lower()).strip()
            # Type names are the variable part of copied constructor prose.  Treating
            # them as identifiers makes copy/paste visible instead of four "unique" blocks.
            value=re.sub(r"\b[a-z][a-z0-9]*(?:exception|configuration|service|resources)\b",
                         "identifier",value)
            if len(value.split())>=3: result.append(value)
    return result

def undocumented_members(path):
    """Return public/protected callables lacking their own Javadoc, including overrides.

    Scope (accepted exclusions, internal work item 1401 A1-M1): only callables declared directly in a top-level public type
    are checked. Members of nested types (builders, callbacks), fields and constants are not, so a clean
    result does not prove them documented; they stay under manual review. Detector limits: a Javadoc block
    is matched only to the nearest following declaration, so stacked or dangling comments are not detected."""
    depth=0; doc=False; state=None; current=None; interface=False
    pending=None; pending_doc=False; missing=[]
    for number,raw in enumerate(path.read_text().splitlines(),1):
        code,state,closed=java_code(raw+'\n',state); doc |= closed
        text=code.strip()
        if not text: continue
        # Annotations do not consume declaration Javadoc; unlike api-inventory, Override is no exemption.
        while text.startswith('@') and not text.startswith('@interface'):
            annotation=re.match(r"@[\w.]+(?:\([^)]*\))?\s*",text)
            if not annotation: break
            text=text[annotation.end():].lstrip()
        if not text: continue
        if depth==0:
            match=re.match(r"public\s+(?:(?:abstract|final|sealed|non-sealed|static|strictfp)\s+)*(class|interface|enum|record|@interface)\s+(\w+)",text)
            if match: interface=match.group(1) in ('interface','@interface');current=match.group(2)
        elif depth==1 and current:
            if pending is not None:
                # a field wrapped across lines ends at its initializer or semicolon, before any call
                # parenthesis; it must not be reported, nor swallow the next callable
                paren=text.find('(')
                field_end=(';' in text and paren<0) or (0<=text.find('=')<(paren if paren>=0 else len(text)))
                pending += ' '+text
                if field_end:
                    pending=None;pending_doc=False
                elif '(' in pending:
                    if not pending_doc: missing.append(f"line {number}: {pending[:100]}")
                    pending=None;pending_doc=False
            else:
                callable_start=re.match(r"(?:public|protected)\b",text) or (interface and not text.startswith(('private ','class ','interface ','enum ','record ','}')))
                if callable_start:
                    if '(' in text:
                        if not doc: missing.append(f"line {number}: {text[:100]}")
                    elif not any(x in text for x in (';','=','{')):
                        pending=text;pending_doc=doc
        doc=False
        depth += text.count('{')-text.count('}')
        # Leave the type only when its body actually closed: an Allman or multi-line header reaches depth 0
        # on the lines before its opening brace and must keep the declaration state (internal work item 1401 A1-H1).
        if depth<=0:
            depth=0
            if '}' in text: current=None;interface=False;pending=None;pending_doc=False
    return missing

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1]);ap.add_argument('--packages',nargs='*',default=list(COVERED_PACKAGES));a=ap.parse_args()
    inventory_path=a.root/'docs/api-inventory/inventory.json'
    failures=[]
    if not inventory_path.is_file():
        print(f"INVENTORY RED missing {inventory_path}")
        return True
    try:
        inventory=json.loads(inventory_path.read_text())
    except (OSError, ValueError) as error:
        print(f"INVENTORY RED unreadable: {error}")
        return True
    if not inventory.get('artifacts'):
        print("INVENTORY RED no artifacts")
        return True
    expected=collections.defaultdict(set)
    classifications={}
    for artifact in inventory['artifacts']:
        for package in artifact.get('packages',[]):
            name=package.get('package')
            if name in a.packages:
                classifications[name]=package.get('classification')
                expected[name].update(package.get('public_types') or [])
    for package in a.packages:
        if package not in classifications:
            failures.append(f"{package}: absent from inventory")
        elif classifications[package] != 'supported-public':
            failures.append(f"{package}: classification is {classifications[package]!r}, not supported-public")
        elif not expected[package]:
            failures.append(f"{package}: empty public_types inventory")
    files=list(a.root.glob('**/src/main/java/**/*.java')); bypkg=collections.defaultdict(list)
    for path in files:
        match=re.search(r"\bpackage\s+([\w.]+)\s*;",path.read_text())
        if match and match.group(1) in a.packages: bypkg[match.group(1)].append(path)
    for package in a.packages:
        present={p.stem for p in bypkg[package]}
        if expected[package]-present: failures.append(f"{package}: absent types {sorted(expected[package]-present)}")
        for path in bypkg[package]:
            if path.stem not in expected[package]: continue
            source=path.read_text()
            # A declaration Javadoc may legally precede one or more annotations.
            pattern=r"/\*\*.*?\*/\s*(?:@[\w.]+(?:\([^;]*?\))?\s*)*public\s+(?:abstract\s+|final\s+)*(?:class|interface|enum|record)\s+"+re.escape(path.stem)+r"\b"
            if not re.search(pattern,source,re.S): failures.append(f"{path}: missing type Javadoc")
            if re.search(r"(?m)^\s*@(?:[\w.]+)(?:\([^\n]*\))?\s*$\n\s*/\*\*",source):
                failures.append(f"{path}: declaration Javadoc appears after an annotation")
            failures += [f"{path}: missing explicit member Javadoc: {item}" for item in undocumented_members(path)]
        status='GREEN' if not any(f.startswith(package) or f"/{package.replace('.', '/')}/" in f for f in failures) else 'RED '
        print(f"AC1401-1 {status} {package}")
    checked=sum(1 for package in a.packages for path in bypkg[package] if path.stem in expected[package])
    if checked == 0: failures.append("zero checked public types")
    docs=[p for package,values in bypkg.items() for p in values if p.stem in expected[package]]
    counts=collections.Counter(prose(docs)); extra=[]
    for path in docs:
        low=path.read_text().lower();extra += [f"{path}: banned phrase {x}" for x in BANNED if x in low]
    extra += [f"repeated {n}: {s}" for s,n in counts.items() if n>3]
    print('EXTRA_no_boilerplate '+('RED' if extra else 'GREEN'));failures+=extra
    for failure in failures: print('  '+failure)
    return bool(failures)
if __name__=='__main__':sys.exit(main())
