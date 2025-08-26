# Edenic - Stock Market Gamification App

Edenic is an Android application that gamifies the stock market trading experience, allowing users to trade virtual stocks with real-time market data in a risk-free environment while competing with friends on a global leaderboard.

## Features

### Real-Time Trading
- Buy and sell stocks with current market data from Yahoo Finance API
- View detailed stock information and price charts
- Track profit and loss on your investments
- Portfolio management with comprehensive analytics

### Social Trading
- Connect with other traders and share stock recommendations
- Real-time chat system with read receipts
- Send stock cards directly in conversations
- Find and add new trading friends

### Gamification Elements
- Global leaderboard to compete with other traders
- Performance tracking with daily change percentages
- Visual indicators for portfolio performance
- User profiles with trading statistics

### User Experience
- Modern, intuitive interface with Material Design
- Dark theme optimized for market data visualization
- Smooth navigation with bottom bar layout
- Onboarding screens for new users
- Profile customization

## Technical Implementation

### Architecture
- Java-based Android application
- Firebase Realtime Database for data persistence
- Firebase Authentication with Google Sign-In
- Firebase Cloud Messaging for push notifications

### APIs and Libraries
- Yahoo Finance API for real-time stock data
- Firebase for backend and authentication
- Glide for image loading and caching
- Retrofit for network requests
- Google Sheets integration for reliable market data

### Key Components
- Real-time price updates with WebSocket connections
- Transaction history with filtering options
- Chat system with message delivery status
- Persistent user sessions
- Push notifications for price alerts and social interactions

## Setup and Installation

1. Clone the repository
2. Configure Firebase:
    - Create a Firebase project
    - Add the Android app to your Firebase project
    - Download the `google-services.json` file and place it in the app directory
3. Configure the Yahoo Finance API credentials in the project
4. Build and run the app on an Android device or emulator

## Requirements

- Android 8.0 (API level 26) or higher
- Google Play Services
- Internet connection

## Acknowledgments

- Stock data provided by Yahoo Finance API
- Icons and graphics from various open-source repositories
- Thanks to all contributors who have helped shape this project

## Co-author  
[@Hrutuja](https://github.com/hrutujaX)


