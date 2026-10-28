// @author Navaneeth Mysore G
// @author Claude
/// Opens the browser's directory picker and reports the chosen folder's name.
///
/// WHY THIS FILE EXISTS (internal work item 611 / N08). `import_screen.dart` called
/// `dart:html`'s `FileUploadInputElement` directly. `dart:html` is gone from the
/// Dart 3.x toolchain this repo's container pins (Flutter 3.44.5), so the screen
/// stopped compiling — and because it is imported by three widget tests, the
/// WHOLE frontend suite failed to load:
///
///     import_screen.dart:338:24: Error: Method not found: 'FileUploadInputElement'.
///
/// That went unnoticed because the Flutter tests had never been run inside the
/// container; the acceptance run for internal work item 611 is what executed them for the first time.
///
/// ⚠️ A STRAIGHT SWAP TO `package:web` DOES NOT WORK, and that is the whole reason
/// for the conditional import below. `package:web` is built on `dart:js_interop`,
/// which is only usable when compiling to JS or wasm — these widget tests run on
/// the Dart **VM**, so importing it unconditionally moves the failure rather than
/// fixing it. The web implementation is therefore reached only when
/// `dart.library.js_interop` is available, and the stub answers everywhere else.
library;

export 'directory_picker_stub.dart'
    if (dart.library.js_interop) 'directory_picker_web.dart';
