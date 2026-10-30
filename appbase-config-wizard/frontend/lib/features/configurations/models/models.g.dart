// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_ConfigSummary _$ConfigSummaryFromJson(Map<String, dynamic> json) =>
    _ConfigSummary(
      fqcn: json['fqcn'] as String,
      simpleName: json['simpleName'] as String,
      layer: json['layer'] as String,
      module: json['module'] as String,
    );

Map<String, dynamic> _$ConfigSummaryToJson(_ConfigSummary instance) =>
    <String, dynamic>{
      'fqcn': instance.fqcn,
      'simpleName': instance.simpleName,
      'layer': instance.layer,
      'module': instance.module,
    };

_FieldSummary _$FieldSummaryFromJson(Map<String, dynamic> json) =>
    _FieldSummary(
      name: json['name'] as String,
      typeName: json['typeName'] as String,
      isRequired: json['required'] as bool?,
      defaultValue: json['defaultValue'] as String?,
      nestedTypeFqcn: json['nestedTypeFqcn'] as String?,
      enumValues: (json['enumValues'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      elementNestedTypeFqcn: json['elementNestedTypeFqcn'] as String?,
    );

Map<String, dynamic> _$FieldSummaryToJson(_FieldSummary instance) =>
    <String, dynamic>{
      'name': instance.name,
      'typeName': instance.typeName,
      'required': instance.isRequired,
      'defaultValue': instance.defaultValue,
      'nestedTypeFqcn': instance.nestedTypeFqcn,
      'enumValues': instance.enumValues,
      'elementNestedTypeFqcn': instance.elementNestedTypeFqcn,
    };

_ConfigDetail _$ConfigDetailFromJson(Map<String, dynamic> json) =>
    _ConfigDetail(
      fqcn: json['fqcn'] as String,
      fields: (json['fields'] as List<dynamic>)
          .map((e) => FieldSummary.fromJson(e as Map<String, dynamic>))
          .toList(),
      defaultValues: json['defaultValues'] as Map<String, dynamic>? ?? const {},
    );

Map<String, dynamic> _$ConfigDetailToJson(_ConfigDetail instance) =>
    <String, dynamic>{
      'fqcn': instance.fqcn,
      'fields': instance.fields,
      'defaultValues': instance.defaultValues,
    };

_BuildResult _$BuildResultFromJson(Map<String, dynamic> json) => _BuildResult(
  yaml: json['yaml'] as String?,
  errorMessage: json['errorMessage'] as String?,
);

Map<String, dynamic> _$BuildResultToJson(_BuildResult instance) =>
    <String, dynamic>{
      'yaml': instance.yaml,
      'errorMessage': instance.errorMessage,
    };

_WriteResult _$WriteResultFromJson(Map<String, dynamic> json) => _WriteResult(
  path: json['path'] as String?,
  errorMessage: json['errorMessage'] as String?,
);

Map<String, dynamic> _$WriteResultToJson(_WriteResult instance) =>
    <String, dynamic>{
      'path': instance.path,
      'errorMessage': instance.errorMessage,
    };
