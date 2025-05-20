package com.example.textb.chat

import android.content.Context
import android.util.Log
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User

/**
 * Manager class for CometChat operations
 */
object CometChatManager {
    private const val TAG = "CometChatManager"
    
    // CometChat credentials
    private const val APP_ID = "2757064e22625650"
    private const val REGION = "in"
    private const val AUTH_KEY = "fe791696c30931a0634c6112289632fa27e90245"
    
    /**
     * Initialize CometChat SDK
     */
    fun init(context: Context, callback: (Boolean, String?) -> Unit) {
        val appSettings = CometChat.AppSettings.Builder()
            .subscribePresenceForAllUsers()
            .setRegion(REGION)
            .build()
            
        CometChat.init(context, APP_ID, appSettings, object : CallbackListener<String>() {
            override fun onSuccess(message: String?) {
                Log.d(TAG, "CometChat initialization successful: $message")
                callback(true, null)
            }
            
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "CometChat initialization failed: ${e?.message}")
                callback(false, e?.message)
            }
        })
    }
    
    /**
     * Login user to CometChat
     */
    fun loginUser(userId: String, callback: (Boolean, User?, String?) -> Unit) {
        CometChat.login(userId, AUTH_KEY, object : CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "CometChat login successful: ${user.uid}")
                callback(true, user, null)
            }
            
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "CometChat login failed: ${e?.message}")
                callback(false, null, e?.message)
            }
        })
    }
    
    /**
     * Register a new user with CometChat
     */
    fun registerUser(userId: String, name: String, callback: (Boolean, User?, String?) -> Unit) {
        val user = User()
        user.uid = userId
        user.name = name
        
        CometChat.createUser(user, AUTH_KEY, object : CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "User created successfully: ${user.uid}")
                callback(true, user, null)
            }
            
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "User creation failed: ${e?.message}")
                callback(false, null, e?.message)
            }
        })
    }
    
    /**
     * Logout from CometChat
     */
    fun logout(callback: (Boolean, String?) -> Unit) {
        CometChat.logout(object : CallbackListener<String>() {
            override fun onSuccess(message: String?) {
                Log.d(TAG, "CometChat logout successful")
                callback(true, null)
            }
            
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "CometChat logout failed: ${e?.message}")
                callback(false, e?.message)
            }
        })
    }
    
    /**
     * Get the current logged in user
     */
    fun getCurrentUser(): User? {
        return CometChat.getLoggedInUser()
    }
}
