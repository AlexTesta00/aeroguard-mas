package simulation

import domain.Aircraft
import domain.AircraftPriority
import domain.AirspaceSector
import domain.ConflictType
import domain.EmergencyStatus
import domain.FlightLevel
import domain.Position
import domain.Route
import domain.SeparationConfiguration
import domain.SimulationState
import domain.Velocity
import domain.Waypoint
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ConflictDetectorTest {
    private val detector = ConflictDetector()

    @Test
    fun `detects current conflict when horizontal and vertical separation are both below thresholds`() {
        val state =
            SimulationState(
                tick = 0,
                scenarioName = "unit_conflict",
                aircraft =
                    listOf(
                        testAircraft("AZA123", Position(0.0, 0.0), 30000),
                        testAircraft("DLH456", Position(3.0, 0.0), 30000),
                    ).associateBy { it.id },
                separation = SeparationConfiguration(horizontal = 5.0, verticalFeet = 1000),
                sector = AirspaceSector.default(),
            )

        val conflicts = detector.detectCurrentConflicts(state)

        assertEquals(1, conflicts.size)
        val conflict = conflicts.single()
        assertEquals(ConflictType.HORIZONTAL_SEPARATION_LOSS, conflict.type)
        assertEquals(setOf("AZA123", "DLH456"), conflict.aircraftIds)
        assertEquals(3.0, conflict.horizontalDistance, 0.000001)
        assertEquals(0, conflict.verticalDistanceFeet)
    }

    @Test
    fun `does not detect conflict when vertical separation is sufficient`() {
        val state =
            SimulationState(
                tick = 0,
                scenarioName = "vertical_safe",
                aircraft =
                    listOf(
                        testAircraft("AZA123", Position(0.0, 0.0), 30000),
                        testAircraft("DLH456", Position(3.0, 0.0), 32000),
                    ).associateBy { it.id },
                separation = SeparationConfiguration(horizontal = 5.0, verticalFeet = 1000),
                sector = AirspaceSector.default(),
            )

        val conflicts = detector.detectCurrentConflicts(state)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun `predicts simple conflict within horizon`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val predictions =
            detector.predictConflicts(
                state = state,
                horizonTicks = 6,
            )

        val predicted =
            predictions.firstOrNull {
                it.aircraftIds == setOf("AZA123", "DLH456")
            }

        assertNotNull(predicted)
        assertEquals(ConflictType.PREDICTED_CONFLICT, predicted!!.type)
        assertEquals(0, predicted.tick)
        assertTrue(predicted.predictedAtTick in 1..6)
    }

    private fun testAircraft(
        id: String,
        position: Position,
        altitudeFeet: Int,
    ): Aircraft =
        Aircraft(
            id = id,
            position = position,
            flightLevel = FlightLevel(altitudeFeet),
            velocity = Velocity(1.0),
            route = Route(listOf(Waypoint("W-$id", Position(10.0, 10.0)))),
            priority = AircraftPriority.NORMAL,
            emergencyStatus = EmergencyStatus.NONE,
        )
}
