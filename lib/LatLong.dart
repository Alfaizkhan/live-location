class LatLong {
  final String _latitude;
  final String _longitude;

  LatLong([this._latitude, this._longitude]);

  /// get latitude.
  String get latitude => _latitude;

  /// get longitude.
  String get longitude => _longitude;

  /// return the string of latitude and longitude.
  @override
  String toString() {
    return 'Latitude: $_latitude, Longitude: $_longitude';
  }
}
