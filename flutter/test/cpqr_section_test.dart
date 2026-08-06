import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:quick_pay_sample/cpqr_controller.dart';
import 'package:quick_pay_sample/cpqr_section.dart';
import 'package:qr_flutter/qr_flutter.dart';

void main() {
  testWidgets('shows loading and generated YAQR', (tester) async {
    final completer = Completer<String>();
    String? receivedPayload;
    final controller = CpqrController((merchantPayload) {
      receivedPayload = merchantPayload;
      return completer.future;
    });
    addTearDown(controller.dispose);

    await tester.pumpWidget(_TestApp(controller: controller));
    await tester.tap(find.byKey(const Key('cpqr-generate')));
    await tester.pump();

    expect(receivedPayload, 'test');
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    completer.complete('yaqr-value');
    await tester.pumpAndSettle();

    expect(find.text('yaqr-value'), findsOneWidget);
    expect(find.byType(QrImageView), findsOneWidget);
  });

  testWidgets('forwards edited payload unchanged and renders error',
      (tester) async {
    String? receivedPayload;
    final controller = CpqrController((merchantPayload) async {
      receivedPayload = merchantPayload;
      throw StateError('request failed');
    });
    addTearDown(controller.dispose);

    await tester.pumpWidget(_TestApp(controller: controller));
    await tester.enterText(
      find.byKey(const Key('cpqr-payload')),
      '  custom payload  ',
    );
    await tester.tap(find.byKey(const Key('cpqr-generate')));
    await tester.pumpAndSettle();

    expect(receivedPayload, '  custom payload  ');
    expect(find.textContaining('request failed'), findsOneWidget);
    expect(find.byKey(const Key('cpqr-result')), findsNothing);
  });
}

class _TestApp extends StatelessWidget {
  const _TestApp({required this.controller});

  final CpqrController controller;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: CpqrSection(controller: controller),
        ),
      ),
    );
  }
}
