# Weather App

## Overview
Weather App is an Android application that provides real-time weather information for any city. The app uses the WeatherAPI to fetch current weather conditions and forecasts, and integrates Firebase Realtime Database for data persistence.

## Features
- 🌍 Location-based weather detection
- 🔍 City search functionality
- 📊 Hourly weather forecast
- 🌞 Day/Night background adaptation
- 📱 Clean and intuitive UI

## Screenshots
![Screen1](ScreenShot/screen_1.jpg)
![Screen2](ScreenShot/screen_2.jpg)

## Prerequisites
- Android Studio
- Android device or emulator running Android 5.0 (Lollipop) or higher
- Internet connection

## Installation

### Clone the Repository
```bash
https://github.com/OussamaRemli/WeatherApp.git
```

### Setup WeatherAPI
1. Sign up at [WeatherAPI](https://www.weatherapi.com/)
2. Get your API key
3. Replace the API key in `MainActivity.java`:
```java
String url = "https://api.weatherapi.com/v1/forecast.json?key=YOUR_API_KEY&q=" + cityName + "&days=1&aqi=no&alerts=no";
```

### Firebase Configuration
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download and add the `google-services.json` to your project's `app` directory

### Dependencies
Ensure the following dependencies are added to your `build.gradle`:
- Volley for API requests
- Picasso for image loading
- Firebase Realtime Database
- Firebase Authentication

## Permissions
The app requires the following permissions:
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- Internet access

## Technologies Used
- Java
- Android SDK
- WeatherAPI
- Firebase Realtime Database
- Volley
- Picasso

## App Structure
- `MainActivity.java`: Main activity handling location, weather fetching, and UI updates
- `WeatherRVAdapter.java`: RecyclerView adapter for hourly forecast
- `WeatherRVModal.java`: Data model for weather information

## How It Works
1. App detects user's current location
2. Fetches weather data from WeatherAPI
3. Displays current weather and hourly forecast
4. Stores forecast data in Firebase Realtime Database
5. Allows manual city search
