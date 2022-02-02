package ru.nekoguys.game.persistence.commongame

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.nekoguys.game.persistence.GamePersistenceTest
import ru.nekoguys.game.persistence.commongame.model.DbGameProperties
import ru.nekoguys.game.persistence.commongame.model.DbGameSession
import ru.nekoguys.game.persistence.commongame.model.DbGameType
import ru.nekoguys.game.persistence.commongame.repository.DbGamePropertiesRepository
import ru.nekoguys.game.persistence.commongame.repository.DbGameSessionRepository
import ru.nekoguys.game.persistence.user.model.DbUser
import ru.nekoguys.game.persistence.user.model.DbUserRole
import ru.nekoguys.game.persistence.user.repository.DbUserRepository

@GamePersistenceTest
internal class DbGameSessionRepositoryTest @Autowired constructor(
    private val dbGamePropertiesRepository: DbGamePropertiesRepository,
    private val dbGameSessionRepository: DbGameSessionRepository,
    private val dbUserRepository: DbUserRepository,
) {
    @Test
    fun `insert and retrieval`(): Unit = runBlocking {
        val user = DbUser(
            email = "email",
            password = "qwerty",
            role = DbUserRole.TEACHER,
        ).let { dbUserRepository.save(it) }

        val properties = DbGameProperties(
            id = null,
            creatorId = user.id!!,
            gameType = DbGameType.COMPETITION,
        ).let { dbGamePropertiesRepository.save(it) }

        val gameSession = DbGameSession(
            id = null,
            propertiesId = properties.id!!,
        ).let { dbGameSessionRepository.save(it) }

        assertThat(dbGameSessionRepository.findById(gameSession.id!!))
            .isEqualTo(gameSession)
    }
}
