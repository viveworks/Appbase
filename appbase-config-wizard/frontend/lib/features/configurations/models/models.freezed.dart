// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint, type=warning, deprecated_member_use, deprecated_member_use_from_same_package
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$ConfigSummary {

 String get fqcn; String get simpleName; String get layer; String get module;
/// Create a copy of ConfigSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ConfigSummaryCopyWith<ConfigSummary> get copyWith => _$ConfigSummaryCopyWithImpl<ConfigSummary>(this as ConfigSummary, _$identity);

  /// Serializes this ConfigSummary to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ConfigSummary;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ConfigSummary&&(identical(other.fqcn, _this.fqcn) || other.fqcn == _this.fqcn)&&(identical(other.simpleName, _this.simpleName) || other.simpleName == _this.simpleName)&&(identical(other.layer, _this.layer) || other.layer == _this.layer)&&(identical(other.module, _this.module) || other.module == _this.module));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ConfigSummary;
  return Object.hash(runtimeType,_this.fqcn,_this.simpleName,_this.layer,_this.module);
}

@override
String toString() {
  final _this = this as ConfigSummary;
  return 'ConfigSummary(fqcn: ${_this.fqcn}, simpleName: ${_this.simpleName}, layer: ${_this.layer}, module: ${_this.module})';
}


}

