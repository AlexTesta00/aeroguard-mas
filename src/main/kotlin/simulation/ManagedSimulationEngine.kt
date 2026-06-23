package simulation

import domain.Scenario
import domain.SimulationState
import planning.ResolutionPlanner
import planning.StripsResolutionPlanner
import reasoning.SafetyReasoner

data class ManagedSimulationRunResult(
    val runResult: SimulationRunResult,
    val conflictResolutionPlan: domain.ResolutionPlan?,
    val appliedManeuvers: List<ScheduledManeuver>,
)

class ManagedSimulationEngine(
    private val safetyReasoner: SafetyReasoner,
    private val aircraftMover: AircraftMover = AircraftMover(),
    private val maneuverApplier: ManeuverApplier = ManeuverApplier(),
    private val predictionHorizonTicks: Int = 6,
    private val conflictDetector: ConflictDetector =
        ConflictDetector(
            aircraftMover = aircraftMover,
            safetyReasoner = safetyReasoner,
        ),
    private val resolutionPlanner: ResolutionPlanner =
        StripsResolutionPlanner(
            safetyReasoner = safetyReasoner,
        ),
) {
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

        val states = mutableListOf(initialState)
        val currentConflicts = mutableListOf<domain.Conflict>()

        var state = initialState

        for (nextTick in 1..scenario.maxTicks) {
            val movedState = advanceOneTick(state, nextTick)
            val maneuveredState =
                applyScheduledManeuvers(
                    state = movedState,
                    tick = nextTick,
                    scheduledManeuvers = scheduledManeuvers,
                )

            states += maneuveredState
            currentConflicts += conflictDetector.detectCurrentConflicts(maneuveredState)

            state = maneuveredState
        }

        val runResult =
            SimulationRunResult(
                scenarioName = scenario.name,
                states = states,
                currentConflicts = currentConflicts.distinctBy { it.id },
                predictedConflicts = initialPredictedConflicts.distinctBy { it.id },
            )

        return ManagedSimulationRunResult(
            runResult = runResult,
            conflictResolutionPlan = resolutionPlan,
            appliedManeuvers = scheduledManeuvers,
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
