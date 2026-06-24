package cli

import domain.Conflict
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.ResolutionPlan
import domain.SimulationState
import events.JsonlSimulationEventSink
import events.SimulationEvent
import events.SimulationEventRecorder
import explanation.ExplanationService
import integration.JasonAgentCatalog
import integration.JasonAgentSmokeAnalyzer
import integration.JsonScenarioLoader
import integration.ScenarioLoadingException
import planning.formatAsPlannerAction
import reasoning.SafetyReasoner
import reasoning.TuPrologSafetyReasoner
import replanning.WeatherReplanningDecision
import replanning.WeatherReplanningService
import simulation.ManagedSimulationEngine
import simulation.ScheduledManeuver
import java.nio.file.Path
import kotlin.system.exitProcess

object AppInfo {
    const val NAME: String = "AeroGuard-MAS"
    const val VERSION: String = "1.0-SNAPSHOT"

    fun banner(): String = "$NAME $VERSION - Multi-Agent Airspace Conflict Manager"
}

data class CliOptions(
    val scenarioPath: Path? = null,
    val eventsPath: Path? = null,
    val explain: Boolean = false,
)

fun parseCliOptions(args: Array<String>): CliOptions {
    var scenarioPath: Path? = null
    var eventsPath: Path? = null
    var explain = false
    var index = 0

    while (index < args.size) {
        when (val arg = args[index]) {
            "--scenario" -> {
                require(index + 1 < args.size) { "Missing value after --scenario." }
                scenarioPath = Path.of(args[index + 1])
                index += 2
            }

            "--events" -> {
                require(index + 1 < args.size) { "Missing value after --events." }
                eventsPath = Path.of(args[index + 1])
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
        eventsPath = eventsPath,
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

    printJasonSummary()

    val scenarioPath = options.scenarioPath
    if (scenarioPath == null) {
        println()
        println("AeroGuard-MAS CLI is ready.")
        println("Run tests with: ./gradlew test")
        println("Run Jason smoke check with: ./gradlew runJasonSmoke")
        println("Run a simple conflict demo with:")
        println(
            "./gradlew run --args=\"--scenario scenarios/simple_conflict.json " +
                "--events build/aeroguard/events/simple_conflict_events.jsonl --explain\"",
        )
        println("Run a weather replanning demo with:")
        println(
            "./gradlew run --args=\"--scenario scenarios/weather_replanning.json " +
                "--events build/aeroguard/events/weather_replanning_events.jsonl --explain\"",
        )
        return
    }

    val scenario =
        try {
            JsonScenarioLoader().load(scenarioPath)
        } catch (ex: ScenarioLoadingException) {
            System.err.println(ex.message)
            exitProcess(1)
        }

    println()
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

    val managedSimulation =
        ManagedSimulationEngine(
            safetyReasoner = reasoner,
            predictionHorizonTicks = 6,
        ).run(scenario)

    val result = managedSimulation.runResult
    val conflictResolutionPlan = managedSimulation.conflictResolutionPlan

    println()
    println("Simulation completed.")
    println("Ticks simulated: ${scenario.maxTicks}")
    println("Stored states: ${result.states.size}")

    printConflictSummary(
        title = "Current conflicts detected after managed simulation",
        conflicts = result.currentConflicts,
    )

    printConflictSummary(
        title = "Predicted conflicts generated before maneuver application",
        conflicts = result.predictedConflicts,
    )

    println()
    println("Final state:")
    printFinalState(result.finalState)

    println()
    println("Symbolic reasoning summary:")
    printPrioritySummary(result.finalState, reasoner)
    printSampleManeuverCheck(result.finalState, reasoner)

    println()
    printResolutionPlan(
        title = "Conflict resolution plan candidate",
        plan = conflictResolutionPlan,
    )

    println()
    printAppliedManeuvers(managedSimulation.appliedManeuvers)

    val weatherDecisions = managedSimulation.weatherReplanningDecisions

    println()
    printWeatherDecisions(weatherDecisions)

    val explanationService = ExplanationService(reasoner)

    val explanations =
        buildList {
            addAll(
                explanationService.explainRun(
                    runResult = result,
                    resolutionPlan = conflictResolutionPlan,
                ),
            )

            weatherDecisions.forEach { decision ->
                addAll(explanationService.explainWeatherReplanning(decision))
            }
        }

    val additionalEvents: List<SimulationEvent> =
        weatherDecisions.flatMap { decision ->
            decision.events
        }

    val eventsPath =
        options.eventsPath
            ?: Path.of("build/aeroguard/events/simulation_events.jsonl")

    JsonlSimulationEventSink(eventsPath).use { sink ->
        SimulationEventRecorder(sink).recordRun(
            runResult = result,
            resolutionPlan = conflictResolutionPlan,
            explanations = explanations,
            additionalEvents = additionalEvents,
        )
    }

    println()
    println("Events written to: $eventsPath")

    if (options.explain) {
        println()
        println("Symbolic explanations:")

        reasoner.explainDecision("unsafe_pair").forEach { explanation ->
            println("- $explanation")
        }

        reasoner.explainDecision("maneuver_climb_allowed").forEach { explanation ->
            println("- $explanation")
        }

        explanations.forEach { explanation ->
            println("- ${explanation.message}")
        }
    }
}

private fun printJasonSummary() {
    val report =
        JasonAgentSmokeAnalyzer()
            .analyze(JasonAgentCatalog(Path.of("src/main/agents")).loadRequiredAgents())

    println("Jason agents:")
    println("- smokeStatus=${if (report.passed) "PASS" else "FAIL"}")
    println("- agents=${report.agentSnapshots.map { it.agentName }}")
    println("- delegations=${report.delegations.map { "${it.from}->${it.to}/${it.performative}" }}")
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
                    "speed=${aircraft.velocity.horizontalUnitsPerTick}, " +
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

private fun printResolutionPlan(
    title: String,
    plan: ResolutionPlan?,
) {
    println("$title:")

    if (plan == null) {
        println("- none")
        return
    }

    println("- planId=${plan.id}")
    println("- conflictId=${plan.conflictId}")
    println("- maneuvers=${plan.maneuvers.map { maneuver -> maneuver.formatAsPlannerAction() }}")
    println("- explanation=${plan.explanation}")
}

private fun printAppliedManeuvers(appliedManeuvers: List<ScheduledManeuver>) {
    println("Applied maneuvers:")

    if (appliedManeuvers.isEmpty()) {
        println("- none")
        return
    }

    appliedManeuvers.forEach { scheduled ->
        println(
            "- tick=${scheduled.tick}, " +
                "aircraft=${scheduled.maneuver.aircraftId}, " +
                "maneuver=${scheduled.maneuver.formatAsPlannerAction()}",
        )
    }
}

private fun printWeatherDecisions(decisions: List<WeatherReplanningDecision>) {
    println("Weather replanning:")

    if (decisions.isEmpty()) {
        println("- none")
        return
    }

    decisions.forEach { decision ->
        println(
            "- tick=${decision.tick}, aircraft=${decision.aircraftId}, " +
                "zone=${decision.zone.id}, plan=${decision.resolutionPlan.id}, " +
                "maneuvers=${decision.resolutionPlan.maneuvers.map { it.formatAsPlannerAction() }}",
        )
    }
}
