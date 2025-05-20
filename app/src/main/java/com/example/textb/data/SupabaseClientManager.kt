package com.example.textb.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseClientManager {
    // Supabase project URL
    private const val SUPABASE_URL = "https://fxzlfzjyvtbujnhomqtn.supabase.co"
    
    // Anon key for client-side operations
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ4emxmemp5dnRidWpuaG9tcXRuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc2Mzk0ODUsImV4cCI6MjA2MzIxNTQ4NX0.Tg7sVZ0RD9RVD8roOZ1n2uXy9K7udHVotTkU3KkcGu4"

    // Create a single instance of the Supabase client
    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            // Install required modules
            install(Postgrest)
            install(GoTrue)
            install(Storage)
        }
    }

    // Get the Supabase client instance
    fun getClient(): SupabaseClient = supabaseClient
}
