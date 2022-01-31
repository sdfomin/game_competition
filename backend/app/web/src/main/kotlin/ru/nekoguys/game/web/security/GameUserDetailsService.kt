package ru.nekoguys.game.web.security

import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ru.nekoguys.game.entity.user.model.UserRole
import ru.nekoguys.game.entity.user.repository.UserRepository

@Component
class GameUserDetailsService(
    private val userRepository: UserRepository,
) : ReactiveUserDetailsService {

    override fun findByUsername(email: String): Mono<UserDetails> =
        mono { userRepository.findByEmail(email)?.toUserDetails() }
}

private fun ru.nekoguys.game.entity.user.model.User.toUserDetails(): UserDetails =
    User.withUsername(email)
        .password(password)
        .authorities(role.toAuthoritiesList())
        .accountExpired(false)
        .accountLocked(false)
        .credentialsExpired(false)
        .disabled(false)
        .build()

private fun UserRole.toAuthoritiesList(): List<GrantedAuthority> {
    val role = this
    return buildList {
        if (role is UserRole.Admin) { add(SimpleGrantedAuthority("ROLE_ADMIN")) }
        if (role is UserRole.Teacher) { add(SimpleGrantedAuthority("ROLE_TEACHER")) }
        if (role is UserRole.Student) { add(SimpleGrantedAuthority("ROLE_STUDENT")) }
    }
}
