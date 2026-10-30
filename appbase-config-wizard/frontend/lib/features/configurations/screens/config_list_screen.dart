import 'package:appbase_config_wizard/shared/widgets/appbase_progress_indicator_widget.dart';
import 'package:appbase_config_wizard/shared/widgets/appbase_widget_constants.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import '../providers/config_providers.dart';
import '../storage/configuration_filter_storage.dart';
import 'config_form_screen.dart';

/// Config list screen — shows the searchable list of all configuration classes.
///
/// The user picks a class to navigate to [ConfigFormScreen].
class ConfigListScreen extends ConsumerStatefulWidget {
  /// Creates the searchable configuration catalogue screen.
  const ConfigListScreen({super.key});

  @override
  ConsumerState<ConfigListScreen> createState() => _ConfigListScreenState();
}

class _ConfigListScreenState extends ConsumerState<ConfigListScreen> {
  late final TextEditingController _searchController;
  final _searchFocus = FocusNode();
  final _listFocus = FocusNode();
  late String _filter;
  int _keyboardCursor = -1;
  int _restorePublicationGeneration = 0;

  @override
  void initState() {
    super.initState();
    _filter = readConfigurationFilter();
    _searchController = TextEditingController();
    WidgetsBinding.instance.addSemanticsEnabledListener(
      _handleSemanticsEnabledChanged,
    );
    _scheduleRestoredFilterPublication(
      reconnectFocus: WidgetsBinding.instance.semanticsEnabled,
    );
  }

  void _handleSemanticsEnabledChanged() {
    if (!WidgetsBinding.instance.semanticsEnabled || _filter.isEmpty) return;
    // CanvasKit creates a separate SemanticTextField when accessibility is
    // enabled. Reconnect so that new field, rather than only the earlier
    // FLT-TEXT-EDITING-HOST, receives the restored editing state.
    _scheduleRestoredFilterPublication(reconnectFocus: true);
  }

