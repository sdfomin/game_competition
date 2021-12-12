package ru.selemilka.game.rps.processor

import org.springframework.stereotype.Service
import ru.selemilka.game.core.base.Command
import ru.selemilka.game.core.base.MessageToPlayer
import ru.selemilka.game.rps.model.RpsPlayer
import ru.selemilka.game.rps.model.RpsSessionSettings
import ru.selemilka.game.rps.model.RpsStage
import ru.selemilka.game.rps.model.Turn
import ru.selemilka.game.rps.storage.RpsSessionStorage

/**
 * Команды в камень-ножницы-бумаге
 * Их отправляют только игроки [RpsPlayer], других отправителей нет.
 *
 * Все наследники [RpsRootCommand] перечислены здесь,
 * потому что нам нужно заранее знать типы всех команд, чтобы их обрабатывать
 */
sealed interface RpsRootCommand : Command<RpsPlayer> {
    object JoinGame : RpsRootCommand

    data class SubmitAnswer(val turn: Turn) : RpsRootCommand
}

/**
 * Все сообщения в камень-ножницы-бумаге - подтипы [RpsRootMessage].
 *
 * Наследники [RpsRootCommand] не перечислены здесь, потому что
 * для использования sealed нужно, чтобы всё находилось в одном файле.
 * Это неудобно и плохо масштабируется
 */
sealed interface RpsRootMessage {
    data class JoinGame(val inner: JoinGameMessage) : RpsRootMessage

    data class SubmitAnswer(val inner: RoundMessage) : RpsRootMessage

    data class IncorrectStage(
        val current: RpsStage,
        val expected: Collection<RpsStage>,
    ) : RpsRootMessage

    object SessionDoesNotExists : RpsRootMessage
}

typealias RpsResponse<Msg> = MessageToPlayer<RpsPlayer, Msg>

@Service
class RpsRootProcessor(
    private val joinGameProcessor: RpsJoinGameProcessor,
    private val roundProcessor: RpsRoundProcessor,
) {
    suspend fun process(
        player: RpsPlayer,
        command: RpsRootCommand,
    ): List<RpsResponse<RpsRootMessage>> {
        return when (command) {
            is RpsRootCommand.JoinGame -> joinGameProcessor.validateAndProcess(player, command)
            is RpsRootCommand.SubmitAnswer -> roundProcessor.validateAndProcess(player, command)
        }
    }
}

abstract class RpsRootSubProcessor<in Cmd : RpsRootCommand, out Msg : RpsRootMessage>(
    private val sessionStorage: RpsSessionStorage,
) {
    suspend fun validateAndProcess(
        player: RpsPlayer,
        command: Cmd,
    ): List<RpsResponse<RpsRootMessage>> {
        val session = sessionStorage.loadSession(player.sessionId)

        return when {
            session == null -> respond {
                player { +RpsRootMessage.SessionDoesNotExists }
            }

            session.stage in expectedStages -> respond {
                player { +RpsRootMessage.IncorrectStage(session.stage, expectedStages) }
            }

            else -> process(player, command, session.settings)
        }
    }

    protected abstract val expectedStages: Set<RpsStage>

    protected abstract suspend fun process(
        player: RpsPlayer,
        command: Cmd,
        settings: RpsSessionSettings,
    ): List<RpsResponse<RpsRootMessage>>
}
