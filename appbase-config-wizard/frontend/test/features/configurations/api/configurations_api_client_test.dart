// Regression coverage for internal work item 1449/internal work item 1492 A10-05: ApiClient's HTTP calls had no
// deadline, so a hung connection (proxy stall, dead backend, network
// partition) left the caller's Future — and therefore any UI awaiting it —
// loading forever. Every request must fail deterministically after a bounded
// duration instead.

import 'dart:async';

import 'package:appbase_config_wizard/features/configurations/api/configurations_api_client.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

/// An [http.Client] whose every request hangs forever — simulating a stalled
/// connection (proxy timeout, dead backend) rather than a fast failure.
class _NeverRespondingClient extends http.BaseClient {
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) {
    return Completer<http.StreamedResponse>().future; // never completes
  }
}

void main() {
  group('A10-05: ApiClient bounds every HTTP call with a deadline', () {
    test('getConfig fails after the bounded timeout instead of hanging forever', () async {
      final client = ApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(
        client.getConfig('com.appbase.Foo'),
        throwsA(isA<TimeoutException>()),
      );
    });

    test('listConfigs fails after the bounded timeout instead of hanging forever', () async {
      final client = ApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(client.listConfigs(), throwsA(isA<TimeoutException>()));
    });

    test('writeConfig fails after the bounded timeout instead of hanging forever', () async {
      final client = ApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(
        client.writeConfig('com.appbase.Foo', const {}),
        throwsA(isA<TimeoutException>()),
      );
    });

    test('buildConfig fails after the bounded timeout instead of hanging forever', () async {
      final client = ApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(
        client.buildConfig('com.appbase.Foo', const {}),
        throwsA(isA<TimeoutException>()),
      );
    });

    test('getDefaultValues fails after the bounded timeout instead of hanging forever', () async {
      final client = ApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(
        client.getDefaultValues('com.appbase.Foo'),
        throwsA(isA<TimeoutException>()),
      );
    });
  });
}
