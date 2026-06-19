package cli

import domain.Conflict
import domain.SimulationState
import integration.JsonScenarioLoader
import integration.ScenarioLoadingException
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
        println("Run a scenario with: ./gradlew run --args=\"--scenario scenarios/simple_conflict.json\"")
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

    val engine = SimulationEngine(predictionHorizonTicks = 6)
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

    if (options.explain) {
        println()
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
