import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter/material.dart';

/// Tracks the current [ThemeMode] for the config wizard UI.
final themeModeProvider = StateProvider<ThemeMode>((ref) => ThemeMode.system);
