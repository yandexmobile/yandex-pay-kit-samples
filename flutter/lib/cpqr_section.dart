import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qr_flutter/qr_flutter.dart';

import 'cpqr_controller.dart';

final class CpqrSection extends StatefulWidget {
  const CpqrSection({required this.controller, super.key});

  final CpqrController controller;

  @override
  State<CpqrSection> createState() => _CpqrSectionState();
}

final class _CpqrSectionState extends State<CpqrSection> {
  late final TextEditingController _payloadController;

  @override
  void initState() {
    super.initState();
    _payloadController = TextEditingController(text: 'test');
  }

  @override
  void dispose() {
    _payloadController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: AnimatedBuilder(
          animation: widget.controller,
          builder: (context, _) {
            final state = widget.controller.state;
            final isLoading = state is CpqrLoading;
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Кассовый QR-код',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 12),
                TextField(
                  key: const Key('cpqr-payload'),
                  controller: _payloadController,
                  enabled: !isLoading,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: 'Payload мерчанта',
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton.icon(
                  key: const Key('cpqr-generate'),
                  onPressed: isLoading
                      ? null
                      : () =>
                          widget.controller.generate(_payloadController.text),
                  icon: isLoading
                      ? const SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.qr_code_2),
                  label: const Text('Получить CPQR'),
                ),
                if (state case CpqrSuccess(:final value)) ...[
                  const SizedBox(height: 16),
                  _CpqrResult(value: value),
                ],
                if (state case CpqrFailure(:final message)) ...[
                  const SizedBox(height: 8),
                  Text(
                    message,
                    key: const Key('cpqr-error'),
                    style:
                        TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                ],
              ],
            );
          },
        ),
      ),
    );
  }
}

final class _CpqrResult extends StatelessWidget {
  const _CpqrResult({required this.value});

  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      key: const Key('cpqr-result'),
      children: [
        QrImageView(
          data: value,
          size: 180,
          errorCorrectionLevel: QrErrorCorrectLevel.M,
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: SelectableText(
                value,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              ),
            ),
            IconButton(
              key: const Key('cpqr-copy'),
              tooltip: 'Копировать CPQR',
              onPressed: () {
                Clipboard.setData(ClipboardData(text: value));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('CPQR скопирован')),
                );
              },
              icon: const Icon(Icons.copy),
            ),
          ],
        ),
      ],
    );
  }
}