  void _scheduleRestoredFilterPublication({required bool reconnectFocus}) {
    if (_filter.isEmpty) return;
    final generation = ++_restorePublicationGeneration;
    if (reconnectFocus) _searchFocus.unfocus();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || generation != _restorePublicationGeneration) return;
      _searchFocus.requestFocus();
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || generation != _restorePublicationGeneration) return;
        setState(() {
          _searchController.value = TextEditingValue(
            text: _filter,
            selection: TextSelection.collapsed(offset: _filter.length),
          );
        });
      });
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeSemanticsEnabledListener(
      _handleSemanticsEnabledChanged,
    );
    _searchController.dispose();
    _searchFocus.dispose();
    _listFocus.dispose();
    super.dispose();
  }

  List<ConfigSummary> _applyFilter(List<ConfigSummary> all) {
    if (_filter.isEmpty) return all;
    final q = _filter.toLowerCase();
    return all.where((c) {
      return c.simpleName.toLowerCase().contains(q) ||
          c.fqcn.toLowerCase().contains(q) ||
          c.layer.toLowerCase().contains(q) ||
          c.module.toLowerCase().contains(q);
    }).toList();
  }

  void _moveCursor(int delta, int listSize) {
    if (listSize == 0) return;
    setState(() {
      _keyboardCursor = (_keyboardCursor + delta).clamp(0, listSize - 1);
    });
  }

  @override
  Widget build(BuildContext context) {
    final configsAsync = ref.watch(configListProvider);

    return Padding(
      padding: AppbaseWidgetConstants.formPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── Search box with ↓ key to enter list navigation ──────────────
          KeyboardListener(
            focusNode: FocusNode(),
            onKeyEvent: (event) {
              if (event is KeyDownEvent &&
                  event.logicalKey == LogicalKeyboardKey.arrowDown) {
                configsAsync.whenData((configs) {
                  final filtered = _applyFilter(configs);
                  if (filtered.isNotEmpty) {
                    setState(() => _keyboardCursor = 0);
                    _listFocus.requestFocus();
                  }
                });
              }
            },
            child: TextField(
              controller: _searchController,
              focusNode: _searchFocus,
              decoration: const InputDecoration(
                labelText: 'Search configurations…',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
              onChanged: (v) {
                writeConfigurationFilter(v);
                setState(() {
                  _filter = v;
                  _keyboardCursor = -1;
                });
              },
            ),
          ),
          const SizedBox(height: AppbaseWidgetConstants.formElementsSpace),
          // ── Config list with keyboard navigation ─────────────────────────
          Expanded(
            child: configsAsync.when(
              loading: () => const AppbaseProgressIndicatorWidget(),
              error: (err, _) => Center(
                child: Text('Error loading configs: $err',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.error)),
              ),
              data: (configs) {
                final filtered = _applyFilter(configs);
                if (filtered.isEmpty) {
                  return const Center(
                      child: Text('No matching configurations.'));
                }
                return Focus(
                  focusNode: _listFocus,
                  onKeyEvent: (node, event) {
                    if (event is! KeyDownEvent) return KeyEventResult.ignored;
                    final key = event.logicalKey;
                    if (key == LogicalKeyboardKey.arrowDown) {
                      _moveCursor(1, filtered.length);
                      return KeyEventResult.handled;
                    }
                    if (key == LogicalKeyboardKey.arrowUp) {
                      if (_keyboardCursor <= 0) {
                        setState(() => _keyboardCursor = -1);
                        _searchFocus.requestFocus();
                        return KeyEventResult.handled;
                      }
                      _moveCursor(-1, filtered.length);
                      return KeyEventResult.handled;
                    }
                    if (key == LogicalKeyboardKey.enter &&
                        _keyboardCursor >= 0 &&
                        _keyboardCursor < filtered.length) {
                      _selectConfig(filtered[_keyboardCursor]);
                      return KeyEventResult.handled;
                    }
                    if (key == LogicalKeyboardKey.escape) {
                      setState(() => _keyboardCursor = -1);
                      _searchFocus.requestFocus();
                      return KeyEventResult.handled;
                    }
                    return KeyEventResult.ignored;
                  },
                  child: ListView.separated(
                    itemCount: filtered.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final c = filtered[index];
                      final isDark = Theme.of(context).brightness == Brightness.dark;
                      final layerColor = isDark
                          ? const Color(0xFFA5D6A7)
                          : Theme.of(context).colorScheme.primary;
                      final moduleColor = isDark
                          ? const Color(0xFF80DEEA)
                          : Theme.of(context).colorScheme.secondary;
                      final isHighlighted = index == _keyboardCursor;
                      return ListTile(
                        selected: isHighlighted,
                        selectedTileColor: Theme.of(context)
                            .colorScheme
                            .primaryContainer
                            .withAlpha(80),
                        title: Text(c.simpleName,
                            style: TextStyle(
                                fontWeight: FontWeight.w600,
                                fontSize: 15,
                                color: isDark
                                    ? Colors.white
                                    : Theme.of(context)
                                        .colorScheme
                                        .onSurface)),
                        subtitle: Text(c.fqcn,
                            style: TextStyle(
                                fontSize: 12,
                                color: isDark
                                    ? const Color(0xFFBBBBCC)
                                    : Theme.of(context)
                                        .colorScheme
                                        .onSurfaceVariant)),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            if (isHighlighted)
                              const Icon(Icons.keyboard_return, size: 16),
                            const SizedBox(width: 4),
                            _badge(context, c.layer, layerColor),
                            const SizedBox(width: 4),
                            _badge(context, c.module, moduleColor),
                          ],
                        ),
                        onTap: () => _selectConfig(c),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _badge(BuildContext context, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withAlpha(30),
        border: Border.all(color: color.withAlpha(80)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(label,
          style: TextStyle(
              fontSize: 12, color: color, fontWeight: FontWeight.w600)),
    );
  }

  Future<void> _selectConfig(ConfigSummary summary) async {
    if (mounted) {
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => ConfigFormScreen(summary: summary),
        ),
      );
    }
  }
}
