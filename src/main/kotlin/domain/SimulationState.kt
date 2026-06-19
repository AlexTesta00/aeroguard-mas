package domain

data class SimulationState(
    val tick: Int,
    val scenarioName: String,
    val aircraft: Map<String, Aircraft>,
    val separation: SeparationConfiguration,
    val sector: AirspaceSector,
    val weatherZones: List<WeatherZone> = emptyList(),
    val agentSnapshots: List<AgentSnapshot> = emptyList(),
) {
    init {
        require(tick >= 0) { "Simulation tick must be non-negative." }
        require(scenarioName.isNotBlank()) { "Scenario name must not be blank." }
        require(aircraft.isNotEmpty()) { "Simulation state must contain at least one aircraft." }
    }

    companion object {
        fun fromScenario(scenario: Scenario): SimulationState =
            SimulationState(
                tick = 0,
                scenarioName = scenario.name,
                aircraft = scenario.aircraft.associateBy { it.id },
                separation = scenario.separation,
                sector = scenario.sector,
                weatherZones = scenario.weatherZones,
            )
    }
}
