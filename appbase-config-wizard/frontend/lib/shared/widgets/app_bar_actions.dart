import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/output_directory_provider.dart';
import 'theme_toggle.dart';

/// Returns the standard AppBar actions shared across all config wizard screens:
/// output-directory folder icon and light/dark theme toggle.
List<Widget> sharedAppBarActions(BuildContext context, WidgetRef ref) => [
      IconButton(
        icon: const Icon(Icons.folder_open),
        tooltip: 'Set output directory',
        onPressed: () => _showOutputDirDialog(context, ref),
      ),
      const ThemeToggle(),
      const SizedBox(width: 8),
    ];

Future<void> _showOutputDirDialog(BuildContext context, WidgetRef ref) async {
  final current = ref.read(outputDirectoryProvider);
  final controller = TextEditingController(text: current ?? '');
  await showDialog<void>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('Output Directory'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Directory where YAML files are written.\n'
            'Leave blank to use the server default (system temp).',
            style: TextStyle(fontSize: 13),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: controller,
            decoration: const InputDecoration(
              labelText: 'Directory path',
              hintText: '/home/user/configs',
              border: OutlineInputBorder(),
            ),
            autofocus: true,
            onSubmitted: (_) {
              _applyDir(ctx, ref, controller.text);
            },
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => _applyDir(ctx, ref, controller.text),
          child: const Text('Apply'),
        ),
      ],
    ),
  );
  controller.dispose();
}

void _applyDir(BuildContext ctx, WidgetRef ref, String value) {
  final dir = value.trim();
  ref.read(outputDirectoryProvider.notifier).state = dir.isEmpty ? null : dir;
  Navigator.of(ctx).pop();
}
