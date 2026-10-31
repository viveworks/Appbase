// @author Navaneeth Mysore G
// @author Claude
/// Web implementation of the directory picker, on `package:web` rather than the
/// removed `dart:html`.
///
/// ⚠️ REACHED ONLY VIA THE CONDITIONAL EXPORT in `directory_picker.dart`. Importing
/// this file directly would drag `dart:js_interop` into VM builds and break the
/// widget tests all over again — which is the defect this replaced.
library;

import 'dart:js_interop';

import 'package:web/web.dart' as web;

/// Opens the browser's directory picker (`webkitdirectory`) and reports the first
/// path segment of the selection — the folder name.
///
/// ⚠️ `webkitRelativePath` IS THE POINT. A browser never hands out a real
/// filesystem path, so the picker cannot fill in the directory the backend needs.
/// What it can give is `"dirname/subdir/file.yaml"`, whose first segment names the
/// folder the operator chose; that is placed in the field as a HINT and the real
/// path is still typed. Preserved from the `dart:html` version verbatim, because it
/// is the behaviour the screen's comment documents.
void pickDirectoryName(void Function(String name) onDirectoryName) {
  final input = web.HTMLInputElement()
    ..type = 'file'
    ..accept = '.yaml,.yml'
    ..setAttribute('webkitdirectory', '');

  // ⚠️ addEventListener, NOT `.onChange.listen`. `package:web` exposes DOM events
  // through js_interop callbacks; the Stream-based API was a `dart:html` affordance
  // and does not exist here.
  input.addEventListener(
    'change',
    ((web.Event _) {
      final files = input.files;
      if (files == null || files.length == 0) return;
      final relativePath = files.item(0)?.webkitRelativePath ?? '';
      final parts = relativePath.split('/');
      if (parts.length > 1) onDirectoryName(parts[0]);
    }).toJS,
  );

  input.click();
}
