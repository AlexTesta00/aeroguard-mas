package domain

/**
 * Ordered waypoint route followed by an aircraft.
 *
 * Routes are immutable and require unique waypoint names to keep rerouting and GUI
 * route snapshots unambiguous.
 */
data class Route(
    val waypoints: List<Waypoint>,
) {
    init {
        require(waypoints.isNotEmpty()) { "Route must contain at least one waypoint." }
        require(waypoints.map { it.name }.toSet().size == waypoints.size) {
            "Waypoint names in a route must be unique."
        }
    }

    val firstWaypoint: Waypoint
        get() = waypoints.first()
}
