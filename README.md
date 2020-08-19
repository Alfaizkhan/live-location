# Live location
A Flutter plugin for detects location on Android devices.

<img src="https://raw.githubusercontent.com/Alfaizkhan/live-location/master/screenshot-1594614061065.jpg" width="240" height="480"> 

## Installation

Add `live_location` as a dependency in your pubspec.yaml file.

## Usage

```dart

/// Method to get the Live Location.
LatLongPosition position = await LiveLocation.getLatLong;

```

Using Stream.
```dart
// When you get the location for the first time, The location will be NULL.
// Stream Broadcast the Location every 15 seconds using LiveLocation.getLatLong;
LiveLocation.start(your_input);

```

## License

This plugin is open source project and the license is MIT LICENSE.
