package domain

data class Aircraft(
    val id: String,
    val position: Position,
    val flightLevel: FlightLevel,
    val velocity: Velocity,
    val route: Route,
    val priority: AircraftPriority,
    val emergencyStatus: EmergencyStatus,
    val activeWaypointIndex: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "Aircraft id must not be blank." }
        require(activeWaypointIndex in route.waypoints.indices) {
            "Active waypoint index must point to an existing waypoint."
        }
    }

    val hasEmergency: Boolean
        get() = emergencyStatus.isActive

    val activeWaypoint: Waypoint
        get() = route.waypoints[activeWaypointIndex]
}
