// @author Navaneeth Mysore G
// @author Claude
/// Non-web implementation of the directory picker: there is no browser, so there
/// is nothing to open.
///
/// ⚠️ IT DOES NOTHING, DELIBERATELY, AND MUST NOT THROW. This file is what the
/// widget tests compile against (they run on the Dart VM), and it is also what a
/// desktop or mobile build would get. `_handleBrowse` is a CONVENIENCE that
/// pre-fills a text field the operator can type into by hand; failing loudly here
/// would turn "this platform has no directory picker" into a crash on a path the
/// user can still complete manually.
library;

/// Opens a directory picker and calls [onDirectoryName] with the selected
/// folder's name. A no-op off the web.
void pickDirectoryName(void Function(String name) onDirectoryName) {
  // No browser, no picker. The caller's text field stays as the operator left it.
}
