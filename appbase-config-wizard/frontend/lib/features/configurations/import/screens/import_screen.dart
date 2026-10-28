
import 'package:flutter/material.dart';

import '../directory_picker.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../models/models.dart';
import '../../providers/config_providers.dart';
import '../../widgets/field_input.dart';
import '../../../../shared/providers/output_directory_provider.dart';
import '../../../../shared/widgets/app_bar_actions.dart';
import '../models/models.dart';
import '../providers/import_providers.dart';

/// Import screen — horizontal panel split: file list (left) + property editor (right).
///
/// **Left panel (FILES):**
/// - Directory path TextField + Scan button
/// - Scrollable list of discovered YAML files; each tile shows a confidence badge
///   (green ≥ 70 %, amber < 70 %).
///
/// **Right panel (EDITOR):**
/// - If multiple [TypeCandidate]s exist, a dropdown lets the user pick one.
/// - [FieldInput] widgets for each field of the resolved [ConfigDetail].
/// - Save button that opens a three-option save dialog (overwrite / session dir
///   / custom path), matching the spec (Section 5.2).
///
/// Nested object fields open [NestedConfigFormScreen] exactly as in the create flow.
class ImportScreen extends ConsumerStatefulWidget {
  /// Creates the YAML discovery and import workspace.
  const ImportScreen({super.key});

  @override
  ConsumerState<ImportScreen> createState() => _ImportScreenState();
}

class _ImportScreenState extends ConsumerState<ImportScreen> {
  final _dirController = TextEditingController();
  final _customPathController = TextEditingController();

  FileEntry? _selectedFile;
  TypeCandidate? _selectedType;
  final Map<String, dynamic> _values = {};

  ConfigDetail? _detail;
  bool _loadingDetail = false;
  String? _detailError;

  /// Monotonic token guarding [_selectType]'s async completion against a
  /// stale, out-of-order response (A10-03, internal work item 1449/internal work item 1492) — see that method.
  int _typeLoadToken = 0;

  @override
  void dispose() {
    _dirController.dispose();
    _customPathController.dispose();
    super.dispose();
  }

