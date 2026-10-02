import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lab1_calculator/main.dart';

void main() {
  testWidgets('calculates an expression and adds it to history', (
    tester,
  ) async {
    await tester.pumpWidget(const CalculatorApp());
    for (final label in ['1', '2', '+', '5', '×', '3', '=']) {
      final key = find.widgetWithText(FilledButton, label);
      await tester.ensureVisible(key);
      await tester.tap(key);
      await tester.pump();
    }
    expect(find.text('= 27'), findsWidgets);
    expect(find.text('12+5×3'), findsNWidgets(2));
  });
}
