package domain

enum class ManeuverType {
    CLIMB,
    DESCEND,
    TURN_LEFT,
    TURN_RIGHT,
    SLOW_DOWN,
    RESUME_SPEED,
    ENTER_HOLDING,
    CONTINUE_ROUTE,
    AVOID_WEATHER_ZONE,
    REROUTE_TO_WAYPOINT,
}

data class Maneuver(
    val aircraftId: String,
    val type: ManeuverType,
    val targetFlightLevel: FlightLevel? = null,
    val targetWaypoint: Waypoint? = null,
    val reason: String? = null,
) {
    init {
        require(aircraftId.isNotBlank()) { "Maneuver aircraft id must not be blank." }
    }
}
