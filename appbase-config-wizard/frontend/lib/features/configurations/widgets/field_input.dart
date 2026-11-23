import 'dart:convert';

import 'package:flutter/material.dart';

import '../models/models.dart';
import '../screens/nested_config_form_screen.dart';

/// Renders a single [FieldSummary] as an appropriate input widget.
///
/// - Nested config fields ([FieldSummary.nestedTypeFqcn] != null) → tappable card
///   with a "Set Values" button that opens [NestedConfigFormScreen].
/// - `Map` types → [_MapEditor] with add/delete key-value rows.
/// - Enum types ([FieldSummary.enumValues] != null) → [DropdownButtonFormField].
/// - `bool` / `Boolean` → [Switch]
/// - `int` / `Integer` / `long` / `Long` → [TextFormField] with number keyboard
/// - All other types → [TextFormField]
///
/// The current [value] is supplied by the parent form; changes are reported via
/// [onChanged]. The [breadcrumb] list identifies the path from the root config
/// to the current screen and is forwarded to any sub-form opened from this widget.
class FieldInput extends StatefulWidget {
  /// Metadata that determines which input control is rendered.
  final FieldSummary field;

  /// Current value supplied by the owning form.
  final dynamic value;

  /// Receives normalized values whenever the control changes.
  final ValueChanged<dynamic> onChanged;

  /// Breadcrumb path *leading to* this form (not including this field's name).
  /// Forwarded to [NestedConfigFormScreen] as context for its breadcrumb title.
  final List<String> breadcrumb;

  /// Creates an input bound to [field] and [value].
  const FieldInput({
    super.key,
    required this.field,
    required this.value,
    required this.onChanged,
    this.breadcrumb = const [],
  });

  @override
  State<FieldInput> createState() => _FieldInputState();
}

