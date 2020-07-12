import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:live_location/live_location.dart';

import 'package:location_permissions/location_permissions.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _latitude;
  String _longitude;

  /// initialize state.
  @override
  void initState() {
    super.initState();
    requestLocationPermission();
    LiveLocation.start(5);
    getLocation();
  }

  Future<void> getLocation() async {
    try {
      LiveLocation.onChange.listen((values) => setState(() {
            _latitude = values.latitude;
            _longitude = values.longitude;
          }));
    } on PlatformException catch (e) {
      print('PlatformException $e');
    }
  }

  void requestLocationPermission() async {
    PermissionStatus permission =
        await LocationPermissions().requestPermissions();
    print('permissions: $permission');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Live Location Plugin'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Center(
              child: Column(
            children: <Widget>[
              Text('Latitude: $_latitude, Longitude: $_longitude'),
            ],
          )),
        ),
      ),
    );
  }
}
