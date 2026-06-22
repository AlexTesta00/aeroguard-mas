package planning

import domain.Maneuver
import domain.ManeuverType

fun Maneuver.formatAsPlannerAction(): String =
    when (type) {
        ManeuverType.CLIMB ->
            "climb($aircraftId,${targetFlightLevel?.feet ?: "unknown"})"

        ManeuverType.DESCEND ->
            "descend($aircraftId,${targetFlightLevel?.feet ?: "unknown"})"

        ManeuverType.TURN_LEFT ->
            "turn_left($aircraftId,${targetWaypoint?.name ?: "unknown"})"

        ManeuverType.TURN_RIGHT ->
            "turn_right($aircraftId,${targetWaypoint?.name ?: "unknown"})"

        ManeuverType.SLOW_DOWN ->
            "slow_down($aircraftId)"

        ManeuverType.RESUME_SPEED ->
            "resume_speed($aircraftId)"

        ManeuverType.ENTER_HOLDING ->
            "enter_holding($aircraftId)"

        ManeuverType.CONTINUE_ROUTE ->
            "continue_route($aircraftId)"

        ManeuverType.AVOID_WEATHER_ZONE ->
            "avoid_weather_zone($aircraftId)"

        ManeuverType.REROUTE_TO_WAYPOINT ->
            "reroute_to_waypoint($aircraftId,${targetWaypoint?.name ?: "unknown"})"
    }