class _FieldInputState extends State<FieldInput> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(
        text: widget.value != null && !_isNested && !_isMap && !_isCollection && !_isEnum
            ? widget.value.toString()
            : '');
  }

  @override
  void didUpdateWidget(FieldInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_isNested && !_isMap && !_isCollection && !_isEnum && !_isBool && oldWidget.value != widget.value) {
      final text = widget.value != null ? widget.value.toString() : '';
      if (_controller.text != text) {
        _controller.text = text;
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  bool get _isNested => widget.field.nestedTypeFqcn != null;

  bool get _isMap {
    final t = widget.field.typeName;
    return t == 'Map' || t.startsWith('Map<');
  }

  bool get _isCollection {
    final t = widget.field.typeName;
    return t == 'List' || t == 'Set' || t == 'Collection'
        || t == 'ArrayList' || t == 'LinkedList'
        || t == 'HashSet' || t == 'LinkedHashSet' || t == 'TreeSet'
        || t.startsWith('List<') || t.startsWith('Set<') || t.startsWith('Collection<');
  }

  bool get _isEnum => widget.field.enumValues != null && widget.field.enumValues!.isNotEmpty;

  bool get _isBool {
    final t = widget.field.typeName;
    return t == 'bool' || t == 'Boolean' || t == 'boolean';
  }

  bool get _isNumeric {
    final t = widget.field.typeName;
    return t == 'int' || t == 'Integer' || t == 'long' || t == 'Long';
  }

  @override
  Widget build(BuildContext context) {
    if (_isNested) return _buildNestedField(context);
    if (_isMap) return _buildMapField(context);
    if (_isCollection) return _buildCollectionField(context);
    if (_isEnum) return _buildEnumField(context);
    if (_isBool) return _buildBoolField(context);
    return _buildTextField();
  }

  // ── Nested config object ──────────────────────────────────────────────────

  Widget _buildNestedField(BuildContext context) {
    final nestedValues =
        widget.value is Map ? widget.value as Map<dynamic, dynamic> : null;
    final fieldCount = nestedValues?.length ?? 0;
    final isConfigured = nestedValues != null && fieldCount > 0;

    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.hardEdge,
      child: ListTile(
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        title: Text(
          widget.field.name,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        subtitle: Text(
          isConfigured
              ? '$fieldCount field(s) configured'
              : 'Using defaults (${widget.field.typeName})',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: isConfigured
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.onSurfaceVariant,
              ),
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isConfigured)
              IconButton(
                icon: const Icon(Icons.clear),
                tooltip: 'Reset to defaults',
                onPressed: () => widget.onChanged(null),
              ),
            Icon(
              Icons.chevron_right,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ],
        ),
        onTap: () => _openNestedForm(context),
      ),
    );
  }

  Future<void> _openNestedForm(BuildContext context) async {
    final initial = widget.value is Map
        ? Map<String, dynamic>.from(widget.value as Map)
        : null;
    final result = await Navigator.of(context).push<Map<String, dynamic>>(
      MaterialPageRoute<Map<String, dynamic>>(
        builder: (_) => NestedConfigFormScreen(
          breadcrumb: widget.breadcrumb,
          fieldName: widget.field.name,
          nestedTypeFqcn: widget.field.nestedTypeFqcn!,
          initialValues: initial,
        ),
      ),
    );
    if (result != null) {
      widget.onChanged(result);
    }
  }

  // ── Map key-value editor ──────────────────────────────────────────────────

  Widget _buildMapField(BuildContext context) {
    final current = widget.value is Map
        ? Map<String, dynamic>.from(widget.value as Map)
        : <String, dynamic>{};
    return _MapEditor(
      fieldName: widget.field.name,
      initialValue: current,
      onChanged: widget.onChanged,
      elementNestedTypeFqcn: widget.field.elementNestedTypeFqcn,
      breadcrumb: [...widget.breadcrumb, widget.field.name],
    );
  }

  // ── List / Set editor ─────────────────────────────────────────────────────

  Widget _buildCollectionField(BuildContext context) {
    final current = widget.value is List
        ? List<dynamic>.from(widget.value as List)
        : <dynamic>[];
    return _ListSetEditor(
      fieldName: widget.field.name,
      typeName: widget.field.typeName,
      initialValue: current,
      onChanged: widget.onChanged,
      elementNestedTypeFqcn: widget.field.elementNestedTypeFqcn,
      breadcrumb: [...widget.breadcrumb, widget.field.name],
    );
  }

  // ── Enum dropdown ─────────────────────────────────────────────────────────

  Widget _buildEnumField(BuildContext context) {
    final values = widget.field.enumValues!;
    final current = widget.value?.toString();
    final selected = values.contains(current) ? current : values.first;
    return DropdownButtonFormField<String>(
      initialValue: selected,
      decoration: InputDecoration(
        labelText: widget.field.name,
        border: const OutlineInputBorder(),
        isDense: true,
      ),
      items: values
          .map((v) => DropdownMenuItem(value: v, child: Text(v)))
          .toList(),
      onChanged: widget.onChanged,
    );
  }

  // ── Bool toggle ───────────────────────────────────────────────────────────

  Widget _buildBoolField(BuildContext context) {
    final boolVal = widget.value is bool
        ? widget.value as bool
        : (widget.value?.toString().toLowerCase() == 'true');
    return Row(
      children: [
        Expanded(
          child: Text(
            widget.field.name,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
        Switch(
          value: boolVal,
          onChanged: widget.onChanged,
        ),
      ],
    );
  }

  // ── Text / number field ───────────────────────────────────────────────────

  Widget _buildTextField() {
    final field = widget.field;
    return TextFormField(
      controller: _controller,
      decoration: InputDecoration(
        labelText: field.name,
        hintText: field.defaultValue,
        helperText:
            field.defaultValue != null ? 'default: ${field.defaultValue}' : null,
        border: const OutlineInputBorder(),
        isDense: true,
      ),
      keyboardType: _isNumeric
          ? const TextInputType.numberWithOptions(signed: true)
          : TextInputType.text,
      onChanged: (text) {
        if (_isNumeric) {
          widget.onChanged(text.isEmpty ? null : num.tryParse(text) ?? text);
        } else {
          widget.onChanged(text);
        }
      },
    );
  }
}

// ── Map key-value row editor ──────────────────────────────────────────────────

class _MapRow {
  final TextEditingController keyCtrl;
  final TextEditingController valueCtrl;

  _MapRow(String key, String value)
      : keyCtrl = TextEditingController(text: key),
        valueCtrl = TextEditingController(text: value);

  void dispose() {
    keyCtrl.dispose();
    valueCtrl.dispose();
  }
}

/// Inline key-value editor for [Map] configuration fields.
///
/// Each entry is rendered as a key text field and a value text field on the
/// same row, with a delete button. An "Add entry" button appends a new blank
/// row. Value cells accept plain strings or JSON arrays/objects (e.g.
/// `["READ","WRITE"]`) — JSON is parsed before being sent to the server so
/// fields typed as `Map<String, List<String>>` work correctly.
class _MapEditor extends StatefulWidget {
  final String fieldName;
  final Map<String, dynamic> initialValue;
  final ValueChanged<Map<String, dynamic>> onChanged;
  final String? elementNestedTypeFqcn;
  final List<String> breadcrumb;

  const _MapEditor({
    required this.fieldName,
    required this.initialValue,
    required this.onChanged,
    this.elementNestedTypeFqcn,
    this.breadcrumb = const [],
  });

  @override
  State<_MapEditor> createState() => _MapEditorState();
}

class _MapEditorState extends State<_MapEditor> {
  late List<_MapRow> _rows;

  @override
  void initState() {
    super.initState();
    _rows = _buildRows(widget.initialValue);
  }

  @override
  void didUpdateWidget(_MapEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Only reset rows when the parent seeded a new value from outside
    // (i.e. the change did not originate from this editor).
    final incoming = _encodeMap(widget.initialValue);
    final current = _encodeMap(_snapshot());
    if (incoming != _encodeMap(oldWidget.initialValue) && incoming != current) {
      for (final r in _rows) { r.dispose(); }
      _rows = _buildRows(widget.initialValue);
    }
  }

  @override
  void dispose() {
    for (final r in _rows) { r.dispose(); }
    super.dispose();
  }

  List<_MapRow> _buildRows(Map<String, dynamic> map) => map.entries
      .map((e) => _MapRow(e.key, _encodeValue(e.value)))
      .toList();

  String _encodeValue(dynamic v) =>
      v is String ? v : jsonEncode(v);

  dynamic _decodeValue(String text) {
    final t = text.trim();
    if (t.startsWith('[') || t.startsWith('{')) {
      try { return jsonDecode(t); } catch (_) {}
    }
    return text;
  }

  Map<String, dynamic> _snapshot() {
    final result = <String, dynamic>{};
    for (final row in _rows) {
      final k = row.keyCtrl.text.trim();
      if (k.isNotEmpty) result[k] = _decodeValue(row.valueCtrl.text);
    }
    return result;
  }

  String _encodeMap(Map<String, dynamic> m) {
    try { return jsonEncode(m); } catch (_) { return m.toString(); }
  }

  void _notify() => widget.onChanged(_snapshot());

  void _addRow() {
    setState(() => _rows.add(_MapRow('', '')));
  }

  void _removeRow(int index) {
    setState(() {
      _rows[index].dispose();
      _rows.removeAt(index);
    });
    _notify();
  }

  Widget _buildValueCell(BuildContext context, int i) {
    if (widget.elementNestedTypeFqcn != null) {
      // Value is a nested config object — show a card/button instead of a text field
      final rawVal = _rows[i].valueCtrl.text.trim();
      Map<String, dynamic>? parsed;
      if (rawVal.startsWith('{')) {
        try { parsed = jsonDecode(rawVal) as Map<String, dynamic>?; } catch (_) {}
      }
      final isSet = parsed != null && parsed.isNotEmpty;
      return GestureDetector(
        onTap: () => _openElementSubForm(context, i, parsed),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            border: Border.all(color: Theme.of(context).colorScheme.outline),
            borderRadius: BorderRadius.circular(4),
          ),
          child: Text(
            isSet ? '${parsed.length} field(s) set' : 'Tap to configure…',
            style: TextStyle(
              color: isSet
                  ? Theme.of(context).colorScheme.primary
                  : Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
        ),
      );
    }
    return TextFormField(
      controller: _rows[i].valueCtrl,
      decoration: const InputDecoration(
        labelText: 'Value',
        hintText: 'String or JSON array/object',
        border: OutlineInputBorder(),
        isDense: true,
      ),
      onChanged: (_) => _notify(),
    );
  }

  Future<void> _openElementSubForm(BuildContext context, int rowIndex, Map<String, dynamic>? initial) async {
    final result = await Navigator.of(context).push<Map<String, dynamic>>(
      MaterialPageRoute<Map<String, dynamic>>(
        builder: (_) => NestedConfigFormScreen(
          breadcrumb: widget.breadcrumb,
          fieldName: '${widget.fieldName}[value]',
          nestedTypeFqcn: widget.elementNestedTypeFqcn!,
          initialValues: initial,
        ),
      ),
    );
    if (result != null) {
      setState(() {
        _rows[rowIndex].valueCtrl.text = jsonEncode(result);
      });
      _notify();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(widget.fieldName, style: theme.textTheme.bodyMedium),
        const SizedBox(height: 8),
        if (_rows.isEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: Text(
              'No entries — tap Add entry below.',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
                fontStyle: FontStyle.italic,
              ),
            ),
          ),
        for (int i = 0; i < _rows.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  flex: 4,
                  child: TextFormField(
                    controller: _rows[i].keyCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Key',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    onChanged: (_) => _notify(),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  flex: 5,
                  child: _buildValueCell(context, i),
                ),
                const SizedBox(width: 4),
                IconButton(
                  icon: Icon(
                    Icons.remove_circle_outline,
                    size: 20,
                    color: theme.colorScheme.error,
                  ),
                  tooltip: 'Remove entry',
                  onPressed: () => _removeRow(i),
                ),
              ],
            ),
          ),
        TextButton.icon(
          onPressed: _addRow,
          icon: const Icon(Icons.add, size: 18),
          label: const Text('Add entry'),
        ),
      ],
    );
  }
}

