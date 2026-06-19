package domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DomainInvariantTest {
    @Test
    fun `position computes euclidean distance`() {
        val origin = Position(0.0, 0.0)
        val target = Position(3.0, 4.0)

        assertEquals(5.0, origin.distanceTo(target), 0.000001)
    }

    @Test
    fun `position rejects non finite coordinates`() {
        assertThrows(IllegalArgumentException::class.java) {
            Position(Double.NaN, 0.0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            Position(0.0, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `flight level rejects negative altitude`() {
        assertThrows(IllegalArgumentException::class.java) {
            FlightLevel(-100)
        }
    }

    @Test
    fun `velocity rejects negative speed`() {
        assertThrows(IllegalArgumentException::class.java) {
            Velocity(-1.0)
        }
    }

    @Test
    fun `route requires at least one waypoint`() {
        assertThrows(IllegalArgumentException::class.java) {
            Route(emptyList())
        }
    }

    @Test
    fun `aircraft requires non blank identifier`() {
        val route = Route(listOf(Waypoint("W1", Position(1.0, 1.0))))

        assertThrows(IllegalArgumentException::class.java) {
            Aircraft(
                id = " ",
                position = Position(0.0, 0.0),
                flightLevel = FlightLevel(30000),
                velocity = Velocity(1.0),
                route = route,
                priority = AircraftPriority.NORMAL,
                emergencyStatus = EmergencyStatus.NONE,
            )
        }
    }
}
