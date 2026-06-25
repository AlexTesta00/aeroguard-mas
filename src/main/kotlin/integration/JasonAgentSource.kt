package integration

import java.nio.file.Path

/**
 * Loaded Jason agent source file with metadata.
 */
data class JasonAgentSource(
    val agentName: String,
    val fileName: String,
    val path: Path,
    val source: String,
) {
    init {
        require(agentName.isNotBlank()) { "Jason agent name must not be blank." }
        require(fileName.endsWith(".asl")) { "Jason agent file must have .asl extension." }
        require(source.isNotBlank()) { "Jason agent source must not be blank." }
    }
}
