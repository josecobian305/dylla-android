package app.dylla.services

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication wrapper.
 * Provides suspend functions for sign-in, sign-up, Google sign-in, and sign-out.
 */
object FirebaseAuthService {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /** Currently signed-in user, or null. */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Whether a user is currently authenticated. */
    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Sign in with email and password.
     * @return Result wrapping the FirebaseUser on success, or the exception on failure.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Sign-in succeeded but user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new account with email, password, and display name.
     * @return Result wrapping the FirebaseUser on success, or the exception on failure.
     */
    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Set display name on the newly created user
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdate).await()
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Sign-up succeeded but user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with a Google ID token obtained from Google Sign-In.
     *
     * Usage:
     * 1. Launch the Google Sign-In intent from your Activity.
     * 2. In onActivityResult, extract the GoogleSignInAccount and call
     *    signInWithGoogle(activity, account.idToken).
     *
     * @param activity The calling Activity (unused here but kept for API symmetry
     *                 with the iOS pattern and potential future credential-manager use).
     * @param idToken  The Google ID token from GoogleSignInAccount.idToken.
     * @return Result wrapping the FirebaseUser on success.
     */
    suspend fun signInWithGoogle(activity: Activity, idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Google sign-in succeeded but user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send a password reset email.
     * @return Result.success(Unit) on success, or the exception on failure.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listen for auth state changes. Returns a lambda that removes the listener.
     */
    fun addAuthStateListener(onChanged: (FirebaseUser?) -> Unit): () -> Unit {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            onChanged(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }
}
