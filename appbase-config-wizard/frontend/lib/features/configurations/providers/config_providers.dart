import 'dart:async';

import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../api/configurations_api_client.dart';
import '../models/models.dart';
part 'config_providers.g.dart';

/// Singleton [ApiClient] shared across all providers.
///
/// Disposes the underlying [http.Client] when the provider is removed from
/// the widget tree, preventing socket resource leaks.
@riverpod
ApiClient apiClient(Ref ref) {
  final client = ApiClient();
  ref.onDispose(client.dispose);
  return client;
}

/// Loads the full list of configuration classes from the API.
@riverpod
Future<List<ConfigSummary>> configList(Ref ref) {
  return ref.watch(apiClientProvider).listConfigs();
}

/// AsyncNotifier that manages the currently selected [ConfigDetail].
///
/// Call [ConfigDetailNotifier.load] with the FQCN of the chosen class to fetch
/// its fields and defaults.
@Riverpod(name: 'configDetailNotifierProvider')
class ConfigDetailNotifier extends _$ConfigDetailNotifier {
  @override
  FutureOr<ConfigDetail?> build() async => null;

  /// Fetches and caches the detail for [fqcn].
  Future<void> load(String fqcn) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard<ConfigDetail?>(
        () => ref.read(apiClientProvider).getConfig(fqcn));
  }
}

/// AsyncNotifier that triggers a YAML build and caches the [BuildResult].
@Riverpod(name: 'buildResultNotifierProvider')
class BuildResultNotifier extends _$BuildResultNotifier {
  @override
  FutureOr<BuildResult?> build() async => null;

  /// Calls POST /api/configurations/{fqcn}/build with the supplied [values].
  ///
  /// Returns the [BuildResult] directly so callers do not have to re-read
  /// provider state (which would be null if the request threw an error).
  Future<BuildResult?> buildConfig(String fqcn, Map<String, dynamic> values) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard<BuildResult?>(
        () => ref.read(apiClientProvider).buildConfig(fqcn, values));
    return state.value;
  }
}

/// AsyncNotifier that triggers a write-to-file action and caches the [WriteResult].
@Riverpod(name: 'writeResultNotifierProvider')
class WriteResultNotifier extends _$WriteResultNotifier {
  @override
  FutureOr<WriteResult?> build() async => null;

  /// Calls POST /api/configurations/{fqcn}/write with the supplied [values] and optional [outputPath].
  Future<void> writeConfig(String fqcn, Map<String, dynamic> values,
      {String? outputPath}) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard<WriteResult?>(
        () => ref.read(apiClientProvider)
            .writeConfig(fqcn, values, outputPath: outputPath));
  }
}
