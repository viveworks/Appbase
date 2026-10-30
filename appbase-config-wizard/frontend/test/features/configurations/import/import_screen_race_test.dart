// Regression coverage for internal work item 1449/internal work item 1492 A10-03: an out-of-order async
// completion race in the Import screen's config-type loader. A user who
// switches the selected config type faster than the network can respond
// must always end up seeing the fields for their LATEST choice, never a
// stale, later-arriving response for an earlier one.

import 'dart:async';

import 'package:appbase_config_wizard/features/configurations/api/configurations_api_client.dart';
import 'package:appbase_config_wizard/features/configurations/import/api/import_api_client.dart';
import 'package:appbase_config_wizard/features/configurations/import/models/models.dart'
    as import_models;
import 'package:appbase_config_wizard/features/configurations/import/providers/import_providers.dart';
import 'package:appbase_config_wizard/features/configurations/import/screens/import_screen.dart';
import 'package:appbase_config_wizard/features/configurations/models/models.dart';
import 'package:appbase_config_wizard/features/configurations/providers/config_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// An [ApiClient] whose `getConfig` is gated per-fqcn by a caller-supplied
/// [Completer], so a test can resolve two in-flight requests in a chosen
/// order — reproducing an out-of-order network completion deterministically.
class _GatedApiClient extends ApiClient {
  final Map<String, Completer<ConfigDetail>> gates;
  final List<String> requested = [];

  _GatedApiClient(this.gates);

  @override
  Future<ConfigDetail> getConfig(String fqcn) {
    requested.add(fqcn);
    return gates[fqcn]!.future;
  }
}

/// An [ImportApiClient] whose `scan` resolves immediately with a fixed
/// result — no gating needed for A10-03, which only cares about the
/// config-type load, not the scan itself.
class _FixedScanApiClient extends ImportApiClient {
  final import_models.ImportScanResult result;
  _FixedScanApiClient(this.result);

  @override
  Future<import_models.ImportScanResult> scan(String dir) async => result;
}

const _fileEntry = import_models.FileEntry(
  path: '/configs/service.yaml',
  fileName: 'service.yaml',
  candidates: [
    import_models.TypeCandidate(
      fqcn: 'com.appbase.TypeA',
      simpleName: 'TypeA',
      confidence: 0.9,
      signals: [],
    ),
    import_models.TypeCandidate(
      fqcn: 'com.appbase.TypeB',
      simpleName: 'TypeB',
      confidence: 0.5,
      signals: [],
    ),
  ],
);

Future<void> _settle(WidgetTester tester) async {
  for (var i = 0; i < 10; i++) {
    await tester.pump(const Duration(milliseconds: 20));
  }
}

void main() {
  testWidgets(
      'A10-03: switching config type while a load is in flight discards the stale, later-arriving completion',
      (tester) async {
    final gateA = Completer<ConfigDetail>();
    final gateB = Completer<ConfigDetail>();
    final apiClient = _GatedApiClient({
      'com.appbase.TypeA': gateA,
      'com.appbase.TypeB': gateB,
    });
    final importClient = _FixedScanApiClient(
      const import_models.ImportScanResult(files: [_fileEntry]),
    );

    await tester.pumpWidget(ProviderScope(
      overrides: [
        apiClientProvider.overrideWithValue(apiClient),
        importApiClientProvider.overrideWithValue(importClient),
      ],
      child: const MaterialApp(home: ImportScreen()),
    ));
    await tester.pump();

    // Scan — resolves immediately (ungated), surfacing the one file.
    await tester.enterText(find.byType(TextField).first, '/configs');
    await tester.tap(find.text('Scan'));
    await _settle(tester);

    // Select the file — auto-loads its first candidate, TypeA (gated on gateA).
    await tester.tap(find.text('service.yaml'));
    await _settle(tester);
    expect(apiClient.requested, ['com.appbase.TypeA']);
    expect(find.byType(CircularProgressIndicator), findsOneWidget,
        reason: 'TypeA load is still in flight');

    // Switch to TypeB via the dropdown BEFORE TypeA resolves — starts a
    // second, gated load for TypeB.
    await tester.tap(find.byType(DropdownButtonFormField<import_models.TypeCandidate>));
    await _settle(tester);
    await tester.tap(find.text('TypeB (50%)').last);
    await _settle(tester);
    expect(apiClient.requested, ['com.appbase.TypeA', 'com.appbase.TypeB']);

    // Resolve OUT OF ORDER: TypeB (the current, latest selection) first...
    gateB.complete(_detail('com.appbase.TypeB', 'fieldB'));
    await _settle(tester);
    expect(find.text('fieldB'), findsOneWidget);

    // ...then TypeA's stale response arrives LATE. It must be discarded —
    // the screen must still show TypeB's type and TypeB's field, never
    // TypeA's field silently replacing it underneath the TypeB label.
    gateA.complete(_detail('com.appbase.TypeA', 'fieldA'));
    await _settle(tester);

    expect(find.text('fieldB'), findsOneWidget,
        reason: 'the stale TypeA completion must not overwrite the current TypeB fields');
    expect(find.text('fieldA'), findsNothing,
        reason: 'TypeA arrived late and must be discarded, not shown');
  });

  // A "does a late getConfig response crash after ImportScreen is unmounted"
  // test was attempted here (per review) and deliberately NOT kept: removing
  // the `!mounted` half of the guard in _selectType produced no observable
  // exception in this harness (flutter_riverpod's ConsumerState appears to
  // already guard setState internally after disposal) — the test could not
  // be made to fail under that mutation, so it would have been a vacuous,
  // always-green assertion. Per this repo's mutation-verification standard,
  // a test that cannot fail is not shipped. The `!mounted` check itself is
  // retained as cheap, correct defensive code even though this harness can't
  // independently prove it load-bearing.
}

ConfigDetail _detail(String fqcn, String onlyFieldName) => ConfigDetail(
      fqcn: fqcn,
      fields: [FieldSummary(name: onlyFieldName, typeName: 'String')],
    );
