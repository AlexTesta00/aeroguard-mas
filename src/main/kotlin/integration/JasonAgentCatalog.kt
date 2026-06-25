package integration

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Loads the required Jason AgentSpeak source files from disk.
 *
 * The catalog is intentionally lightweight: it validates that all mandatory agent files
 * can be read and returns their source for smoke analysis.
 */
class JasonAgentCatalog(
    private val agentsDirectory: Path = Path.of("agents"),
) {
    fun loadRequiredAgents(): List<JasonAgentSource> =
        REQUIRED_AGENTS.map { expected ->
            val path = agentsDirectory.resolve(expected.fileName)

            val source =
                try {
                    Files.readString(path)
                } catch (ex: IOException) {
                    throw JasonIntegrationException(
                        "Unable to read Jason agent '${expected.agentName}' at $path.",
                        ex,
                    )
                }

            JasonAgentSource(
                agentName = expected.agentName,
                fileName = expected.fileName,
                path = path,
                source = source,
            )
        }

    companion object {
        val REQUIRED_AGENTS: List<ExpectedJasonAgent> =
            listOf(
                ExpectedJasonAgent("aircraft_agent", "aircraft.asl"),
                ExpectedJasonAgent("sector_controller", "sector_controller.asl"),
                ExpectedJasonAgent("conflict_detector", "conflict_detector.asl"),
                ExpectedJasonAgent("resolution_planner", "resolution_planner.asl"),
                ExpectedJasonAgent("explanation_agent", "explanation_agent.asl"),
            )
    }
}

/**
 * Expected Jason agent descriptor.
 */
data class ExpectedJasonAgent(
    val agentName: String,
    val fileName: String,
)

/**
 * Exception thrown when Jason source integration fails.
 */
class JasonIntegrationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
