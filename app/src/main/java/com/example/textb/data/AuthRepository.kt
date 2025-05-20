package com.example.textb.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.example.textb.chat.CometChatManager
import kotlin.coroutines.resume

class AuthRepository {
    private val supabaseClient: SupabaseClient = SupabaseClientManager.getClient()
    
    suspend fun signUp(email: String, password: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Sign up with Supabase
            val response = supabaseClient.gotrue.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = JsonObject(mapOf("name" to JsonPrimitive(name)))
            }
            
            // Register user with CometChat
            val userId = response.user?.id ?: return@withContext Result.failure(Exception("Failed to get user ID"))
            var cometChatSuccess = false
            
            // Use a suspending function wrapper for the callback-based CometChat registration
            val cometChatResult = kotlin.runCatching {
                suspendCancellableCoroutine<Unit> { continuation ->
                    CometChatManager.registerUser(userId, name) { success, _, error ->
                        cometChatSuccess = success
                        if (success) {
                            continuation.resume(Unit, null)
                        } else {
                            continuation.resume(Unit, null) // Still continue even if CometChat fails
                        }
                    }
                }
            }
            
            // Log in to CometChat if registration was successful
            if (cometChatSuccess) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    CometChatManager.loginUser(userId) { success, _, _ ->
                        continuation.resume(Unit, null)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Sign in with Supabase
            val response = supabaseClient.gotrue.loginWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Log in to CometChat
            val userId = response.user?.id ?: return@withContext Result.failure(Exception("Failed to get user ID"))
            
            // Use a suspending function wrapper for the callback-based CometChat login
            suspendCancellableCoroutine<Unit> { continuation ->
                CometChatManager.loginUser(userId) { success, _, error ->
                    if (success) {
                        continuation.resume(Unit, null)
                    } else {
                        // If login fails, try to register the user first
                        val userName = response.user?.userMetadata?.get("name")?.toString() ?: email
                        CometChatManager.registerUser(userId, userName) { regSuccess, _, _ ->
                            if (regSuccess) {
                                // Try login again
                                CometChatManager.loginUser(userId) { _, _, _ ->
                                    continuation.resume(Unit, null)
                                }
                            } else {
                                continuation.resume(Unit, null) // Continue even if CometChat fails
                            }
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Sign out from Supabase
            supabaseClient.gotrue.logout()
            
            // Sign out from CometChat
            suspendCancellableCoroutine<Unit> { continuation ->
                CometChatManager.logout { _, _ ->
                    continuation.resume(Unit, null)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = supabaseClient.gotrue.currentUserOrNull()?.id
}