/// @nodoc
abstract mixin class $ConfigSummaryCopyWith<$Res>  {
  factory $ConfigSummaryCopyWith(ConfigSummary value, $Res Function(ConfigSummary) _then) = _$ConfigSummaryCopyWithImpl;
@useResult
$Res call({
 String fqcn, String simpleName, String layer, String module
});




}
/// @nodoc
class _$ConfigSummaryCopyWithImpl<$Res>
    implements $ConfigSummaryCopyWith<$Res> {
  _$ConfigSummaryCopyWithImpl(this._self, this._then);

  final ConfigSummary _self;
  final $Res Function(ConfigSummary) _then;

/// Create a copy of ConfigSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? fqcn = null,Object? simpleName = null,Object? layer = null,Object? module = null,}) {
  return _then(ConfigSummary(
fqcn: null == fqcn ? _self.fqcn : fqcn // ignore: cast_nullable_to_non_nullable
as String,simpleName: null == simpleName ? _self.simpleName : simpleName // ignore: cast_nullable_to_non_nullable
as String,layer: null == layer ? _self.layer : layer // ignore: cast_nullable_to_non_nullable
as String,module: null == module ? _self.module : module // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [ConfigSummary].
extension ConfigSummaryPatterns on ConfigSummary {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ConfigSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ConfigSummary() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ConfigSummary value)  $default,){
final _that = this;
switch (_that) {
case _ConfigSummary():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ConfigSummary value)?  $default,){
final _that = this;
switch (_that) {
case _ConfigSummary() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String fqcn,  String simpleName,  String layer,  String module)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ConfigSummary() when $default != null:
return $default(_that.fqcn,_that.simpleName,_that.layer,_that.module);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String fqcn,  String simpleName,  String layer,  String module)  $default,) {final _that = this;
switch (_that) {
case _ConfigSummary():
return $default(_that.fqcn,_that.simpleName,_that.layer,_that.module);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String fqcn,  String simpleName,  String layer,  String module)?  $default,) {final _that = this;
switch (_that) {
case _ConfigSummary() when $default != null:
return $default(_that.fqcn,_that.simpleName,_that.layer,_that.module);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ConfigSummary implements ConfigSummary {
  const _ConfigSummary({required this.fqcn, required this.simpleName, required this.layer, required this.module});
  factory _ConfigSummary.fromJson(Map<String, dynamic> json) => _$ConfigSummaryFromJson(json);

@override final  String fqcn;
@override final  String simpleName;
@override final  String layer;
@override final  String module;

/// Create a copy of ConfigSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ConfigSummaryCopyWith<_ConfigSummary> get copyWith => __$ConfigSummaryCopyWithImpl<_ConfigSummary>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ConfigSummaryToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ConfigSummary&&(identical(other.fqcn, fqcn) || other.fqcn == fqcn)&&(identical(other.simpleName, simpleName) || other.simpleName == simpleName)&&(identical(other.layer, layer) || other.layer == layer)&&(identical(other.module, module) || other.module == module));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,fqcn,simpleName,layer,module);
}

@override
String toString() {
    return 'ConfigSummary(fqcn: $fqcn, simpleName: $simpleName, layer: $layer, module: $module)';
}


}

/// @nodoc
abstract mixin class _$ConfigSummaryCopyWith<$Res> implements $ConfigSummaryCopyWith<$Res> {
  factory _$ConfigSummaryCopyWith(_ConfigSummary value, $Res Function(_ConfigSummary) _then) = __$ConfigSummaryCopyWithImpl;
@override @useResult
$Res call({
 String fqcn, String simpleName, String layer, String module
});




}
/// @nodoc
class __$ConfigSummaryCopyWithImpl<$Res>
    implements _$ConfigSummaryCopyWith<$Res> {
  __$ConfigSummaryCopyWithImpl(this._self, this._then);

  final _ConfigSummary _self;
  final $Res Function(_ConfigSummary) _then;

/// Create a copy of ConfigSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? fqcn = null,Object? simpleName = null,Object? layer = null,Object? module = null,}) {
  return _then(_ConfigSummary(
fqcn: null == fqcn ? _self.fqcn : fqcn // ignore: cast_nullable_to_non_nullable
as String,simpleName: null == simpleName ? _self.simpleName : simpleName // ignore: cast_nullable_to_non_nullable
as String,layer: null == layer ? _self.layer : layer // ignore: cast_nullable_to_non_nullable
as String,module: null == module ? _self.module : module // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$FieldSummary {

 String get name; String get typeName;@JsonKey(name: 'required') bool? get isRequired; String? get defaultValue;/// Non-null when this field holds a nested Appbase config object.
/// The value is the FQCN used to load sub-form fields via GET /api/configurations/{fqcn}.
 String? get nestedTypeFqcn;/// Non-null when the field type is a Java enum; contains the ordered list of
/// enum constant names. Clients should render a dropdown instead of free text.
 List<String>? get enumValues;/// Non-null when this field is a Map/List/Set whose element type is a nested
/// config class. The UI should render a sub-form button per element/value.
 String? get elementNestedTypeFqcn;
/// Create a copy of FieldSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$FieldSummaryCopyWith<FieldSummary> get copyWith => _$FieldSummaryCopyWithImpl<FieldSummary>(this as FieldSummary, _$identity);

  /// Serializes this FieldSummary to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as FieldSummary;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is FieldSummary&&(identical(other.name, _this.name) || other.name == _this.name)&&(identical(other.typeName, _this.typeName) || other.typeName == _this.typeName)&&(identical(other.isRequired, _this.isRequired) || other.isRequired == _this.isRequired)&&(identical(other.defaultValue, _this.defaultValue) || other.defaultValue == _this.defaultValue)&&(identical(other.nestedTypeFqcn, _this.nestedTypeFqcn) || other.nestedTypeFqcn == _this.nestedTypeFqcn)&&const DeepCollectionEquality().equals(other.enumValues, _this.enumValues)&&(identical(other.elementNestedTypeFqcn, _this.elementNestedTypeFqcn) || other.elementNestedTypeFqcn == _this.elementNestedTypeFqcn));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as FieldSummary;
  return Object.hash(runtimeType,_this.name,_this.typeName,_this.isRequired,_this.defaultValue,_this.nestedTypeFqcn,const DeepCollectionEquality().hash(_this.enumValues),_this.elementNestedTypeFqcn);
}

@override
String toString() {
  final _this = this as FieldSummary;
  return 'FieldSummary(name: ${_this.name}, typeName: ${_this.typeName}, isRequired: ${_this.isRequired}, defaultValue: ${_this.defaultValue}, nestedTypeFqcn: ${_this.nestedTypeFqcn}, enumValues: ${_this.enumValues}, elementNestedTypeFqcn: ${_this.elementNestedTypeFqcn})';
}


}

/// @nodoc
abstract mixin class $FieldSummaryCopyWith<$Res>  {
  factory $FieldSummaryCopyWith(FieldSummary value, $Res Function(FieldSummary) _then) = _$FieldSummaryCopyWithImpl;
@useResult
$Res call({
 String name, String typeName,@JsonKey(name: 'required') bool? isRequired, String? defaultValue, String? nestedTypeFqcn, List<String>? enumValues, String? elementNestedTypeFqcn
});




}
/// @nodoc
class _$FieldSummaryCopyWithImpl<$Res>
    implements $FieldSummaryCopyWith<$Res> {
  _$FieldSummaryCopyWithImpl(this._self, this._then);

  final FieldSummary _self;
  final $Res Function(FieldSummary) _then;

/// Create a copy of FieldSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? name = null,Object? typeName = null,Object? isRequired = freezed,Object? defaultValue = freezed,Object? nestedTypeFqcn = freezed,Object? enumValues = freezed,Object? elementNestedTypeFqcn = freezed,}) {
  return _then(FieldSummary(
name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,typeName: null == typeName ? _self.typeName : typeName // ignore: cast_nullable_to_non_nullable
as String,isRequired: freezed == isRequired ? _self.isRequired : isRequired // ignore: cast_nullable_to_non_nullable
as bool?,defaultValue: freezed == defaultValue ? _self.defaultValue : defaultValue // ignore: cast_nullable_to_non_nullable
as String?,nestedTypeFqcn: freezed == nestedTypeFqcn ? _self.nestedTypeFqcn : nestedTypeFqcn // ignore: cast_nullable_to_non_nullable
as String?,enumValues: freezed == enumValues ? _self.enumValues : enumValues // ignore: cast_nullable_to_non_nullable
as List<String>?,elementNestedTypeFqcn: freezed == elementNestedTypeFqcn ? _self.elementNestedTypeFqcn : elementNestedTypeFqcn // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [FieldSummary].
extension FieldSummaryPatterns on FieldSummary {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _FieldSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _FieldSummary() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _FieldSummary value)  $default,){
final _that = this;
switch (_that) {
case _FieldSummary():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _FieldSummary value)?  $default,){
final _that = this;
switch (_that) {
case _FieldSummary() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String name,  String typeName, @JsonKey(name: 'required')  bool? isRequired,  String? defaultValue,  String? nestedTypeFqcn,  List<String>? enumValues,  String? elementNestedTypeFqcn)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _FieldSummary() when $default != null:
return $default(_that.name,_that.typeName,_that.isRequired,_that.defaultValue,_that.nestedTypeFqcn,_that.enumValues,_that.elementNestedTypeFqcn);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String name,  String typeName, @JsonKey(name: 'required')  bool? isRequired,  String? defaultValue,  String? nestedTypeFqcn,  List<String>? enumValues,  String? elementNestedTypeFqcn)  $default,) {final _that = this;
switch (_that) {
case _FieldSummary():
return $default(_that.name,_that.typeName,_that.isRequired,_that.defaultValue,_that.nestedTypeFqcn,_that.enumValues,_that.elementNestedTypeFqcn);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String name,  String typeName, @JsonKey(name: 'required')  bool? isRequired,  String? defaultValue,  String? nestedTypeFqcn,  List<String>? enumValues,  String? elementNestedTypeFqcn)?  $default,) {final _that = this;
switch (_that) {
case _FieldSummary() when $default != null:
return $default(_that.name,_that.typeName,_that.isRequired,_that.defaultValue,_that.nestedTypeFqcn,_that.enumValues,_that.elementNestedTypeFqcn);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _FieldSummary implements FieldSummary {
  const _FieldSummary({required this.name, required this.typeName, @JsonKey(name: 'required') this.isRequired, this.defaultValue, this.nestedTypeFqcn,  List<String>? enumValues, this.elementNestedTypeFqcn}): _enumValues = enumValues;
  factory _FieldSummary.fromJson(Map<String, dynamic> json) => _$FieldSummaryFromJson(json);

@override final  String name;
@override final  String typeName;
@override@JsonKey(name: 'required') final  bool? isRequired;
@override final  String? defaultValue;
/// Non-null when this field holds a nested Appbase config object.
/// The value is the FQCN used to load sub-form fields via GET /api/configurations/{fqcn}.
@override final  String? nestedTypeFqcn;
/// Non-null when the field type is a Java enum; contains the ordered list of
/// enum constant names. Clients should render a dropdown instead of free text.
 final  List<String>? _enumValues;
/// Non-null when the field type is a Java enum; contains the ordered list of
/// enum constant names. Clients should render a dropdown instead of free text.
@override List<String>? get enumValues {
  final value = _enumValues;
  if (value == null) return null;
  if (_enumValues is EqualUnmodifiableListView) return _enumValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(value);
}

/// Non-null when this field is a Map/List/Set whose element type is a nested
/// config class. The UI should render a sub-form button per element/value.
@override final  String? elementNestedTypeFqcn;

/// Create a copy of FieldSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$FieldSummaryCopyWith<_FieldSummary> get copyWith => __$FieldSummaryCopyWithImpl<_FieldSummary>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$FieldSummaryToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _FieldSummary&&(identical(other.name, name) || other.name == name)&&(identical(other.typeName, typeName) || other.typeName == typeName)&&(identical(other.isRequired, isRequired) || other.isRequired == isRequired)&&(identical(other.defaultValue, defaultValue) || other.defaultValue == defaultValue)&&(identical(other.nestedTypeFqcn, nestedTypeFqcn) || other.nestedTypeFqcn == nestedTypeFqcn)&&const DeepCollectionEquality().equals(other.enumValues, _enumValues)&&(identical(other.elementNestedTypeFqcn, elementNestedTypeFqcn) || other.elementNestedTypeFqcn == elementNestedTypeFqcn));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,name,typeName,isRequired,defaultValue,nestedTypeFqcn,const DeepCollectionEquality().hash(_enumValues),elementNestedTypeFqcn);
}

@override
String toString() {
    return 'FieldSummary(name: $name, typeName: $typeName, isRequired: $isRequired, defaultValue: $defaultValue, nestedTypeFqcn: $nestedTypeFqcn, enumValues: $enumValues, elementNestedTypeFqcn: $elementNestedTypeFqcn)';
}


}

/// @nodoc
abstract mixin class _$FieldSummaryCopyWith<$Res> implements $FieldSummaryCopyWith<$Res> {
  factory _$FieldSummaryCopyWith(_FieldSummary value, $Res Function(_FieldSummary) _then) = __$FieldSummaryCopyWithImpl;
@override @useResult
$Res call({
 String name, String typeName,@JsonKey(name: 'required') bool? isRequired, String? defaultValue, String? nestedTypeFqcn, List<String>? enumValues, String? elementNestedTypeFqcn
});




}
/// @nodoc
class __$FieldSummaryCopyWithImpl<$Res>
    implements _$FieldSummaryCopyWith<$Res> {
  __$FieldSummaryCopyWithImpl(this._self, this._then);

  final _FieldSummary _self;
  final $Res Function(_FieldSummary) _then;

/// Create a copy of FieldSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? name = null,Object? typeName = null,Object? isRequired = freezed,Object? defaultValue = freezed,Object? nestedTypeFqcn = freezed,Object? enumValues = freezed,Object? elementNestedTypeFqcn = freezed,}) {
  return _then(_FieldSummary(
name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,typeName: null == typeName ? _self.typeName : typeName // ignore: cast_nullable_to_non_nullable
as String,isRequired: freezed == isRequired ? _self.isRequired : isRequired // ignore: cast_nullable_to_non_nullable
as bool?,defaultValue: freezed == defaultValue ? _self.defaultValue : defaultValue // ignore: cast_nullable_to_non_nullable
as String?,nestedTypeFqcn: freezed == nestedTypeFqcn ? _self.nestedTypeFqcn : nestedTypeFqcn // ignore: cast_nullable_to_non_nullable
as String?,enumValues: freezed == enumValues ? _self._enumValues : enumValues // ignore: cast_nullable_to_non_nullable
as List<String>?,elementNestedTypeFqcn: freezed == elementNestedTypeFqcn ? _self.elementNestedTypeFqcn : elementNestedTypeFqcn // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}


/// @nodoc
mixin _$ConfigDetail {

 String get fqcn; List<FieldSummary> get fields; Map<String, dynamic> get defaultValues;
/// Create a copy of ConfigDetail
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ConfigDetailCopyWith<ConfigDetail> get copyWith => _$ConfigDetailCopyWithImpl<ConfigDetail>(this as ConfigDetail, _$identity);

  /// Serializes this ConfigDetail to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ConfigDetail;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ConfigDetail&&(identical(other.fqcn, _this.fqcn) || other.fqcn == _this.fqcn)&&const DeepCollectionEquality().equals(other.fields, _this.fields)&&const DeepCollectionEquality().equals(other.defaultValues, _this.defaultValues));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ConfigDetail;
  return Object.hash(runtimeType,_this.fqcn,const DeepCollectionEquality().hash(_this.fields),const DeepCollectionEquality().hash(_this.defaultValues));
}

@override
String toString() {
  final _this = this as ConfigDetail;
  return 'ConfigDetail(fqcn: ${_this.fqcn}, fields: ${_this.fields}, defaultValues: ${_this.defaultValues})';
}


}

/// @nodoc
abstract mixin class $ConfigDetailCopyWith<$Res>  {
  factory $ConfigDetailCopyWith(ConfigDetail value, $Res Function(ConfigDetail) _then) = _$ConfigDetailCopyWithImpl;
@useResult
$Res call({
 String fqcn, List<FieldSummary> fields, Map<String, dynamic> defaultValues
});




}
/// @nodoc
class _$ConfigDetailCopyWithImpl<$Res>
    implements $ConfigDetailCopyWith<$Res> {
  _$ConfigDetailCopyWithImpl(this._self, this._then);

  final ConfigDetail _self;
  final $Res Function(ConfigDetail) _then;

/// Create a copy of ConfigDetail
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? fqcn = null,Object? fields = null,Object? defaultValues = null,}) {
  return _then(ConfigDetail(
fqcn: null == fqcn ? _self.fqcn : fqcn // ignore: cast_nullable_to_non_nullable
as String,fields: null == fields ? _self.fields : fields // ignore: cast_nullable_to_non_nullable
as List<FieldSummary>,defaultValues: null == defaultValues ? _self.defaultValues : defaultValues // ignore: cast_nullable_to_non_nullable
as Map<String, dynamic>,
  ));
}

}


/// Adds pattern-matching-related methods to [ConfigDetail].
extension ConfigDetailPatterns on ConfigDetail {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ConfigDetail value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ConfigDetail() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ConfigDetail value)  $default,){
final _that = this;
switch (_that) {
case _ConfigDetail():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ConfigDetail value)?  $default,){
final _that = this;
switch (_that) {
case _ConfigDetail() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String fqcn,  List<FieldSummary> fields,  Map<String, dynamic> defaultValues)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ConfigDetail() when $default != null:
return $default(_that.fqcn,_that.fields,_that.defaultValues);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String fqcn,  List<FieldSummary> fields,  Map<String, dynamic> defaultValues)  $default,) {final _that = this;
switch (_that) {
case _ConfigDetail():
return $default(_that.fqcn,_that.fields,_that.defaultValues);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String fqcn,  List<FieldSummary> fields,  Map<String, dynamic> defaultValues)?  $default,) {final _that = this;
switch (_that) {
case _ConfigDetail() when $default != null:
return $default(_that.fqcn,_that.fields,_that.defaultValues);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ConfigDetail implements ConfigDetail {
  const _ConfigDetail({required this.fqcn, required  List<FieldSummary> fields,  Map<String, dynamic> defaultValues = const {}}): _fields = fields,_defaultValues = defaultValues;
  factory _ConfigDetail.fromJson(Map<String, dynamic> json) => _$ConfigDetailFromJson(json);

@override final  String fqcn;
 final  List<FieldSummary> _fields;
@override List<FieldSummary> get fields {
  if (_fields is EqualUnmodifiableListView) return _fields;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_fields);
}

 final  Map<String, dynamic> _defaultValues;
@override@JsonKey() Map<String, dynamic> get defaultValues {
  if (_defaultValues is EqualUnmodifiableMapView) return _defaultValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_defaultValues);
}


/// Create a copy of ConfigDetail
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ConfigDetailCopyWith<_ConfigDetail> get copyWith => __$ConfigDetailCopyWithImpl<_ConfigDetail>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ConfigDetailToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ConfigDetail&&(identical(other.fqcn, fqcn) || other.fqcn == fqcn)&&const DeepCollectionEquality().equals(other.fields, _fields)&&const DeepCollectionEquality().equals(other.defaultValues, _defaultValues));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,fqcn,const DeepCollectionEquality().hash(_fields),const DeepCollectionEquality().hash(_defaultValues));
}

@override
String toString() {
    return 'ConfigDetail(fqcn: $fqcn, fields: $fields, defaultValues: $defaultValues)';
}


}

