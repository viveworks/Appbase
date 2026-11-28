# Part 8 — Agent integration

[Guide index](README.md) · previous: [REST documentation](07-swagger.md) · next: [developer MCP](09-developer-mcp.md)

The repository ships `appbase-codeagents/`, a set of instruction packages for an AI coding assistant
(Claude Code). This part says what each part is, whether it is meant for someone **using** Appbase, and
how it is switched on. Everything here was read from the tree. No assistant run was performed while
writing it.

## What is in `appbase-codeagents/`

| Path | What it is | For a person building on Appbase? |
|---|---|---|
| `appbase-codeagents/claude/skills/add-appbase-service/` | scaffolds a new Appbase module (a recipe plus six layer recipes) | **Yes**: this is the integration surface |

### Scaffolding a new module: `add-appbase-service`

`appbase-codeagents/claude/skills/add-appbase-service/SKILL.md` makes the assistant interview you, one
question at a time, then generate the boilerplate and Maven sub-modules. It asks for the service name
(PascalCase), a lowercase package suffix, the layer, and the library (it searches for candidates and
asks which to use). For a client it also asks for the raw client type and the key type. For a service
it asks whether technology-specific sub-modules and HTTP endpoints are needed. It prints a summary and
waits for "yes" before it writes anything.

The layers and the recipe each dispatches to, with the status the file itself states:

| Layer | What it adds | Recipe | Status in the file |
|---|---|---|---|
| Client | a new `appbase-client-<name>` module | `client-service.md` | tested |
| Service | a new `appbase-service-<name>` module | `service-layer.md` | tested (technology sub-modules untested) |
| Service-core | a utility inside `appbase-service-core` | `service-core-extension.md` | tested |
| Core | a cross-cutting service in `appbase-core`, plus a management endpoint | `core-service.md` | **experimental**, untested end to end |
| Client-core | shared client infrastructure in `appbase-client-core` | `client-core-extension.md` | **experimental**, untested end to end |
| Container | a new servlet-container bootstrap | `add-appbase-servlet-container.md` | tested for Jetty, Undertow and Tomcat |

It is for **extending Appbase itself** (a new client or service module inside this repository), not for
generating an application that merely uses Appbase. How an application uses Appbase is parts 1 to 4.

Cautions found in the recipes:

- They write library versions into the **repository root** `pom.xml` and never into a module pom.
- They name reference modules as `appbase-client/appbase-client-aerospike/`, a path that does not exist at
  the repository root; the modules are under `appbase-java/appbase-client/`. The recipes still make
  sense, but expect to translate paths.
- They write Java package names and a groupId as they are today. Whether the pending rename changes them is not verified.

## How the packages are switched on

Inside this repository, `.claude/skills/` contains six symbolic links. The `add-appbase-service` link
points into the exported `appbase-codeagents/` tree and resolves here. The other five links point into
an `internal-agents-docs` tree that is absent from this export, so they do not make those packages
available in this checkout.

For **your own project**, the same mechanism is to copy or link the package directory into that
project's `.claude/skills/`. **The tree does not document an install procedure** beyond this
repository's own links, so that step is an inference from how the repository does it and is not
verified here. Because the recipes assume the layout of the Appbase repository, `add-appbase-service`
is only useful inside a checkout of Appbase.

## What is not covered

- A procedure to scaffold an **application** on Appbase: none exists in the tree.
- Running the assistant packages in a container or in automation.
- The maintainers' workflow and its control plane.
