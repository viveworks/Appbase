import 'package:appbase_config_wizard/features/configurations/models/models.dart';
import 'package:appbase_config_wizard/features/configurations/providers/config_providers.dart';
import 'package:appbase_config_wizard/features/configurations/screens/config_list_screen.dart';
import 'package:appbase_config_wizard/features/configurations/storage/configuration_filter_storage.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('configuration filter survives screen recreation', (tester) async {
    writeConfigurationFilter('');
    addTearDown(() => writeConfigurationFilter(''));

    Future<void> pumpList() => tester.pumpWidget(ProviderScope(
          overrides: [
            configListProvider.overrideWith((ref) async => const [
                  ConfigSummary(
                    fqcn: 'example.RedisConfiguration',
                    simpleName: 'RedisConfiguration',
                    layer: 'client',
                    module: 'redis',
                  ),
                ]),
          ],
          child: const MaterialApp(home: Scaffold(body: ConfigListScreen())),
        ));

    await pumpList();
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'redis');
    await tester.pump();

    await tester.pumpWidget(const SizedBox.shrink());
    await pumpList();
    await tester.pumpAndSettle();

    // Reproduce the browser ordering: the editing connection was opened while
    // semantics was disabled, then CanvasKit created its semantic text field.
    // Emptying only the controller leaves the app's restored filter intact.
    var restoredField = tester.widget<TextField>(find.byType(TextField));
    restoredField.controller!.clear();
    final semantics = tester.ensureSemantics();
    addTearDown(semantics.dispose);

    expect(
      restoredField.focusNode!.hasFocus,
      isFalse,
      reason: 'enabling semantics must disconnect the pre-semantics focus',
    );
    await tester.pump();
    restoredField = tester.widget<TextField>(find.byType(TextField));
    expect(restoredField.focusNode!.hasFocus, isTrue);
    expect(
      restoredField.controller!.text,
      isEmpty,
      reason: 'the complete editing value is published after focus reconnects',
    );
    await tester.pump();

    // VM-level contract: the post-focus frame republishes the complete editing
    // value from app state and the semantic field observes it.
    restoredField = tester.widget<TextField>(find.byType(TextField));
    expect(
      restoredField.controller!.value,
      const TextEditingValue(
        text: 'redis',
        selection: TextSelection.collapsed(offset: 5),
      ),
    );
    expect(
      tester.getSemantics(find.byType(TextField)).attributedValue.string,
      'redis',
    );
    expect(
      restoredField.focusNode!.hasFocus,
      isTrue,
      reason: 'a restored web filter must activate the text-input proxy',
    );
    expect(find.text('RedisConfiguration'), findsOneWidget);
  });
}
