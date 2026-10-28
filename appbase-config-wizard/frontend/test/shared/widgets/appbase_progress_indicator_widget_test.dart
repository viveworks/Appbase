import 'package:appbase_config_wizard/shared/widgets/appbase_progress_indicator_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('local progress indicator is centered and uses the primary color',
      (WidgetTester tester) async {
    const primary = Colors.deepPurple;
    await tester.pumpWidget(MaterialApp(
      theme: ThemeData(colorScheme: ColorScheme.light(primary: primary)),
      home: AppbaseProgressIndicatorWidget(),
    ));

    expect(find.byType(Center), findsOneWidget);
    final indicator = tester.widget<CircularProgressIndicator>(
      find.byType(CircularProgressIndicator),
    );
    expect(indicator.backgroundColor, primary);
  });
}
