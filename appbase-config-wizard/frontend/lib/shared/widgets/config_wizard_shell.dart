import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/configurations/screens/config_list_screen.dart';
import '../../features/configurations/screens/import_screen.dart';
import 'app_bar_actions.dart';

class _TabEntry {
  const _TabEntry({required this.label, required this.icon});
  final String label;
  final IconData icon;
}

/// Root shell widget with persistent AppBar and TabBar.
class ConfigWizardShell extends ConsumerStatefulWidget {
  /// Creates the shell that owns top-level navigation and shared actions.
  const ConfigWizardShell({super.key});

  @override
  ConsumerState<ConfigWizardShell> createState() => _ConfigWizardShellState();
}

class _ConfigWizardShellState extends ConsumerState<ConfigWizardShell>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  final _configurationsNavigatorKey = GlobalKey<NavigatorState>();

  static const _tabs = [
    _TabEntry(label: 'Configurations', icon: Icons.settings),
    // future: _TabEntry(label: 'Deploy', icon: Icons.rocket_launch),
  ];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _tabs.length, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _navigateToImport() {
    _configurationsNavigatorKey.currentState?.push(
      MaterialPageRoute(builder: (_) => const ImportScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Appbase Config Wizard',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 22)),
            Text('powered by Appbase',
                style: TextStyle(
                  fontSize: 11,
                  color: Theme.of(context)
                      .colorScheme
                      .onSurface
                      .withAlpha(160),
                )),
          ],
        ),
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          tabs: _tabs.map((t) => Tab(icon: Icon(t.icon), text: t.label)).toList(),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.upload_file),
            tooltip: 'Import YAML configurations',
            onPressed: _navigateToImport,
          ),
          ...sharedAppBarActions(context, ref),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          NavigatorPopHandler<void>(
            onPopWithResult: (_) =>
                _configurationsNavigatorKey.currentState?.pop(),
            child: Navigator(
              key: _configurationsNavigatorKey,
              onGenerateRoute: (settings) => MaterialPageRoute(
                builder: (_) => const ConfigListScreen(),
              ),
            ),
          ),
          // future tab bodies added here
        ],
      ),
    );
  }
}
