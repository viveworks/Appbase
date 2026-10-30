import 'package:appbase_config_wizard/shared/widgets/appbase_progress_indicator_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import '../providers/config_providers.dart';
import '../widgets/field_input.dart';

/// Sub-form screen for editing a nested Appbase configuration object.
///
/// Loaded when the user taps "Set Values" on a nested field in [ConfigFormScreen].
/// Pops with [Map<String, dynamic>] on "Apply" or `null` on "Cancel".
///
/// The [breadcrumb] list shows the path leading to this sub-form; the current
/// field ([fieldName]) is appended when rendering the AppBar title.
class NestedConfigFormScreen extends ConsumerStatefulWidget {
  /// Labels of the parent configuration objects leading to this field.
  final List<String> breadcrumb;

  /// Name of the field being edited at this level.
  final String fieldName;

  /// Fully qualified type loaded to obtain this nested object's fields.
  final String nestedTypeFqcn;

  /// Existing values used to seed the sub-form, or null for defaults.
  final Map<String, dynamic>? initialValues;

  /// Creates an editor that returns the nested values through the navigator.
  const NestedConfigFormScreen({
    super.key,
    required this.breadcrumb,
    required this.fieldName,
    required this.nestedTypeFqcn,
    this.initialValues,
  });

  @override
  ConsumerState<NestedConfigFormScreen> createState() =>
      _NestedConfigFormScreenState();
}

class _NestedConfigFormScreenState
    extends ConsumerState<NestedConfigFormScreen> {
  ConfigDetail? _detail;
  String? _error;
  final Map<String, dynamic> _values = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final detail =
          await ref.read(apiClientProvider).getConfig(widget.nestedTypeFqcn);
      if (!mounted) return;
      setState(() {
        _detail = detail;
        if (widget.initialValues != null) {
          _values.addAll(widget.initialValues!);
        } else {
          for (final field in detail.fields) {
            if (field.defaultValue != null) {
              _values[field.name] = field.defaultValue;
            }
          }
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    }
  }

  List<String> get _fullCrumbs => [...widget.breadcrumb, widget.fieldName];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          tooltip: 'Cancel',
          onPressed: () => Navigator.of(context).pop(null),
        ),
        title: _BreadcrumbTitle(
          crumbs: _fullCrumbs,
          onCrumbTap: (index) {
            // Pop back to the tapped level: last crumb = this screen (0 pops),
            // second-to-last = parent (1 pop), etc.
            final pops = _fullCrumbs.length - 1 - index;
            final nav = Navigator.of(context);
            for (int i = 0; i < pops; i++) {
              if (nav.canPop()) nav.pop(null);
            }
          },
        ),
        actions: const [],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_error != null) {
      return Center(
        child: Text(
          'Error loading nested config: $_error',
          style: TextStyle(color: Theme.of(context).colorScheme.error),
        ),
      );
    }
    if (_detail == null) {
      return const Center(child: AppbaseProgressIndicatorWidget());
    }
    return Column(
      children: [
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.all(20),
            itemCount: _detail!.fields.length,
            separatorBuilder: (_, __) => const SizedBox(height: 16),
            itemBuilder: (context, i) {
              final field = _detail!.fields[i];
              return FieldInput(
                field: field,
                breadcrumb: _fullCrumbs,
                value: _values[field.name],
                onChanged: (v) => setState(() => _values[field.name] = v),
              );
            },
          ),
        ),
        const Divider(height: 1),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              OutlinedButton(
                onPressed: () => Navigator.of(context).pop(null),
                child: const Text('Cancel'),
              ),
              const SizedBox(width: 12),
              FilledButton.icon(
                onPressed: () => Navigator.of(context)
                    .pop(Map<String, dynamic>.from(_values)),
                icon: const Icon(Icons.check, size: 18),
                label: const Text('Apply'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Horizontal breadcrumb trail rendered in the AppBar title.
///
/// Non-last segments are tappable when [onCrumbTap] is provided;
/// tapping a segment at [index] navigates back to that level.
class _BreadcrumbTitle extends StatelessWidget {
  final List<String> crumbs;

  /// Called with the index of the tapped crumb (0-based).
  /// Null = no tapping (all segments are plain text).
  final void Function(int index)? onCrumbTap;

  const _BreadcrumbTitle({required this.crumbs, this.onCrumbTap});

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.titleMedium;
    final dimStyle = style?.copyWith(
      color: Theme.of(context).colorScheme.onSurfaceVariant,
    );
    final linkStyle = dimStyle?.copyWith(
      decoration: TextDecoration.underline,
      decorationColor: Theme.of(context).colorScheme.onSurfaceVariant,
    );

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: crumbs.asMap().entries.expand((e) {
          final isLast = e.key == crumbs.length - 1;
          final Widget label = isLast
              ? Text(e.value, style: style, overflow: TextOverflow.ellipsis)
              : onCrumbTap != null
                  ? InkWell(
                      onTap: () => onCrumbTap!(e.key),
                      borderRadius: BorderRadius.circular(4),
                      child: Text(
                        e.value,
                        style: linkStyle,
                        overflow: TextOverflow.ellipsis,
                      ),
                    )
                  : Text(e.value, style: dimStyle, overflow: TextOverflow.ellipsis);

          return [
            if (e.key > 0)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Text('›', style: dimStyle),
              ),
            label,
          ];
        }).toList(),
      ),
    );
  }
}
