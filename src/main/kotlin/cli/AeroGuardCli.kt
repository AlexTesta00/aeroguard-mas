package cli

import domain.Conflict
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.ResolutionPlan
import domain.SimulationState
import integration.JsonScenarioLoader
import integration.ScenarioLoadingException
import planning.StripsResolutionPlanner
import reasoning.SafetyReasoner
import reasoning.TuPrologSafetyReasoner
import simulation.ConflictDetector
import simulation.SimulationEngine
import java.nio.file.Path
import kotlin.system.exitProcess

object AppInfo {
    const val NAME: String = "AeroGuard-MAS"
    const val VERSION: String = "0.1.0-SNAPSHOT"

    fun banner(): String = "$NAME $VERSION - Multi-Agent Airspace Conflict Manager"
}

data class CliOptions(
    val scenarioPath: Path? = null,
    val explain: Boolean = false,
)

fun parseCliOptions(args: Array<String>): CliOptions {
    var scenarioPath: Path? = null
    var explain = false
    var index = 0

    while (index < args.size) {
        when (val arg = args[index]) {
            "--scenario" -> {
                require(index + 1 < args.size) { "Missing value after --scenario." }
                scenarioPath = Path.of(args[index + 1])
                index += 2
            }

            "--explain" -> {
                explain = true
                index += 1
            }

            else -> error("Unknown argument: $arg")
        }
    }

    return CliOptions(
        scenarioPath = scenarioPath,
        explain = explain,
    )
}

fun main(args: Array<String>) {
    println(AppInfo.banner())

    val options =
        try {
            parseCliOptions(args)
        } catch (ex: IllegalArgumentException) {
            System.err.println("Invalid arguments: ${ex.message}")
            exitProcess(1)
        } catch (ex: IllegalStateException) {
            System.err.println("Invalid arguments: ${ex.message}")
            exitProcess(1)
        }

    val scenarioPath = options.scenarioPath
    if (scenarioPath == null) {
        println("Run tests with: ./gradlew test")
        println("Run a scenario with: ./gradlew run --args=\"--scenario scenarios/simple_conflict.json --explain\"")
        return
    }

    val scenario =
        try {
            JsonScenarioLoader().load(scenarioPath)
        } catch (ex: ScenarioLoadingException) {
            System.err.println(ex.message)
            exitProcess(1)
        }

    println("Loaded scenario: ${scenario.name}")
    println("Max ticks: ${scenario.maxTicks}")
    println(
        "Separation: horizontal=${scenario.separation.horizontal}, " +
            "vertical=${scenario.separation.verticalFeet}ft",
    )
    println("Aircraft count: ${scenario.aircraft.size}")
    scenario.aircraft.forEach { aircraft ->
        println(
            "- ${aircraft.id}: " +
                "pos=(${aircraft.position.x}, ${aircraft.position.y}), " +
                "altitude=${aircraft.flightLevel.feet}, " +
                "speed=${aircraft.velocity.horizontalUnitsPerTick}, " +
                "priority=${aircraft.priority}, " +
                "emergency=${aircraft.emergencyStatus}, " +
                "nextWaypoint=${aircraft.activeWaypoint.name}",
        )
    }

    if (scenario.weatherZones.isNotEmpty()) {
        println("Weather zones: ${scenario.weatherZones.joinToString { it.id }}")
    }

    if (scenario.dynamicEvents.isNotEmpty()) {
        println("Dynamic events: ${scenario.dynamicEvents.size}")
    }

    val reasoner = TuPrologSafetyReasoner.fromClasspath()
    val detector = ConflictDetector(safetyReasoner = reasoner)
    val engine =
        SimulationEngine(
            conflictDetector = detector,
            predictionHorizonTicks = 6,
        )

    val result = engine.run(scenario)

    println()
    println("Simulation completed.")
    println("Ticks simulated: ${scenario.maxTicks}")
    println("Stored states: ${result.states.size}")

    printConflictSummary(
        title = "Current conflicts detected",
        conflicts = result.currentConflicts,
    )

    printConflictSummary(
        title = "Predicted conflicts generated",
        conflicts = result.predictedConflicts,
    )

    println()
    println("Final state:")
    printFinalState(result.finalState)

    println()
    println("Symbolic reasoning summary:")
    printPrioritySummary(result.finalState, reasoner)
    printSampleManeuverCheck(result.finalState, reasoner)

    val resolutionPlanner = StripsResolutionPlanner(reasoner)
    val conflictForPlanning =
        result.predictedConflicts.firstOrNull()
            ?: result.currentConflicts.firstOrNull()

    val resolutionPlan =
        conflictForPlanning?.let { conflict ->
            val planningState =
                result.states.firstOrNull { it.tick == conflict.tick }
                    ?: result.finalState

            resolutionPlanner.planResolution(
                conflict = conflict,
                state = planningState,
            )
        }

    println()
    printResolutionPlan(resolutionPlan)

    if (options.explain) {
        println()
        println("Symbolic explanations:")
        reasoner.explainDecision("unsafe_pair").forEach { explanation ->
            println("- $explanation")
        }
        reasoner.explainDecision("maneuver_climb_allowed").forEach { explanation ->
            println("- $explanation")
        }

        if (resolutionPlan != null) {
            println("- ${resolutionPlan.explanation}")
        }
    }
}

