package ru.nekoguys.game.persistence.competition.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import ru.nekoguys.game.entity.competition.model.CompetitionDemandFormula
import ru.nekoguys.game.entity.competition.model.CompetitionExpensesFormula
import ru.nekoguys.game.entity.competition.model.CompetitionSettings

@Table("competition_game_props")
data class DbCompetitionProperties(
    @Id
    @Column("id")
    var parentId: Long?,

    var autoRoundEnding: Boolean,

    var demandFormula: String,

    @Column("end_round_before_all_answered")
    val endRoundBeforeAllAnswered: Boolean,

    var expensesFormula: String,

    var instruction: String,

    var maxTeamSize: Int,

    var maxTeamsAmount: Int,

    var name: String,

    var roundLengthInSeconds: Int,

    var roundsCount: Int,

    var showOtherTeamsMembers: Boolean,

    @Column("show_prev_round_results")
    var showPreviousRoundResults: Boolean,

    var showStudentsResultsTable: Boolean,

    @Column("team_loss_upperbound")
    var teamLossLimit: Int,
) : Persistable<Long> {

    @Transient
    private var isNew: Boolean = parentId == null

    fun asNew(): DbCompetitionProperties =
        apply { isNew = true }

    override fun isNew(): Boolean = isNew

    override fun getId(): Long? = parentId
}

fun CompetitionSettings.toDbCompetitionProperties(
    parentId: Long?,
): DbCompetitionProperties =
    DbCompetitionProperties(
        parentId = parentId,
        autoRoundEnding = isAutoRoundEnding,
        demandFormula = demandFormula.toDbString(),
        endRoundBeforeAllAnswered = shouldEndRoundBeforeAllAnswered,
        expensesFormula = expensesFormula.toDbString(),
        instruction = instruction,
        maxTeamSize = maxTeamSize,
        maxTeamsAmount = maxTeamsAmount,
        name = name,
        roundLengthInSeconds = roundLength,
        roundsCount = roundsCount,
        showOtherTeamsMembers = showOtherTeamsMembers,
        showPreviousRoundResults = shouldShowStudentPreviousRoundResults,
        showStudentsResultsTable = shouldShowResultTableInEnd,
        teamLossLimit = teamLossUpperbound,
    )

fun DbCompetitionProperties.toCompetitionSettings() =
    CompetitionSettings(
        demandFormula = demandFormula.toCompetitionDemandFormulaOrNull() ?: error(""),
        expensesFormula = expensesFormula.toCompetitionExpensesFormulaOrNull() ?: error(""),
        instruction = instruction,
        isAutoRoundEnding = autoRoundEnding,
        maxTeamSize = maxTeamSize,
        maxTeamsAmount = maxTeamsAmount,
        name = name,
        roundLength = roundLengthInSeconds,
        roundsCount = roundsCount,
        shouldEndRoundBeforeAllAnswered = endRoundBeforeAllAnswered,
        shouldShowResultTableInEnd = showStudentsResultsTable,
        shouldShowStudentPreviousRoundResults = showPreviousRoundResults,
        showOtherTeamsMembers = showOtherTeamsMembers,
        teamLossUpperbound = teamLossLimit,
    )

fun CompetitionExpensesFormula.toDbString(): String =
    "${xSquareCoefficient};${xCoefficient};${freeCoefficient}"

fun String.toCompetitionExpensesFormulaOrNull(): CompetitionExpensesFormula? =
    split(";")
        .map { it.toDoubleOrNull() ?: return null }
        .takeIf { it.size == 3 }
        ?.let { (a, b, c) -> CompetitionExpensesFormula(a, b, c) }

fun CompetitionDemandFormula.toDbString(): String =
    "${xCoefficient};${freeCoefficient}"

fun String.toCompetitionDemandFormulaOrNull(): CompetitionDemandFormula? =
    split(";")
        .map { it.toDoubleOrNull() ?: return null }
        .takeIf { it.size == 2 }
        ?.let { (a, b) -> CompetitionDemandFormula(a, b) }
