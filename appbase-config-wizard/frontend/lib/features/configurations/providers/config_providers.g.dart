// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'config_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning
/// Singleton [ApiClient] shared across all providers.
///
/// Disposes the underlying [http.Client] when the provider is removed from
/// the widget tree, preventing socket resource leaks.

@ProviderFor(apiClient)
final apiClientProvider = ApiClientProvider._();

/// Singleton [ApiClient] shared across all providers.
///
/// Disposes the underlying [http.Client] when the provider is removed from
/// the widget tree, preventing socket resource leaks.

final class ApiClientProvider
    extends $FunctionalProvider<ApiClient, ApiClient, ApiClient>
    with $Provider<ApiClient> {
  /// Singleton [ApiClient] shared across all providers.
  ///
  /// Disposes the underlying [http.Client] when the provider is removed from
  /// the widget tree, preventing socket resource leaks.
  ApiClientProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'apiClientProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$apiClientHash();

  @$internal
  @override
  $ProviderElement<ApiClient> $createElement($ProviderPointer pointer) =>
      $ProviderElement(pointer);

  @override
  ApiClient create(Ref ref) {
    return apiClient(ref);
  }

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(ApiClient value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<ApiClient>(value),
    );
  }
}

String _$apiClientHash() => r'8968b7981f1d295f65b537530402fae01cc4027d';

/// Loads the full list of configuration classes from the API.

@ProviderFor(configList)
final configListProvider = ConfigListProvider._();

/// Loads the full list of configuration classes from the API.

final class ConfigListProvider
    extends
        $FunctionalProvider<
          AsyncValue<List<ConfigSummary>>,
          List<ConfigSummary>,
          FutureOr<List<ConfigSummary>>
        >
    with
        $FutureModifier<List<ConfigSummary>>,
        $FutureProvider<List<ConfigSummary>> {
  /// Loads the full list of configuration classes from the API.
  ConfigListProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'configListProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$configListHash();

  @$internal
  @override
  $FutureProviderElement<List<ConfigSummary>> $createElement(
    $ProviderPointer pointer,
  ) => $FutureProviderElement(pointer);

  @override
  FutureOr<List<ConfigSummary>> create(Ref ref) {
    return configList(ref);
  }
}

String _$configListHash() => r'ffca440f80a912515ec80c0affd01a0a505ee84c';

/// AsyncNotifier that manages the currently selected [ConfigDetail].
///
/// Call [ConfigDetailNotifier.load] with the FQCN of the chosen class to fetch
/// its fields and defaults.

@ProviderFor(ConfigDetailNotifier)
final configDetailNotifierProvider = ConfigDetailNotifierProvider._();

/// AsyncNotifier that manages the currently selected [ConfigDetail].
///
/// Call [ConfigDetailNotifier.load] with the FQCN of the chosen class to fetch
/// its fields and defaults.
final class ConfigDetailNotifierProvider
    extends $AsyncNotifierProvider<ConfigDetailNotifier, ConfigDetail?> {
  /// AsyncNotifier that manages the currently selected [ConfigDetail].
  ///
  /// Call [ConfigDetailNotifier.load] with the FQCN of the chosen class to fetch
  /// its fields and defaults.
  ConfigDetailNotifierProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'configDetailNotifierProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$configDetailNotifierHash();

  @$internal
  @override
  ConfigDetailNotifier create() => ConfigDetailNotifier();
}

String _$configDetailNotifierHash() =>
    r'7953567333606d933701db9fbe03db8ff2eaa1b2';

/// AsyncNotifier that manages the currently selected [ConfigDetail].
///
/// Call [ConfigDetailNotifier.load] with the FQCN of the chosen class to fetch
/// its fields and defaults.

abstract class _$ConfigDetailNotifier extends $AsyncNotifier<ConfigDetail?> {
  FutureOr<ConfigDetail?> build();
  @$mustCallSuper
  @override
  WhenComplete runBuild() {
    final ref = this.ref as $Ref<AsyncValue<ConfigDetail?>, ConfigDetail?>;
    final element =
        ref.element
            as $ClassProviderElement<
              AnyNotifier<AsyncValue<ConfigDetail?>, ConfigDetail?>,
              AsyncValue<ConfigDetail?>,
              Object?,
              Object?
            >;
    return element.handleCreate(ref, build);
  }
}

/// AsyncNotifier that triggers a YAML build and caches the [BuildResult].

@ProviderFor(BuildResultNotifier)
final buildResultNotifierProvider = BuildResultNotifierProvider._();

/// AsyncNotifier that triggers a YAML build and caches the [BuildResult].
final class BuildResultNotifierProvider
    extends $AsyncNotifierProvider<BuildResultNotifier, BuildResult?> {
  /// AsyncNotifier that triggers a YAML build and caches the [BuildResult].
  BuildResultNotifierProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'buildResultNotifierProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$buildResultNotifierHash();

  @$internal
  @override
  BuildResultNotifier create() => BuildResultNotifier();
}

String _$buildResultNotifierHash() =>
    r'25f1275d186eef8dd0800b4d4bb13e02fa6cddf0';

/// AsyncNotifier that triggers a YAML build and caches the [BuildResult].

abstract class _$BuildResultNotifier extends $AsyncNotifier<BuildResult?> {
  FutureOr<BuildResult?> build();
  @$mustCallSuper
  @override
  WhenComplete runBuild() {
    final ref = this.ref as $Ref<AsyncValue<BuildResult?>, BuildResult?>;
    final element =
        ref.element
            as $ClassProviderElement<
              AnyNotifier<AsyncValue<BuildResult?>, BuildResult?>,
              AsyncValue<BuildResult?>,
              Object?,
              Object?
            >;
    return element.handleCreate(ref, build);
  }
}

/// AsyncNotifier that triggers a write-to-file action and caches the [WriteResult].

@ProviderFor(WriteResultNotifier)
final writeResultNotifierProvider = WriteResultNotifierProvider._();

/// AsyncNotifier that triggers a write-to-file action and caches the [WriteResult].
final class WriteResultNotifierProvider
    extends $AsyncNotifierProvider<WriteResultNotifier, WriteResult?> {
  /// AsyncNotifier that triggers a write-to-file action and caches the [WriteResult].
  WriteResultNotifierProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'writeResultNotifierProvider',
        isAutoDispose: true,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$writeResultNotifierHash();

  @$internal
  @override
  WriteResultNotifier create() => WriteResultNotifier();
}

String _$writeResultNotifierHash() =>
    r'8b397fcf5a8340971f3cab1c77d4b677259e2aa9';

/// AsyncNotifier that triggers a write-to-file action and caches the [WriteResult].

abstract class _$WriteResultNotifier extends $AsyncNotifier<WriteResult?> {
  FutureOr<WriteResult?> build();
  @$mustCallSuper
  @override
  WhenComplete runBuild() {
    final ref = this.ref as $Ref<AsyncValue<WriteResult?>, WriteResult?>;
    final element =
        ref.element
            as $ClassProviderElement<
              AnyNotifier<AsyncValue<WriteResult?>, WriteResult?>,
              AsyncValue<WriteResult?>,
              Object?,
              Object?
            >;
    return element.handleCreate(ref, build);
  }
}