// ── List / Set value editor ───────────────────────────────────────────────────

/// Inline editor for [List] and [Set] configuration fields.
///
/// Each entry is a single value row with a delete button. Values can be plain
/// strings or JSON arrays/objects (e.g. `["READ","WRITE"]`) — JSON is parsed
/// before being sent to the server so fields typed as `List<List<String>>`
/// or `Set<SomeComplexType>` work correctly.
class _ListSetEditor extends StatefulWidget {
  final String fieldName;
  final String typeName;
  final List<dynamic> initialValue;
  final ValueChanged<List<dynamic>> onChanged;
  final String? elementNestedTypeFqcn;
  final List<String> breadcrumb;

  const _ListSetEditor({
    required this.fieldName,
    required this.typeName,
    required this.initialValue,
    required this.onChanged,
    this.elementNestedTypeFqcn,
    this.breadcrumb = const [],
  });

  @override
  State<_ListSetEditor> createState() => _ListSetEditorState();
}

class _ListSetEditorState extends State<_ListSetEditor> {
  late List<TextEditingController> _controllers;

  @override
  void initState() {
    super.initState();
    _controllers = widget.initialValue
        .map((v) => TextEditingController(text: _encodeValue(v)))
        .toList();
  }

  @override
  void didUpdateWidget(_ListSetEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    final incoming = _encodeList(widget.initialValue);
    final current = _encodeList(_snapshot());
    if (incoming != _encodeList(oldWidget.initialValue) && incoming != current) {
      for (final c in _controllers) { c.dispose(); }
      _controllers = widget.initialValue
          .map((v) => TextEditingController(text: _encodeValue(v)))
          .toList();
    }
  }

