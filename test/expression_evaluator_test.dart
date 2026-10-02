import 'package:flutter_test/flutter_test.dart';
import 'package:lab1_calculator/calculator/expression_evaluator.dart';

void main() {
  const evaluator = ExpressionEvaluator();

  group('ExpressionEvaluator', () {
    test('respects arithmetic precedence', () {
      expect(evaluator.evaluate('12 + 5 × 3'), 27);
    });

    test('respects parentheses', () {
      expect(evaluator.evaluate('(12 + 5) × 3'), 51);
    });

    test('supports decimal values, percent, and unary minus', () {
      expect(evaluator.evaluate('15.5 × 2'), 31);
      expect(evaluator.evaluate('200 × 10%'), 20);
      expect(evaluator.evaluate('-2 × (3 + 4)'), -14);
    });

    test('accepts keyboard alternatives for multiplication and division', () {
      expect(evaluator.evaluate('20 / 4 + 7'), 12);
      expect(evaluator.evaluate('2 * 8'), 16);
    });

    test('reports invalid expressions without crashing', () {
      for (final expression in [
        '',
        '1..2',
        '(1 + 2',
        '1 + × 2',
        '1++2',
        '2 +',
        '1 ÷ 0',
      ]) {
        expect(
          () => evaluator.evaluate(expression),
          throwsA(isA<CalculationException>()),
        );
      }
    });
  });
}