private fun printConflictSummary(
    title: String,
    conflicts: List<Conflict>,
) {
    println()
    println("$title: ${conflicts.size}")

    if (conflicts.isEmpty()) {
        println("- none")
        return
    }

    conflicts.take(10).forEach { conflict ->
        val predictionText =
            conflict.predictedAtTick
                ?.let { ", predictedAtTick=$it" }
                .orEmpty()

        println(
            "- tick=${conflict.tick}$predictionText, " +
                "type=${conflict.type}, " +
                "aircraft=${conflict.aircraftIds.sorted()}, " +
                "horizontal=${"%.2f".format(conflict.horizontalDistance)}, " +
                "vertical=${conflict.verticalDistanceFeet}ft",
        )
    }

    if (conflicts.size > 10) {
        println("- ... ${conflicts.size - 10} more")
    }
}

private fun printFinalState(state: SimulationState) {
    state.aircraft.values
        .sortedBy { it.id }
        .forEach { aircraft ->
            println(
                "- ${aircraft.id}: " +
                    "pos=(${String.format("%.2f", aircraft.position.x)}, " +
                    "${String.format("%.2f", aircraft.position.y)}), " +
                    "altitude=${aircraft.flightLevel.feet}, " +
                    "activeWaypoint=${aircraft.activeWaypoint.name}",
            )
        }
}

private fun printPrioritySummary(
    state: SimulationState,
    reasoner: SafetyReasoner,
) {
    println("Aircraft priorities from Prolog:")
    state.aircraft.values
        .sortedBy { it.id }
        .forEach { aircraft ->
            println("- ${aircraft.id}: priorityScore=${reasoner.priorityOf(aircraft.id, state)}")
        }
}

private fun printSampleManeuverCheck(
    state: SimulationState,
    reasoner: SafetyReasoner,
) {
    val firstAircraft =
        state.aircraft.values
            .sortedBy { it.id }
            .firstOrNull() ?: return
    val targetAltitude = firstAircraft.flightLevel.feet + 2000

    val maneuver =
        Maneuver(
            aircraftId = firstAircraft.id,
            type = ManeuverType.CLIMB,
            targetFlightLevel = FlightLevel(targetAltitude),
            reason = "Sample symbolic maneuver feasibility check",
        )

    val allowed = reasoner.isManeuverAllowed(firstAircraft.id, maneuver, state)

    println(
        "Sample maneuver feasibility: " +
            "climb(${firstAircraft.id}, $targetAltitude) allowed=$allowed",
    )
}

private fun printResolutionPlan(plan: ResolutionPlan?) {
    println("Resolution plan candidate:")

    if (plan == null) {
        println("- none")
        return
    }

    println("- planId=${plan.id}")
    println("- conflictId=${plan.conflictId}")
    println("- maneuvers=${plan.maneuvers.map { maneuver -> formatManeuver(maneuver) }}")
    println("- explanation=${plan.explanation}")
}

private fun formatManeuver(maneuver: Maneuver): String =
    when (maneuver.type) {
        ManeuverType.CLIMB ->
            "climb(${maneuver.aircraftId},${maneuver.targetFlightLevel?.feet})"

        ManeuverType.DESCEND ->
            "descend(${maneuver.aircraftId},${maneuver.targetFlightLevel?.feet})"

        ManeuverType.SLOW_DOWN ->
            "slow_down(${maneuver.aircraftId})"

        ManeuverType.RESUME_SPEED ->
            "resume_speed(${maneuver.aircraftId})"

        ManeuverType.ENTER_HOLDING ->
            "enter_holding(${maneuver.aircraftId})"

        ManeuverType.CONTINUE_ROUTE ->
            "continue_route(${maneuver.aircraftId})"

        ManeuverType.TURN_LEFT ->
            "turn_left(${maneuver.aircraftId})"

        ManeuverType.TURN_RIGHT ->
            "turn_right(${maneuver.aircraftId})"

        ManeuverType.AVOID_WEATHER_ZONE ->
            "avoid_weather_zone(${maneuver.aircraftId})"

        ManeuverType.REROUTE_TO_WAYPOINT ->
            "reroute_to_waypoint(${maneuver.aircraftId})"
    }
