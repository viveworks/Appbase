import 'dart:html' as html;

/// Real browser DOM implementation, selected only when `dart:html` exists.
/// This is what a screen reader actually reads --
/// the surface APP2872-F1 says the prior widget-semantics test never
/// touched.
const bool isBrowserDomAvailable = true;

/// Finds the element by browser accessibility role and name, then activates
/// it through the DOM's native `click()` API. The F1 target holds a public
/// semantics handle while inspecting this ARIA tree.
bool activateDomElementWithRoleAndAccessibleName(
  String role,
  String accessibleName,
) {
  final candidates = html.document.querySelectorAll(
    '[role="$role"], button, input[type="button"]',
  );
  final matches = <html.Element>[];
  for (final element in candidates) {
    final explicitRole = element.getAttribute('role');
    if (explicitRole == role ||
        (explicitRole == null && _impliesRole(element, role))) {
      if (_accessibleName(element) == accessibleName) {
        matches.add(element);
      }
    }
  }
  if (matches.length != 1) return false;
  matches.single.click();
  return true;
}

String _accessibleName(html.Element element) {
  final ariaLabel = element.getAttribute('aria-label')?.trim();
  if (ariaLabel != null && ariaLabel.isNotEmpty) return ariaLabel;

  final labelledBy = element.getAttribute('aria-labelledby')?.trim();
  if (labelledBy != null && labelledBy.isNotEmpty) {
    final labels = labelledBy
        .split(RegExp(r'\s+'))
        .map((id) => html.document.getElementById(id)?.text?.trim() ?? '')
        .where((label) => label.isNotEmpty)
        .join(' ');
    if (labels.isNotEmpty) return labels;
  }

  return element.text?.trim() ?? '';
}

bool _impliesRole(html.Element element, String role) {
  if (role != 'button') return false;
  final tag = element.tagName.toLowerCase();
  return tag == 'button' ||
      (tag == 'input' && (element.getAttribute('type') ?? '') == 'button');
}

/// Dispatches a real browser history-back navigation -- the same
/// `popstate` path a user's Back button fires -- as distinct from calling
/// Flutter's `Navigator.pop()` directly in-app (APP2872-F2).
Future<void> dispatchBrowserHistoryBack() async {
  final popState = html.window.onPopState.first;
  html.window.history.back();
  await popState.timeout(const Duration(seconds: 5));
}
