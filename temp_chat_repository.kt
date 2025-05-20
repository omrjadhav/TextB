package com.example.textbook.data.repository

import android.content.Context
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ChatRepository @Inject constructor() {
    
    private val auth = FirebaseAuth.getInstance()
    
    // Initialize CometChat - this is done in the Application class
    suspend fun initializeChat(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if user is logged in to both Firebase and CometChat
            if (auth.currentUser != null && CometChat.getLoggedInUser() != null) {
                Result.success(true)
            } else {
                Result.failure(Exception("User not logged in"))
            }
        } catch (e: Exception) {
            Timber.e("Chat initialization failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Get conversations using CometChat
    suspend fun getConversations(): Result<List<ChatConversation>> {
        return try {
            suspendCancellableCoroutine { continuation ->
                val conversationsRequest = ConversationsRequest.ConversationsRequestBuilder()
                    .setLimit(50)
                    .build()
                
                conversationsRequest.fetchNext(object : CometChat.CallbackListener<List<Conversation>>() {
                    override fun onSuccess(conversations: List<Conversation>) {
                        val chatConversations = conversations.map { conversation ->
                            val lastMessage = conversation.lastMessage
                            val user = conversation.conversationWith as? User
                            val group = conversation.conversationWith as? Group
                            
                            ChatConversation(
                                id = conversation.conversationId,
                                name = user?.name ?: group?.name ?: "Unknown",
                                lastMessage = lastMessage?.text ?: "",
                                lastMessageTimestamp = conversation.lastMessage?.sentAt?.toLong() ?: 0,
                                unreadCount = conversation.unreadMessageCount,
                                participantIds = listOf(CometChat.getLoggedInUser()?.uid ?: "", user?.uid ?: "")
                            )
                        }
                        continuation.resume(Result.success(chatConversations))
                    }
                    
                    override fun onError(e: CometChatException) {
                        Timber.e("Failed to get conversations: ${e.message}")
                        continuation.resume(Result.failure(e))
                    }
                })
            }
        } catch (e: Exception) {
            Timber.e("Failed to get conversations: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Get real-time messages for a conversation using CometChat
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        // First, send the existing messages
        val messagesRequest = CometChat.getMessagesRequest(
            conversationId, 
            CometChatConstants.RECEIVER_TYPE_USER, 
            30
        )
        
        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<com.cometchat.chat.models.BaseMessage>>() {
            override fun onSuccess(baseMessages: List<com.cometchat.chat.models.BaseMessage>) {
                val messages = baseMessages.filterIsInstance<TextMessage>().map { message ->
                    ChatMessage(
                        id = message.muid,
                        text = message.text,
                        senderId = message.sender.uid,
                        receiverId = message.receiverUid,
                        timestamp = message.sentAt,
                        isRead = message.readAt > 0
                    )
                }
                trySend(messages)
            }
            
            override fun onError(e: CometChatException) {
                Timber.e("Failed to get initial messages: ${e.message}")
                // Don't close the flow on error, just log it
            }
        })
        
        // Set up the message listener for real-time updates
        val listenerID = "MESSAGES_LISTENER_$conversationId"
        CometChat.addMessageListener(listenerID, object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage) {
                if (message.receiverUid == conversationId || message.sender.uid == conversationId) {
                    // Refresh the entire message list when a new message is received
                    messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<com.cometchat.chat.models.BaseMessage>>() {
                        override fun onSuccess(baseMessages: List<com.cometchat.chat.models.BaseMessage>) {
                            val messages = baseMessages.filterIsInstance<TextMessage>().map { msg ->
                                ChatMessage(
                                    id = msg.muid,
                                    text = msg.text,
                                    senderId = msg.sender.uid,
                                    receiverId = msg.receiverUid,
                                    timestamp = msg.sentAt,
                                    isRead = msg.readAt > 0
                                )
                            }
                            trySend(messages)
                        }
                        
                        override fun onError(e: CometChatException) {
                            Timber.e("Failed to refresh messages: ${e.message}")
                        }
                    })
                }
            }
        })
        
        awaitClose {
            CometChat.removeMessageListener(listenerID)
        }
    }
    
    // Get messages for a conversation using CometChat
    suspend fun getMessages(conversationId: String): Result<List<ChatMessage>> {
        return suspendCancellableCoroutine { continuation ->
            val messagesRequest = CometChat.getMessagesRequest(
                conversationId, 
                CometChatConstants.RECEIVER_TYPE_USER, 
                30
            )
            
            messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<com.cometchat.chat.models.BaseMessage>>() {
                override fun onSuccess(baseMessages: List<com.cometchat.chat.models.BaseMessage>) {
                    val messages = baseMessages.filterIsInstance<TextMessage>().map { message ->
                        ChatMessage(
                            id = message.muid,
                            text = message.text,
                            senderId = message.sender.uid,
                            receiverId = message.receiverUid,
                            timestamp = message.sentAt,
                            isRead = message.readAt > 0
                        )
                    }
                    continuation.resume(Result.success(messages))
                }
                
                override fun onError(e: CometChatException) {
                    Timber.e("Failed to get messages: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
            })
        }
    }
    
    // Get user details using CometChat
    suspend fun getUserDetails(userId: String): Result<User> {
        return suspendCancellableCoroutine { continuation ->
            CometChat.getUser(userId, object : CometChat.CallbackListener<User>() {
                override fun onSuccess(user: User) {
                    continuation.resume(Result.success(user))
                }
                
                override fun onError(e: CometChatException) {
                    Timber.e("Failed to get user details: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
            })
        }
    }
    
    // Send message using CometChat
    suspend fun sendMessage(text: String, receiverId: String, conversationId: String): Result<TextMessage> {
        return suspendCancellableCoroutine { continuation ->
            val textMessage = TextMessage(receiverId, text, CometChatConstants.RECEIVER_TYPE_USER)
            
            CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
                override fun onSuccess(message: TextMessage) {
                    continuation.resume(Result.success(message))
                }
                
                override fun onError(e: CometChatException) {
                    Timber.e("Failed to send message: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
            })
        }
    }
    
    // Mark message as read using CometChat
    suspend fun markMessageAsRead(messageId: String, senderId: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            // Get the message first
            CometChat.getMessageByID(messageId, object : CometChat.CallbackListener<com.cometchat.chat.models.BaseMessage>() {
                override fun onSuccess(message: com.cometchat.chat.models.BaseMessage) {
                    // Mark the message as read
                    CometChat.markAsRead(message, object : CometChat.CallbackListener<Void>() {
                        override fun onSuccess(result: Void?) {
                            continuation.resume(Result.success(Unit))
                        }
                        
                        override fun onError(e: CometChatException) {
                            Timber.e("Failed to mark message as read: ${e.message}")
                            continuation.resume(Result.failure(e))
                        }
                    })
                }
                
                override fun onError(e: CometChatException) {
                    Timber.e("Failed to get message: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
            })
        }
    }
    
    // Get or create a conversation using CometChat
    suspend fun getOrCreateConversation(otherUserId: String): Result<String> {
        // In CometChat, conversations are created automatically when messages are exchanged
        // So we just return the user ID as the conversation ID
        return Result.success(otherUserId)
    }
    
    // Data classes for chat functionality
    data class ChatUser(
        val id: String,
        val name: String,
        val avatar: String? = null
    )
    
    data class ChatMessage(
        val id: String,
        val text: String,
        val senderId: String,
        val receiverId: String,
        val timestamp: Long,
        val isRead: Boolean = false
    )
    
    data class ChatConversation(
        val id: String,
        val name: String,
        val lastMessage: String,
        val lastMessageTimestamp: Long,
        val unreadCount: Int = 0,
        val participantIds: List<String> = emptyList()
    )
    
    // Convert CometChat User to ChatUser
    fun convertUser(user: User): ChatUser {
        return ChatUser(
            id = user.uid,
            name = user.name,
            avatar = user.avatar
        )
    }
    
    // Convert CometChat TextMessage to ChatMessage
    fun convertMessage(message: TextMessage): ChatMessage {
        return ChatMessage(
            id = message.muid,
            text = message.text,
            senderId = message.sender.uid,
            receiverId = message.receiverUid,
            timestamp = message.sentAt,
            isRead = message.readAt > 0
        )
    }
}
