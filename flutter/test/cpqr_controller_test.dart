import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:quick_pay_sample/cpqr_controller.dart';

void main() {
  test('forwards exact merchant payload and exposes loading then success',
      () async {
    final completer = Completer<String>();
    String? receivedPayload;
    final controller = CpqrController((merchantPayload) {
      receivedPayload = merchantPayload;
      return completer.future;
    });

    final request = controller.generate('  merchant payload  ');

    expect(receivedPayload, '  merchant payload  ');
    expect(controller.state, isA<CpqrLoading>());

    completer.complete('yaqr-value');
    await request;

    expect(controller.state, const CpqrSuccess('yaqr-value'));
  });

  test('failure clears previous YAQR and exposes error', () async {
    var shouldFail = false;
    final controller = CpqrController((_) async {
      if (shouldFail) throw StateError('network failed');
      return 'old-yaqr';
    });

    await controller.generate('first');
    shouldFail = true;
    await controller.generate('second');

    final state = controller.state;
    expect(state, isA<CpqrFailure>());
    expect((state as CpqrFailure).message, contains('network failed'));
  });

  test('new request clears previous YAQR while loading', () async {
    final responses = <Completer<String>>[
      Completer<String>()..complete('first-yaqr'),
      Completer<String>(),
    ];
    var invocation = 0;
    final controller = CpqrController((_) => responses[invocation++].future);

    await controller.generate('first');
    final secondRequest = controller.generate('second');

    expect(controller.state, isA<CpqrLoading>());

    responses[1].complete('second-yaqr');
    await secondRequest;
  });

  test('ignores duplicate request while loading', () async {
    final completer = Completer<String>();
    var calls = 0;
    final controller = CpqrController((_) {
      calls++;
      return completer.future;
    });

    final first = controller.generate('first');
    await controller.generate('second');

    expect(calls, 1);
    completer.complete('yaqr');
    await first;
  });

  test('does not notify after disposal when request completes', () async {
    final completer = Completer<String>();
    final controller = CpqrController((_) => completer.future);
    final request = controller.generate('payload');

    controller.dispose();
    completer.complete('yaqr');

    await expectLater(request, completes);
  });
}
