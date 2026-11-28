# Part 5 — Configuration wizard

[Guide index](README.md) · previous: [bootstrap flavors](04-bootstrap-flavors.md) · next: [Docker infrastructure](06-docker-infra.md)

## Configuration wizard — local segment

The wizard generates valid YAML configuration for Appbase services. It scans the configuration
classes on its classpath and asks for each value. Its output is the kind of file that
[part 2](02-client-integrations.md) and [part 4](04-bootstrap-flavors.md) read. The module is
`appbase-config-wizard`; the entry point is the picocli application `ConfigWizardApp`, which has two
subcommands: `console` (the default when you name none) and `web`.

### Console mode (no Flutter needed)

```sh
appbase-config-wizard/bin/start-console.sh
```

The wizard jar comes from the part 1 `mvn clean install -DskipTests -P quick-profile` build, so
run that first when following the guide in order. If the jar is missing, the script performs the
narrower fallback build `mvn clean package -pl appbase-config-wizard -am -DskipTests -P quick-profile`.
That fallback preserves the test compilation needed by test-jar dependencies while skipping test
execution. The part 1 build was **run once and exited 0, and it produced the wizard jar** used for
the recorded console validation. The script then starts the
interactive console. To choose where the YAML goes, name the `console` subcommand explicitly, because
the output options belong to it: `appbase-config-wizard/bin/start-console.sh console -o <file>` (or
`--output-dir <dir>`). Otherwise it is written to the JVM temporary directory (`java.io.tmpdir`) as
`appbase-<ClassName>-<timestamp>.yaml`.

**Success:** choose **Configurations → Create new YAML configuration**, choose one of the listed
configuration classes, complete its prompts, and confirm that the YAML file exists afterwards.

The console is a text interface. In the repository's own console tests it is driven through a terminal
(a Python `pexpect` suite under `appbase-config-wizard/test/console/`, described in
`appbase-config-wizard/test/console/README.md`): a list of configuration classes with a module tag
that you narrow by typing, then one prompt per field.

### Web mode (needs Flutter to build the UI)

The web UI is a Flutter app compiled into the jar, and **it is not built by default**. Build it
explicitly first, then start the server:

```sh
./build.sh --mode quick --modules appbase-config-wizard --wizard-frontend
appbase-config-wizard/bin/start-web.sh 8080
# … use it at the printed URL …
appbase-config-wizard/bin/stop-web.sh
```

The server binds to **127.0.0.1 only** and has **no authentication**. It is a local developer tool, so
never expose it. If the port is busy it tries the next ones. The jar's `web` command supports
`--root <dir>` to limit which directory it may read and write (default: the current directory), but
`start-web.sh` does not forward that option. To use it, run
`java -jar appbase-config-wizard/target/appbase-config-wizard-1.0.0.jar web --root <dir>`.

⚠️ Starting `start-web.sh` **without** the frontend build above gives a server with a blank page.

The wizard's own HTTP API serves its OpenAPI document at `/api/openapi.json` and a Swagger UI at
`/api/swagger-ui/index.html` (asserted in
`appbase-config-wizard/src/test/java/com/viveworks/appbase/configwizard/web/ConfigWizardWebServerTest.java`).
That is the wizard's API, **not** the REST surface of an Appbase service (see [part 7](07-swagger.md)).

More detail: [`appbase-config-wizard/README.md`](../../appbase-config-wizard/README.md).
