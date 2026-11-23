import 'package:appbase_config_wizard/shared/widgets/appbase_widget_constants.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import '../providers/config_providers.dart';
import '../../../shared/providers/output_directory_provider.dart';

/// Output screen — displays the generated YAML with copy and write-to-file actions.
///
/// The write action is gated here so users always review the YAML preview
/// before committing to disk (mirrors the config wizard console's askWrite() prompt).
class YamlOutputScreen extends ConsumerStatefulWidget {
  /// Fully qualified configuration type represented by the generated YAML.
  final String fqcn;

  /// Build response to render, including any server-reported failure.
  final BuildResult? buildResult;

  /// Values used for a subsequent write request; null disables writing.
  final Map<String, dynamic>? fieldValues;

  /// Creates a preview for [buildResult] and its source [fieldValues].
  const YamlOutputScreen({
    super.key,
    required this.fqcn,
    required this.buildResult,
    this.fieldValues,
  });

  @override
  ConsumerState<YamlOutputScreen> createState() => _YamlOutputScreenState();
}

class _YamlOutputScreenState extends ConsumerState<YamlOutputScreen> {
  bool _writing = false;

  @override
  Widget build(BuildContext context) {
    final result = widget.buildResult;
    final hasError = result?.errorMessage != null;
    final yaml = result?.yaml ?? '';
    final canWrite = !hasError && yaml.isNotEmpty && widget.fieldValues != null;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Generated YAML'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        actions: [
          if (!hasError && yaml.isNotEmpty)
            OutlinedButton.icon(
              onPressed: () => _copyToClipboard(context, yaml),
              icon: const Icon(Icons.copy, size: 16),
              label: const Text('Copy'),
            ),
          if (canWrite)
            Padding(
              padding: const EdgeInsets.only(left: 8, right: 4),
              child: _writing
                  ? const SizedBox(
                      width: 32,
                      height: 32,
                      child: CircularProgressIndicator(strokeWidth: 3),
                    )
                  : FilledButton.icon(
                      onPressed: _handleWrite,
                      icon: const Icon(Icons.save, size: 16),
                      label: const Text('Write to File'),
                    ),
            ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: hasError
            ? _buildError(context, result!.errorMessage!)
            : _buildYaml(context, yaml),
      ),
    );
  }

  Widget _buildError(BuildContext context, String message) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(Icons.error_outline,
            color: Theme.of(context).colorScheme.error, size: 32),
        const SizedBox(height: 8),
        Text(
          'Build failed',
          style: Theme.of(context)
              .textTheme
              .titleMedium
              ?.copyWith(color: Theme.of(context).colorScheme.error),
        ),
        const SizedBox(height: 8),
        Expanded(
          child: SingleChildScrollView(
            child: SelectableText(
              message,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildYaml(BuildContext context, String yaml) {
    if (yaml.isEmpty) {
      return const Center(child: Text('No YAML generated.'));
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Configuration for ${widget.fqcn}',
          style: Theme.of(context)
              .textTheme
              .titleSmall
              ?.copyWith(fontStyle: FontStyle.italic),
        ),
        const SizedBox(height: 12),
        Expanded(
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                  color: Theme.of(context).colorScheme.outlineVariant),
            ),
            child: SingleChildScrollView(
              child: SelectableText(
                yaml,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _handleWrite() async {
    setState(() => _writing = true);
    try {
      final outputDir = ref.read(outputDirectoryProvider);
      await ref
          .read(writeResultNotifierProvider.notifier)
          .writeConfig(widget.fqcn, Map<String, dynamic>.from(widget.fieldValues!),
              outputPath: outputDir);

      if (mounted) {
        final result = ref.read(writeResultNotifierProvider).value;
        final msg = result?.errorMessage != null
            ? 'Error: ${result!.errorMessage}'
            : 'Written to: ${result?.path ?? 'unknown'}';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(msg),
            duration: AppbaseWidgetConstants.snackBarDurationMillis,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _writing = false);
    }
  }

  Future<void> _copyToClipboard(BuildContext context, String text) async {
    await Clipboard.setData(ClipboardData(text: text));
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('YAML copied to clipboard'),
          duration: AppbaseWidgetConstants.snackBarDurationMillis,
        ),
      );
    }
  }
}
