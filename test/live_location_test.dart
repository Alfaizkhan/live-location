import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:live_location/live_location.dart';

void main() {
  const MethodChannel channel = MethodChannel('live_location');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return false;
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getLatLong', () async {
    expect(await LiveLocation.getLatLong, false);
  });
}
