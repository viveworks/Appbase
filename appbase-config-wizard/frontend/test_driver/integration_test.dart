// Bridges `flutter drive` to the integration-test suite. The F1 target enables
// web semantics in app code through WidgetTester.ensureSemantics().
import 'package:integration_test/integration_test_driver.dart';

Future<void> main() => integrationDriver();
