import 'package:flutter_test/flutter_test.dart';
import 'package:lab1_calculator/calculator/calculator_controller.dart';

void main() {
  group('CalculatorController', () {
    test(
      'stores no more than ten calculations and restores a selected item',
      () {
        final controller = CalculatorController();
        for (var index = 0; index < 11; index++) {
          controller
            ..input('$index')
            ..calculate()
            ..clear();
        }
        expect(controller.history, hasLength(10));
        expect(controller.history.first.expression, '10');

        controller.selectHistory(controller.history.last);
        expect(controller.expression, '1');
        expect(controller.result, isEmpty);
      },
    );

    test('updates and recalls calculator memory', () {
      final controller = CalculatorController();
      controller
        ..input('2')
        ..input('+')
        ..input('3')
        ..calculate()
        ..addToMemory()
        ..clear()
        ..recallMemory();
      expect(controller.memory, 5);
      expect(controller.expression, '5');
    });

    test('preserves complete state through serialization', () {
      final source = CalculatorController();
      source
        ..input('4')
        ..calculate()
        ..addToMemory();
      final restored = CalculatorController()..restore(source.serialize());
      expect(restored.expression, '4');
      expect(restored.result, '4');
      expect(restored.memory, 4);
      expect(restored.history, hasLength(1));
    });
  });
}
