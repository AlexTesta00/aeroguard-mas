package domain

/**
 * Immutable domain model for one simulated aircraft.
 *
 * The aircraft stores its current position, flight level, velocity, route, priority,
 * emergency status, and active waypoint index. Simulation components create updated
 * copies instead of mutating this object in place.
 *
 * @property id unique aircraft identifier.
 * @property position current two-dimensional position.
 * @property flightLevel current discrete altitude.
 * @property velocity horizontal speed expressed in units per tick.
 * @property route ordered waypoint route.
 * @property priority symbolic priority used by the reasoner.
 * @property emergencyStatus current emergency state.
 * @property activeWaypointIndex index of the waypoint currently being followed.
 */
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
