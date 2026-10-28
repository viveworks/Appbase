// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_ImportScanResult _$ImportScanResultFromJson(Map<String, dynamic> json) =>
    _ImportScanResult(
      files: (json['files'] as List<dynamic>)
          .map((e) => FileEntry.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$ImportScanResultToJson(_ImportScanResult instance) =>
    <String, dynamic>{'files': instance.files};

_FileEntry _$FileEntryFromJson(Map<String, dynamic> json) => _FileEntry(
  path: json['path'] as String,
  fileName: json['fileName'] as String,
  candidates: (json['candidates'] as List<dynamic>)
      .map((e) => TypeCandidate.fromJson(e as Map<String, dynamic>))
      .toList(),
);

Map<String, dynamic> _$FileEntryToJson(_FileEntry instance) =>
    <String, dynamic>{
      'path': instance.path,
      'fileName': instance.fileName,
      'candidates': instance.candidates,
    };

_TypeCandidate _$TypeCandidateFromJson(Map<String, dynamic> json) =>
    _TypeCandidate(
      fqcn: json['fqcn'] as String,
      simpleName: json['simpleName'] as String,
      confidence: (json['confidence'] as num).toDouble(),
      signals: (json['signals'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
    );

Map<String, dynamic> _$TypeCandidateToJson(_TypeCandidate instance) =>
    <String, dynamic>{
      'fqcn': instance.fqcn,
      'simpleName': instance.simpleName,
      'confidence': instance.confidence,
      'signals': instance.signals,
    };

_ImportWriteResult _$ImportWriteResultFromJson(Map<String, dynamic> json) =>
    _ImportWriteResult(
      path: json['path'] as String?,
      errorMessage: json['errorMessage'] as String?,
    );

Map<String, dynamic> _$ImportWriteResultToJson(_ImportWriteResult instance) =>
    <String, dynamic>{
      'path': instance.path,
      'errorMessage': instance.errorMessage,
    };
