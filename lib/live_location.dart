import 'dart:async';
import 'package:flutter/services.dart';

import 'LatLong.dart';

class LiveLocation {
  static const MethodChannel _channel = const MethodChannel('live_location');
  static var changeController = new StreamController<LatLong>.broadcast();
  static Timer getLocationTimer;

  /// get location
  static Future<void> getLocation() async {
    List<String> pos;
    try {
      pos = await LiveLocation.getLatLong;
      changeController
          .add(new LatLong(pos[0], pos[1]));
    } on PlatformException catch (e) {
      print('PlatformException: $e');
    }
  }

  /// location with repeating by timer
  static start(time) {
    getLocationTimer =
        Timer.periodic(Duration(seconds: time), (Timer t) => getLocation());
  }

  /// stop repeating by timer
  static stop() {
    getLocationTimer.cancel();
  }
  /// the stream getter where others can listen to.
  static Stream<LatLong> get onChange => changeController.stream;

  /// Method to get the current location.
  static Future<List<String>> get getLatLong async {
    final String latitude = await _channel.invokeMethod('getLatitude');
    final String longitude = await _channel.invokeMethod('getLongitude');
    return [latitude, longitude];
  }


  void dispose() {
    changeController.close();
  }
}
