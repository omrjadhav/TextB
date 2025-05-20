package com.example.textb.data

import com.example.textb.data.models.ProfileData
import com.example.textb.data.models.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class UserRepository {
    private val supabaseClient = SupabaseClientManager.getClient()
    private val authRepository = AuthRepository()
    
    // Configure JSON to ignore unknown keys
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID from auth repository
            val currentUserId = authRepository.getCurrentUser()
                ?: return@withContext Result.failure(Exception("No user logged in"))
            
            // Get profile from Supabase using user ID
            val profilesData = supabaseClient
                .postgrest
                .from("profiles")
                .select(columns = Columns.ALL)
                .decodeList<ProfileData>()
            
            // Find the current user's profile
            val profileDataFound = profilesData.firstOrNull { profile -> profile.id == currentUserId }
            
            if (profileDataFound != null) {
                // Create user profile from profile data
                val userProfile = UserProfile(
                    name = profileDataFound.name ?: "",
                    email = profileDataFound.email ?: "",
                    phone = profileDataFound.phone ?: "",
                    university = profileDataFound.university ?: "",
                    major = profileDataFound.major ?: "",
                    profileImageUrl = profileDataFound.profile_image_url ?: ""
                )
                
                Result.success(userProfile)
            } else {
                // If profile doesn't exist in Supabase, create a basic one
                val userProfile = UserProfile(
                    name = "",
                    email = "",
                    phone = "",
                    university = "",
                    major = "",
                    profileImageUrl = ""
                )
                
                // Create the profile in Supabase
                createProfileInSupabase(currentUserId, userProfile)
                
                Result.success(userProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun createProfileInSupabase(userId: String, userProfile: UserProfile) {
        try {
            val profileData = ProfileData(
                id = userId,
                name = userProfile.name,
                email = userProfile.email,
                phone = userProfile.phone,
                university = userProfile.university,
                major = userProfile.major,
                profile_image_url = userProfile.profileImageUrl
            )
            
            supabaseClient
                .postgrest
                .from("profiles")
                .insert(profileData)
        } catch (e: Exception) {
            // Log error but don't fail the operation
            e.printStackTrace()
        }
    }
    
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID from auth repository
            val currentUserId = authRepository.getCurrentUser()
                ?: return@withContext Result.failure(Exception("No user logged in"))
            
            // Update profile in Supabase
            val profileData = ProfileData(
                id = currentUserId,
                name = userProfile.name,
                email = userProfile.email,
                phone = userProfile.phone,
                university = userProfile.university,
                major = userProfile.major,
                profile_image_url = userProfile.profileImageUrl
            )
            
            supabaseClient
                .postgrest
                .from("profiles")
                .insert(profileData, upsert = true)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
