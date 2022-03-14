package ru.nekoguys.game.persistence.competition.repository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import ru.nekoguys.game.entity.commongame.model.CommonSession
import ru.nekoguys.game.entity.competition.competitionProcessError
import ru.nekoguys.game.entity.competition.model.CompetitionPlayer
import ru.nekoguys.game.entity.competition.model.CompetitionTeam
import ru.nekoguys.game.entity.competition.repository.CompetitionPlayerRepository
import ru.nekoguys.game.entity.competition.repository.CompetitionTeamRepository
import ru.nekoguys.game.persistence.competition.model.DbCompetitionTeam

@Repository
class CompetitionTeamRepositoryImpl(
    private val competitionPlayerRepository: CompetitionPlayerRepository,
    private val dbCompetitionTeamRepository: DbCompetitionTeamRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val transactionalOperator: TransactionalOperator,
) : CompetitionTeamRepository {

    override suspend fun create(
        creator: CompetitionPlayer.Unknown,
        name: String,
        password: String,
        maxTeams: Int,
    ): CompetitionTeam = transactionalOperator.executeAndAwait {
        val dbTeam = DbCompetitionTeam(
            id = null,
            sessionId = creator.sessionId.number,
            teamNumber = -1, // номер пересчитается после создания кампании
            name = name,
            password = password,
            banRound = null,
        )
            .let { dbCompetitionTeamRepository.save(it) }
            .let { generateAndSaveTeamNumber(it, maxTeams) }

        val captain = CompetitionPlayer.TeamCaptain(
            sessionId = creator.sessionId,
            user = creator.user,
            teamId = CompetitionTeam.Id(dbTeam.id!!)
        ).also { competitionPlayerRepository.save(it) }

        createCompetitionTeam(
            dbTeam = dbTeam,
            captain = captain,
            teamMembers = emptyList(),
        )
    }!!

    private suspend fun generateAndSaveTeamNumber(
        dbTeam: DbCompetitionTeam,
        maxTeams: Int,
    ): DbCompetitionTeam {
        val teamNumber = dbCompetitionTeamRepository
            .countBySessionIdAndIdLessThanEqual(dbTeam.sessionId, dbTeam.id!!)

        if (teamNumber > maxTeams) {
            competitionProcessError(
                "There are too much teams in competition, max amount: $maxTeams"
            )
        }

        return dbCompetitionTeamRepository.save(dbTeam.copy(teamNumber = teamNumber))
    }

    override suspend fun update(
        teamsDiff: Collection<Pair<CompetitionTeam, CompetitionTeam>>,
    ): Unit = transactionalOperator.executeAndAwait {
        teamsDiff
            .filter { (from, to) ->
                require(from.id == to.id)
                require(from.sessionId == to.sessionId)
                require(from.numberInGame == to.numberInGame)
                require(from.captain == to.captain)

                from.name != to.name
                        || from.captain != to.captain
                        || from.teamMembers != to.teamMembers
                        || from.isBanned != to.isBanned
            }
            .map { (_, team) ->
                DbCompetitionTeam(
                    id = team.id.number,
                    sessionId = team.sessionId.number,
                    teamNumber = team.numberInGame,
                    name = team.name,
                    password = "1234",
                    banRound = null,
                )
            }
            .let { dbCompetitionTeamRepository.saveAll(it) }
            .collect()

        teamsDiff
            .forEach { (from, to) ->
                val newTeamMembers = to.teamMembers - from.teamMembers.toSet()
                for (teamMember in newTeamMembers) {
                    competitionPlayerRepository.save(teamMember)
                }
            }
    }!!

    override suspend fun findByName(
        sessionId: CommonSession.Id,
        teamName: String,
    ): CompetitionTeam? {
        val dbTeam = dbCompetitionTeamRepository
            .findBySessionIdAndName(
                sessionId = sessionId.number,
                name = teamName,
            )
            ?: return null

        val members = competitionPlayerRepository
            .loadAllInTeam(sessionId, CompetitionTeam.Id(dbTeam.id!!))
            .toList()

        return createCompetitionTeam(
            dbTeam = dbTeam,
            captain = members
                .filterIsInstance<CompetitionPlayer.TeamCaptain>()
                .single(),
            teamMembers = members
                .filterIsInstance<CompetitionPlayer.TeamMember>()
        )
    }

    override fun findBySessionId(
        sessionId: Long,
    ): Flow<CompetitionTeam> = flow {
        coroutineScope {
            val allMembersDeferred = async {
                competitionPlayerRepository
                    .loadAllInSession(sessionId)
                    .toList()
            }

            val dbTeamsDeferred = async {
                dbCompetitionTeamRepository
                    .findAllBySessionId(sessionId)
                    .toList()
            }

            val allMembers = allMembersDeferred
                .await()
                .groupBy { it.teamId.number }

            dbTeamsDeferred
                .await()
                .map { dbTeam ->
                    val members = allMembers[dbTeam.id!!].orEmpty()
                    val captain = members
                        .filterIsInstance<CompetitionPlayer.TeamCaptain>()
                        .single()
                    val teamMembers = members
                        .filterIsInstance<CompetitionPlayer.TeamMember>()

                    createCompetitionTeam(
                        dbTeam,
                        captain = captain,
                        teamMembers = teamMembers,
                    )
                }
                .asFlow()
        }.collect { team -> emit(team) }
    }

    @OptIn(FlowPreview::class)
    override suspend fun findAllBySessionIds(
        sessionIds: Iterable<Long>,
    ): Map<CommonSession.Id, List<CompetitionTeam>> =
        sessionIds
            .asFlow()
            .flatMapMerge { sessionId ->
                findBySessionId(sessionId)
                    .map { sessionId to it }
            }
            .toList()
            .groupBy({ CommonSession.Id(it.first) }) { it.second }

    override suspend fun findAllTeamIdsBySessionIds(
        sessionIds: Collection<Long>,
    ): Map<CommonSession.Id, List<CompetitionTeam.Id>> =
        dbCompetitionTeamRepository
            .findAllBySessionIdIn(sessionIds)
            .toList()
            .groupBy({ CommonSession.Id(it.sessionId) }) {
                CompetitionTeam.Id(it.id!!)
            }

    override suspend fun load(
        teamId: CompetitionTeam.Id
    ): CompetitionTeam {
        val dbTeam = dbCompetitionTeamRepository
            .findById(teamId.number)
            ?: error("Team with id $teamId must exist")

        val sessionId = CommonSession.Id(dbTeam.sessionId)
        val members = competitionPlayerRepository
            .loadAllInTeam(sessionId, CompetitionTeam.Id(dbTeam.id!!))
            .toList()

        return createCompetitionTeam(
            dbTeam = dbTeam,
            captain = members
                .filterIsInstance<CompetitionPlayer.TeamCaptain>()
                .single(),
            teamMembers = members
                .filterIsInstance<CompetitionPlayer.TeamMember>()
        )
    }
}

private fun createCompetitionTeam(
    dbTeam: DbCompetitionTeam,
    captain: CompetitionPlayer.TeamCaptain,
    teamMembers: List<CompetitionPlayer.TeamMember>,
): CompetitionTeam =
    CompetitionTeam(
        id = captain.teamId,
        sessionId = CommonSession.Id(dbTeam.sessionId),
        name = dbTeam.name,
        numberInGame = dbTeam.teamNumber,
        captain = captain,
        teamMembers = teamMembers,
        isBanned = dbTeam.banRound != null,
        password = dbTeam.password,
    )
