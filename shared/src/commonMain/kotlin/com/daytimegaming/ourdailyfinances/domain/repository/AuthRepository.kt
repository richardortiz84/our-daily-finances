package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentUser(): Flow<User?>
    suspend fun login(email: String, password: String)
    suspend fun register(email: String, password: String)
    suspend fun signOut()
    suspend fun getIdToken(forceRefresh: Boolean = false): String
}
