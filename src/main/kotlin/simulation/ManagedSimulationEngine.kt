package simulation

import domain.Conflict
import domain.ResolutionPlan
import domain.Scenario
import domain.SimulationState
import planning.ResolutionPlanner
import planning.SecondaryConflictAwareResolutionPlanner
import reasoning.SafetyReasoner
import replanning.WeatherReplanningDecision
import replanning.WeatherReplanningService

data class ManagedSimulationRunResult(
    val runResult: SimulationRunResult,
    val conflictResolutionPlan: ResolutionPlan?,
    val appliedManeuvers: List<ScheduledManeuver>,
    val weatherReplanningDecisions: List<WeatherReplanningDecision> = emptyList(),
)

/**
 * Managed simulation engine integrating movement, planning, maneuver application, and
 * dynamic replanning.
 */
class ManagedSimulationEngine(
    private val safetyReasoner: SafetyReasoner,
    private val aircraftMover: AircraftMover = AircraftMover(),
    private val maneuverApplier: ManeuverApplier = ManeuverApplier(),
    private val predictionHorizonTicks: Int = 8,
    private val conflictDetector: ConflictDetector =
        ConflictDetector(
            aircraftMover = aircraftMover,
            safetyReasoner = safetyReasoner,
        ),
    private val resolutionPlanner: ResolutionPlanner =
        SecondaryConflictAwareResolutionPlanner(
            safetyReasoner = safetyReasoner,
            aircraftMover = aircraftMover,
            maneuverApplier = maneuverApplier,
            predictionHorizonTicks = predictionHorizonTicks,
        ),
) {
    init {
        require(predictionHorizonTicks >= 0) {
            "Prediction horizon must be non-negative."
        }
    }

    fun run(scenario: Scenario): ManagedSimulationRunResult {
        val initialState = SimulationState.fromScenario(scenario)

        val initialCurrentConflicts = conflictDetector.detectCurrentConflicts(initialState)
        val initialPredictedConflicts =
            conflictDetector.predictConflicts(
                state = initialState,
                horizonTicks = predictionHorizonTicks,
            )

        val conflictForPlanning =
            initialPredictedConflicts.firstOrNull()
                ?: initialCurrentConflicts.firstOrNull()

        val resolutionPlan =
            conflictForPlanning?.let { conflict ->
                resolutionPlanner.planResolution(
                    conflict = conflict,
                    state = initialState,
                )
            }

        val scheduledManeuvers =
            resolutionPlan
                ?.maneuvers
                ?.map { maneuver ->
                    ScheduledManeuver(
                        tick = minOf(1, scenario.maxTicks),
                        maneuver = maneuver,
                    )
                }.orEmpty()

        val weatherReplanningService = WeatherReplanningService(safetyReasoner)
        val weatherReplanningDecisions = mutableListOf<WeatherReplanningDecision>()

        val states = mutableListOf<SimulationState>()
        val currentConflicts = mutableListOf<Conflict>()

        var state = initialState

        states += state
        currentConflicts += initialCurrentConflicts

        for (nextTick in 1..scenario.maxTicks) {
            val movedState =
                advanceOneTick(
                    state = state,
                    nextTick = nextTick,
                )

            var maneuveredState =
                applyScheduledManeuvers(
                    state = movedState,
                    tick = nextTick,
                    scheduledManeuvers = scheduledManeuvers,
                )

            val weatherDecisionsForTick =
                weatherReplanningService.planWeatherReplanningForTick(
                    scenario = scenario,
                    state = maneuveredState,
                )

            if (weatherDecisionsForTick.isNotEmpty()) {
                weatherReplanningDecisions += weatherDecisionsForTick

                val weatherManeuvers =
                    weatherDecisionsForTick.flatMap { decision ->
                        decision.resolutionPlan.maneuvers
                    }

                maneuveredState =
                    maneuverApplier.applyAll(
                        state = maneuveredState,
                        maneuvers = weatherManeuvers,
                    )
            }

            states += maneuveredState
            currentConflicts += conflictDetector.detectCurrentConflicts(maneuveredState)

            state = maneuveredState
        }

        val runResult =
            SimulationRunResult(
                scenarioName = scenario.name,
                states = states,
                currentConflicts = currentConflicts.distinctBy { conflict -> conflict.id },
                predictedConflicts = initialPredictedConflicts.distinctBy { conflict -> conflict.id },
            )

        return ManagedSimulationRunResult(
            runResult = runResult,
            conflictResolutionPlan = resolutionPlan,
            appliedManeuvers = scheduledManeuvers,
            weatherReplanningDecisions = weatherReplanningDecisions,
        )
    }

    private fun advanceOneTick(
        state: SimulationState,
        nextTick: Int,
    ): SimulationState {
        val movedAircraft =
            state.aircraft.values
                .map { aircraft -> aircraftMover.advanceOneTick(aircraft) }
                .associateBy { aircraft -> aircraft.id }

        return state.copy(
            tick = nextTick,
            aircraft = movedAircraft,
        )
    }

    private fun applyScheduledManeuvers(
        state: SimulationState,
        tick: Int,
        scheduledManeuvers: List<ScheduledManeuver>,
    ): SimulationState {
        val maneuversForTick =
            scheduledManeuvers
                .filter { scheduled -> scheduled.tick == tick }
                .map { scheduled -> scheduled.maneuver }

        if (maneuversForTick.isEmpty()) {
            return state
        }

        return maneuverApplier.applyAll(
            state = state,
            maneuvers = maneuversForTick,
        )
    }
}