  @override
  void dispose() {
    for (final c in _controllers) { c.dispose(); }
    super.dispose();
  }

  String _encodeValue(dynamic v) => v is String ? v : jsonEncode(v);

  dynamic _decodeValue(String text) {
    final t = text.trim();
    if (t.startsWith('[') || t.startsWith('{')) {
      try { return jsonDecode(t); } catch (_) {}
    }
    return text;
  }

  List<dynamic> _snapshot() =>
      _controllers.map((c) => _decodeValue(c.text)).toList();

  String _encodeList(List<dynamic> l) {
    try { return jsonEncode(l); } catch (_) { return l.toString(); }
  }

  void _notify() => widget.onChanged(_snapshot());

  void _addRow() {
    setState(() => _controllers.add(TextEditingController()));
  }

  void _removeRow(int index) {
    setState(() {
      _controllers[index].dispose();
      _controllers.removeAt(index);
    });
    _notify();
  }

  Widget _buildValueCell(BuildContext context, int i) {
    if (widget.elementNestedTypeFqcn != null) {
      final rawVal = _controllers[i].text.trim();
      Map<String, dynamic>? parsed;
      if (rawVal.startsWith('{')) {
        try { parsed = jsonDecode(rawVal) as Map<String, dynamic>?; } catch (_) {}
      }
      final isSet = parsed != null && parsed.isNotEmpty;
      return GestureDetector(
        onTap: () => _openElementSubForm(context, i, parsed),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            border: Border.all(color: Theme.of(context).colorScheme.outline),
            borderRadius: BorderRadius.circular(4),
          ),
          child: Text(
            isSet ? '${parsed.length} field(s) set' : 'Tap to configure…',
            style: TextStyle(
              color: isSet
                  ? Theme.of(context).colorScheme.primary
                  : Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
        ),
      );
    }
    return TextFormField(
      controller: _controllers[i],
      decoration: const InputDecoration(
        labelText: 'Value',
        hintText: 'String or JSON array/object',
        border: OutlineInputBorder(),
        isDense: true,
      ),
      onChanged: (_) => _notify(),
    );
  }

