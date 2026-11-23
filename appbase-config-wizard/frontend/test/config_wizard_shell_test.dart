import 'package:appbase_config_wizard/shared/widgets/config_wizard_shell.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('ConfigWizardShell shows tab bar with Configurations tab', (tester) async {
    await tester.pumpWidget(const ProviderScope(
      child: MaterialApp(home: ConfigWizardShell()),
    ));
    expect(find.text('Configurations'), findsOneWidget);
  });

  testWidgets('ConfigWizardShell header is always visible', (tester) async {
    await tester.pumpWidget(const ProviderScope(
      child: MaterialApp(home: ConfigWizardShell()),
    ));
    expect(find.text('Appbase Config Wizard'), findsOneWidget);
  });

  testWidgets('system Back pops the active nested configuration route',
      (tester) async {
    await tester.pumpWidget(const ProviderScope(
      child: MaterialApp(home: ConfigWizardShell()),
    ));

    await tester.tap(find.byTooltip('Import YAML configurations'));
    await tester.pumpAndSettle();
    expect(find.text('Import YAML Configurations'), findsOneWidget);

    expect(await tester.binding.handlePopRoute(), isTrue);
    await tester.pumpAndSettle();

    expect(find.text('Import YAML Configurations'), findsNothing);
    expect(find.text('Configurations'), findsOneWidget);
  });
}
