import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'support/browser_accessibility_probe_web.dart';
import 'support/config_form_back_test_harness.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'APP2872-F2-REAL: browser history Back pops the configuration form',
    (tester) async {
      expect(isBrowserDomAvailable, isTrue);
      await openConfigForm(tester);

      await dispatchBrowserHistoryBack();
      await tester.pumpAndSettle();

      expectConfigFormPopped();
    },
  );
}
