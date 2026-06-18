package cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AeroGuardCliOptionsTest {
    @Test
    fun `parses scenario argument`() {
        val options =
            parseCliOptions(
                arrayOf("--scenario", "scenarios/simple_conflict.json"),
            )

        assertEquals(Path.of("scenarios/simple_conflict.json"), options.scenarioPath)
    }

    @Test
    fun `parses explain flag`() {
        val options =
            parseCliOptions(
                arrayOf("--scenario", "scenarios/simple_conflict.json", "--explain"),
            )

        assertTrue(options.explain)
    }
}
