package com.example.textb

import android.app.Application
import android.util.Log
import com.example.textb.chat.CometChatManager

class TextBApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize CometChat
        CometChatManager.init(this) { success, error ->
            if (success) {
                Log.d("TextBApplication", "CometChat initialized successfully")
            } else {
                Log.e("TextBApplication", "CometChat initialization failed: $error")
            }
        }
        
        Log.d("TextBApplication", "Application started")
    }
}
