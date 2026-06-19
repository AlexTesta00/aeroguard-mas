package simulation

import domain.Aircraft
import domain.Conflict
import domain.ConflictType
import domain.SimulationState

class ConflictDetector(
    private val aircraftMover: AircraftMover = AircraftMover(),
) {
    fun detectCurrentConflicts(state: SimulationState): List<Conflict> {
        val aircraft = state.aircraft.values.sortedBy { it.id }
        val conflicts = mutableListOf<Conflict>()

        for (i in aircraft.indices) {
            for (j in i + 1 until aircraft.size) {
                val first = aircraft[i]
                val second = aircraft[j]
                val conflict = detectPairConflict(state, first, second)

                if (conflict != null) {
                    conflicts += conflict
                }
            }
        }

        return conflicts
    }

    fun predictConflicts(
        state: SimulationState,
        horizonTicks: Int,
    ): List<Conflict> {
        require(horizonTicks >= 0) { "Prediction horizon must be non-negative." }

        val earliestPredictionByPair = linkedMapOf<Set<String>, Conflict>()
        var futureState = state

        repeat(horizonTicks) {
            futureState = advanceStateWithoutReasoning(futureState)

            detectCurrentConflicts(futureState).forEach { currentConflict ->
                val pairKey = currentConflict.aircraftIds.toSortedSet()
                earliestPredictionByPair.putIfAbsent(
                    pairKey,
                    currentConflict.copy(
                        id = "predicted-${state.tick}-${futureState.tick}-${pairKey.joinToString("-")}",
                        tick = state.tick,
                        type = ConflictType.PREDICTED_CONFLICT,
                        predictedAtTick = futureState.tick,
                    ),
                )
            }
        }

        return earliestPredictionByPair.values.toList()
    }

    private fun detectPairConflict(
        state: SimulationState,
        first: Aircraft,
        second: Aircraft,
    ): Conflict? {
        val horizontalDistance = first.position.distanceTo(second.position)
        val verticalDistance = first.flightLevel.verticalDistanceTo(second.flightLevel)

        val horizontalUnsafe = horizontalDistance < state.separation.horizontal
        val verticalUnsafe = verticalDistance < state.separation.verticalFeet

        if (!horizontalUnsafe || !verticalUnsafe) {
            return null
        }

        return Conflict(
            id = "current-${state.tick}-${first.id}-${second.id}",
            tick = state.tick,
            aircraftIds = setOf(first.id, second.id),
            type = ConflictType.HORIZONTAL_SEPARATION_LOSS,
            horizontalDistance = horizontalDistance,
            verticalDistanceFeet = verticalDistance,
        )
    }

    private fun advanceStateWithoutReasoning(state: SimulationState): SimulationState {
        val movedAircraft =
            state.aircraft.values
                .map { aircraftMover.advanceOneTick(it) }
                .associateBy { it.id }

        return state.copy(
            tick = state.tick + 1,
            aircraft = movedAircraft,
        )
    }
}
