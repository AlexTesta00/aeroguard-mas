package planning

import domain.Conflict
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.ResolutionPlan
import domain.SimulationState
import reasoning.SafetyReasoner

class StripsResolutionPlanner(
    private val safetyReasoner: SafetyReasoner,
    private val stripsPlanner: StripsPlanner = StripsPlanner(),
) : ResolutionPlanner {
    override fun planResolution(
        conflict: Conflict,
        state: SimulationState,
    ): ResolutionPlan? {
        val maneuverAircraftId =
            selectAircraftForManeuver(conflict, state)
                ?: return null

        val candidateManeuvers =
            generateCandidateManeuvers(
                aircraftId = maneuverAircraftId,
                state = state,
            ).filter { maneuver ->
                safetyReasoner.isManeuverAllowed(
                    aircraftId = maneuverAircraftId,
                    maneuver = maneuver,
                    state = state,
                )
            }

        if (candidateManeuvers.isEmpty()) {
            return null
        }

        val problem =
            buildProblem(
                conflict = conflict,
                aircraftId = maneuverAircraftId,
                candidateManeuvers = candidateManeuvers,
            )

        val actions = stripsPlanner.plan(problem) ?: return null
        val maneuvers = actions.mapNotNull { it.maneuver }

        if (maneuvers.isEmpty()) {
            return null
        }

        return ResolutionPlan(
            id = "strips-${conflict.id}",
            conflictId = conflict.id,
            maneuvers = maneuvers,
            explanation =
                buildExplanation(
                    conflict = conflict,
                    maneuverAircraftId = maneuverAircraftId,
                    actions = actions,
                    state = state,
                ),
        )
    }

    private fun selectAircraftForManeuver(
        conflict: Conflict,
        state: SimulationState,
    ): String? {
        val knownAircraft =
            conflict.aircraftIds
                .filter { aircraftId -> aircraftId in state.aircraft }

        if (knownAircraft.size < 2) {
            return null
        }

        return knownAircraft
            .sortedWith(
                compareBy<String> { aircraftId ->
                    safetyReasoner.priorityOf(aircraftId, state)
                }.thenByDescending { aircraftId ->
                    aircraftId
                },
            ).firstOrNull()
    }

    private fun generateCandidateManeuvers(
        aircraftId: String,
        state: SimulationState,
    ): List<Maneuver> {
        val aircraft = state.aircraft[aircraftId] ?: return emptyList()
        val currentFeet = aircraft.flightLevel.feet

        return listOf(
            Maneuver(
                aircraftId = aircraftId,
                type = ManeuverType.CLIMB,
                targetFlightLevel = FlightLevel(currentFeet + 2000),
                reason = "Increase vertical separation",
            ),
            Maneuver(
                aircraftId = aircraftId,
                type = ManeuverType.DESCEND,
                targetFlightLevel = FlightLevel(maxOf(0, currentFeet - 2000)),
                reason = "Increase vertical separation",
            ),
            Maneuver(
                aircraftId = aircraftId,
                type = ManeuverType.CLIMB,
                targetFlightLevel = FlightLevel(currentFeet + 1000),
                reason = "Reach minimum vertical separation",
            ),
            Maneuver(
                aircraftId = aircraftId,
                type = ManeuverType.DESCEND,
                targetFlightLevel = FlightLevel(maxOf(0, currentFeet - 1000)),
                reason = "Reach minimum vertical separation",
            ),
            Maneuver(
                aircraftId = aircraftId,
                type = ManeuverType.SLOW_DOWN,
                reason = "Delay aircraft to reduce convergence",
            ),
        )
    }

    private fun buildProblem(
        conflict: Conflict,
        aircraftId: String,
        candidateManeuvers: List<Maneuver>,
    ): StripsProblem {
        val conflictUnresolved = Proposition.conflictUnresolved(conflict.id)
        val conflictResolved = Proposition.conflictResolved(conflict.id)
        val aircraftAvailable = Proposition.aircraftAvailable(aircraftId)

        val actions =
            candidateManeuvers.map { maneuver ->
                val maneuverName = maneuver.toPlannerTerm()

                StripsAction(
                    name = maneuverName,
                    preconditions =
                        setOf(
                            conflictUnresolved,
                            aircraftAvailable,
                        ),
                    addEffects =
                        setOf(
                            conflictResolved,
                            Proposition.separationRestored(conflict.id),
                            Proposition.maneuverSelected(
                                aircraftId = maneuver.aircraftId,
                                maneuverName = maneuverName,
                            ),
                        ),
                    deleteEffects = setOf(conflictUnresolved),
                    maneuver = maneuver,
                )
            }

        return StripsProblem(
            initialState =
                setOf(
                    conflictUnresolved,
                    aircraftAvailable,
                ),
            goal = setOf(conflictResolved),
            actions = actions,
            maxDepth = 3,
        )
    }

    private fun buildExplanation(
        conflict: Conflict,
        maneuverAircraftId: String,
        actions: List<StripsAction>,
        state: SimulationState,
    ): String {
        val aircraftIds = conflict.aircraftIds.sorted()
        val prioritySummary =
            aircraftIds.joinToString { aircraftId ->
                "$aircraftId=${safetyReasoner.priorityOf(aircraftId, state)}"
            }

        return "STRIPS selected ${actions.joinToString { it.name }} " +
            "for conflict ${conflict.id}. " +
            "Aircraft $maneuverAircraftId was selected for maneuvering " +
            "based on symbolic priority scores: $prioritySummary."
    }

    private fun Maneuver.toPlannerTerm(): String =
        when (type) {
            ManeuverType.CLIMB ->
                "climb($aircraftId,${targetFlightLevel?.feet ?: "unknown"})"

            ManeuverType.DESCEND ->
                "descend($aircraftId,${targetFlightLevel?.feet ?: "unknown"})"

            ManeuverType.TURN_LEFT ->
                "turn_left($aircraftId,${targetWaypoint?.name ?: "unknown"})"

            ManeuverType.TURN_RIGHT ->
                "turn_right($aircraftId,${targetWaypoint?.name ?: "unknown"})"

            ManeuverType.SLOW_DOWN ->
                "slow_down($aircraftId)"

            ManeuverType.RESUME_SPEED ->
                "resume_speed($aircraftId)"

            ManeuverType.ENTER_HOLDING ->
                "enter_holding($aircraftId)"

            ManeuverType.CONTINUE_ROUTE ->
                "continue_route($aircraftId)"

            ManeuverType.AVOID_WEATHER_ZONE ->
                "avoid_weather_zone($aircraftId)"

            ManeuverType.REROUTE_TO_WAYPOINT ->
                "reroute_to_waypoint($aircraftId,${targetWaypoint?.name ?: "unknown"})"
        }
}
