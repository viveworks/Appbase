import 'package:appbase_config_wizard/features/configurations/import/screens/import_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('ImportScreen shows directory input and scan button',
      (tester) async {
    await tester.pumpWidget(const ProviderScope(
        child: MaterialApp(home: ImportScreen())));
    await tester.pump();
    expect(find.byType(TextField), findsOneWidget,
        reason: 'Directory path input');
    expect(find.text('Scan'), findsOneWidget);
  });

  testWidgets('ImportScreen shows files panel and editor panel layout',
      (tester) async {
    await tester.pumpWidget(const ProviderScope(
        child: MaterialApp(home: ImportScreen())));
    await tester.pump();
    expect(find.text('FILES'), findsOneWidget);
    expect(find.text('EDITOR'), findsOneWidget);
  });
}
