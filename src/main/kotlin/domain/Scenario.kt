package domain

/**
 * Complete input scenario for an AeroGuard-MAS simulation run.
 *
 * A scenario defines aircraft, separation thresholds, sector bounds, optional weather
 * zones, and optional dynamic events.
 */
data class Scenario(
    val name: String,
    val maxTicks: Int,
    val separation: SeparationConfiguration,
    val aircraft: List<Aircraft>,
    val sector: AirspaceSector = AirspaceSector.default(),
    val weatherZones: List<WeatherZone> = emptyList(),
    val dynamicEvents: List<DynamicScenarioEvent> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "Scenario name must not be blank." }
        require(maxTicks > 0) { "Scenario maxTicks must be positive." }
        require(aircraft.isNotEmpty()) { "Scenario must contain at least one aircraft." }

        val aircraftIds = aircraft.map { it.id }
        require(aircraftIds.toSet().size == aircraftIds.size) {
            "Aircraft ids must be unique."
        }

        val weatherZoneIds = weatherZones.map { it.id }
        require(weatherZoneIds.toSet().size == weatherZoneIds.size) {
            "Weather zone ids must be unique."
        }
    }

    fun aircraftById(id: String): Aircraft =
        aircraft.firstOrNull { it.id == id }
            ?: error("Aircraft '$id' not found in scenario '$name'.")
}
