package simulation

import domain.Aircraft
import domain.AircraftPriority
import domain.EmergencyStatus
import domain.FlightLevel
import domain.Position
import domain.Route
import domain.Velocity
import domain.Waypoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AircraftMoverTest {
    private val mover = AircraftMover()

    @Test
    fun `moves aircraft toward active waypoint by speed per tick`() {
        val aircraft =
            Aircraft(
                id = "AZA123",
                position = Position(0.0, 0.0),
                flightLevel = FlightLevel(30000),
                velocity = Velocity(1.0),
                route = Route(listOf(Waypoint("W1", Position(3.0, 4.0)))),
                priority = AircraftPriority.NORMAL,
                emergencyStatus = EmergencyStatus.NONE,
            )

        val moved = mover.advanceOneTick(aircraft)

        assertEquals(0.6, moved.position.x, 0.000001)
        assertEquals(0.8, moved.position.y, 0.000001)
        assertEquals(0, moved.activeWaypointIndex)
    }

    @Test
    fun `continues to next waypoint when speed exceeds first leg distance`() {
        val aircraft =
            Aircraft(
                id = "DLH456",
                position = Position(0.0, 0.0),
                flightLevel = FlightLevel(31000),
                velocity = Velocity(2.0),
                route =
                    Route(
                        listOf(
                            Waypoint("W1", Position(1.0, 0.0)),
                            Waypoint("W2", Position(3.0, 0.0)),
                        ),
                    ),
                priority = AircraftPriority.NORMAL,
                emergencyStatus = EmergencyStatus.NONE,
            )

        val moved = mover.advanceOneTick(aircraft)

        assertEquals(2.0, moved.position.x, 0.000001)
        assertEquals(0.0, moved.position.y, 0.000001)
        assertEquals(1, moved.activeWaypointIndex)
    }
}
