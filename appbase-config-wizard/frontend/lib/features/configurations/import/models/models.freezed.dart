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
mixin _$ImportScanResult {

 List<FileEntry> get files;
/// Create a copy of ImportScanResult
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ImportScanResultCopyWith<ImportScanResult> get copyWith => _$ImportScanResultCopyWithImpl<ImportScanResult>(this as ImportScanResult, _$identity);

  /// Serializes this ImportScanResult to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ImportScanResult;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ImportScanResult&&const DeepCollectionEquality().equals(other.files, _this.files));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ImportScanResult;
  return Object.hash(runtimeType,const DeepCollectionEquality().hash(_this.files));
}

@override
String toString() {
  final _this = this as ImportScanResult;
  return 'ImportScanResult(files: ${_this.files})';
}


}

/// @nodoc
abstract mixin class $ImportScanResultCopyWith<$Res>  {
  factory $ImportScanResultCopyWith(ImportScanResult value, $Res Function(ImportScanResult) _then) = _$ImportScanResultCopyWithImpl;
@useResult
$Res call({
 List<FileEntry> files
});




}
/// @nodoc
class _$ImportScanResultCopyWithImpl<$Res>
    implements $ImportScanResultCopyWith<$Res> {
  _$ImportScanResultCopyWithImpl(this._self, this._then);

  final ImportScanResult _self;
  final $Res Function(ImportScanResult) _then;

/// Create a copy of ImportScanResult
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? files = null,}) {
  return _then(ImportScanResult(
files: null == files ? _self.files : files // ignore: cast_nullable_to_non_nullable
as List<FileEntry>,
  ));
}

}


/// Adds pattern-matching-related methods to [ImportScanResult].
extension ImportScanResultPatterns on ImportScanResult {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ImportScanResult value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ImportScanResult() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ImportScanResult value)  $default,){
final _that = this;
switch (_that) {
case _ImportScanResult():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ImportScanResult value)?  $default,){
final _that = this;
switch (_that) {
case _ImportScanResult() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<FileEntry> files)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ImportScanResult() when $default != null:
return $default(_that.files);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<FileEntry> files)  $default,) {final _that = this;
switch (_that) {
case _ImportScanResult():
return $default(_that.files);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<FileEntry> files)?  $default,) {final _that = this;
switch (_that) {
case _ImportScanResult() when $default != null:
return $default(_that.files);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ImportScanResult implements ImportScanResult {
  const _ImportScanResult({required  List<FileEntry> files}): _files = files;
  factory _ImportScanResult.fromJson(Map<String, dynamic> json) => _$ImportScanResultFromJson(json);

 final  List<FileEntry> _files;
@override List<FileEntry> get files {
  if (_files is EqualUnmodifiableListView) return _files;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_files);
}


/// Create a copy of ImportScanResult
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ImportScanResultCopyWith<_ImportScanResult> get copyWith => __$ImportScanResultCopyWithImpl<_ImportScanResult>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ImportScanResultToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ImportScanResult&&const DeepCollectionEquality().equals(other.files, _files));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,const DeepCollectionEquality().hash(_files));
}

@override
String toString() {
    return 'ImportScanResult(files: $files)';
}


}

/// @nodoc
abstract mixin class _$ImportScanResultCopyWith<$Res> implements $ImportScanResultCopyWith<$Res> {
  factory _$ImportScanResultCopyWith(_ImportScanResult value, $Res Function(_ImportScanResult) _then) = __$ImportScanResultCopyWithImpl;
@override @useResult
$Res call({
 List<FileEntry> files
});




}
/// @nodoc
class __$ImportScanResultCopyWithImpl<$Res>
    implements _$ImportScanResultCopyWith<$Res> {
  __$ImportScanResultCopyWithImpl(this._self, this._then);

  final _ImportScanResult _self;
  final $Res Function(_ImportScanResult) _then;

/// Create a copy of ImportScanResult
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? files = null,}) {
  return _then(_ImportScanResult(
files: null == files ? _self._files : files // ignore: cast_nullable_to_non_nullable
as List<FileEntry>,
  ));
}


}


/// @nodoc
mixin _$FileEntry {

 String get path; String get fileName; List<TypeCandidate> get candidates;
/// Create a copy of FileEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$FileEntryCopyWith<FileEntry> get copyWith => _$FileEntryCopyWithImpl<FileEntry>(this as FileEntry, _$identity);

  /// Serializes this FileEntry to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as FileEntry;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is FileEntry&&(identical(other.path, _this.path) || other.path == _this.path)&&(identical(other.fileName, _this.fileName) || other.fileName == _this.fileName)&&const DeepCollectionEquality().equals(other.candidates, _this.candidates));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as FileEntry;
  return Object.hash(runtimeType,_this.path,_this.fileName,const DeepCollectionEquality().hash(_this.candidates));
}

