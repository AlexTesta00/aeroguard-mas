package integration

import domain.Scenario
import java.nio.file.Path

/**
 * Abstraction for loading scenarios from external sources.
 */
interface ScenarioLoader {
    fun load(path: Path): Scenario
}

/**
 * Exception thrown when a scenario cannot be loaded or validated.
 */
class ScenarioLoadingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
