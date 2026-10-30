# Appbase Config Wizard

Interactive tool for generating valid YAML configuration files for Appbase services.

## Console Mode

```bash
bin/start-console.sh
```

The script builds the JAR automatically if needed, then launches the interactive console.

## Web Mode

```bash
# Start
bin/start-web.sh [port]    # default port: 8080

# Stop
bin/stop-web.sh
```

The script builds the JAR automatically if needed, starts the server in the background,
and prints the URL once ready. Open the URL in your browser.

## Output

Generated YAML is written to the path you specify, or to a temp file
(`$TMPDIR/appbase-<ClassName>-<timestamp>.yaml`) if no path is given.

---
Powered by Appbase
