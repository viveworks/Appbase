// Regression coverage for internal work item 1449/internal work item 1492 A10-04: ImportScanNotifier's scan()
// races on network completion order, not request order. A user who scans a
// second, different directory before the first scan's response arrives must
// end up seeing the SECOND (latest-requested) directory's result, even if
// the FIRST request's response happens to arrive later.

import 'dart:async';

import 'package:appbase_config_wizard/features/configurations/import/api/import_api_client.dart';
import 'package:appbase_config_wizard/features/configurations/import/models/models.dart';
import 'package:appbase_config_wizard/features/configurations/import/providers/import_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// An [ImportApiClient] whose `scan` is gated per-directory by a caller
/// supplied [Completer], so a test can resolve two in-flight scans in a
/// chosen order — reproducing an out-of-order network completion
/// deterministically.
class _GatedImportApiClient extends ImportApiClient {
  final Map<String, Completer<ImportScanResult>> gates;
  final List<String> requested = [];

  _GatedImportApiClient(this.gates);

  @override
  Future<ImportScanResult> scan(String dir) {
    requested.add(dir);
    return gates[dir]!.future;
  }
}

ImportScanResult _result(String tag) => ImportScanResult(files: [
      FileEntry(path: '/$tag.yaml', fileName: '$tag.yaml', candidates: const []),
    ]);

void main() {
  test(
      'A10-04: scanning a second directory before the first resolves keeps the LATEST request\'s result, even if it arrives first',
      () async {
    final gateA = Completer<ImportScanResult>();
    final gateB = Completer<ImportScanResult>();
    final client = _GatedImportApiClient({'/dirA': gateA, '/dirB': gateB});

    final container = ProviderContainer(overrides: [
      importApiClientProvider.overrideWithValue(client),
    ]);
    addTearDown(container.dispose);

    final notifier = container.read(importScanNotifierProvider.notifier);

    // Fire both scans without awaiting the first — the real UI reachable
    // shape: the operator changes the directory and clicks Scan again
    // before the first request has returned.
    final scanA = notifier.scan('/dirA');
    final scanB = notifier.scan('/dirB');

    // Resolve OUT OF ORDER: dirB (the later request) completes first...
    gateB.complete(_result('dirB'));
    await scanB;
    expect(
      container.read(importScanNotifierProvider).value?.files.single.fileName,
      'dirB.yaml',
    );

    // ...then dirA's stale response arrives LATE. It must be discarded — the
    // notifier must still report dirB's result, never silently revert to
    // dirA's stale (and, from the operator's perspective, wrong) files.
    gateA.complete(_result('dirA'));
    await scanA;

    expect(
      container.read(importScanNotifierProvider).value?.files.single.fileName,
      'dirB.yaml',
      reason: 'the stale dirA completion must not overwrite the current dirB result',
    );
  });

  test('A10-04: repeated scans of the SAME directory settle on the latest call, not a stale earlier one',
      () async {
    final gate1 = Completer<ImportScanResult>();
    final gate2 = Completer<ImportScanResult>();
    var call = 0;
    // Two calls to the same dir need two distinct gates keyed by call order,
    // not by dir string (both calls target the same dir) — override scan
    // directly via a small local subclass instance instead of the shared
    // gates map keyed by dir.
    final repeatedClient = _RepeatedGateClient(() => (++call == 1) ? gate1.future : gate2.future);

    final container = ProviderContainer(overrides: [
      importApiClientProvider.overrideWithValue(repeatedClient),
    ]);
    addTearDown(container.dispose);
    final notifier = container.read(importScanNotifierProvider.notifier);

    final first = notifier.scan('/same-dir');
    final second = notifier.scan('/same-dir');

    // The SECOND call's response arrives first...
    gate2.complete(_result('second'));
    await second;
    expect(
      container.read(importScanNotifierProvider).value?.files.single.fileName,
      'second.yaml',
    );

    // ...then the first call's stale response arrives late and must be discarded.
    gate1.complete(_result('first'));
    await first;

    expect(
      container.read(importScanNotifierProvider).value?.files.single.fileName,
      'second.yaml',
      reason: 'a repeated click on the same directory must not let the earlier click\'s stale response win',
    );
  });
}

class _RepeatedGateClient extends ImportApiClient {
  final Future<ImportScanResult> Function() nextGate;
  _RepeatedGateClient(this.nextGate);

  @override
  Future<ImportScanResult> scan(String dir) => nextGate();
}
