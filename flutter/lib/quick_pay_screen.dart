import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:yandex_pay_quickpay/yandex_pay_quickpay.dart';

import 'cpqr_controller.dart';
import 'cpqr_section.dart';

// region State

sealed class QrScreenState {
  const QrScreenState();
}

class QrLoading extends QrScreenState {
  const QrLoading();
}

class QrReady extends QrScreenState {
  const QrReady(this.sessionId);
  final String sessionId;
}

class PaymentDisabled extends QrScreenState {
  const PaymentDisabled();
}

class PaymentSuccess extends QrScreenState {
  const PaymentSuccess();
}

class PaymentFailed extends QrScreenState {
  const PaymentFailed();
}

class QrError extends QrScreenState {
  const QrError(this.message);
  final String message;
}

String _friendlySdkError(Object e) {
  final s = e.toString();
  if (s.contains('not supported on iOS Simulator') ||
      s.contains('Code=-1020') ||
      (s.contains('LocalAuthentication') && s.contains('Simulator'))) {
    return 'На iOS Simulator нельзя завершить включение быстрой оплаты: '
        'SDK создаёт криптоключ (Secure Enclave / Face ID), на симуляторе это не поддерживается. '
        'Запустите на реальном iPhone.';
  }
  if (s.contains('Could not get jwk') || s.contains('jwk')) {
    return 'Не удалось получить ключ шифрования (JWK) для быстрой оплаты. '
        'Часто это сеть, VPN, прокси, неверное время на устройстве или ограничения окружения (эмулятор без Google Play). '
        'Проверьте подключение и повторите; при необходимости используйте устройство с сервисами Google.';
  }
  return s;
}

// endregion

class QuickPayScreen extends StatefulWidget {
  const QuickPayScreen({super.key});

  @override
  State<QuickPayScreen> createState() => QuickPayScreenState();
}

