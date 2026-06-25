package simulation

import domain.Conflict
import domain.Scenario
import domain.SimulationState

data class SimulationTickResult(
    val state: SimulationState,
    val currentConflicts: List<Conflict>,
    val predictedConflicts: List<Conflict>,
)

data class SimulationRunResult(
    val scenarioName: String,
    val states: List<SimulationState>,
    val currentConflicts: List<Conflict>,
    val predictedConflicts: List<Conflict>,
) {
    val finalState: SimulationState
        get() = states.last()
}

/**
 * Baseline simulation engine that advances aircraft and detects conflicts without
 * managed planning feedback.
 */
class SimulationEngine(
    private val aircraftMover: AircraftMover = AircraftMover(),
    private val conflictDetector: ConflictDetector = ConflictDetector(aircraftMover),
    private val predictionHorizonTicks: Int = 5,
) {
    init {
        require(predictionHorizonTicks >= 0) {
            "Prediction horizon must be non-negative."
        }
    }

    fun advanceOneTick(state: SimulationState): SimulationTickResult {
        val nextAircraft =
            state.aircraft.values
                .map { aircraftMover.advanceOneTick(it) }
                .associateBy { it.id }

        val nextState =
            state.copy(
                tick = state.tick + 1,
                aircraft = nextAircraft,
            )

        return SimulationTickResult(
            state = nextState,
            currentConflicts = conflictDetector.detectCurrentConflicts(nextState),
            predictedConflicts =
                conflictDetector.predictConflicts(
                    state = nextState,
                    horizonTicks = predictionHorizonTicks,
                ),
        )
    }

    fun run(scenario: Scenario): SimulationRunResult {
        val states = mutableListOf<SimulationState>()
        val currentConflicts = mutableListOf<Conflict>()
        val predictedConflicts = mutableListOf<Conflict>()

        var state = SimulationState.fromScenario(scenario)
        states += state

        currentConflicts += conflictDetector.detectCurrentConflicts(state)
        predictedConflicts +=
            conflictDetector.predictConflicts(
                state = state,
                horizonTicks = predictionHorizonTicks,
            )

        repeat(scenario.maxTicks) {
            val tickResult = advanceOneTick(state)
            state = tickResult.state
            states += state
            currentConflicts += tickResult.currentConflicts
            predictedConflicts += tickResult.predictedConflicts
        }

        return SimulationRunResult(
            scenarioName = scenario.name,
            states = states,
            currentConflicts = currentConflicts.distinctBy { it.id },
            predictedConflicts = predictedConflicts.distinctBy { it.id },
        )
    }
}
