package planning

import domain.Conflict
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.ResolutionPlan
import domain.SimulationState
import reasoning.SafetyReasoner
import simulation.AircraftMover
import simulation.ConflictDetector
import simulation.ManeuverApplier

/**
 * Resolution planner that prevents secondary conflicts through forward simulation.
 *
 * Candidate maneuvers are checked by the symbolic reasoner and then simulated over a
 * short horizon. A maneuver is accepted only if it does not introduce new conflicts.
 */
class SecondaryConflictAwareResolutionPlanner(
    private val safetyReasoner: SafetyReasoner,
    private val aircraftMover: AircraftMover = AircraftMover(),
    private val maneuverApplier: ManeuverApplier = ManeuverApplier(),
    private val predictionHorizonTicks: Int = 8,
    private val conflictDetector: ConflictDetector =
        ConflictDetector(
            aircraftMover = aircraftMover,
            safetyReasoner = safetyReasoner,
        ),
) : ResolutionPlanner {
    init {
        require(predictionHorizonTicks >= 1) {
            "Prediction horizon must be at least 1 for secondary-conflict prevention."
        }
    }

    override fun planResolution(
        conflict: Conflict,
        state: SimulationState,
    ): ResolutionPlan? {
        val candidateManeuvers =
            generateCandidateManeuvers(
                conflict = conflict,
                state = state,
            )

        val rejected = mutableListOf<String>()

        candidateManeuvers.forEach { maneuver ->
            val allowed =
                safetyReasoner.isManeuverAllowed(
                    aircraftId = maneuver.aircraftId,
                    maneuver = maneuver,
                    state = state,
                )

            if (!allowed) {
                rejected += "${maneuver.formatAsPlannerAction()} rejected by symbolic safety reasoner"
                return@forEach
            }

            val secondaryConflicts =
                simulateManeuverAndFindConflicts(
                    state = state,
                    maneuver = maneuver,
                )

            if (secondaryConflicts.isEmpty()) {
                return ResolutionPlan(
                    id = "secondary-safe-${conflict.id}",
                    conflictId = conflict.id,
                    maneuvers = listOf(maneuver),
                    explanation =
                        buildString {
                            append("Secondary-aware STRIPS selected ${maneuver.formatAsPlannerAction()} ")
                            append("for conflict ${conflict.id}. ")
                            append("The maneuver was accepted because forward simulation found no secondary conflicts ")
                            append("within $predictionHorizonTicks ticks.")
                            if (rejected.isNotEmpty()) {
                                append(" Rejected alternatives: ")
                                append(rejected.joinToString("; "))
                            }
                        },
                )
            }

            rejected +=
                buildString {
                    append("${maneuver.formatAsPlannerAction()} rejected because it creates secondary conflicts: ")
                    append(
                        secondaryConflicts.joinToString { secondary ->
                            "${secondary.aircraftIds.sorted()} at tick ${secondary.tick}"
                        },
                    )
                }
        }

        return null
    }

    private fun generateCandidateManeuvers(
        conflict: Conflict,
        state: SimulationState,
    ): List<Maneuver> {
        val candidateAircraftIds =
            conflict.aircraftIds
                .filter { aircraftId -> aircraftId in state.aircraft }
                .sortedWith(
                    compareBy<String> { aircraftId ->
                        safetyReasoner.priorityOf(aircraftId, state)
                    }.thenByDescending { aircraftId -> aircraftId },
                )

        return candidateAircraftIds.flatMap { aircraftId ->
            val aircraft = state.aircraft.getValue(aircraftId)
            val currentAltitude = aircraft.flightLevel.feet

            listOf(
                Maneuver(
                    aircraftId = aircraftId,
                    type = ManeuverType.CLIMB,
                    targetFlightLevel = FlightLevel(currentAltitude + 2000),
                    reason = "Increase vertical separation",
                ),
                Maneuver(
                    aircraftId = aircraftId,
                    type = ManeuverType.DESCEND,
                    targetFlightLevel = FlightLevel(currentAltitude - 2000),
                    reason = "Increase vertical separation without entering occupied upper level",
                ),
                Maneuver(
                    aircraftId = aircraftId,
                    type = ManeuverType.SLOW_DOWN,
                    reason = "Delay convergence to avoid conflict",
                ),
            )
        }
    }

    private fun simulateManeuverAndFindConflicts(
        state: SimulationState,
        maneuver: Maneuver,
    ): List<Conflict> {
        var simulatedState = state
        val detectedConflicts = mutableListOf<Conflict>()

        for (step in 1..predictionHorizonTicks) {
            val movedState =
                advanceOneTick(
                    state = simulatedState,
                    nextTick = state.tick + step,
                )

            val maneuveredState =
                if (step == 1) {
                    maneuverApplier.apply(
                        state = movedState,
                        maneuver = maneuver,
                    )
                } else {
                    movedState
                }

            detectedConflicts += conflictDetector.detectCurrentConflicts(maneuveredState)
            simulatedState = maneuveredState
        }

        return detectedConflicts.distinctBy { conflict -> conflict.id }
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
}