class QuickPayScreenState extends State<QuickPayScreen>
    with WidgetsBindingObserver {
  static QuickPayScreenState? instance;

  QrScreenState _state = const QrLoading();
  bool _sdkReady = false;
  bool _isEnabling = false;
  bool _isDisablingManually = false;
  bool _skipNextDisabledListener = false;
  int _paymentMethodsViewKey = 0;
  late final CpqrController _cpqrController;

  @override
  void initState() {
    super.initState();
    _cpqrController = CpqrController(YQuickPay.instance.getCPQR);
    instance = this;
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) => _initAndCheck());
  }

  @override
  void dispose() {
    _cpqrController.dispose();
    WidgetsBinding.instance.removeObserver(this);
    if (instance == this) instance = null;
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _bumpPaymentMethodsPlatformView();
    }
  }

  void _bumpPaymentMethodsPlatformView() {
    if (mounted) setState(() => _paymentMethodsViewKey++);
  }

  // region SDK init

  Future<void> _initAndCheck() async {
    if (mounted) setState(() => _sdkReady = true);
    await _checkPaymentState();
  }

  Future<void> _checkPaymentState({bool showErrors = false}) async {
    try {
      final enabled = await YQuickPay.instance.isQuickPaymentEnabled();
      if (enabled) {
        await _refreshSession();
      } else {
        _setState(const PaymentDisabled());
      }
    } catch (e) {
      if (showErrors) {
        _setState(QrError(_friendlySdkError(e)));
      } else {
        _setState(const PaymentDisabled());
      }
    }
  }

  Future<void> _refreshSession() async {
    _setState(const QrLoading());
    try {
      final sessionId = await YQuickPay.instance.getPaymentSessionId();
      _setState(QrReady(sessionId));
    } catch (e) {
      _setState(QrError(_friendlySdkError(e)));
    }
  }

  // endregion

  // region Actions

  Future<void> _enableQuickPayment() async {
    _isEnabling = true;
    try {
      final ok = await YQuickPay.instance.enableQuickPayment();
      if (_isEnabling) {
        _isEnabling = false;
        _bumpPaymentMethodsPlatformView();
        if (!ok) {
          _setState(
            const QrError(
              'Не удалось включить быструю оплату (отмена или ошибка сервиса). '
              'Если в логах QPAY_ExtDebug есть «Could not get jwk», проверьте сеть, VPN, время на устройстве '
              'и используйте образ с Google Play.',
            ),
          );
          return;
        }
        await _checkPaymentState(showErrors: true);
      }
    } catch (e) {
      _isEnabling = false;
      _bumpPaymentMethodsPlatformView();
      _setState(QrError(_friendlySdkError(e)));
    }
  }

  Future<void> _disableQuickPayment() async {
    _isDisablingManually = true;
    try {
      await YQuickPay.instance.disableQuickPayment();
      _isDisablingManually = false;
      _skipNextDisabledListener = true;
      _setState(const PaymentDisabled());
    } catch (e) {
      _isDisablingManually = false;
      _setState(QrError(_friendlySdkError(e)));
    }
  }

  Future<void> _logout() async {
    await YQuickPay.instance.logout();
    _setState(const PaymentDisabled());
  }

  // endregion

  // region Listener handlers

  void handlePaymentEnabledStateChanged(bool isEnabled) {
    if (!isEnabled && _skipNextDisabledListener) {
      _skipNextDisabledListener = false;
      return;
    }
    if (isEnabled) {
      _skipNextDisabledListener = false;
      _isEnabling = false;
      _refreshSession();
    } else if (_isEnabling) {
      // Task in flight — catch block will handle it
    } else if (_isDisablingManually) {
      // Manual disable: QR stays until disableQuickPayment() completes
    } else if (_state is QrReady) {
      // User turned quick pay off via the SDK widget switch
      _setState(const PaymentDisabled());
    } else {
      // Native widget finished (e.g. returned from Passport) but quick pay is still off.
      // Recreate PlatformView so Android view is not stuck; do not call enable again (would loop on JWK etc.).
      _bumpPaymentMethodsPlatformView();
      _checkPaymentState(showErrors: false);
    }
  }

  void handleSessionExpired() => _refreshSession();

  void handlePaymentResult(QuickPayResult result) {
    if (result is QuickPaySuccess) {
      _setState(const PaymentSuccess());
    } else {
      _setState(const PaymentFailed());
    }
  }

  // endregion

  void _setState(QrScreenState state) {
    if (mounted) setState(() => _state = state);
  }

  // region UI

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('QuickPay Demo'),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (_sdkReady)
              YandexPaymentMethodsWidget(
                key: ValueKey(_paymentMethodsViewKey),
                width: double.infinity,
              ),
            const SizedBox(height: 16),
            _buildStateContent(colorScheme),
            const SizedBox(height: 24),
            CpqrSection(controller: _cpqrController),
            const SizedBox(height: 24),
            _buildButtons(colorScheme),
          ],
        ),
      ),
    );
  }

  Widget _buildStateContent(ColorScheme colorScheme) {
    return switch (_state) {
      QrLoading() => const Column(
          children: [
            SizedBox(height: 32),
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Загрузка…', textAlign: TextAlign.center),
          ],
        ),
      QrReady(:final sessionId) => Column(
          children: [
            const SizedBox(height: 16),
            Center(
              child: QrImageView(
                data: sessionId,
                size: 256,
                errorCorrectionLevel: QrErrorCorrectLevel.M,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Покажите QR-код на кассе',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 16),
            ),
          ],
        ),
      PaymentDisabled() => const Padding(
          padding: EdgeInsets.symmetric(vertical: 16),
          child: Text(
            'Быстрая оплата не включена',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 16),
          ),
        ),
      PaymentSuccess() => const Text(
          'Оплата прошла успешно',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 16, color: Colors.green),
        ),
      PaymentFailed() => const Text(
          'Оплата не прошла',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 16, color: Colors.red),
        ),
      QrError(:final message) => Text(
          'Ошибка: $message',
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 16, color: Colors.red),
        ),
    };
  }

  Widget _buildButtons(ColorScheme colorScheme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ..._stateButtons(colorScheme),
        const SizedBox(height: 8),
        _logoutButton(colorScheme),
      ],
    );
  }

  List<Widget> _stateButtons(ColorScheme colorScheme) {
    return switch (_state) {
      PaymentDisabled() => [_enableButton(colorScheme)],
      QrReady() => [
          _disableButton(colorScheme),
          const SizedBox(height: 8),
          _refreshButton(colorScheme),
        ],
      PaymentSuccess() || PaymentFailed() || QrError() => [
          _refreshButton(colorScheme),
        ],
      _ => [],
    };
  }

  Widget _enableButton(ColorScheme colorScheme) => FilledButton(
        onPressed: _enableQuickPayment,
        style: FilledButton.styleFrom(
          backgroundColor: colorScheme.primary,
          foregroundColor: colorScheme.onPrimary,
          shape: const StadiumBorder(),
          padding: const EdgeInsets.symmetric(vertical: 14),
        ),
        child: const Text('Включить быструю оплату'),
      );

  Widget _disableButton(ColorScheme colorScheme) => FilledButton.tonal(
        onPressed: _disableQuickPayment,
        style: FilledButton.styleFrom(
          backgroundColor: colorScheme.secondaryContainer,
          foregroundColor: colorScheme.onSecondaryContainer,
          shape: const StadiumBorder(),
          padding: const EdgeInsets.symmetric(vertical: 14),
        ),
        child: const Text('Отключить быструю оплату'),
      );

  Widget _refreshButton(ColorScheme colorScheme) => OutlinedButton(
        onPressed: _refreshSession,
        style: OutlinedButton.styleFrom(
          foregroundColor: colorScheme.primary,
          shape: const StadiumBorder(),
          padding: const EdgeInsets.symmetric(vertical: 14),
        ),
        child: const Text('Обновить QR-код'),
      );

  Widget _logoutButton(ColorScheme colorScheme) => TextButton(
        onPressed: _logout,
        style: TextButton.styleFrom(
          foregroundColor: colorScheme.primary,
          padding: const EdgeInsets.symmetric(vertical: 14),
        ),
        child: const Text('Выйти из аккаунта'),
      );

  // endregion
}
