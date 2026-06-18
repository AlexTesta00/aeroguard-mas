package cli

import integration.JsonScenarioLoader
import integration.ScenarioLoadingException
import java.nio.file.Path
import kotlin.system.exitProcess

object AppInfo {
    const val NAME: String = "AeroGuard-MAS"
    const val VERSION: String = "0.1.0-SNAPSHOT"

    fun banner(): String = "$NAME $VERSION — Multi-Agent Airspace Conflict Manager"
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
        println("Scenario loader are ready.")
        println("Run tests with: ./gradlew test")
        println("Load a scenario with: ./gradlew run --args=\"--scenario scenarios/simple_conflict.json\"")
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
                "nextWaypoint=${aircraft.route.firstWaypoint.name}",
        )
    }

    if (scenario.weatherZones.isNotEmpty()) {
        println("Weather zones: ${scenario.weatherZones.joinToString { it.id }}")
    }

    if (scenario.dynamicEvents.isNotEmpty()) {
        println("Dynamic events: ${scenario.dynamicEvents.size}")
    }

    if (options.explain) {
        println("Explain mode requested.")
    }

    println("Scenario loading completed.")
}