  Future<void> _openElementSubForm(BuildContext context, int index, Map<String, dynamic>? initial) async {
    final result = await Navigator.of(context).push<Map<String, dynamic>>(
      MaterialPageRoute<Map<String, dynamic>>(
        builder: (_) => NestedConfigFormScreen(
          breadcrumb: widget.breadcrumb,
          fieldName: '${widget.fieldName}[$index]',
          nestedTypeFqcn: widget.elementNestedTypeFqcn!,
          initialValues: initial,
        ),
      ),
    );
    if (result != null) {
      setState(() {
        _controllers[index].text = jsonEncode(result);
      });
      _notify();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(widget.fieldName, style: theme.textTheme.bodyMedium),
        const SizedBox(height: 4),
        Text(
          widget.typeName,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 8),
        if (_controllers.isEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: Text(
              'No values — tap Add value below.',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
                fontStyle: FontStyle.italic,
              ),
            ),
          ),
        for (int i = 0; i < _controllers.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '#${i + 1}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _buildValueCell(context, i),
                ),
                const SizedBox(width: 4),
                IconButton(
                  icon: Icon(
                    Icons.remove_circle_outline,
                    size: 20,
                    color: theme.colorScheme.error,
                  ),
                  tooltip: 'Remove value',
                  onPressed: () => _removeRow(i),
                ),
              ],
            ),
          ),
        TextButton.icon(
          onPressed: _addRow,
          icon: const Icon(Icons.add, size: 18),
          label: const Text('Add value'),
        ),
      ],
    );
  }
}
