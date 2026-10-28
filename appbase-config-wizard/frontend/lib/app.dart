import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'shared/providers/theme_provider.dart';
import 'shared/widgets/config_wizard_shell.dart';

/// Root application widget.
///
/// Reads [themeModeProvider] to drive [MaterialApp.themeMode] so that the
/// [ThemeToggle] widget can update the theme from anywhere in the tree.
class ConfigWizardApp extends ConsumerStatefulWidget {
  /// Creates the root widget for the configuration wizard.
  const ConfigWizardApp({super.key});

  @override
  ConsumerState<ConfigWizardApp> createState() => _ConfigWizardAppState();
}

class _ConfigWizardAppState extends ConsumerState<ConfigWizardApp> {
  @override
  Widget build(BuildContext context) {
    final themeMode = ref.watch(themeModeProvider);
    return MaterialApp(
      title: 'Appbase Config Wizard',
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: _lightTheme(),
      darkTheme: _darkTheme(),
      // ⚠️ SelectionArea GOES UNDER THE NAVIGATOR, NOT AROUND IT (internal work item 611 / N08). This was
      // `builder: (context, child) => SelectionArea(child: child!)`, and MaterialApp's
      // builder wraps the NAVIGATOR — which is the widget that owns the Overlay. So
      // SelectionArea sat ABOVE the Overlay and its SelectableRegion could never find one:
      //
      //     No Overlay widget found.
      //     SelectableRegion widgets require an Overlay widget ancestor within the
      //     closest LookupBoundary.
      //
      // That is a runtime assertion, not a test artefact — the app throws while building.
      // It surfaced only when the frontend tests were first run inside the container for
      // internal work item 611; before that they could not even compile (the dart:html break), so nothing
      // had ever built this tree.
      //
      // Wrapping `home` puts selection inside the Navigator's route, below the Overlay.
      // ⚠️ Selection is therefore per-ROUTE, not global. `home` is the only route this app
      // pushes, so the behaviour is unchanged today; a future pushed route that wants
      // selectable text must wrap itself the same way rather than relying on an ancestor.
      home: const SelectionArea(child: ConfigWizardShell()),
    );
  }
}

ThemeData _lightTheme() {
  const seed = Color(0xFF1565C0);
  final cs = ColorScheme.fromSeed(seedColor: seed, brightness: Brightness.light);
  return ThemeData(
    useMaterial3: true,
    colorScheme: cs,
    appBarTheme: AppBarTheme(
      elevation: 0,
      titleTextStyle: TextStyle(
        color: cs.onSurface,
        fontSize: 18,
        fontWeight: FontWeight.w600,
      ),
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: cs.surfaceContainerLowest,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: cs.outlineVariant, width: 1),
      ),
    ),
    dividerTheme: DividerThemeData(color: cs.outlineVariant, thickness: 1),
    inputDecorationTheme: InputDecorationTheme(
      border: const OutlineInputBorder(),
      labelStyle: TextStyle(color: cs.onSurfaceVariant, fontWeight: FontWeight.w500),
      floatingLabelStyle: TextStyle(color: cs.primary, fontWeight: FontWeight.w600),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
    ),
    listTileTheme: ListTileThemeData(
      titleTextStyle: TextStyle(
        color: cs.onSurface,
        fontWeight: FontWeight.w600,
        fontSize: 15,
      ),
      subtitleTextStyle: TextStyle(
        color: cs.onSurfaceVariant,
        fontSize: 11,
      ),
    ),
    tabBarTheme: TabBarThemeData(
      labelColor: cs.primary,
      unselectedLabelColor: cs.onSurfaceVariant,
      indicatorColor: cs.primary,
      dividerColor: Colors.transparent,
    ),
  );
}

ThemeData _darkTheme() {
  const seed = Color(0xFF1565C0);
  final cs = ColorScheme.fromSeed(seedColor: seed, brightness: Brightness.dark);
  return ThemeData(
    useMaterial3: true,
    colorScheme: cs,
    scaffoldBackgroundColor: const Color(0xFF0D1117),
    appBarTheme: AppBarTheme(
      elevation: 0,
      titleTextStyle: TextStyle(
        color: cs.onSurface,
        fontSize: 18,
        fontWeight: FontWeight.w600,
      ),
    ),
    textTheme: const TextTheme(
      displayLarge: TextStyle(color: Colors.white),
      displayMedium: TextStyle(color: Colors.white),
      displaySmall: TextStyle(color: Colors.white),
      headlineLarge: TextStyle(color: Colors.white),
      headlineMedium: TextStyle(color: Colors.white),
      headlineSmall: TextStyle(color: Colors.white),
      titleLarge: TextStyle(color: Colors.white),
      titleMedium: TextStyle(color: Colors.white),
      titleSmall: TextStyle(color: Color(0xFFDDDDDD)),
      bodyLarge: TextStyle(color: Color(0xFFE0E0E0)),
      bodyMedium: TextStyle(color: Color(0xFFE0E0E0)),
      bodySmall: TextStyle(color: Color(0xFFBBBBCC)),
      labelLarge: TextStyle(color: Colors.white),
      labelMedium: TextStyle(color: Color(0xFFDDDDDD)),
      labelSmall: TextStyle(color: Color(0xFFBBBBCC)),
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: const Color(0xFF1A2230),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: cs.outlineVariant, width: 1),
      ),
    ),
    dividerTheme: DividerThemeData(color: cs.outlineVariant, thickness: 1),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: const Color(0xFF1565C0),
        foregroundColor: Colors.white,
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF1565C0),
        foregroundColor: Colors.white,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: const Color(0xFF90CAF9),
        side: const BorderSide(color: Color(0xFF90CAF9)),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: const Color(0xFF90CAF9),
      ),
    ),
    listTileTheme: const ListTileThemeData(
      titleTextStyle: TextStyle(
        color: Colors.white,
        fontWeight: FontWeight.w600,
        fontSize: 15,
      ),
      subtitleTextStyle: TextStyle(
        color: Color(0xFFAAAAAA),
        fontSize: 11,
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: const OutlineInputBorder(),
      labelStyle: const TextStyle(
        color: Color(0xFFBBBBCC),
        fontWeight: FontWeight.w500,
      ),
      floatingLabelStyle: const TextStyle(
        color: Color(0xFF90CAF9),
        fontWeight: FontWeight.w600,
      ),
      hintStyle: const TextStyle(color: Color(0xFF777788)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
      enabledBorder: OutlineInputBorder(
        borderSide: BorderSide(color: cs.outlineVariant),
      ),
    ),
    tabBarTheme: TabBarThemeData(
      labelColor: cs.primary,
      unselectedLabelColor: cs.onSurfaceVariant,
      indicatorColor: cs.primary,
      dividerColor: Colors.transparent,
    ),
  );
}
