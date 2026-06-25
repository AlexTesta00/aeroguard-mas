package events

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Event sink that writes simulation events to a UTF-8 JSONL file.
 *
 * Parent directories are created automatically and existing files are overwritten to
 * keep scenario runs reproducible.
 */
class JsonlSimulationEventSink(
    private val outputPath: Path,
) : SimulationEventSink,
    AutoCloseable {
    private val writer = createWriter(outputPath)

    override fun emit(event: SimulationEvent) {
        writer.write(event.toJsonLine())
        writer.newLine()
        writer.flush()
    }

    override fun close() {
        writer.close()
    }

    companion object {
        private fun createWriter(outputPath: Path) =
            run {
                outputPath.parent?.let { parent ->
                    Files.createDirectories(parent)
                }

                val options: Array<OpenOption> =
                    arrayOf(
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                    )

                Files.newBufferedWriter(
                    outputPath,
                    StandardCharsets.UTF_8,
                    *options,
                )
            }
    }
}
