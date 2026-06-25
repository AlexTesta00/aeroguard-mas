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

/**
 * Maneuver types supported by the planner and simulation engine.
 * Some maneuvers have direct physical effects, such as climb, descend, slow down, and
 * reroute. Others are symbolic or explanatory and are combined with physical maneuvers.
 * Planned action assigned to a specific aircraft.
 *
 * Optional target fields are populated depending on the maneuver type. For example,
 * climb/descend use [targetFlightLevel], while reroute uses [targetWaypoint].
 */
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