/// @nodoc
abstract mixin class _$ConfigDetailCopyWith<$Res> implements $ConfigDetailCopyWith<$Res> {
  factory _$ConfigDetailCopyWith(_ConfigDetail value, $Res Function(_ConfigDetail) _then) = __$ConfigDetailCopyWithImpl;
@override @useResult
$Res call({
 String fqcn, List<FieldSummary> fields, Map<String, dynamic> defaultValues
});




}
/// @nodoc
class __$ConfigDetailCopyWithImpl<$Res>
    implements _$ConfigDetailCopyWith<$Res> {
  __$ConfigDetailCopyWithImpl(this._self, this._then);

  final _ConfigDetail _self;
  final $Res Function(_ConfigDetail) _then;

/// Create a copy of ConfigDetail
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? fqcn = null,Object? fields = null,Object? defaultValues = null,}) {
  return _then(_ConfigDetail(
fqcn: null == fqcn ? _self.fqcn : fqcn // ignore: cast_nullable_to_non_nullable
as String,fields: null == fields ? _self._fields : fields // ignore: cast_nullable_to_non_nullable
as List<FieldSummary>,defaultValues: null == defaultValues ? _self._defaultValues : defaultValues // ignore: cast_nullable_to_non_nullable
as Map<String, dynamic>,
  ));
}


}


