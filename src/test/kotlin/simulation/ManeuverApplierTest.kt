package simulation

import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.Position
import domain.SimulationState
import domain.Waypoint
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ManeuverApplierTest {
    private val loader = JsonScenarioLoader()
    private val applier = ManeuverApplier()

    @Test
    fun `applies climb maneuver by changing aircraft flight level`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val maneuver =
            Maneuver(
                aircraftId = "DLH456",
                type = ManeuverType.CLIMB,
                targetFlightLevel = FlightLevel(32000),
                reason = "Create vertical separation",
            )

        val updated = applier.apply(state, maneuver)

        assertEquals(
            32000,
            updated.aircraft
                .getValue("DLH456")
                .flightLevel.feet,
        )
        assertEquals(
            30000,
            updated.aircraft
                .getValue("AZA123")
                .flightLevel.feet,
        )
    }

    @Test
    fun `applies descend maneuver by changing aircraft flight level`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val maneuver =
            Maneuver(
                aircraftId = "DLH456",
                type = ManeuverType.DESCEND,
                targetFlightLevel = FlightLevel(28000),
                reason = "Create vertical separation",
            )

        val updated = applier.apply(state, maneuver)

        assertEquals(
            28000,
            updated.aircraft
                .getValue("DLH456")
                .flightLevel.feet,
        )
    }

    @Test
    fun `applies slow down maneuver by reducing velocity`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val before =
            state.aircraft
                .getValue("DLH456")
                .velocity.horizontalUnitsPerTick

        val maneuver =
            Maneuver(
                aircraftId = "DLH456",
                type = ManeuverType.SLOW_DOWN,
                reason = "Delay convergence",
            )

        val updated = applier.apply(state, maneuver)
        val after =
            updated.aircraft
                .getValue("DLH456")
                .velocity.horizontalUnitsPerTick

        assertTrue(after < before)
        assertTrue(after >= 0.25)
    }

    @Test
    fun `applies reroute maneuver by replacing active route with target waypoint`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val targetWaypoint =
            Waypoint(
                name = "SAFE_WP",
                position = Position(12.0, 12.0),
            )

        val maneuver =
            Maneuver(
                aircraftId = "DLH456",
                type = ManeuverType.REROUTE_TO_WAYPOINT,
                targetWaypoint = targetWaypoint,
                reason = "Avoid conflict",
            )

        val updated = applier.apply(state, maneuver)
        val updatedAircraft = updated.aircraft.getValue("DLH456")

        assertEquals("SAFE_WP", updatedAircraft.activeWaypoint.name)
        assertEquals(0, updatedAircraft.activeWaypointIndex)
        assertEquals(targetWaypoint, updatedAircraft.route.waypoints.first())
    }
}
