package com.example.blooddonorfinder.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Authentication repository wrapping Firebase Authentication.
 * Handles sign-in, anonymous testing sessions, and user ID resolution.
 *
 * NOTE: strictly uses Firebase Auth SDK directly for authentication tokens.
 * Passwords are never persisted, logged, or saved to Firestore.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Returns the currently signed-in FirebaseUser, or null if unauthenticated.
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Returns the UID of the currently authenticated user, or null.
     */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Checks if a user session is currently active.
     */
    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Authenticates anonymously without requiring credentials.
     * Useful for laboratory demonstration and immediate Firestore access.
     */
    suspend fun signInAnonymously(): Result<String> {
        return try {
            val authResult = auth.signInAnonymously().awaitTask()
            val uid = authResult.user?.uid ?: ""
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Guarantees an active authenticated Firebase user session.
     * Returns existing UID if signed in, or automatically signs in anonymously.
     */
    suspend fun ensureAuthenticated(): Result<String> {
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            return Result.success(currentUid)
        }
        return signInAnonymously()
    }

    /**
     * Registers a new user account with Email and Password using Firebase Auth.
     * Passwords are processed exclusively by Firebase Auth backend.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
            val uid = authResult.user?.uid ?: ""
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with Email and Password.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
            val uid = authResult.user?.uid ?: ""
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user session.
     */
    fun signOut() {
        auth.signOut()
    }
}
