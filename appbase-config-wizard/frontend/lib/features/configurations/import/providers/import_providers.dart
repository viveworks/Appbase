import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../api/import_api_client.dart';
import '../models/models.dart';

part 'import_providers.g.dart';

/// Singleton [ImportApiClient] disposed when the provider leaves the tree.
@riverpod
ImportApiClient importApiClient(Ref ref) {
  final client = ImportApiClient();
  ref.onDispose(client.dispose);
  return client;
}

/// Holds the result of the most recent import scan.
///
/// `null` means no scan has been run yet.
/// Transitions through [AsyncValue.loading] while the HTTP call is in flight.
@Riverpod(name: 'importScanNotifierProvider')
class ImportScanNotifier extends _$ImportScanNotifier {
  @override
  AsyncValue<ImportScanResult?> build() => const AsyncValue.data(null);

  /// Monotonic request token (A10-04, internal work item 1449/internal work item 1492): this is a "last
  /// completion wins" race without it — scanning a second directory before
  /// the first request returns starts two overlapping HTTP calls, and
  /// whichever happens to arrive LAST (not whichever was requested last)
  /// would win, silently showing a stale directory's files for the
  /// currently-displayed path. A token discards any completion that is not
  /// the one from the most recently REQUESTED call.
  int _token = 0;

  /// Calls GET /api/configurations/import/scan?path=[dir].
  Future<void> scan(String dir) async {
    final token = ++_token;
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard<ImportScanResult?>(
        () => ref.read(importApiClientProvider).scan(dir));
    if (token != _token) return;
    state = result;
  }
}
