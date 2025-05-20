package com.example.textb.chat

import android.util.Log
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.MessagesRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.models.MessageReceipt
import com.cometchat.pro.models.TextMessage
import com.cometchat.pro.models.TypingIndicator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Service class for handling real-time messaging with CometChat
 */
class RealTimeMessagingService {
    private val TAG = "RealTimeMessagingService"
    
    /**
     * Send a text message to a user
     */
    fun sendMessage(receiverId: String, text: String, callback: (TextMessage?, String?) -> Unit) {
        val textMessage = TextMessage(receiverId, text, CometChatConstants.RECEIVER_TYPE_USER)
        
        CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
            override fun onSuccess(message: TextMessage) {
                Log.d(TAG, "Message sent successfully: ${message.messageId}")
                callback(message, null)
            }
            
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "Message sending failed: ${e?.message}")
                callback(null, e?.message)
            }
        })
    }
    
    /**
     * Mark a message as read
     */
    fun markAsRead(messageId: String, receiverId: String) {
        CometChat.markAsRead(messageId, receiverId, CometChatConstants.RECEIVER_TYPE_USER)
    }
    
    /**
     * Send typing indicator to a user
     */
    fun sendTypingIndicator(receiverId: String) {
        val typingIndicator = TypingIndicator(receiverId, CometChatConstants.RECEIVER_TYPE_USER)
        CometChat.startTyping(typingIndicator)
    }
    
    /**
     * Stop typing indicator
     */
    fun stopTypingIndicator(receiverId: String) {
        val typingIndicator = TypingIndicator(receiverId, CometChatConstants.RECEIVER_TYPE_USER)
        CometChat.endTyping(typingIndicator)
    }
    
    /**
     * Get previous messages with a user
     */
    suspend fun getMessages(userId: String, limit: Int = 30): List<BaseMessage> = suspendCancellableCoroutine { continuation ->
        val messagesRequest = MessagesRequest.MessagesRequestBuilder()
            .setUID(userId)
            .setLimit(limit)
            .build()
            
        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(messages: List<BaseMessage>) {
                continuation.resume(messages)
            }
            
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "Error fetching messages: ${e?.message}")
                continuation.resume(emptyList())
            }
        })
    }
    
    /**
     * Listen for real-time messages, typing indicators, and read receipts
     */
    fun listenForRealTimeUpdates(
        onMessageReceived: (BaseMessage) -> Unit,
        onTypingStarted: (TypingIndicator) -> Unit,
        onTypingEnded: (TypingIndicator) -> Unit,
        onMessageRead: (MessageReceipt) -> Unit
    ) {
        // Message listener
        CometChat.addMessageListener("MESSAGE_LISTENER_ID", object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage) {
                Log.d(TAG, "Text message received: ${message.messageId}")
                onMessageReceived(message)
                
                // Mark message as read
                markAsRead(message.id, message.sender.uid)
            }
        })
        
        // Typing indicator listener
        CometChat.addMessageListener("TYPING_LISTENER_ID", object : CometChat.MessageListener() {
            override fun onTypingStarted(typingIndicator: TypingIndicator) {
                Log.d(TAG, "Typing started: ${typingIndicator.sender.uid}")
                onTypingStarted(typingIndicator)
            }
            
            override fun onTypingEnded(typingIndicator: TypingIndicator) {
                Log.d(TAG, "Typing ended: ${typingIndicator.sender.uid}")
                onTypingEnded(typingIndicator)
            }
        })
        
        // Read receipt listener
        CometChat.addMessageListener("READ_RECEIPT_LISTENER_ID", object : CometChat.MessageListener() {
            override fun onMessagesRead(messageReceipt: MessageReceipt) {
                Log.d(TAG, "Message read: ${messageReceipt.messageId}")
                onMessageRead(messageReceipt)
            }
        })
    }
    
    /**
     * Remove all listeners
     */
    fun removeAllListeners() {
        CometChat.removeMessageListener("MESSAGE_LISTENER_ID")
        CometChat.removeMessageListener("TYPING_LISTENER_ID")
        CometChat.removeMessageListener("READ_RECEIPT_LISTENER_ID")
    }
}