  // ── Build ─────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Import YAML Configurations'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        actions: sharedAppBarActions(context, ref),
      ),
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(child: _buildFilePanel()),
          const VerticalDivider(width: 1, thickness: 1),
          Expanded(flex: 2, child: _buildEditorPanel()),
        ],
      ),
    );
  }

  // ── File panel ────────────────────────────────────────────────────────────

  Widget _buildFilePanel() {
    final scanAsync = ref.watch(importScanNotifierProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _panelHeader('FILES'),
        Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // ⚠️ ADAPTIVE, BECAUSE THE FIXED Row OVERFLOWED (internal work item 611 / N08). The controls were a
              // single Row — Expanded(TextField), Browse, Scan — and the two buttons have
              // intrinsic minimum widths that the Expanded cannot absorb, so a narrow panel
              // blew the layout:
              //
              //     A RenderFlex overflowed by 19 pixels on the right.
              //     Row:.../import_screen.dart:93:15
              //
              // Expanded shrinks its own child to zero but cannot shrink a sibling below its
              // minimum, so the row had no width at which it could recover. This is a real
              // responsiveness defect, not a test-viewport artefact: it fires in the app at any
              // panel width the buttons do not fit, and the import screen is a side panel whose
              // width the operator controls. It had never been seen because the frontend suite
              // could not compile in the container until this same task fixed it.
              LayoutBuilder(
                builder: (context, constraints) {
                  final field = TextField(
                    controller: _dirController,
                    decoration: const InputDecoration(
                      labelText: 'Directory path',
                      hintText: '/path/to/configs',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    onSubmitted: (_) => _handleScan(),
                  );
                  final browse = OutlinedButton.icon(
                    onPressed: _handleBrowse,
                    icon: const Icon(Icons.folder_open, size: 16),
                    label: const Text('Browse'),
                  );
                  final scan = FilledButton(
                    onPressed: _handleScan,
                    child: const Text('Scan'),
                  );
                  // Below this width the buttons cannot sit beside the field without clipping
                  // it to nothing; stack instead, which keeps every control reachable.
                  if (constraints.maxWidth < 420) {
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        field,
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(child: browse),
                            const SizedBox(width: 8),
                            Expanded(child: scan),
                          ],
                        ),
                      ],
                    );
                  }
                  return Row(
                    children: [
                      Expanded(child: field),
                      const SizedBox(width: 8),
                      browse,
                      const SizedBox(width: 8),
                      scan,
                    ],
                  );
                },
              ),
            ],
          ),
        ),
        Expanded(
          child: scanAsync.when(
            data: (result) {
              if (result == null) {
                return const Center(
                    child: Text('Enter a directory path and press Scan.',
                        textAlign: TextAlign.center));
              }
              if (result.files.isEmpty) {
                return const Center(
                    child: Text('No YAML config files found.'));
              }
              return ListView.separated(
                itemCount: result.files.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, i) => _buildFileTile(result.files[i]),
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (err, _) => Center(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Text('Scan error: $err',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.error)),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFileTile(FileEntry file) {
    final isSelected = _selectedFile?.path == file.path;
    final best =
        file.candidates.isNotEmpty ? file.candidates.first : null;
    return ListTile(
      selected: isSelected,
      selectedTileColor:
          Theme.of(context).colorScheme.primary.withAlpha(20),
      title: Text(file.fileName,
          style: const TextStyle(fontSize: 13)),
      subtitle: best != null
          ? Text(best.simpleName,
              style: const TextStyle(fontSize: 11))
          : null,
      trailing: best != null
          ? _confidenceBadge(context, best.confidence)
          : null,
      onTap: () => _selectFile(file),
    );
  }

  // ── Editor panel ──────────────────────────────────────────────────────────

  Widget _buildEditorPanel() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _panelHeader('EDITOR'),
        Expanded(child: _buildEditorContent()),
      ],
    );
  }

  Widget _buildEditorContent() {
    if (_selectedFile == null) {
      return const Center(child: Text('Select a file to import.'));
    }
    final file = _selectedFile!;
    if (file.candidates.isEmpty) {
      return const Center(
          child: Text('No matching config type found for this file.'));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Type selector
        if (file.candidates.length > 1)
          Padding(
            padding: const EdgeInsets.all(12),
            child: DropdownButtonFormField<TypeCandidate>(
              decoration: const InputDecoration(
                labelText: 'Config Type',
                border: OutlineInputBorder(),
                isDense: true,
              ),
              initialValue: _selectedType,
              items: file.candidates
                  .map((c) => DropdownMenuItem(
                        value: c,
                        child: Text(
                            '${c.simpleName} '
                            '(${(c.confidence * 100).toStringAsFixed(0)}%)'),
                      ))
                  .toList(),
              onChanged: (tc) {
                if (tc != null) _selectType(tc);
              },
            ),
          )
        else
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
            child: Text(
              'Type: ${_selectedType?.simpleName ?? file.candidates.first.simpleName}',
              style: Theme.of(context).textTheme.titleSmall,
            ),
          ),
        const Divider(height: 1),
        // Field editor
        if (_loadingDetail)
          const Expanded(child: Center(child: CircularProgressIndicator()))
        else if (_detailError != null)
          Expanded(
            child: Center(
              child: Text('Error: $_detailError',
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.error)),
            ),
          )
        else if (_detail != null)
          ..._buildFieldEditor(_detail!)
        else
          const Expanded(
              child: Center(child: Text('Select a type to load fields.'))),
      ],
    );
  }

  List<Widget> _buildFieldEditor(ConfigDetail detail) {
    return [
      Expanded(
        child: ListView.separated(
          padding: const EdgeInsets.all(12),
          itemCount: detail.fields.length,
          separatorBuilder: (_, __) => const SizedBox(height: 8),
          itemBuilder: (context, i) {
            final field = detail.fields[i];
            return FieldInput(
              field: field,
              breadcrumb: [
                'Import',
                _selectedType?.simpleName ?? '',
              ],
              value: _values[field.name],
              onChanged: (v) =>
                  setState(() => _values[field.name] = v),
            );
          },
        ),
      ),
      const Divider(height: 1),
      Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            ElevatedButton.icon(
              icon: const Icon(Icons.save),
              label: const Text('Save'),
              onPressed: _showSaveDialog,
            ),
          ],
        ),
      ),
    ];
  }

  // ── Confidence badge ──────────────────────────────────────────────────────

  Widget _confidenceBadge(BuildContext context, double confidence) {
    final isHigh = confidence >= 0.7;
    final color = isHigh ? Colors.green : Colors.amber;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withAlpha(30),
        border: Border.all(color: color.withAlpha(80)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        '${(confidence * 100).toStringAsFixed(0)}%',
        style: TextStyle(
            fontSize: 10,
            color: color,
            fontWeight: FontWeight.w600),
      ),
    );
  }

  // ── Panel header ──────────────────────────────────────────────────────────

  Widget _panelHeader(String label) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      color: Theme.of(context).colorScheme.surfaceContainerLow,
      child: Text(
        label,
        style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 11,
            letterSpacing: 0.5),
      ),
    );
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  /// Opens a browser directory picker (webkitdirectory) so the user can
  /// navigate to a folder. The folder name is extracted from the first
  /// file's [webkitRelativePath] and placed in the directory text field.
  void _handleBrowse() {
    // ⚠️ VIA directory_picker.dart, NOT dart:html (internal work item 611 / N08). This called
    // `html.FileUploadInputElement()` directly; `dart:html` is gone from the Dart 3.x
    // toolchain the container pins, so this screen stopped compiling — and since three
    // widget tests import it, the entire frontend suite failed to LOAD. The picker now
    // lives behind a conditional import so the web build gets `package:web` and the VM
    // (where those tests run) gets a stub.
    pickDirectoryName((name) => setState(() => _dirController.text = name));
  }

  Future<void> _handleScan() async {
    final dir = _dirController.text.trim();
    if (dir.isEmpty) return;
    setState(() {
      _selectedFile = null;
      _selectedType = null;
      _values.clear();
      _detail = null;
      _detailError = null;
    });
    await ref.read(importScanNotifierProvider.notifier).scan(dir);
  }

  Future<void> _selectFile(FileEntry file) async {
    setState(() {
      _selectedFile = file;
      _values.clear();
      _detail = null;
      _detailError = null;
    });
    if (file.candidates.isNotEmpty) {
      await _selectType(file.candidates.first);
    } else {
      setState(() => _selectedType = null);
    }
  }

  Future<void> _selectType(TypeCandidate type) async {
    // Monotonic request token (A10-03, internal work item 1449/internal work item 1492): switching type twice in
    // quick succession starts two overlapping getConfig() calls. Without
    // this, an out-of-order network completion (the FIRST call's response
    // arriving AFTER the second) would overwrite _detail/_values with the
    // stale type's data while _selectedType/the dropdown still shows the
    // later, correct selection — the write button would then send the
    // current type's fqcn with the wrong type's field values.
    final token = ++_typeLoadToken;
    setState(() {
      _selectedType = type;
      _values.clear();
      _detail = null;
      _detailError = null;
      _loadingDetail = true;
    });
    try {
      final detail =
          await ref.read(apiClientProvider).getConfig(type.fqcn);
      if (!mounted || token != _typeLoadToken) return;
      setState(() {
        _detail = detail;
        _loadingDetail = false;
        // Seed metadata defaults
        for (final field in detail.fields) {
          if (field.defaultValue != null) {
            _values.putIfAbsent(field.name, () => field.defaultValue);
          }
        }
      });
    } catch (e) {
      if (!mounted || token != _typeLoadToken) return;
      setState(() {
        _detailError = e.toString();
        _loadingDetail = false;
      });
    }
  }

  Future<void> _showSaveDialog() async {
    final type = _selectedType;
    final file = _selectedFile;
    if (type == null || file == null) return;

    final sessionDir = ref.read(outputDirectoryProvider);
    _customPathController.clear();

    await showDialog<void>(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setDialogState) => AlertDialog(
            title: const Text('Save Configuration'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Option 1: overwrite source file
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.drive_file_rename_outline),
                    title: const Text('Overwrite source file'),
                    subtitle: Text(file.path,
                        style: const TextStyle(fontSize: 11)),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      _doWrite(type.fqcn, outputPath: file.path);
                    },
                  ),
                  // Option 2: session output directory (only shown when set)
                  if (sessionDir != null)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.folder),
                      title: const Text('Session output directory'),
                      subtitle: Text(sessionDir,
                          style: const TextStyle(fontSize: 11)),
                      onTap: () {
                        Navigator.of(ctx).pop();
                        final fileName = _selectedFile!.path.split('/').last;
                        _doWrite(type.fqcn, outputPath: '$sessionDir/$fileName');
                      },
                    ),
                  const Divider(),
                  // Option 3: custom path
                  TextField(
                    controller: _customPathController,
                    decoration: const InputDecoration(
                      labelText: 'Custom output path',
                      hintText: '/path/to/output.yaml',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(ctx).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () {
                  final custom = _customPathController.text.trim();
                  Navigator.of(ctx).pop();
                  _doWrite(type.fqcn,
                      outputPath: custom.isEmpty ? null : custom);
                },
                child: const Text('Save'),
              ),
            ],
          ),
        );
      },
    );
  }

  Future<void> _doWrite(String fqcn, {String? outputPath}) async {
    try {
      final result = await ref.read(importApiClientProvider).write(
            fqcn,
            Map<String, dynamic>.from(_values),
            sourceFile: _selectedFile?.path,
            outputPath: outputPath,
          );
      if (!mounted) return;
      if (result.errorMessage != null) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Save failed: ${result.errorMessage}'),
          backgroundColor: Theme.of(context).colorScheme.error,
        ));
      } else {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Saved to: ${result.path ?? 'server default'}'),
        ));
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('Save error: $e'),
        backgroundColor: Theme.of(context).colorScheme.error,
      ));
    }
  }
}
