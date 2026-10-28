// Freezed applies JsonKey to generated getter targets that are not visible in
// this source file; the generator consumes the annotation correctly.
// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';

part 'models.freezed.dart';
part 'models.g.dart';

/// Summary of a configuration class returned by GET /api/configurations.
@freezed
abstract class ConfigSummary with _$ConfigSummary {
  /// Creates the identity and catalogue placement shown in the configuration list.
  const factory ConfigSummary({
    required String fqcn,
    required String simpleName,
    required String layer,
    required String module,
  }) = _ConfigSummary;

  /// Decodes a configuration summary returned by the service.
  factory ConfigSummary.fromJson(Map<String, dynamic> json) =>
      _$ConfigSummaryFromJson(json);
}

/// A single field within a configuration class.
@freezed
abstract class FieldSummary with _$FieldSummary {
  /// Creates field metadata used to select and seed the matching form control.
  const factory FieldSummary({
    required String name,
    required String typeName,
    @JsonKey(name: 'required') bool? isRequired,
    String? defaultValue,
    /// Non-null when this field holds a nested Appbase config object.
    /// The value is the FQCN used to load sub-form fields via GET /api/configurations/{fqcn}.
    String? nestedTypeFqcn,
    /// Non-null when the field type is a Java enum; contains the ordered list of
    /// enum constant names. Clients should render a dropdown instead of free text.
    List<String>? enumValues,
    /// Non-null when this field is a Map/List/Set whose element type is a nested
    /// config class. The UI should render a sub-form button per element/value.
    String? elementNestedTypeFqcn,
  }) = _FieldSummary;

  /// Decodes field metadata returned in a configuration detail response.
  factory FieldSummary.fromJson(Map<String, dynamic> json) =>
      _$FieldSummaryFromJson(json);
}

/// Detailed view of a configuration class returned by GET /api/configurations/{fqcn}.
@freezed
abstract class ConfigDetail with _$ConfigDetail {
  /// Creates a form description for [fqcn], including server-provided defaults.
  const factory ConfigDetail({
    required String fqcn,
    required List<FieldSummary> fields,
    @Default({}) Map<String, dynamic> defaultValues,
  }) = _ConfigDetail;

  /// Decodes a configuration detail response.
  factory ConfigDetail.fromJson(Map<String, dynamic> json) =>
      _$ConfigDetailFromJson(json);
}

/// Result of POST /api/configurations/{fqcn}/build.
@freezed
abstract class BuildResult with _$BuildResult {
  /// Creates a build outcome containing either generated [yaml] or an error.
  const factory BuildResult({
    String? yaml,
    String? errorMessage,
  }) = _BuildResult;

  /// Decodes a YAML build response.
  factory BuildResult.fromJson(Map<String, dynamic> json) =>
      _$BuildResultFromJson(json);
}

/// Result of POST /api/configurations/{fqcn}/write.
@freezed
abstract class WriteResult with _$WriteResult {
  /// Creates a write outcome containing either the written [path] or an error.
  const factory WriteResult({
    String? path,
    String? errorMessage,
  }) = _WriteResult;

  /// Decodes a configuration write response.
  factory WriteResult.fromJson(Map<String, dynamic> json) =>
      _$WriteResultFromJson(json);
}
