package com.example.textb.ui.screens.chat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.models.MessageReceipt
import com.cometchat.pro.models.TypingIndicator
import com.cometchat.pro.uikit.ui_components.messages.CometChatMessages
import com.example.textb.chat.CometChatManager
import com.example.textb.chat.RealTimeMessagingService
import kotlinx.coroutines.launch

/**
 * Chat screen using CometChat UI components with real-time messaging features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CometChatScreen(
    onBackClick: () -> Unit,
    receiverId: String,
    receiverName: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val messagingService = remember { RealTimeMessagingService() }
    
    // State for typing indicator
    var isReceiverTyping by remember { mutableStateOf(false) }
    var lastMessageRead by remember { mutableStateOf<String?>(null) }
    
    // Set up real-time listeners
    DisposableEffect(receiverId) {
        messagingService.listenForRealTimeUpdates(
            onMessageReceived = { message ->
                // Message received, mark as read
                messagingService.markAsRead(message.id, message.sender.uid)
            },
            onTypingStarted = { typingIndicator ->
                if (typingIndicator.sender.uid == receiverId) {
                    isReceiverTyping = true
                }
            },
            onTypingEnded = { typingIndicator ->
                if (typingIndicator.sender.uid == receiverId) {
                    isReceiverTyping = false
                }
            },
            onMessageRead = { messageReceipt ->
                lastMessageRead = messageReceipt.messageId
            }
        )
        
        onDispose {
            messagingService.removeAllListeners()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(receiverName)
                        if (isReceiverTyping) {
                            Text(
                                "Typing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Check if user is logged in to CometChat
            val currentUser = CometChatManager.getCurrentUser()
            if (currentUser == null) {
                // Show login required message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Please log in to chat")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // This would typically be handled by your auth flow
                            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    ) {
                        Text("Go Back")
                    }
                }
            } else {
                // Use CometChat's UI components for messaging
                AndroidView(
                    factory = { ctx ->
                        // Create CometChat messages component
                        CometChatMessages(ctx).apply {
                            // Set the user to chat with
                            val conversationWith = receiverId
                            val conversationType = CometChatConstants.RECEIVER_TYPE_USER
                            setConversation(conversationWith, conversationType)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // Update typing indicator if needed
                        if (isReceiverTyping) {
                            // The typing indicator is handled by CometChat UI component automatically
                        }
                    }
                )
                
                // Show read receipt indicator if needed
                lastMessageRead?.let { messageId ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { lastMessageRead = null }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text("Message read")
                    }
                }
            }
        }
    }
}
