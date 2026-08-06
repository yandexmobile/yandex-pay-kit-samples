import 'package:flutter/foundation.dart';

typedef CpqrGenerator = Future<String> Function(String merchantPayload);

@immutable
sealed class CpqrState {
  const CpqrState();
}

final class CpqrIdle extends CpqrState {
  const CpqrIdle();
}

final class CpqrLoading extends CpqrState {
  const CpqrLoading();
}

final class CpqrSuccess extends CpqrState {
  const CpqrSuccess(this.value);

  final String value;

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is CpqrSuccess && value == other.value;

  @override
  int get hashCode => value.hashCode;
}

final class CpqrFailure extends CpqrState {
  const CpqrFailure(this.message);

  final String message;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is CpqrFailure && message == other.message;

  @override
  int get hashCode => message.hashCode;
}

final class CpqrController extends ChangeNotifier {
  CpqrController(this._generateCpqr);

  final CpqrGenerator _generateCpqr;

  CpqrState _state = const CpqrIdle();
  bool _isDisposed = false;

  CpqrState get state => _state;

  Future<void> generate(String merchantPayload) async {
    if (_isDisposed || _state is CpqrLoading) return;

    _setState(const CpqrLoading());
    try {
      final value = await _generateCpqr(merchantPayload);
      _setState(CpqrSuccess(value));
    } on Object catch (error) {
      _setState(CpqrFailure(error.toString()));
    }
  }

  void _setState(CpqrState state) {
    if (_isDisposed) return;
    _state = state;
    notifyListeners();
  }

  @override
  void dispose() {
    _isDisposed = true;
    super.dispose();
  }
}
