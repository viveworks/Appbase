import 'package:freezed_annotation/freezed_annotation.dart';

part 'models.freezed.dart';
part 'models.g.dart';

/// Result of GET /api/configurations/import/scan.
///
/// Contains a list of [FileEntry] objects, one per discovered YAML file.
@freezed
abstract class ImportScanResult with _$ImportScanResult {
  /// Creates a scan result from the YAML files discovered by the backend.
  const factory ImportScanResult({required List<FileEntry> files}) =
      _ImportScanResult;

  /// Decodes an import scan response.
  factory ImportScanResult.fromJson(Map<String, dynamic> json) =>
      _$ImportScanResultFromJson(json);
}

/// A single YAML file discovered during scan.
@freezed
abstract class FileEntry with _$FileEntry {
  /// Creates a discovered YAML file and its ranked configuration candidates.
  const factory FileEntry({
    required String path,
    required String fileName,
    required List<TypeCandidate> candidates,
  }) = _FileEntry;

  /// Decodes a scanned-file entry.
  factory FileEntry.fromJson(Map<String, dynamic> json) =>
      _$FileEntryFromJson(json);
}

/// A candidate Appbase configuration class that may match a scanned YAML file.
@freezed
abstract class TypeCandidate with _$TypeCandidate {
  /// Creates a possible configuration type with its confidence and evidence.
  const factory TypeCandidate({
    required String fqcn,
    required String simpleName,
    required double confidence,
    required List<String> signals,
  }) = _TypeCandidate;

  /// Decodes a type candidate from a scan response.
  factory TypeCandidate.fromJson(Map<String, dynamic> json) =>
      _$TypeCandidateFromJson(json);
}

/// Result of POST /api/configurations/import/{fqcn}/write.
@freezed
abstract class ImportWriteResult with _$ImportWriteResult {
  /// Creates an import write outcome containing a path or an error message.
  const factory ImportWriteResult({String? path, String? errorMessage}) =
      _ImportWriteResult;

  /// Decodes an import write response.
  factory ImportWriteResult.fromJson(Map<String, dynamic> json) =>
      _$ImportWriteResultFromJson(json);
}
