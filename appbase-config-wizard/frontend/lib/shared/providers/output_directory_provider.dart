import 'package:flutter_riverpod/legacy.dart';

/// Session-level output directory for YAML writes; null means server default.
final outputDirectoryProvider = StateProvider<String?>((ref) => null);
