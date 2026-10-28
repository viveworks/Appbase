import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'app.dart';

/// Starts the config wizard inside Riverpod's root provider scope.
void main() => runApp(const ProviderScope(child: ConfigWizardApp()));
