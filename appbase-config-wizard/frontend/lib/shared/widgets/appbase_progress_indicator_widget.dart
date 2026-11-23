import 'package:flutter/material.dart';

// @author Navaneeth Mysore G
// @author Claude
/// Centers a progress indicator using the active theme's primary color.
class AppbaseProgressIndicatorWidget extends StatelessWidget {
  /// Creates the centered busy indicator used by loading screens.
  const AppbaseProgressIndicatorWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: CircularProgressIndicator(
        backgroundColor: Theme.of(context).colorScheme.primary,
      ),
    );
  }
}
