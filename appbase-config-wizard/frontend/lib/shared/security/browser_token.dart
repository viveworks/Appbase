/// Reads the per-run browser token the wizard server plants as a same-site cookie
/// (A10-01, internal work item 1449). The wizard UI echoes it back in the `X-Appbase-Wizard-Token`
/// header on every `/api` call so the server can tell a genuine same-origin request
/// from a DNS-rebinding attacker page that cannot read the cookie.
///
/// This is the default (non-web) implementation used on the Dart VM in unit tests,
/// where there is no cookie store; it returns null. The web build swaps in
/// `browser_token_web.dart` via a conditional import.
///
/// @author Navaneeth Mysore G
String? readBrowserToken() => null;
