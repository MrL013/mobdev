import 'dart:convert';

import 'package:flutter/foundation.dart';

import 'expression_evaluator.dart';

class CalculationHistoryItem {
  const CalculationHistoryItem({
    required this.expression,
    required this.result,
  });
  final String expression;
  final String result;
  Map<String, String> toJson() => {'expression': expression, 'result': result};
  factory CalculationHistoryItem.fromJson(Map<String, dynamic> json) =>
      CalculationHistoryItem(
        expression: json['expression'] as String? ?? '',
        result: json['result'] as String? ?? '',
      );
}

class CalculatorController extends ChangeNotifier {
  CalculatorController({ExpressionEvaluator? evaluator})
    : _evaluator = evaluator ?? const ExpressionEvaluator();
  final ExpressionEvaluator _evaluator;
  String expression = '';
  String result = '';
  String? error;
  double memory = 0;
  final List<CalculationHistoryItem> history = [];
  bool _justCalculated = false;

  void input(String value) {
    _clearError();
    if (_justCalculated &&
        (RegExp(r'[0-9.]').hasMatch(value) || value == '(')) {
      expression = '';
      result = '';
    } else if (_justCalculated && _isOperator(value)) {
      expression = _displayNumber(_lastValueOrZero());
      result = '';
    }
    _justCalculated = false;
    expression += value;
    notifyListeners();
  }

  void clear() {
    expression = '';
    result = '';
    error = null;
    _justCalculated = false;
    notifyListeners();
  }

  void backspace() {
    _clearError();
    if (_justCalculated) {
      _justCalculated = false;
      result = '';
    }
    if (expression.isNotEmpty) {
      expression = expression.substring(0, expression.length - 1);
    }
    notifyListeners();
  }

  void toggleSign() {
    _clearError();
    if (_justCalculated) {
      expression = _displayNumber(_lastValueOrZero());
      result = '';
      _justCalculated = false;
    }
    final match = RegExp(r'(\d+(?:\.\d*)?|\.\d+)$').firstMatch(expression);
    if (match == null) return _setError('Сначала введите число');
    final start = match.start;
    final hasUnaryMinus =
        start > 0 &&
        expression[start - 1] == '-' &&
        (start == 1 ||
            _isOperator(expression[start - 2]) ||
            expression[start - 2] == '(');
    expression = hasUnaryMinus
        ? expression.substring(0, start - 1) + expression.substring(start)
        : '${expression.substring(0, start)}-${expression.substring(start)}';
    notifyListeners();
  }

  void calculate() {
    try {
      final formatted = _displayNumber(_evaluator.evaluate(expression));
      result = formatted;
      error = null;
      history.insert(
        0,
        CalculationHistoryItem(expression: expression, result: formatted),
      );
      if (history.length > 10) history.removeLast();
      _justCalculated = true;
    } on CalculationException catch (exception) {
      result = '';
      _setError(exception.message, notify: false);
      _justCalculated = false;
    }
    notifyListeners();
  }

  void addToMemory() => _changeMemory(1);
  void subtractFromMemory() => _changeMemory(-1);

  void recallMemory() {
    _clearError();
    if (_justCalculated) {
      expression = '';
      result = '';
      _justCalculated = false;
    }
    expression += _displayNumber(memory);
    notifyListeners();
  }

  void clearMemory() {
    memory = 0;
    notifyListeners();
  }

  void selectHistory(CalculationHistoryItem item) {
    expression = item.expression;
    result = '';
    error = null;
    _justCalculated = false;
    notifyListeners();
  }

  String serialize() => jsonEncode({
    'expression': expression,
    'result': result,
    'error': error,
    'memory': memory,
    'history': history.map((item) => item.toJson()).toList(),
  });

  void restore(String serialized) {
    if (serialized.isEmpty) return;
    try {
      final json = jsonDecode(serialized) as Map<String, dynamic>;
      expression = json['expression'] as String? ?? '';
      result = json['result'] as String? ?? '';
      error = json['error'] as String?;
      memory = (json['memory'] as num?)?.toDouble() ?? 0;
      history
        ..clear()
        ..addAll(
          ((json['history'] as List?) ?? const []).whereType<Map>().map(
            (item) => CalculationHistoryItem.fromJson(
              Map<String, dynamic>.from(item),
            ),
          ),
        );
      _justCalculated = result.isNotEmpty;
    } catch (_) {}
  }

  void _changeMemory(int multiplier) {
    try {
      memory += multiplier * _currentValue();
      _clearError();
    } on CalculationException catch (exception) {
      _setError(exception.message, notify: false);
    }
    notifyListeners();
  }

  double _currentValue() => result.isNotEmpty
      ? double.parse(result)
      : _evaluator.evaluate(expression);
  double _lastValueOrZero() {
    try {
      return _currentValue();
    } catch (_) {
      return 0;
    }
  }

  bool _isOperator(String value) =>
      const {'+', '-', '×', '÷', '*', '/'}.contains(value);
  void _clearError() => error = null;
  void _setError(String message, {bool notify = true}) {
    error = message;
    if (notify) notifyListeners();
  }
}

String _displayNumber(double value) {
  if (value == value.roundToDouble()) return value.toInt().toString();
  return value
      .toStringAsPrecision(12)
      .replaceFirst(RegExp(r'0+$'), '')
      .replaceFirst(RegExp(r'\.$'), '');
}
