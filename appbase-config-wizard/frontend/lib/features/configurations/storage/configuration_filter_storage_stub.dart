String _configurationFilter = '';

/// Reads the last configuration-list filter in non-web builds and widget tests.
String readConfigurationFilter() => _configurationFilter;

/// Stores the configuration-list filter in non-web builds and widget tests.
void writeConfigurationFilter(String value) {
  _configurationFilter = value;
}
