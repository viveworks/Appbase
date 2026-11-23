# Contributing to Appbase

Thank you for helping improve Appbase. This public guide does not require access to maintainer
planning or review tools. Appbase is licensed under the [Apache License 2.0](LICENSE); read that
licence and the [NOTICE](NOTICE) before submitting work.

## Before you start

For a bug, check open issues and include a small reproduction, expected and actual behavior,
and the affected version or commit. Discuss a substantial feature or behavior change in an
issue before implementing it. Small documentation corrections can go straight to a pull request.
Read the [README](README.md) and [Known limitations](docs/Known-Limitations.md) before promising
support for an adapter or API.

Do **not** put a suspected vulnerability, exploit, credential, or sensitive log in a public
issue or PR. Report it privately to **[ops@viveworks.com](mailto:ops@viveworks.com)** with
reproduction steps and affected versions, and allow time to coordinate a fix.

## Get the source

You need Git, **JDK 25 or later**, and **Maven 3.6.3 or later**. Docker is needed only for
tests that start external backends. Flutter is optional for ordinary Java builds; the
configuration wizard's web frontend is an opt-in build target. See `./build.sh --help`.

Fork Appbase on GitHub, then clone your fork and branch from upstream `develop`. Use a short,
descriptive lowercase kebab-case branch name. The example below names standalone issue internal work item 1234;
outside contributors do not need the private maintainer lineage convention:

```sh
git clone https://github.com/<your-account>/Appbase.git
cd Appbase
git remote add upstream https://example.invalid/scratch-rehearsal.git
git fetch upstream develop
git switch -c issue-1234-my-change upstream/develop
```

Keep the branch focused. If `develop` advances during review, update your branch and rerun the
affected checks. Do not include unrelated formatting, build output, generated files, or secrets.

## Make and verify the change

Add or update tests when behavior changes. For documentation-only work, check the commands,
examples, and links you changed. Start with a module-scoped feedback loop, then use the
whole-repository pre-PR checks:

```sh
./build.sh --mode quick --modules appbase-java/appbase-core  # example module path
./build.sh --mode compile                              # whole-repository test compile
./build.sh --mode develop                              # unit tests, coverage, quality gates
```

`--modules` takes repository paths, not Maven artifact IDs; `compile` cannot be narrowed.
`develop` runs the unit-test and coverage build plus configured code-quality checks. Backend
integration tests are disabled by default and require their service and opt-in flag; see
`./build.sh --help` and relevant module documentation. `staging` and `release` add slower
checks. If a relevant check cannot run locally, explain why in the PR and say what did run.
Never report a skipped test as passing.

## Licence and provenance

Contribute only material you have the right to submit under the project's Apache-2.0 terms.
Check the licence and attribution of any copied code, documentation, image, font, or other
asset. Preserve applicable copyright, licence, and NOTICE information, and identify source
and licence in the PR. Do not import incompatible or undisclosed third-party material; ask
maintainers before copying anything whose provenance or compatibility is unclear.

For a `pom.xml` change, inspect direct and newly introduced transitive dependencies. Include
a `Licence impact:` section in the PR naming each added or changed
`groupId:artifactId:version`, its SPDX licence, and new transitives. For example:

```text
Licence impact:
- com.example:driver:2.0.0 — Apache-2.0
- org.example:new-transitive:1.4.0 — MIT (new transitive)
```

The [licence-change workflow](.github/workflows/licence-change.yml) may check this declaration
against the full dependency graph and refuse unmapped or incompatible additions.
These provenance and dependency obligations are additional to the commit sign-off below.

## Sign off every commit

Appbase uses the [Developer Certificate of Origin 1.1](https://developercertificate.org/).
The sign-off certifies your right to submit the contribution under the project's licence.
Use your own real name and email, matching the commit author; never sign for someone else:

```sh
git config user.name "Your Name"
git config user.email "your.email@example.com"
git commit --signoff -m "Describe your change"
scripts/ci/check-signoff.sh --base upstream/develop --head HEAD
```

Each commit needs its own `Signed-off-by: Your Name <your.email@example.com>` trailer. Recheck
after amending or rebasing. The DCO check verifies the trailer and author-email match, but
cannot establish legal ownership by itself. [LICENSE](LICENSE) section 5 states the default
terms for intentionally submitted contributions; resolve any different terms with the
maintainers before submitting.

## Open, review, and merge a pull request

Push your topic branch to your fork and open a PR against Appbase's `develop` branch. A draft
PR can collect early feedback; mark it ready for review when its scope and evidence are clear.
In the description:

1. Link the issue or explain the problem and intended behavior.
2. Summarize changes, compatibility or migration effects, and new dependencies or assets.
3. Give the commands and checks you actually ran, their results, and any you could not run.
   Include `Licence impact:` for a `pom.xml` change.
4. Add reproduction steps or before/after evidence when they help verify the change.

The public PR workflow uses GitHub's proposed merge commit for the PR against `develop`, not
the isolated PR head, and runs `./build.sh --mode develop` with a read-only token and no
repository secrets. It provides build, unit-test, and coverage feedback; it does
**not** replace review, prove integration behavior, or automatically attest to every DCO or
licence requirement. Maintainers may run separate DCO or licence checks on the exact candidate
revision. Respond to review comments with a change or a reasoned explanation. Push follow-up
commits and rerun relevant checks; earlier CI or review may not cover a new head.

Maintainers decide whether scope, tests, documentation, security, and licence expectations
are met. They may request changes or close an unsuitable PR. After necessary reviews and
checks pass, a maintainer merges into `develop`; do not assume a particular merge strategy.
Release publication is a separate maintainer decision, not automatic on PR merge.

## Troubleshooting and help

| Problem | What to check |
|---|---|
| Build rejects the toolchain | Run `java -version` and `mvn -version`; verify JDK 25+ and Maven 3.6.3+, then check `JAVA_HOME` and your `PATH`. Older toolchains fail early or at plugin checks. |
| Module build misses something | Use a repository path with `--modules`; see `./build.sh --help`. Run unnarrowed `compile` before the PR. |
| Docker-backed tests do not run | They are opt-in. Check Docker and the backend, then use the flag listed by `./build.sh --help`. Skipped is not passed. |
| Wizard frontend is absent | Ordinary Java builds skip Flutter. Use `--wizard-frontend` only with the UI toolchain installed. |
| DCO check fails | Inspect each author and sign-off with `scripts/ci/check-signoff.sh --base upstream/develop --head HEAD`; correct your own commits and push the updated branch. |
| CI differs from local | Compare the PR head SHA, JDK, command, and flags with `.github/workflows/pr.yml`; share the failing log with secrets redacted. |

For ordinary questions, open or comment on a public issue with the smallest useful
reproduction and relevant log excerpt. For security issues or sensitive data, email
**[ops@viveworks.com](mailto:ops@viveworks.com)** instead.
