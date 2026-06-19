package integration

import domain.Scenario
import java.nio.file.Path

interface ScenarioLoader {
    fun load(path: Path): Scenario
}

class ScenarioLoadingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