@override
String toString() {
  final _this = this as FileEntry;
  return 'FileEntry(path: ${_this.path}, fileName: ${_this.fileName}, candidates: ${_this.candidates})';
}


}

/// @nodoc
abstract mixin class $FileEntryCopyWith<$Res>  {
  factory $FileEntryCopyWith(FileEntry value, $Res Function(FileEntry) _then) = _$FileEntryCopyWithImpl;
@useResult
$Res call({
 String path, String fileName, List<TypeCandidate> candidates
});




}
/// @nodoc
class _$FileEntryCopyWithImpl<$Res>
    implements $FileEntryCopyWith<$Res> {
  _$FileEntryCopyWithImpl(this._self, this._then);

  final FileEntry _self;
  final $Res Function(FileEntry) _then;

/// Create a copy of FileEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? path = null,Object? fileName = null,Object? candidates = null,}) {
  return _then(FileEntry(
path: null == path ? _self.path : path // ignore: cast_nullable_to_non_nullable
as String,fileName: null == fileName ? _self.fileName : fileName // ignore: cast_nullable_to_non_nullable
as String,candidates: null == candidates ? _self.candidates : candidates // ignore: cast_nullable_to_non_nullable
as List<TypeCandidate>,
  ));
}

}


/// Adds pattern-matching-related methods to [FileEntry].
extension FileEntryPatterns on FileEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _FileEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _FileEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _FileEntry value)  $default,){
final _that = this;
switch (_that) {
case _FileEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _FileEntry value)?  $default,){
final _that = this;
switch (_that) {
case _FileEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String path,  String fileName,  List<TypeCandidate> candidates)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _FileEntry() when $default != null:
return $default(_that.path,_that.fileName,_that.candidates);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String path,  String fileName,  List<TypeCandidate> candidates)  $default,) {final _that = this;
switch (_that) {
case _FileEntry():
return $default(_that.path,_that.fileName,_that.candidates);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String path,  String fileName,  List<TypeCandidate> candidates)?  $default,) {final _that = this;
switch (_that) {
case _FileEntry() when $default != null:
return $default(_that.path,_that.fileName,_that.candidates);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _FileEntry implements FileEntry {
  const _FileEntry({required this.path, required this.fileName, required  List<TypeCandidate> candidates}): _candidates = candidates;
  factory _FileEntry.fromJson(Map<String, dynamic> json) => _$FileEntryFromJson(json);

@override final  String path;
@override final  String fileName;
 final  List<TypeCandidate> _candidates;
@override List<TypeCandidate> get candidates {
  if (_candidates is EqualUnmodifiableListView) return _candidates;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_candidates);
}


/// Create a copy of FileEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$FileEntryCopyWith<_FileEntry> get copyWith => __$FileEntryCopyWithImpl<_FileEntry>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$FileEntryToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _FileEntry&&(identical(other.path, path) || other.path == path)&&(identical(other.fileName, fileName) || other.fileName == fileName)&&const DeepCollectionEquality().equals(other.candidates, _candidates));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,path,fileName,const DeepCollectionEquality().hash(_candidates));
}

@override
String toString() {
    return 'FileEntry(path: $path, fileName: $fileName, candidates: $candidates)';
}


}

/// @nodoc
abstract mixin class _$FileEntryCopyWith<$Res> implements $FileEntryCopyWith<$Res> {
  factory _$FileEntryCopyWith(_FileEntry value, $Res Function(_FileEntry) _then) = __$FileEntryCopyWithImpl;
@override @useResult
$Res call({
 String path, String fileName, List<TypeCandidate> candidates
});




}
/// @nodoc
class __$FileEntryCopyWithImpl<$Res>
    implements _$FileEntryCopyWith<$Res> {
  __$FileEntryCopyWithImpl(this._self, this._then);

  final _FileEntry _self;
  final $Res Function(_FileEntry) _then;

/// Create a copy of FileEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? path = null,Object? fileName = null,Object? candidates = null,}) {
  return _then(_FileEntry(
path: null == path ? _self.path : path // ignore: cast_nullable_to_non_nullable
as String,fileName: null == fileName ? _self.fileName : fileName // ignore: cast_nullable_to_non_nullable
as String,candidates: null == candidates ? _self._candidates : candidates // ignore: cast_nullable_to_non_nullable
as List<TypeCandidate>,
  ));
}


}


