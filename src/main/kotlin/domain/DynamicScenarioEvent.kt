package domain

enum class DynamicScenarioEventType {
    ACTIVATE_WEATHER_ZONE,
    DECLARE_EMERGENCY,
    DECLARE_LOW_FUEL,
}

data class DynamicScenarioEvent(
    val tick: Int,
    val type: DynamicScenarioEventType,
    val targetId: String,
    val value: String? = null,
) {
    init {
        require(tick >= 0) { "Dynamic event tick must be non-negative." }
        require(targetId.isNotBlank()) { "Dynamic event target id must not be blank." }
    }
}
