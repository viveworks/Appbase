import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/theme_provider.dart';

/// Icon-button that cycles through [ThemeMode.system], [ThemeMode.light],
/// and [ThemeMode.dark] and updates [themeModeProvider].
class ThemeToggle extends ConsumerWidget {
  /// Creates a button bound to the nearest [themeModeProvider].
  const ThemeToggle({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mode = ref.watch(themeModeProvider);
    return IconButton(
      tooltip: _tooltip(mode),
      icon: Icon(_icon(mode)),
      onPressed: () => ref.read(themeModeProvider.notifier).state = _next(mode),
    );
  }

  IconData _icon(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.light:
        return Icons.light_mode;
      case ThemeMode.dark:
        return Icons.dark_mode;
      case ThemeMode.system:
        return Icons.brightness_auto;
    }
  }

  String _tooltip(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.light:
        return 'Light mode (click for dark)';
      case ThemeMode.dark:
        return 'Dark mode (click for system)';
      case ThemeMode.system:
        return 'System theme (click for light)';
    }
  }

  ThemeMode _next(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.system:
        return ThemeMode.light;
      case ThemeMode.light:
        return ThemeMode.dark;
      case ThemeMode.dark:
        return ThemeMode.system;
    }
  }
}
