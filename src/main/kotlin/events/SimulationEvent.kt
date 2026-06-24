package events

sealed interface SimulationEvent {
    val tick: Int
    val type: String
}

data class AircraftStateEvent(
    override val tick: Int,
    val aircraft: String,
    val x: Double,
    val y: Double,
    val altitude: Int,
    val speed: Double,
    val status: String,
    val priority: String,
) : SimulationEvent {
    override val type: String = "aircraft_state"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(aircraft.isNotBlank()) { "Aircraft id must not be blank." }
        require(status.isNotBlank()) { "Aircraft status must not be blank." }
        require(priority.isNotBlank()) { "Aircraft priority must not be blank." }
    }
}

data class ConflictDetectedEvent(
    override val tick: Int,
    val aircraft: List<String>,
    val severity: String,
    val horizontalDistance: Double,
    val verticalDistance: Int,
    val predictedAtTick: Int? = null,
) : SimulationEvent {
    override val type: String = "conflict_detected"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(aircraft.size >= 2) { "Conflict event must contain at least two aircraft." }
        require(severity.isNotBlank()) { "Severity must not be blank." }
        require(horizontalDistance >= 0.0) { "Horizontal distance must be non-negative." }
        require(verticalDistance >= 0) { "Vertical distance must be non-negative." }
        require(predictedAtTick == null || predictedAtTick >= tick) {
            "Predicted tick must be greater than or equal to event tick."
        }
    }
}

data class PlanGeneratedEvent(
    override val tick: Int,
    val planner: String,
    val aircraft: String,
    val actions: List<String>,
) : SimulationEvent {
    override val type: String = "plan_generated"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(planner.isNotBlank()) { "Planner name must not be blank." }
        require(aircraft.isNotBlank()) { "Aircraft id must not be blank." }
        require(actions.isNotEmpty()) { "Plan event must contain at least one action." }
    }
}

data class ManeuverSelectedEvent(
    override val tick: Int,
    val aircraft: String,
    val maneuver: String,
    val targetAltitude: Int? = null,
    val reason: String,
) : SimulationEvent {
    override val type: String = "maneuver_selected"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(aircraft.isNotBlank()) { "Aircraft id must not be blank." }
        require(maneuver.isNotBlank()) { "Maneuver must not be blank." }
        require(reason.isNotBlank()) { "Reason must not be blank." }
    }
}

data class BeliefUpdatedEvent(
    override val tick: Int,
    val agent: String,
    val belief: String,
) : SimulationEvent {
    override val type: String = "belief_update"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(agent.isNotBlank()) { "Agent name must not be blank." }
        require(belief.isNotBlank()) { "Belief must not be blank." }
    }
}

data class ExplanationEvent(
    override val tick: Int,
    val agent: String,
    val message: String,
) : SimulationEvent {
    override val type: String = "explanation"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(agent.isNotBlank()) { "Agent name must not be blank." }
        require(message.isNotBlank()) { "Explanation message must not be blank." }
    }
}

data class WeatherZoneActivatedEvent(
    override val tick: Int,
    val zone: String,
    val x: Double,
    val y: Double,
    val radius: Double,
) : SimulationEvent {
    override val type: String = "weather_zone_activated"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(zone.isNotBlank()) { "Weather zone id must not be blank." }
        require(radius > 0.0) { "Weather zone radius must be positive." }
    }
}

data class ReplanningTriggeredEvent(
    override val tick: Int,
    val aircraft: String,
    val reason: String,
) : SimulationEvent {
    override val type: String = "replanning_triggered"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(aircraft.isNotBlank()) { "Aircraft id must not be blank." }
        require(reason.isNotBlank()) { "Replanning reason must not be blank." }
    }
}

data class RouteWaypointDto(
    val name: String,
    val x: Double,
    val y: Double,
) {
    init {
        require(name.isNotBlank()) { "Waypoint name must not be blank." }
    }
}

data class RouteSnapshotEvent(
    override val tick: Int,
    val aircraft: String,
    val waypoints: List<RouteWaypointDto>,
) : SimulationEvent {
    override val type: String = "route_snapshot"

    init {
        require(tick >= 0) { "Event tick must be non-negative." }
        require(aircraft.isNotBlank()) { "Aircraft id must not be blank." }
        require(waypoints.isNotEmpty()) { "Route snapshot must contain at least one waypoint." }
    }
}