/// @nodoc
mixin _$TypeCandidate {

 String get fqcn; String get simpleName; double get confidence; List<String> get signals;
/// Create a copy of TypeCandidate
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$TypeCandidateCopyWith<TypeCandidate> get copyWith => _$TypeCandidateCopyWithImpl<TypeCandidate>(this as TypeCandidate, _$identity);

  /// Serializes this TypeCandidate to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as TypeCandidate;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is TypeCandidate&&(identical(other.fqcn, _this.fqcn) || other.fqcn == _this.fqcn)&&(identical(other.simpleName, _this.simpleName) || other.simpleName == _this.simpleName)&&(identical(other.confidence, _this.confidence) || other.confidence == _this.confidence)&&const DeepCollectionEquality().equals(other.signals, _this.signals));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as TypeCandidate;
  return Object.hash(runtimeType,_this.fqcn,_this.simpleName,_this.confidence,const DeepCollectionEquality().hash(_this.signals));
}

@override
String toString() {
  final _this = this as TypeCandidate;
  return 'TypeCandidate(fqcn: ${_this.fqcn}, simpleName: ${_this.simpleName}, confidence: ${_this.confidence}, signals: ${_this.signals})';
}


}

/// @nodoc
abstract mixin class $TypeCandidateCopyWith<$Res>  {
  factory $TypeCandidateCopyWith(TypeCandidate value, $Res Function(TypeCandidate) _then) = _$TypeCandidateCopyWithImpl;
@useResult
$Res call({
 String fqcn, String simpleName, double confidence, List<String> signals
});




}
/// @nodoc
class _$TypeCandidateCopyWithImpl<$Res>
    implements $TypeCandidateCopyWith<$Res> {
  _$TypeCandidateCopyWithImpl(this._self, this._then);

  final TypeCandidate _self;
  final $Res Function(TypeCandidate) _then;

/// Create a copy of TypeCandidate
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? fqcn = null,Object? simpleName = null,Object? confidence = null,Object? signals = null,}) {
  return _then(TypeCandidate(
fqcn: null == fqcn ? _self.fqcn : fqcn // ignore: cast_nullable_to_non_nullable
as String,simpleName: null == simpleName ? _self.simpleName : simpleName // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as double,signals: null == signals ? _self.signals : signals // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [TypeCandidate].
extension TypeCandidatePatterns on TypeCandidate {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _TypeCandidate value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _TypeCandidate() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _TypeCandidate value)  $default,){
final _that = this;
switch (_that) {
case _TypeCandidate():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _TypeCandidate value)?  $default,){
final _that = this;
switch (_that) {
case _TypeCandidate() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String fqcn,  String simpleName,  double confidence,  List<String> signals)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _TypeCandidate() when $default != null:
return $default(_that.fqcn,_that.simpleName,_that.confidence,_that.signals);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String fqcn,  String simpleName,  double confidence,  List<String> signals)  $default,) {final _that = this;
switch (_that) {
case _TypeCandidate():
return $default(_that.fqcn,_that.simpleName,_that.confidence,_that.signals);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String fqcn,  String simpleName,  double confidence,  List<String> signals)?  $default,) {final _that = this;
switch (_that) {
case _TypeCandidate() when $default != null:
return $default(_that.fqcn,_that.simpleName,_that.confidence,_that.signals);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _TypeCandidate implements TypeCandidate {
  const _TypeCandidate({required this.fqcn, required this.simpleName, required this.confidence, required  List<String> signals}): _signals = signals;
  factory _TypeCandidate.fromJson(Map<String, dynamic> json) => _$TypeCandidateFromJson(json);

@override final  String fqcn;
@override final  String simpleName;
@override final  double confidence;
 final  List<String> _signals;
@override List<String> get signals {
  if (_signals is EqualUnmodifiableListView) return _signals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_signals);
}


/// Create a copy of TypeCandidate
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$TypeCandidateCopyWith<_TypeCandidate> get copyWith => __$TypeCandidateCopyWithImpl<_TypeCandidate>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$TypeCandidateToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _TypeCandidate&&(identical(other.fqcn, fqcn) || other.fqcn == fqcn)&&(identical(other.simpleName, simpleName) || other.simpleName == simpleName)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&const DeepCollectionEquality().equals(other.signals, _signals));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,fqcn,simpleName,confidence,const DeepCollectionEquality().hash(_signals));
}

@override
String toString() {
    return 'TypeCandidate(fqcn: $fqcn, simpleName: $simpleName, confidence: $confidence, signals: $signals)';
}


}

