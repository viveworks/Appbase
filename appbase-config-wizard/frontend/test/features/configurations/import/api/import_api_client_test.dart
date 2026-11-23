// Regression coverage for internal work item 1449/internal work item 1492 A10-05: ImportApiClient's HTTP calls
// had no deadline, so a hung connection left the caller's Future — and
// therefore any UI awaiting it — loading forever. Every request must fail
// deterministically after a bounded duration instead.

import 'dart:async';

import 'package:appbase_config_wizard/features/configurations/import/api/import_api_client.dart';
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
  group('A10-05: ImportApiClient bounds every HTTP call with a deadline', () {
    test('scan fails after the bounded timeout instead of hanging forever', () async {
      final client = ImportApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(client.scan('/configs'), throwsA(isA<TimeoutException>()));
    });

    test('write fails after the bounded timeout instead of hanging forever', () async {
      final client = ImportApiClient(
        client: _NeverRespondingClient(),
        timeout: const Duration(milliseconds: 50),
      );

      await expectLater(
        client.write('com.appbase.Foo', const {}),
        throwsA(isA<TimeoutException>()),
      );
    });
  });
}
