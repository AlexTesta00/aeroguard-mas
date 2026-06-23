package explanation

import domain.Conflict
import domain.ResolutionPlan
import domain.SimulationState
import planning.formatAsPlannerAction
import reasoning.SafetyReasoner
import replanning.WeatherReplanningDecision
import simulation.SimulationRunResult

class ExplanationService(
    private val safetyReasoner: SafetyReasoner,
) {
    fun explainRun(
        runResult: SimulationRunResult,
        resolutionPlan: ResolutionPlan?,
    ): List<DecisionExplanation> {
        if (runResult.currentConflicts.isEmpty() && runResult.predictedConflicts.isEmpty()) {
            return listOf(
                DecisionExplanation(
                    tick = runResult.finalState.tick,
                    agent = "explanation_agent",
                    decisionId = "no_conflict_baseline",
                    message = "No conflicts were detected in scenario '${runResult.scenarioName}'.",
                ),
            )
        }

        val conflict =
            runResult.predictedConflicts.firstOrNull()
                ?: runResult.currentConflicts.firstOrNull()
                ?: return emptyList()

        val state =
            runResult.states.firstOrNull { it.tick == conflict.tick }
                ?: runResult.finalState

        return if (resolutionPlan != null) {
            explainResolution(
                conflict = conflict,
                state = state,
                plan = resolutionPlan,
            )
        } else {
            explainUnresolvedConflict(
                conflict = conflict,
                state = state,
            )
        }
    }

    fun explainResolution(
        conflict: Conflict,
        state: SimulationState,
        plan: ResolutionPlan,
    ): List<DecisionExplanation> {
        val symbolicFacts = safetyReasoner.explainDecision("unsafe_pair")
        val maneuverFacts = safetyReasoner.explainDecision("maneuver_climb_allowed")
        val actions =
            plan.maneuvers.joinToString { maneuver ->
                maneuver.formatAsPlannerAction()
            }

        return listOf(
            DecisionExplanation(
                tick = conflict.tick,
                agent = "explanation_agent",
                decisionId = "resolution_plan_${plan.id}",
                message = "STRIPS generated plan '${plan.id}' with actions [$actions] for conflict '${conflict.id}'.",
                supportingFacts = symbolicFacts + maneuverFacts,
            ),
            DecisionExplanation(
                tick = conflict.tick,
                agent = "sector_controller",
                decisionId = "selected_resolution_${plan.id}",
                message = plan.explanation,
                supportingFacts = priorityFacts(conflict, state),
            ),
        )
    }

    fun explainWeatherReplanning(decision: WeatherReplanningDecision): List<DecisionExplanation> {
        val actions =
            decision.resolutionPlan.maneuvers.joinToString { maneuver ->
                maneuver.formatAsPlannerAction()
            }

        return listOf(
            DecisionExplanation(
                tick = decision.tick,
                agent = "explanation_agent",
                decisionId = "weather_replanning_${decision.zone.id}_${decision.aircraftId}",
                message =
                    """Weather replanning was triggered for
                    |${decision.aircraftId} because weather zone
                    |${decision.zone.id} became active near the planned route.
                    """.trimMargin(),
                supportingFacts =
                    listOf(
                        "weather_zone=${decision.zone.id}",
                        "aircraft=${decision.aircraftId}",
                        "actions=[$actions]",
                    ),
            ),
            DecisionExplanation(
                tick = decision.tick,
                agent = "resolution_planner",
                decisionId = "weather_plan_${decision.resolutionPlan.id}",
                message = decision.resolutionPlan.explanation,
                supportingFacts = listOf("planner=strips"),
            ),
        )
    }

    private fun explainUnresolvedConflict(
        conflict: Conflict,
        state: SimulationState,
    ): List<DecisionExplanation> =
        listOf(
            DecisionExplanation(
                tick = conflict.tick,
                agent = "explanation_agent",
                decisionId = "unresolved_${conflict.id}",
                message = "Conflict '${conflict.id}' was detected, but no resolution plan was generated.",
                supportingFacts = safetyReasoner.explainDecision("unsafe_pair") + priorityFacts(conflict, state),
            ),
        )

    private fun priorityFacts(
        conflict: Conflict,
        state: SimulationState,
    ): List<String> =
        conflict.aircraftIds
            .sorted()
            .filter { aircraftId -> aircraftId in state.aircraft }
            .map { aircraftId ->
                "$aircraftId priorityScore=${safetyReasoner.priorityOf(aircraftId, state)}"
            }
}
