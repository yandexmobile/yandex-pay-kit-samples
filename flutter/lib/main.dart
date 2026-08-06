import 'package:flutter/material.dart';
import 'package:yandex_pay_quickpay/yandex_pay_quickpay.dart';

import 'quick_pay_screen.dart';

const _merchantId = String.fromEnvironment(
  'MERCHANT_ID',
  defaultValue: '0df18b44-cb01-4263-b05f-6de81e9b5692',
);

YPayEnvironment _getEnvironment() {
  const name = String.fromEnvironment(
    'QUICKPAY_ENVIRONMENT',
    defaultValue: 'sandbox',
  );
  return switch (name.toLowerCase()) {
    'production' => YPayEnvironment.production,
    _ => YPayEnvironment.sandbox,
  };
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const _QuickPayBootstrap());
}

class _QuickPayBootstrap extends StatefulWidget {
  const _QuickPayBootstrap();

  @override
  State<_QuickPayBootstrap> createState() => _QuickPayBootstrapState();
}

class _QuickPayBootstrapState extends State<_QuickPayBootstrap> {
  bool _ready = false;
  Object? _initError;

  @override
  void initState() {
    super.initState();
    _initSdk();
  }

  Future<void> _initSdk() async {
    try {
      // New modular API — recommended for new integrations.
      //
      // Equivalent legacy API (still works, kept for backward compatibility):
      //
      //   await YandexQuickPay.initialize(
      //     config: QuickPayConfig(merchantId: _merchantId, environment: ...),
      //     listener: QuickPaymentStateListener(...),
      //   );
      //
      final stateListener = YQuickPaymentStateListener(
        onPaymentEnabledStateChanged: (isEnabled) =>
            QuickPayScreenState.instance?.handlePaymentEnabledStateChanged(isEnabled),
        onSessionExpired: () =>
            QuickPayScreenState.instance?.handleSessionExpired(),
        onPaymentResult: (result) =>
            QuickPayScreenState.instance?.handlePaymentResult(result),
      );

      await YPay.initialize(
        environment: _getEnvironment(),
        modules: [
          quickPayModule(
            merchantId: _merchantId,
            stateListener: stateListener,
          ),
        ],
      );

      if (mounted) setState(() => _ready = true);
    } catch (e, st) {
      debugPrint('YPay.initialize failed: $e\n$st');
      if (mounted) setState(() => _initError = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_initError != null) {
      return MaterialApp(
        home: Scaffold(
          body: Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Text(
                'Ошибка инициализации SDK:\n$_initError',
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ),
      );
    }
    if (!_ready) {
      return const MaterialApp(
        home: Scaffold(
          body: Center(child: CircularProgressIndicator()),
        ),
      );
    }
    return const QuickPaySampleApp();
  }
}

class QuickPaySampleApp extends StatelessWidget {
  const QuickPaySampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'QuickPay Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF6750A4)),
        useMaterial3: true,
      ),
      home: const QuickPayScreen(),
    );
  }
}
