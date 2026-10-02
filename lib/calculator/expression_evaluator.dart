class ExpressionEvaluator {
  const ExpressionEvaluator();

  double evaluate(String expression) {
    final tokens = _tokenize(expression);
    if (tokens.isEmpty) {
      throw const CalculationException('Введите выражение');
    }
    return _evaluateRpn(_toRpn(tokens));
  }

  List<_Token> _tokenize(String expression) {
    final tokens = <_Token>[];
    var index = 0;

    while (index < expression.length) {
      final char = expression[index];
      if (char.trim().isEmpty) {
        index++;
        continue;
      }
      if (_isDigit(char) || char == '.') {
        final start = index;
        var dots = 0;
        var digits = 0;
        while (index < expression.length) {
          final current = expression[index];
          if (_isDigit(current)) {
            digits++;
            index++;
          } else if (current == '.') {
            dots++;
            if (dots > 1) {
              throw const CalculationException(
                'В числе может быть только одна десятичная точка',
              );
            }
            index++;
          } else {
            break;
          }
        }
        if (digits == 0) {
          throw const CalculationException('Некорректное число');
        }
        tokens.add(_Token.number(expression.substring(start, index)));
        continue;
      }
      if (char == '+' ||
          char == '-' ||
          char == '×' ||
          char == '*' ||
          char == '÷' ||
          char == '/') {
        tokens.add(_Token.operator(_normalizeOperator(char)));
      } else if (char == '(') {
        tokens.add(const _Token.leftParenthesis());
      } else if (char == ')') {
        tokens.add(const _Token.rightParenthesis());
      } else if (char == '%') {
        tokens.add(const _Token.percent());
      } else {
        throw CalculationException('Недопустимый символ: $char');
      }
      index++;
    }
    return tokens;
  }

  List<_Token> _toRpn(List<_Token> tokens) {
    final output = <_Token>[];
    final operators = <_Token>[];
    _Token? previous;

    for (final token in tokens) {
      switch (token.type) {
        case _TokenType.number:
          output.add(token);
        case _TokenType.percent:
          if (previous == null ||
              previous.type == _TokenType.operator ||
              previous.type == _TokenType.leftParenthesis) {
            throw const CalculationException(
              'Проценту должно предшествовать число',
            );
          }
          output.add(token);
        case _TokenType.operator:
          var operator = token.value!;
          final isUnary =
              previous == null ||
              previous.type == _TokenType.operator ||
              previous.type == _TokenType.leftParenthesis;
          if (isUnary) {
            if (operator == '-') {
              operator = 'u-';
            } else {
              throw const CalculationException(
                'Операция не может находиться в начале выражения',
              );
            }
          }
          final current = _Token.operator(operator);
          while (operators.isNotEmpty &&
              operators.last.type == _TokenType.operator &&
              _shouldPop(current.value!, operators.last.value!)) {
            output.add(operators.removeLast());
          }
          operators.add(current);
        case _TokenType.leftParenthesis:
          operators.add(token);
        case _TokenType.rightParenthesis:
          var foundOpening = false;
          while (operators.isNotEmpty) {
            final top = operators.removeLast();
            if (top.type == _TokenType.leftParenthesis) {
              foundOpening = true;
              break;
            }
            output.add(top);
          }
          if (!foundOpening) {
            throw const CalculationException('Несогласованные скобки');
          }
      }
      previous = token;
    }

    if (previous?.type == _TokenType.operator) {
      throw const CalculationException(
        'Выражение не может заканчиваться оператором',
      );
    }
    while (operators.isNotEmpty) {
      final top = operators.removeLast();
      if (top.type == _TokenType.leftParenthesis) {
        throw const CalculationException('Несогласованные скобки');
      }
      output.add(top);
    }
    return output;
  }

  double _evaluateRpn(List<_Token> tokens) {
    final values = <double>[];
    for (final token in tokens) {
      if (token.type == _TokenType.number) {
        values.add(double.parse(token.value!));
      } else if (token.type == _TokenType.percent) {
        if (values.isEmpty) {
          throw const CalculationException(
            'Некорректное использование процента',
          );
        }
        values.add(values.removeLast() / 100);
      } else if (token.type == _TokenType.operator) {
        final operator = token.value!;
        if (operator == 'u-') {
          if (values.isEmpty) {
            throw const CalculationException('Не хватает операнда');
          }
          final value = values.removeLast();
          values.add(operator == 'u-' ? -value : value);
          continue;
        }
        if (values.length < 2) {
          throw const CalculationException('Не хватает операндов');
        }
        final right = values.removeLast();
        final left = values.removeLast();
        switch (operator) {
          case '+':
            values.add(left + right);
          case '-':
            values.add(left - right);
          case '*':
            values.add(left * right);
          case '/':
            if (right == 0) {
              throw const CalculationException('Деление на ноль');
            }
            values.add(left / right);
          default:
            throw const CalculationException('Неизвестная операция');
        }
      }
    }
    if (values.length != 1 || !values.single.isFinite) {
      throw const CalculationException('Некорректное выражение');
    }
    return values.single;
  }

  bool _shouldPop(String current, String top) => _isRightAssociative(current)
      ? _precedence(current) < _precedence(top)
      : _precedence(current) <= _precedence(top);

  int _precedence(String operator) => switch (operator) {
    'u-' => 3,
    '*' || '/' => 2,
    '+' || '-' => 1,
    _ => 0,
  };

  bool _isRightAssociative(String operator) => operator == 'u-';
  bool _isDigit(String value) =>
      value.codeUnitAt(0) >= 48 && value.codeUnitAt(0) <= 57;
  String _normalizeOperator(String value) => switch (value) {
    '×' => '*',
    '÷' => '/',
    _ => value,
  };
}

class CalculationException implements Exception {
  const CalculationException(this.message);
  final String message;
  @override
  String toString() => message;
}

enum _TokenType { number, operator, leftParenthesis, rightParenthesis, percent }

class _Token {
  const _Token._(this.type, [this.value]);
  factory _Token.number(String value) => _Token._(_TokenType.number, value);
  factory _Token.operator(String value) => _Token._(_TokenType.operator, value);
  const _Token.leftParenthesis() : this._(_TokenType.leftParenthesis);
  const _Token.rightParenthesis() : this._(_TokenType.rightParenthesis);
  const _Token.percent() : this._(_TokenType.percent);
  final _TokenType type;
  final String? value;
}
