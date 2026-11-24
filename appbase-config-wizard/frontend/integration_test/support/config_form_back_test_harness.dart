import 'dart:convert';

import 'package:appbase_config_wizard/features/configurations/api/configurations_api_client.dart';
import 'package:appbase_config_wizard/features/configurations/models/models.dart';
import 'package:appbase_config_wizard/features/configurations/providers/config_providers.dart';
import 'package:appbase_config_wizard/features/configurations/screens/config_form_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

const backButtonAccessibleName = 'Back to configurations';

class _StubConfigDetailClient extends http.BaseClient {
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final body = jsonEncode(const ConfigDetail(
      fqcn: 'example.RedisConfiguration',
      fields: [],
    ).toJson());
    return http.StreamedResponse(
      Stream.value(utf8.encode(body)),
      200,
      headers: {'content-type': 'application/json'},
    );
  }
}

const _summary = ConfigSummary(
  fqcn: 'example.RedisConfiguration',
  simpleName: 'RedisConfiguration',
  layer: 'client',
  module: 'redis',
);

Future<void> openConfigForm(WidgetTester tester) async {
  final navigatorKey = GlobalKey<NavigatorState>();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      apiClientProvider.overrideWith(
        (ref) => ApiClient(client: _StubConfigDetailClient()),
      ),
    ],
    child: MaterialApp(
      home: NavigatorPopHandler<void>(
        onPopWithResult: (_) => navigatorKey.currentState?.pop(),
        child: Navigator(
          key: navigatorKey,
          onGenerateRoute: (settings) => MaterialPageRoute(
            builder: (context) => Scaffold(
              body: Center(
                child: ElevatedButton(
                  onPressed: () => navigatorKey.currentState?.push(
                    MaterialPageRoute(
                      builder: (_) =>
                          const ConfigFormScreen(summary: _summary),
                    ),
                  ),
                  child: const Text('open'),
                ),
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
}

void expectConfigFormPopped() {
  expect(find.byType(ConfigFormScreen), findsNothing);
  expect(find.text('open'), findsOneWidget);
}
