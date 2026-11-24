import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'support/browser_accessibility_probe_web.dart';
import 'support/config_form_back_test_harness.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'APP2872-F1-REAL: browser role and name expose an activatable back button',
    (tester) async {
      expect(isBrowserDomAvailable, isTrue);
      final semanticsHandle = tester.ensureSemantics();
      try {
        await openConfigForm(tester);

        expect(
          activateDomElementWithRoleAndAccessibleName(
            'button',
            backButtonAccessibleName,
          ),
          isTrue,
          reason: 'tester.ensureSemantics() must expose one browser '
              'button with the expected accessible name, and that semantic '
              'element must support native browser activation.',
        );
        await tester.pumpAndSettle();

        expectConfigFormPopped();
      } finally {
        semanticsHandle.dispose();
      }
    },
  );
}
