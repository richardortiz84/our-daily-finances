package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.domain.model.User
import com.daytimegaming.ourdailyfinances.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(private val auth: FirebaseAuth) : AuthRepository {

    override fun currentUser(): Flow<User?> =
        auth.authStateChanged.map { firebaseUser ->
            firebaseUser?.let { User(uid = it.uid, email = it.email) }
        }

    override suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
    }

    override suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String {
        return auth.currentUser?.getIdToken(forceRefresh) ?: ""
    }
}
