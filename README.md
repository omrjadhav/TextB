# TextB - Textbook Exchange Platform

TextB is an Android application built with Kotlin and Jetpack Compose that serves as a textbook exchange platform. It uses Supabase for backend services (authentication, database, storage) and CometChat for real-time messaging.

## Features

- User authentication via Supabase
- Textbook listings and search
- Real-time chat functionality using CometChat
- User profiles

## CometChat Integration

TextB uses CometChat for its real-time messaging functionality. The integration provides:

1. One-on-one messaging between users
2. Conversation list with unread message indicators
3. Message delivery and read receipts

### Setup Instructions

1. Create a CometChat account at [CometChat](https://www.cometchat.com/)
2. Create a new app in the CometChat dashboard
3. Replace the placeholder values in `TextBApplication.kt` with your actual CometChat credentials:
   - `APP_ID`: Your CometChat App ID
   - `REGION`: Your CometChat region (e.g., "us", "eu")
   - `API_KEY`: Your CometChat API Key
4. Replace the API_KEY in `ChatRepository.kt` with the same API key

### Implementation Details

- `TextBApplication.kt`: Initializes CometChat when the app starts
- `ChatRepository.kt`: Handles all chat-related operations using the CometChat SDK
- `ChatViewModel.kt`: Manages the UI state for chat functionality
- `ChatScreen.kt`: Displays the chat interface
- `ConversationListScreen.kt`: Displays the list of conversations

## Dependencies

- Jetpack Compose for UI
- Supabase for backend services
- CometChat SDK for real-time messaging
- Coil for image loading
- Material 3 for design components

## Getting Started

1. Clone the repository
2. Set up your Supabase and CometChat credentials
3. Build and run the app

## License

[Add your license information here]
