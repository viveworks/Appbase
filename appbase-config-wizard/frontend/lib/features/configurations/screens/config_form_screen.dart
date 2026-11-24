import 'package:appbase_config_wizard/shared/widgets/appbase_progress_indicator_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import '../providers/config_providers.dart';
import '../widgets/field_input.dart';
import 'yaml_output_screen.dart';

/// Form screen — shows all fields of the selected config class.
///
/// The user fills in values then taps "Build Config" to preview the YAML.
/// Writing to disk is confirmed from the preview screen.
class ConfigFormScreen extends ConsumerStatefulWidget {
  /// Configuration selected by the user and loaded when this screen opens.
  final ConfigSummary summary;

  /// Creates a form for [summary].
  const ConfigFormScreen({super.key, required this.summary});

  @override
  ConsumerState<ConfigFormScreen> createState() => _ConfigFormScreenState();
}

class _ConfigFormScreenState extends ConsumerState<ConfigFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final Map<String, dynamic> _values = {};
  bool _building = false;

  void _handleBack() => Navigator.of(context).pop();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await ref
          .read(configDetailNotifierProvider.notifier)
          .load(widget.summary.fqcn);
      await _seedDefaults();
    });
  }

  Future<void> _seedDefaults() async {
    try {
      final defaults = await ref
          .read(apiClientProvider)
          .getDefaultValues(widget.summary.fqcn);
      for (final entry in defaults.entries) {
        // Keep native types (Map for nested configs, int/bool for primitives).
        // Storing them as their native type lets FieldInput correctly detect
        // nested Maps via `widget.value is Map` rather than seeing a string.
        _values.putIfAbsent(entry.key, () => entry.value);
      }
    } catch (_) {
      // Fall back to metadata defaults if the server call fails
      final detail = ref.read(configDetailNotifierProvider).value;
      if (detail != null) {
        for (final field in detail.fields) {
          if (!_values.containsKey(field.name) && field.defaultValue != null) {
            _values[field.name] = field.defaultValue;
          }
        }
      }
    }
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final detailAsync = ref.watch(configDetailNotifierProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.summary.simpleName),
        // Flutter Web's accessibility tree renders a button's accessible name
        // as DOM text content, not the `aria-label` HTML attribute, UNLESS
        // the semantics node genuinely has children (engine
        // lib/web_ui/.../semantics/label_and_value.dart,
        // LabelAndValue._getEffectiveRepresentation:
        // `semanticsObject.hasChildren ? ariaLabel : preferredRepresentation`).
        // An earlier version of this wrapper used `excludeSemantics: true`,
        // which strips the IconButton's own semantics and leaves this node
        // with zero children, so it fell through to domText instead —
        // confirmed directly against the rendered semantics tree
        // (`aria-label=null`) by test/web/tests/browser-back.spec.ts. Keeping
        // the IconButton's own semantics as a real child (no exclusion) makes
        // `hasChildren` true, so this label reaches `aria-label` on web.
        leading: Semantics(
          label: 'Back to configurations',
          button: true,
          onTap: _handleBack,
          child: IconButton(
            icon: const Icon(Icons.arrow_back),
            tooltip: 'Back to configurations',
            onPressed: _handleBack,
          ),
        ),
        actions: const [],
      ),
      body: detailAsync.when(
        loading: () => const AppbaseProgressIndicatorWidget(),
        error: (err, _) => Center(
          child: Text(
            'Error loading config: $err',
            style: TextStyle(color: Theme.of(context).colorScheme.error),
          ),
        ),
        data: (detail) {
          if (detail == null) {
            return const Center(child: Text('No config selected.'));
          }
          return _buildForm(detail);
        },
      ),
    );
  }

  Widget _buildForm(ConfigDetail detail) {
    return Form(
      key: _formKey,
      child: Column(
        children: [
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.all(20),
              itemCount: detail.fields.length,
              separatorBuilder: (_, __) => const SizedBox(height: 16),
              itemBuilder: (context, index) {
                final field = detail.fields[index];
                return FieldInput(
                  field: field,
                  breadcrumb: [widget.summary.simpleName],
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
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                OutlinedButton(
                  onPressed: _handleReset,
                  child: const Text('Reset'),
                ),
                const SizedBox(width: 12),
                _building
                    ? const SizedBox(
                        width: 32,
                        height: 32,
                        child: CircularProgressIndicator(strokeWidth: 3),
                      )
                    : FilledButton.icon(
                        onPressed: () => _handleBuild(detail),
                        icon: const Icon(Icons.build, size: 18),
                        label: const Text('Build Config'),
                      ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _handleReset() async {
    setState(() => _values.clear());
    await _seedDefaults();
  }

  Future<void> _handleBuild(ConfigDetail detail) async {
    setState(() => _building = true);
    BuildResult? result;
    try {
      // Call the API directly to avoid the auto-dispose race: buildResultNotifierProvider
      // has no listeners, so Riverpod can dispose it mid-flight, resetting state to
      // AsyncLoading and making state.value return null.
      result = await ref
          .read(apiClientProvider)
          .buildConfig(detail.fqcn, Map<String, dynamic>.from(_values));
    } catch (e) {
      result = BuildResult(yaml: null, errorMessage: e.toString());
    } finally {
      if (mounted) setState(() => _building = false);
    }
    if (!mounted) return;
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => YamlOutputScreen(
          fqcn: detail.fqcn,
          buildResult: result,
          fieldValues: Map<String, dynamic>.from(_values),
        ),
      ),
    );
  }
}
