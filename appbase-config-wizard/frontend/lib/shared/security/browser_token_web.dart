import 'package:web/web.dart' as web;

/// Web implementation of [readBrowserToken]: reads the port-scoped `appbase_wizard_token_<port>`
/// cookie the wizard server set on the app shell (A10-01, internal work item 1449). Returns null
/// when the cookie is absent (e.g. before the shell has loaded).
///
/// @author Navaneeth Mysore G
String? readBrowserToken() {
  final cookies = web.document.cookie;
  if (cookies.isEmpty) {
    return null;
  }
  final location = web.window.location;
  final port = location.port.isNotEmpty
      ? location.port
      : (location.protocol == 'https:' ? '443' : '80');
  final prefix = 'appbase_wizard_token_$port=';
  for (final part in cookies.split(';')) {
    final trimmed = part.trim();
    if (trimmed.startsWith(prefix)) {
      return trimmed.substring(prefix.length);
    }
  }
  return null;
}
