package domain

data class Aircraft(
    val id: String,
    val position: Position,
    val flightLevel: FlightLevel,
    val velocity: Velocity,
    val route: Route,
    val priority: AircraftPriority,
    val emergencyStatus: EmergencyStatus,
) {
    init {
        require(id.isNotBlank()) { "Aircraft id must not be blank." }
    }

    val hasEmergency: Boolean
        get() = emergencyStatus.isActive
}
