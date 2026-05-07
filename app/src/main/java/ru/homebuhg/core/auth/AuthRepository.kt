package ru.homebuhg.core.auth

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.homebuhg.core.di.FirebaseHolder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val firebase: FirebaseHolder) {

    val isFirebaseAvailable: Boolean get() = firebase.isAvailable

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = firebase.auth
        if (auth == null) {
            trySend(null)
            awaitClose()
            return@callbackFlow
        }
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { a ->
            trySend(a.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        val auth = firebase.auth ?: return Result.failure(Exception("Firebase не настроен"))
        return runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            Unit
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        val auth = firebase.auth ?: return Result.failure(Exception("Firebase не настроен"))
        return runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }
    }

    fun signOut() = firebase.auth?.signOut()

    fun currentUserEmail(): String? = firebase.auth?.currentUser?.email
    fun currentUserId(): String? = firebase.auth?.currentUser?.uid
}
