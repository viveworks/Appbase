# Appbase Config Wizard — Flutter Web UI

Flutter web frontend for the Appbase Config Wizard tool.

Provides a browser-based interface for discovering, inspecting, and generating
YAML configuration files for Appbase services.

## Prerequisites

- Flutter SDK 3.x (stable channel)
- Dart SDK (bundled with Flutter)
- A running `appbase-config-wizard` web server (see `../bin/start-web.sh`)

## Development

```bash
# Install dependencies
flutter pub get

# Generate Freezed/Riverpod code (run whenever models change)
dart run build_runner build

# Start the Flutter dev server (proxies API calls to localhost:8080)
flutter run -d chrome
```

## Production Build

The Maven build (`mvn install -pl appbase-config-wizard`) runs `flutter build web` automatically
and packages the output under `src/main/resources/static/web/`. The resulting JAR serves
the UI at the root path (`/`).

## Architecture

| Layer | Package | Description |
|---|---|---|
| Models | `lib/features/configurations/models/` | Freezed data classes, JSON codecs |
| API client | `lib/features/configurations/api/` | Typed HTTP client for `/api/configurations` |
| Providers | `lib/features/configurations/providers/` | Riverpod state management |
| Screens | `lib/features/configurations/screens/` | Config list and form screens |
| Widgets | `lib/features/configurations/widgets/` | Field input widgets, nested form |
| Import | `lib/features/configurations/import/` | YAML import flow |
| Shell | `lib/shared/widgets/` | App shell, navigation, theme |

## API Endpoints

All endpoints are served by the embedded Jetty server at `/api/configurations`:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/configurations` | List all config classes |
| `GET` | `/api/configurations/{fqcn}` | Field details for one class |
| `POST` | `/api/configurations/{fqcn}/build` | Preview YAML without writing |
| `POST` | `/api/configurations/{fqcn}/write` | Write YAML to disk |
| `GET` | `/api/configurations/{fqcn}/default` | Default field values |
| `GET` | `/api/configurations/import/scan?dir=…` | Scan directory for YAML files |
| `POST` | `/api/configurations/import/{fqcn}/write` | Write imported config |
