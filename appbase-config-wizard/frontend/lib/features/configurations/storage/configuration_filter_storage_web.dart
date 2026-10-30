import 'package:web/web.dart' as web;

const _storageKey = 'appbase_config_wizard_configuration_filter';

/// Reads the last configuration-list filter for this browser tab.
String readConfigurationFilter() =>
    web.window.sessionStorage.getItem(_storageKey) ?? '';

/// Stores the configuration-list filter for this browser tab and its reloads.
void writeConfigurationFilter(String value) {
  if (value.isEmpty) {
    web.window.sessionStorage.removeItem(_storageKey);
  } else {
    web.window.sessionStorage.setItem(_storageKey, value);
  }
}
