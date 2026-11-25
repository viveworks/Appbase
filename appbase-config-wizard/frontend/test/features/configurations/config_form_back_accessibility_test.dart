// Regression coverage for internal work item 2828: the configuration wizard's in-app back
// arrow was exposed by Flutter Web as a button with an empty accessible
// name, so a screen-reader user could not identify the control and
// accessible-role selectors were ambiguous.

import 'dart:convert';

import 'package:appbase_config_wizard/features/configurations/api/configurations_api_client.dart';
import 'package:appbase_config_wizard/features/configurations/models/models.dart';
import 'package:appbase_config_wizard/features/configurations/providers/config_providers.dart';
import 'package:appbase_config_wizard/features/configurations/screens/config_form_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

/// Returns a canned `ConfigDetail` JSON response for every request,
/// matching the shape `ApiClient.getConfig` expects.
class _StubConfigDetailClient extends http.BaseClient {
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final body = jsonEncode(const ConfigDetail(
      fqcn: 'example.RedisConfiguration',
      fields: [],
    ).toJson());
    final bytes = utf8.encode(body);
    return http.StreamedResponse(
      Stream.value(bytes),
      200,
      headers: {'content-type': 'application/json'},
    );
  }
}

void main() {
  testWidgets(
    'test_AC2828_H1_back_button_semantic_tap_pops_configuration_form',
    (tester) async {
      const summary = ConfigSummary(
        fqcn: 'example.RedisConfiguration',
        simpleName: 'RedisConfiguration',
        layer: 'client',
        module: 'redis',
      );

      await tester.pumpWidget(ProviderScope(
        overrides: [
          apiClientProvider.overrideWith(
            (ref) => ApiClient(client: _StubConfigDetailClient()),
          ),
        ],
        child: MaterialApp(
          home: Navigator(
            onGenerateRoute: (settings) => MaterialPageRoute(
              builder: (context) => Scaffold(
                body: Center(
                  child: ElevatedButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) =>
                            const ConfigFormScreen(summary: summary),
                      ),
                    ),
                    child: const Text('open'),
                  ),
                ),
              ),
            ),
          ),
        ),
      ));
      await tester.pumpAndSettle();

      await tester.tap(find.text('open'));
      await tester.pumpAndSettle();

      expect(find.byType(ConfigFormScreen), findsOneWidget);

      final backButton = find.semantics.byLabel('Back to configurations');
      expect(backButton, findsOne,
          reason: 'exactly one semantics node must name the back button');

      final backButtonData = backButton.evaluate().single.getSemanticsData();
      expect(backButtonData.hasFlag(SemanticsFlag.isButton), isTrue);
      expect(backButtonData.hasAction(SemanticsAction.tap), isTrue,
          reason: 'assistive technology must be able to activate the button');

      tester.semantics.tap(backButton);
      await tester.pumpAndSettle();

      expect(find.byType(ConfigFormScreen), findsNothing);
      expect(find.text('open'), findsOneWidget);
    },
    semanticsEnabled: true,
  );
}
