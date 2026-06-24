package simulation

import domain.Aircraft
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.Route
import domain.SimulationState
import domain.Velocity
import domain.Waypoint

class ManeuverApplier {
    fun apply(
        state: SimulationState,
        maneuver: Maneuver,
    ): SimulationState {
        val aircraft = state.aircraft[maneuver.aircraftId] ?: return state

        val updatedAircraft =
            when (maneuver.type) {
                ManeuverType.CLIMB,
                ManeuverType.DESCEND,
                -> applyAltitudeChange(aircraft, maneuver)

                ManeuverType.SLOW_DOWN -> applySlowDown(aircraft)

                ManeuverType.RESUME_SPEED -> applyResumeSpeed(aircraft)

                ManeuverType.REROUTE_TO_WAYPOINT,
                ManeuverType.TURN_LEFT,
                ManeuverType.TURN_RIGHT,
                -> applyReroute(aircraft, maneuver.targetWaypoint)

                ManeuverType.AVOID_WEATHER_ZONE -> aircraft

                ManeuverType.ENTER_HOLDING -> applySlowDown(aircraft)

                ManeuverType.CONTINUE_ROUTE -> aircraft
            }

        return state.copy(
            aircraft = state.aircraft + (aircraft.id to updatedAircraft),
        )
    }

    fun applyAll(
        state: SimulationState,
        maneuvers: List<Maneuver>,
    ): SimulationState =
        maneuvers.fold(state) { currentState, maneuver ->
            apply(currentState, maneuver)
        }

    private fun applyAltitudeChange(
        aircraft: Aircraft,
        maneuver: Maneuver,
    ): Aircraft {
        val targetFlightLevel = maneuver.targetFlightLevel ?: return aircraft

        return aircraft.copy(
            flightLevel = FlightLevel(targetFlightLevel.feet),
        )
    }

    private fun applySlowDown(aircraft: Aircraft): Aircraft {
        val currentSpeed = aircraft.velocity.horizontalUnitsPerTick
        val reducedSpeed = maxOf(0.25, currentSpeed * 0.5)

        return aircraft.copy(
            velocity = Velocity(reducedSpeed),
        )
    }

    private fun applyResumeSpeed(aircraft: Aircraft): Aircraft {
        val currentSpeed = aircraft.velocity.horizontalUnitsPerTick
        val resumedSpeed = maxOf(currentSpeed, 1.0)

        return aircraft.copy(
            velocity = Velocity(resumedSpeed),
        )
    }

    private fun applyReroute(
        aircraft: Aircraft,
        targetWaypoint: Waypoint?,
    ): Aircraft {
        if (targetWaypoint == null) {
            return aircraft
        }

        val newRoute =
            Route(
                waypoints = listOf(targetWaypoint),
            )

        return aircraft.copy(
            route = newRoute,
            activeWaypointIndex = 0,
        )
    }
}
