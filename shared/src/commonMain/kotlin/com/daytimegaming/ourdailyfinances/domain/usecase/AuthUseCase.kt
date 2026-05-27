package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.model.User
import com.daytimegaming.ourdailyfinances.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUser(private val repo: AuthRepository) {
    operator fun invoke(): Flow<User?> = repo.currentUser()
}

class LoginUser(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repo.login(email, password)
}

class RegisterUser(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repo.register(email, password)
}

class SignOutUser(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}

class GetIdToken(private val repo: AuthRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): String = repo.getIdToken(forceRefresh)
}

class AuthUseCase(
    val GetCurrentUser: GetCurrentUser,
    val LoginUser: LoginUser,
    val RegisterUser: RegisterUser,
    val SignOutUser: SignOutUser,
    val GetIdToken: GetIdToken,
)
