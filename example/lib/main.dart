import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:live_location/live_location.dart';

import 'package:location_permissions/location_permissions.dart';

void main() => runApp(LocationExample());

class LocationExample extends StatefulWidget {
  @override
  _LocationExampleState createState() => _LocationExampleState();
}

class _LocationExampleState extends State<LocationExample> {
  String _latitude;
  String _longitude;

  /// initialize state.
  @override
  void initState() {
    super.initState();
    requestLocationPermission();

    /// On first run the location will be null
    /// so it called in every 15 seconds to get location
    LiveLocation.start(15);

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
          centerTitle: true,
        ),
        body: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Center(
              child: Column(
            children: <Widget>[
              Text('Latitude: $_latitude',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.w500),
              ),
              Text('Longitude: $_longitude',
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.w500),
              ),
            ],
          )),
        ),
      ),
    );
  }
}
