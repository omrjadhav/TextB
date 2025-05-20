package com.example.textbook.data.repository

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.example.textbook.TextBookApplication
import com.example.textbook.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AuthRepository @Inject constructor() {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun signUp(email: String, password: String, username: String, university: String): Result<User> {
        return try {
            withContext(Dispatchers.IO) {
                // Sign up with Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                
                // Create user profile in Firestore
                val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")
                val user = User(
                    id = userId,
                    email = email,
                    username = username,
                    university = university,
                    createdAt = System.currentTimeMillis().toString()
                )
                
                firestore.collection("users")
                    .document(userId)
                    .set(user)
                    .await()
                
                // Create CometChat user
                val cometChatResult = createCometChatUser(userId, username)
                if (cometChatResult.isFailure) {
                    throw cometChatResult.exceptionOrNull() ?: Exception("Failed to create CometChat user")
                }
                
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun createCometChatUser(uid: String, name: String): Result<com.cometchat.chat.models.User> {
        return suspendCancellableCoroutine { continuation ->
            val user = com.cometchat.chat.models.User()
            user.uid = uid
            user.name = name
            
            CometChat.createUser(user, TextBookApplication.AUTH_KEY, object : CometChat.CallbackListener<com.cometchat.chat.models.User>() {
                override fun onSuccess(user: com.cometchat.chat.models.User) {
                    continuation.resume(Result.success(user))
                }
                
                override fun onError(e: CometChatException) {
                    continuation.resume(Result.failure(Exception(e.message)))
                }
            })
            
            continuation.invokeOnCancellation {
                // No direct way to cancel createUser request in CometChat SDK
            }
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            withContext(Dispatchers.IO) {
                // Sign in with Firebase Auth
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                
                // Get user profile from Firestore
                val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                val user = userDoc.toObject(User::class.java)
                    ?: throw Exception("User profile not found")
                
                // Login to CometChat
                val cometChatResult = loginCometChat(userId)
                if (cometChatResult.isFailure) {
                    throw cometChatResult.exceptionOrNull() ?: Exception("Failed to login to CometChat")
                }
                
                Result.success(user.copy(id = userId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun loginCometChat(uid: String): Result<com.cometchat.chat.models.User> {
        return suspendCancellableCoroutine { continuation ->
            CometChat.login(uid, TextBookApplication.AUTH_KEY, object : CometChat.CallbackListener<com.cometchat.chat.models.User>() {
                override fun onSuccess(user: com.cometchat.chat.models.User) {
                    continuation.resume(Result.success(user))
                }
                
                override fun onError(e: CometChatException) {
                    continuation.resume(Result.failure(Exception(e.message)))
                }
            })
            
            continuation.invokeOnCancellation {
                // No direct way to cancel login request in CometChat SDK
            }
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // Sign out from CometChat
                val cometChatResult = logoutCometChat()
                
                // Sign out from Firebase Auth
                auth.signOut()
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun logoutCometChat(): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            CometChat.logout(object : CometChat.CallbackListener<String>() {
                override fun onSuccess(message: String) {
                    continuation.resume(Result.success(message))
                }
                
                override fun onError(e: CometChatException) {
                    continuation.resume(Result.failure(Exception(e.message)))
                }
            })
        }
    }
    
    suspend fun getCurrentUser(): Result<User?> {
        return try {
            withContext(Dispatchers.IO) {
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    Result.success(null)
                } else {
                    val userDoc = firestore.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()
                    
                    val user = userDoc.toObject(User::class.java)
                    Result.success(user?.copy(id = firebaseUser.uid))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
