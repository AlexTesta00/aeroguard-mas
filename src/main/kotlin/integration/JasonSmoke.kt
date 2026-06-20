package integration

import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val agentsDirectory = args.firstOrNull()?.let { Path.of(it) } ?: Path.of("src/main/agents")

    val report =
        JasonAgentSmokeAnalyzer()
            .analyze(
                JasonAgentCatalog(agentsDirectory).loadRequiredAgents(),
            )

    println(report.toHumanReadableText())

    if (!report.passed) {
        exitProcess(1)
    }
}