/// @nodoc
abstract mixin class _$TypeCandidateCopyWith<$Res> implements $TypeCandidateCopyWith<$Res> {
  factory _$TypeCandidateCopyWith(_TypeCandidate value, $Res Function(_TypeCandidate) _then) = __$TypeCandidateCopyWithImpl;
@override @useResult
$Res call({
 String fqcn, String simpleName, double confidence, List<String> signals
});




}
/// @nodoc
class __$TypeCandidateCopyWithImpl<$Res>
    implements _$TypeCandidateCopyWith<$Res> {
  __$TypeCandidateCopyWithImpl(this._self, this._then);

  final _TypeCandidate _self;
  final $Res Function(_TypeCandidate) _then;

/// Create a copy of TypeCandidate
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? fqcn = null,Object? simpleName = null,Object? confidence = null,Object? signals = null,}) {
  return _then(_TypeCandidate(
fqcn: null == fqcn ? _self.fqcn : fqcn // ignore: cast_nullable_to_non_nullable
as String,simpleName: null == simpleName ? _self.simpleName : simpleName // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as double,signals: null == signals ? _self._signals : signals // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}


/// @nodoc
mixin _$ImportWriteResult {

 String? get path; String? get errorMessage;
/// Create a copy of ImportWriteResult
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ImportWriteResultCopyWith<ImportWriteResult> get copyWith => _$ImportWriteResultCopyWithImpl<ImportWriteResult>(this as ImportWriteResult, _$identity);

  /// Serializes this ImportWriteResult to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ImportWriteResult;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ImportWriteResult&&(identical(other.path, _this.path) || other.path == _this.path)&&(identical(other.errorMessage, _this.errorMessage) || other.errorMessage == _this.errorMessage));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ImportWriteResult;
  return Object.hash(runtimeType,_this.path,_this.errorMessage);
}

@override
String toString() {
  final _this = this as ImportWriteResult;
  return 'ImportWriteResult(path: ${_this.path}, errorMessage: ${_this.errorMessage})';
}


}

/// @nodoc
abstract mixin class $ImportWriteResultCopyWith<$Res>  {
  factory $ImportWriteResultCopyWith(ImportWriteResult value, $Res Function(ImportWriteResult) _then) = _$ImportWriteResultCopyWithImpl;
@useResult
$Res call({
 String? path, String? errorMessage
});




}
/// @nodoc
class _$ImportWriteResultCopyWithImpl<$Res>
    implements $ImportWriteResultCopyWith<$Res> {
  _$ImportWriteResultCopyWithImpl(this._self, this._then);

  final ImportWriteResult _self;
  final $Res Function(ImportWriteResult) _then;

/// Create a copy of ImportWriteResult
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? path = freezed,Object? errorMessage = freezed,}) {
  return _then(ImportWriteResult(
path: freezed == path ? _self.path : path // ignore: cast_nullable_to_non_nullable
as String?,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [ImportWriteResult].
extension ImportWriteResultPatterns on ImportWriteResult {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ImportWriteResult value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ImportWriteResult() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ImportWriteResult value)  $default,){
final _that = this;
switch (_that) {
case _ImportWriteResult():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ImportWriteResult value)?  $default,){
final _that = this;
switch (_that) {
case _ImportWriteResult() when $default != null:
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
case _ImportWriteResult() when $default != null:
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
case _ImportWriteResult():
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
case _ImportWriteResult() when $default != null:
return $default(_that.path,_that.errorMessage);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ImportWriteResult implements ImportWriteResult {
  const _ImportWriteResult({this.path, this.errorMessage});
  factory _ImportWriteResult.fromJson(Map<String, dynamic> json) => _$ImportWriteResultFromJson(json);

@override final  String? path;
@override final  String? errorMessage;

/// Create a copy of ImportWriteResult
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ImportWriteResultCopyWith<_ImportWriteResult> get copyWith => __$ImportWriteResultCopyWithImpl<_ImportWriteResult>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ImportWriteResultToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ImportWriteResult&&(identical(other.path, path) || other.path == path)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,path,errorMessage);
}

@override
String toString() {
    return 'ImportWriteResult(path: $path, errorMessage: $errorMessage)';
}


}

/// @nodoc
abstract mixin class _$ImportWriteResultCopyWith<$Res> implements $ImportWriteResultCopyWith<$Res> {
  factory _$ImportWriteResultCopyWith(_ImportWriteResult value, $Res Function(_ImportWriteResult) _then) = __$ImportWriteResultCopyWithImpl;
@override @useResult
$Res call({
 String? path, String? errorMessage
});




}
/// @nodoc
class __$ImportWriteResultCopyWithImpl<$Res>
    implements _$ImportWriteResultCopyWith<$Res> {
  __$ImportWriteResultCopyWithImpl(this._self, this._then);

  final _ImportWriteResult _self;
  final $Res Function(_ImportWriteResult) _then;

/// Create a copy of ImportWriteResult
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? path = freezed,Object? errorMessage = freezed,}) {
  return _then(_ImportWriteResult(
path: freezed == path ? _self.path : path // ignore: cast_nullable_to_non_nullable
as String?,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on
