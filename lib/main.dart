import 'package:flutter/material.dart';

import 'calculator/calculator_controller.dart';

void main() => runApp(const CalculatorApp());

class CalculatorApp extends StatelessWidget {
  const CalculatorApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Продвинутый калькулятор',
    debugShowCheckedModeBanner: false,
    restorationScopeId: 'calculator_app',
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2457A6)),
      useMaterial3: true,
    ),
    home: const CalculatorPage(),
  );
}

class CalculatorPage extends StatefulWidget {
  const CalculatorPage({super.key});

  @override
  State<CalculatorPage> createState() => _CalculatorPageState();
}

class _CalculatorPageState extends State<CalculatorPage> with RestorationMixin {
  final _controller = CalculatorController();
  final _savedState = RestorableStringN(null);

  @override
  String? get restorationId => 'calculator_page';

  @override
  void initState() {
    super.initState();
    _controller.addListener(_saveState);
  }

  @override
  void restoreState(RestorationBucket? oldBucket, bool initialRestore) {
    registerForRestoration(_savedState, 'calculator_state');
    final state = _savedState.value;
    if (state != null) _controller.restore(state);
  }

  void _saveState() => _savedState.value = _controller.serialize();

  @override
  void dispose() {
    _controller
      ..removeListener(_saveState)
      ..dispose();
    _savedState.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: _controller,
    builder: (context, _) => Scaffold(
      appBar: AppBar(
        title: const Text('Продвинутый калькулятор'),
        centerTitle: true,
      ),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) => Padding(
            padding: const EdgeInsets.all(12),
            child: constraints.maxWidth > 700
                ? Row(
                    children: [
                      Expanded(flex: 3, child: _calculatorPanel()),
                      const SizedBox(width: 16),
                      Expanded(flex: 2, child: _historyPanel()),
                    ],
                  )
                : Column(
                    children: [
                      Expanded(flex: 7, child: _calculatorPanel()),
                      const SizedBox(height: 12),
                      Expanded(flex: 3, child: _historyPanel()),
                    ],
                  ),
          ),
        ),
      ),
    ),
  );

  Widget _calculatorPanel() => Column(
    children: [
      _display(),
      const SizedBox(height: 12),
      Expanded(child: _keypad()),
    ],
  );

  Widget _display() => Semantics(
    label: 'Дисплей калькулятора',
    child: Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            _controller.expression.isEmpty ? '0' : _controller.expression,
            key: const Key('expressionDisplay'),
            textAlign: TextAlign.end,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          if (_controller.result.isNotEmpty)
            Text(
              '= ${_controller.result}',
              key: const Key('resultDisplay'),
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          if (_controller.error != null)
            Text(
              _controller.error!,
              key: const Key('errorDisplay'),
              textAlign: TextAlign.end,
              style: TextStyle(
                color: Theme.of(context).colorScheme.error,
                fontWeight: FontWeight.w600,
              ),
            ),
          const SizedBox(height: 4),
          Text(
            'Память: ${_formatMemory()}',
            style: Theme.of(context).textTheme.labelMedium,
          ),
        ],
      ),
    ),
  );

  Widget _keypad() {
    final keys = <_KeyDefinition>[
      _KeyDefinition('MC', _controller.clearMemory, memory: true),
      _KeyDefinition('MR', _controller.recallMemory, memory: true),
      _KeyDefinition('M+', _controller.addToMemory, memory: true),
      _KeyDefinition('M−', _controller.subtractFromMemory, memory: true),
      _KeyDefinition('C', _controller.clear, destructive: true),
      _KeyDefinition('⌫', _controller.backspace, destructive: true),
      _KeyDefinition('(', () => _controller.input('(')),
      _KeyDefinition(')', () => _controller.input(')')),
      _KeyDefinition('7', () => _controller.input('7')),
      _KeyDefinition('8', () => _controller.input('8')),
      _KeyDefinition('9', () => _controller.input('9')),
      _KeyDefinition('÷', () => _controller.input('÷'), operation: true),
      _KeyDefinition('4', () => _controller.input('4')),
      _KeyDefinition('5', () => _controller.input('5')),
      _KeyDefinition('6', () => _controller.input('6')),
      _KeyDefinition('×', () => _controller.input('×'), operation: true),
      _KeyDefinition('1', () => _controller.input('1')),
      _KeyDefinition('2', () => _controller.input('2')),
      _KeyDefinition('3', () => _controller.input('3')),
      _KeyDefinition('−', () => _controller.input('-'), operation: true),
      _KeyDefinition('±', _controller.toggleSign),
      _KeyDefinition('0', () => _controller.input('0')),
      _KeyDefinition('.', () => _controller.input('.')),
      _KeyDefinition('+', () => _controller.input('+'), operation: true),
      _KeyDefinition('%', () => _controller.input('%'), operation: true),
      _KeyDefinition('=', _controller.calculate, equals: true),
    ];
    return GridView.builder(
      itemCount: keys.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        mainAxisSpacing: 8,
        crossAxisSpacing: 8,
        childAspectRatio: 1.35,
      ),
      itemBuilder: (context, index) => _CalculatorKey(definition: keys[index]),
    );
  }

  Widget _historyPanel() => Card(
    clipBehavior: Clip.antiAlias,
    child: Column(
      children: [
        const ListTile(
          title: Text('История вычислений'),
          subtitle: Text('Последние 10 результатов'),
        ),
        const Divider(height: 1),
        Expanded(
          child: _controller.history.isEmpty
              ? const Center(child: Text('История пока пуста'))
              : ListView.separated(
                  itemCount: _controller.history.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final item = _controller.history[index];
                    return ListTile(
                      title: Text(item.expression),
                      subtitle: Text('= ${item.result}'),
                      onTap: () => _controller.selectHistory(item),
                    );
                  },
                ),
        ),
      ],
    ),
  );

  String _formatMemory() {
    final value = _controller.memory;
    return value == value.roundToDouble()
        ? value.toInt().toString()
        : value.toStringAsPrecision(8);
  }
}

class _KeyDefinition {
  const _KeyDefinition(
    this.label,
    this.onPressed, {
    this.memory = false,
    this.destructive = false,
    this.operation = false,
    this.equals = false,
  });
  final String label;
  final VoidCallback onPressed;
  final bool memory;
  final bool destructive;
  final bool operation;
  final bool equals;
}

class _CalculatorKey extends StatelessWidget {
  const _CalculatorKey({required this.definition});
  final _KeyDefinition definition;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final background = definition.equals
        ? scheme.primary
        : definition.destructive
        ? scheme.errorContainer
        : definition.memory || definition.operation
        ? scheme.secondaryContainer
        : scheme.surfaceContainerHigh;
    final foreground = definition.equals
        ? scheme.onPrimary
        : definition.destructive
        ? scheme.onErrorContainer
        : scheme.onSurface;
    return FilledButton(
      onPressed: definition.onPressed,
      style: FilledButton.styleFrom(
        backgroundColor: background,
        foregroundColor: foreground,
        textStyle: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
      ),
      child: Text(definition.label),
    );
  }
}