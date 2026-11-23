/// Persists the configuration-list filter across a browser reload.
///
/// The VM implementation is an in-memory stand-in for widget tests; web uses
/// the current tab's session storage.
library;

export 'configuration_filter_storage_stub.dart'
    if (dart.library.js_interop) 'configuration_filter_storage_web.dart';