/// @nodoc
mixin _$BuildResult {

 String? get yaml; String? get errorMessage;
/// Create a copy of BuildResult
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BuildResultCopyWith<BuildResult> get copyWith => _$BuildResultCopyWithImpl<BuildResult>(this as BuildResult, _$identity);

  /// Serializes this BuildResult to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as BuildResult;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BuildResult&&(identical(other.yaml, _this.yaml) || other.yaml == _this.yaml)&&(identical(other.errorMessage, _this.errorMessage) || other.errorMessage == _this.errorMessage));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as BuildResult;
  return Object.hash(runtimeType,_this.yaml,_this.errorMessage);
}

@override
String toString() {
  final _this = this as BuildResult;
  return 'BuildResult(yaml: ${_this.yaml}, errorMessage: ${_this.errorMessage})';
}


}

/// @nodoc
abstract mixin class $BuildResultCopyWith<$Res>  {
  factory $BuildResultCopyWith(BuildResult value, $Res Function(BuildResult) _then) = _$BuildResultCopyWithImpl;
@useResult
$Res call({
 String? yaml, String? errorMessage
});




}
/// @nodoc
class _$BuildResultCopyWithImpl<$Res>
    implements $BuildResultCopyWith<$Res> {
  _$BuildResultCopyWithImpl(this._self, this._then);

  final BuildResult _self;
  final $Res Function(BuildResult) _then;

/// Create a copy of BuildResult
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? yaml = freezed,Object? errorMessage = freezed,}) {
  return _then(BuildResult(
yaml: freezed == yaml ? _self.yaml : yaml // ignore: cast_nullable_to_non_nullable
as String?,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [BuildResult].
extension BuildResultPatterns on BuildResult {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BuildResult value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BuildResult() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BuildResult value)  $default,){
final _that = this;
switch (_that) {
case _BuildResult():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BuildResult value)?  $default,){
final _that = this;
switch (_that) {
case _BuildResult() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String? yaml,  String? errorMessage)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BuildResult() when $default != null:
return $default(_that.yaml,_that.errorMessage);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String? yaml,  String? errorMessage)  $default,) {final _that = this;
switch (_that) {
case _BuildResult():
return $default(_that.yaml,_that.errorMessage);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String? yaml,  String? errorMessage)?  $default,) {final _that = this;
switch (_that) {
case _BuildResult() when $default != null:
return $default(_that.yaml,_that.errorMessage);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _BuildResult implements BuildResult {
  const _BuildResult({this.yaml, this.errorMessage});
  factory _BuildResult.fromJson(Map<String, dynamic> json) => _$BuildResultFromJson(json);

@override final  String? yaml;
@override final  String? errorMessage;

/// Create a copy of BuildResult
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BuildResultCopyWith<_BuildResult> get copyWith => __$BuildResultCopyWithImpl<_BuildResult>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$BuildResultToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _BuildResult&&(identical(other.yaml, yaml) || other.yaml == yaml)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,yaml,errorMessage);
}

@override
String toString() {
    return 'BuildResult(yaml: $yaml, errorMessage: $errorMessage)';
}


}

/// @nodoc
abstract mixin class _$BuildResultCopyWith<$Res> implements $BuildResultCopyWith<$Res> {
  factory _$BuildResultCopyWith(_BuildResult value, $Res Function(_BuildResult) _then) = __$BuildResultCopyWithImpl;
@override @useResult
$Res call({
 String? yaml, String? errorMessage
});




}
/// @nodoc
class __$BuildResultCopyWithImpl<$Res>
    implements _$BuildResultCopyWith<$Res> {
  __$BuildResultCopyWithImpl(this._self, this._then);

  final _BuildResult _self;
  final $Res Function(_BuildResult) _then;

/// Create a copy of BuildResult
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? yaml = freezed,Object? errorMessage = freezed,}) {
  return _then(_BuildResult(
yaml: freezed == yaml ? _self.yaml : yaml // ignore: cast_nullable_to_non_nullable
as String?,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}


/// @nodoc
mixin _$WriteResult {

 String? get path; String? get errorMessage;
/// Create a copy of WriteResult
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$WriteResultCopyWith<WriteResult> get copyWith => _$WriteResultCopyWithImpl<WriteResult>(this as WriteResult, _$identity);

  /// Serializes this WriteResult to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as WriteResult;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is WriteResult&&(identical(other.path, _this.path) || other.path == _this.path)&&(identical(other.errorMessage, _this.errorMessage) || other.errorMessage == _this.errorMessage));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as WriteResult;
  return Object.hash(runtimeType,_this.path,_this.errorMessage);
}

@override
String toString() {
  final _this = this as WriteResult;
  return 'WriteResult(path: ${_this.path}, errorMessage: ${_this.errorMessage})';
}


}

/// @nodoc
abstract mixin class $WriteResultCopyWith<$Res>  {
  factory $WriteResultCopyWith(WriteResult value, $Res Function(WriteResult) _then) = _$WriteResultCopyWithImpl;
@useResult
$Res call({
 String? path, String? errorMessage
});




}
/// @nodoc
class _$WriteResultCopyWithImpl<$Res>
    implements $WriteResultCopyWith<$Res> {
  _$WriteResultCopyWithImpl(this._self, this._then);

  final WriteResult _self;
  final $Res Function(WriteResult) _then;

/// Create a copy of WriteResult
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? path = freezed,Object? errorMessage = freezed,}) {
  return _then(WriteResult(
path: freezed == path ? _self.path : path // ignore: cast_nullable_to_non_nullable
as String?,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [WriteResult].
extension WriteResultPatterns on WriteResult {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _WriteResult value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _WriteResult() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _WriteResult value)  $default,){
final _that = this;
switch (_that) {
case _WriteResult():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _WriteResult value)?  $default,){
final _that = this;
switch (_that) {
case _WriteResult() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String? path,  String? errorMessage)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _WriteResult() when $default != null:
return $default(_that.path,_that.errorMessage);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String? path,  String? errorMessage)  $default,) {final _that = this;
switch (_that) {
case _WriteResult():
return $default(_that.path,_that.errorMessage);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String? path,  String? errorMessage)?  $default,) {final _that = this;
switch (_that) {
case _WriteResult() when $default != null:
return $default(_that.path,_that.errorMessage);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _WriteResult implements WriteResult {
  const _WriteResult({this.path, this.errorMessage});
  factory _WriteResult.fromJson(Map<String, dynamic> json) => _$WriteResultFromJson(json);

@override final  String? path;
@override final  String? errorMessage;

/// Create a copy of WriteResult
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$WriteResultCopyWith<_WriteResult> get copyWith => __$WriteResultCopyWithImpl<_WriteResult>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$WriteResultToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _WriteResult&&(identical(other.path, path) || other.path == path)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,path,errorMessage);
}

@override
String toString() {
    return 'WriteResult(path: $path, errorMessage: $errorMessage)';
}


}

/// @nodoc
abstract mixin class _$WriteResultCopyWith<$Res> implements $WriteResultCopyWith<$Res> {
  factory _$WriteResultCopyWith(_WriteResult value, $Res Function(_WriteResult) _then) = __$WriteResultCopyWithImpl;
@override @useResult
$Res call({
 String? path, String? errorMessage
});




}
/// @nodoc
class __$WriteResultCopyWithImpl<$Res>
    implements _$WriteResultCopyWith<$Res> {
  __$WriteResultCopyWithImpl(this._self, this._then);

  final _WriteResult _self;
  final $Res Function(_WriteResult) _then;

/// Create a copy of WriteResult
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? path = freezed,Object? errorMessage = freezed,}) {
  return _then(_WriteResult(
path: freezed == path ? _self.path : path // ignore: cast_nullable_to_non_nullable
as String?,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on
