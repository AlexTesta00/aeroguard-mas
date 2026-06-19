package reasoning

import domain.AircraftPriority
import domain.Conflict
import domain.EmergencyStatus
import domain.Maneuver
import domain.ManeuverType
import domain.SimulationState
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.Solver
import it.unibo.tuprolog.solve.classic.ClassicSolverFactory
import it.unibo.tuprolog.theory.Theory
import it.unibo.tuprolog.theory.parsing.ClausesParser
import it.unibo.tuprolog.core.Integer as LogicInteger

class TuPrologSafetyReasoner(
    theoryText: String,
) : SafetyReasoner {
    private val theory: Theory = parseTheory(theoryText)

    override fun isConflictUnsafe(
        conflict: Conflict,
        state: SimulationState,
    ): Boolean {
        val aircraftIds = conflict.aircraftIds.sorted()
        if (aircraftIds.size < 2) {
            return false
        }

        val goal =
            Struct.of(
                "unsafe_pair",
                Atom.of(aircraftIds[0]),
                Atom.of(aircraftIds[1]),
                conflict.horizontalDistance.toLogicNumber(),
                LogicInteger.of(conflict.verticalDistanceFeet),
                state.separation.horizontal.toLogicNumber(),
                LogicInteger.of(state.separation.verticalFeet),
            )

        return solveYes(goal)
    }

    override fun isManeuverAllowed(
        aircraftId: String,
        maneuver: Maneuver,
        state: SimulationState,
    ): Boolean {
        val aircraft = state.aircraft[aircraftId] ?: return false

        if (maneuver.aircraftId != aircraftId) {
            return false
        }

        val currentFeet = aircraft.flightLevel.feet
        val targetFeet = maneuver.targetFlightLevel?.feet ?: currentFeet

        val goal =
            Struct.of(
                "maneuver_allowed",
                Atom.of(maneuver.type.toPrologAtom()),
                LogicInteger.of(currentFeet),
                LogicInteger.of(targetFeet),
                LogicInteger.of(state.sector.minAltitudeFeet),
                LogicInteger.of(state.sector.maxAltitudeFeet),
            )

        return solveYes(goal)
    }

    override fun priorityOf(
        aircraftId: String,
        state: SimulationState,
    ): Int {
        val aircraft =
            state.aircraft[aircraftId]
                ?: error("Aircraft '$aircraftId' not found in simulation state.")

        val status = Atom.of(aircraft.emergencyStatus.toPrologAtom())
        val declaredPriority = Atom.of(aircraft.priority.toPrologAtom())

        val candidateScores = listOf(100, 90, 50, 10)

        return candidateScores.firstOrNull { candidate ->
            val goal =
                Struct.of(
                    "priority_score",
                    status,
                    declaredPriority,
                    LogicInteger.of(candidate),
                )

            solveYes(goal)
        } ?: error(
            "No Prolog priority rule matched aircraft '$aircraftId' " +
                "with status=${aircraft.emergencyStatus} and priority=${aircraft.priority}.",
        )
    }

    override fun explainDecision(decisionId: String): List<String> {
        val messageVariable = Var.of("Message")

        val goal =
            Struct.of(
                "explanation",
                Atom.of(decisionId),
                messageVariable,
            )

        val explanations =
            solve(goal)
                .filterIsInstance<Solution.Yes>()
                .mapNotNull { solution ->
                    solution.substitution[messageVariable]
                        ?.toString()
                        ?.removeSurrounding("'")
                        ?.trim()
                }.toList()

        return explanations.ifEmpty {
            listOf("No symbolic explanation found for decision '$decisionId'.")
        }
    }

    private fun solveYes(goal: Struct): Boolean = solve(goal).firstOrNull() is Solution.Yes

    private fun solve(goal: Struct): Sequence<Solution> =
        try {
            newSolver().solve(goal)
        } catch (ex: Exception) {
            throw ReasoningException("tuProlog query failed: $goal", ex)
        }

    private fun newSolver(): Solver =
        try {
            ClassicSolverFactory.solverWithDefaultBuiltins(
                staticKb = theory,
            )
        } catch (ex: Exception) {
            throw ReasoningException("Unable to initialize tuProlog solver.", ex)
        }

    companion object {
        private const val DEFAULT_THEORY_RESOURCE = "/airspace_rules.pl"

        fun fromClasspath(): TuPrologSafetyReasoner {
            val stream =
                TuPrologSafetyReasoner::class.java
                    .getResourceAsStream(DEFAULT_THEORY_RESOURCE)
                    ?: error("Prolog theory resource not found: $DEFAULT_THEORY_RESOURCE")

            val text = stream.bufferedReader().use { it.readText() }
            return TuPrologSafetyReasoner(text)
        }

        private fun parseTheory(theoryText: String): Theory =
            try {
                ClausesParser
                    .withDefaultOperators()
                    .parseTheory(theoryText)
            } catch (ex: Exception) {
                throw ReasoningException("Unable to parse tuProlog theory.", ex)
            }
    }
}

private fun Double.toLogicNumber() =
    if (this % 1.0 == 0.0) {
        LogicInteger.of(toInt())
    } else {
        Real.of(this)
    }

private fun EmergencyStatus.toPrologAtom(): String =
    when (this) {
        EmergencyStatus.NONE -> "none"
        EmergencyStatus.GENERAL -> "general"
        EmergencyStatus.LOW_FUEL -> "low_fuel"
    }

private fun AircraftPriority.toPrologAtom(): String =
    when (this) {
        AircraftPriority.NORMAL -> "normal"
        AircraftPriority.HIGH -> "high"
        AircraftPriority.EMERGENCY -> "emergency"
    }

private fun ManeuverType.toPrologAtom(): String =
    when (this) {
        ManeuverType.CLIMB -> "climb"
        ManeuverType.DESCEND -> "descend"
        ManeuverType.TURN_LEFT -> "turn_left"
        ManeuverType.TURN_RIGHT -> "turn_right"
        ManeuverType.SLOW_DOWN -> "slow_down"
        ManeuverType.RESUME_SPEED -> "resume_speed"
        ManeuverType.ENTER_HOLDING -> "enter_holding"
        ManeuverType.CONTINUE_ROUTE -> "continue_route"
        ManeuverType.AVOID_WEATHER_ZONE -> "avoid_weather_zone"
        ManeuverType.REROUTE_TO_WAYPOINT -> "reroute_to_waypoint"
    }
