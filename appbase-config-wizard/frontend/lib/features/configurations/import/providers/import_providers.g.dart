// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'import_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning
/// Singleton [ImportApiClient] disposed when the provider leaves the tree.

@ProviderFor(importApiClient)
final importApiClientProvider = ImportApiClientProvider._();

/// Singleton [ImportApiClient] disposed when the provider leaves the tree.

final class ImportApiClientProvider
    extends
        $FunctionalProvider<ImportApiClient, ImportApiClient, ImportApiClient>
    with $Provider<ImportApiClient> {
  /// Singleton [ImportApiClient] disposed when the provider leaves the tree.
  ImportApiClientProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'importApiClientProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$importApiClientHash();

  @$internal
  @override
  $ProviderElement<ImportApiClient> $createElement($ProviderPointer pointer) =>
      $ProviderElement(pointer);

  @override
  ImportApiClient create(Ref ref) {
    return importApiClient(ref);
  }

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(ImportApiClient value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<ImportApiClient>(value),
    );
  }
}

String _$importApiClientHash() => r'fdcae4a14b3b48cf5bac569ceadd0a309ac0f268';

/// Holds the result of the most recent import scan.
///
/// `null` means no scan has been run yet.
/// Transitions through [AsyncValue.loading] while the HTTP call is in flight.

@ProviderFor(ImportScanNotifier)
final importScanNotifierProvider = ImportScanNotifierProvider._();

/// Holds the result of the most recent import scan.
///
/// `null` means no scan has been run yet.
/// Transitions through [AsyncValue.loading] while the HTTP call is in flight.
final class ImportScanNotifierProvider
    extends
        $NotifierProvider<ImportScanNotifier, AsyncValue<ImportScanResult?>> {
  /// Holds the result of the most recent import scan.
  ///
  /// `null` means no scan has been run yet.
  /// Transitions through [AsyncValue.loading] while the HTTP call is in flight.
  ImportScanNotifierProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'importScanNotifierProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$importScanNotifierHash();

  @$internal
  @override
  ImportScanNotifier create() => ImportScanNotifier();

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(AsyncValue<ImportScanResult?> value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<AsyncValue<ImportScanResult?>>(
        value,
      ),
    );
  }
}

String _$importScanNotifierHash() =>
    r'5d04654667d8ee64c6c7a95dac439009723b84fd';

/// Holds the result of the most recent import scan.
///
/// `null` means no scan has been run yet.
/// Transitions through [AsyncValue.loading] while the HTTP call is in flight.

abstract class _$ImportScanNotifier
    extends $Notifier<AsyncValue<ImportScanResult?>> {
  AsyncValue<ImportScanResult?> build();
  @$mustCallSuper
  @override
  WhenComplete runBuild() {
    final ref =
        this.ref
            as $Ref<
              AsyncValue<ImportScanResult?>,
              AsyncValue<ImportScanResult?>
            >;
    final element =
        ref.element
            as $ClassProviderElement<
              AnyNotifier<
                AsyncValue<ImportScanResult?>,
                AsyncValue<ImportScanResult?>
              >,
              AsyncValue<ImportScanResult?>,
              Object?,
              Object?
            >;
    return element.handleCreate(ref, build);
  }
}
