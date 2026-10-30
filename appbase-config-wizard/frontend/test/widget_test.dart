import 'package:appbase_config_wizard/app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('ConfigWizardApp renders without errors',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: ConfigWizardApp()),
    );

    // Verify the root MaterialApp widget is present
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
