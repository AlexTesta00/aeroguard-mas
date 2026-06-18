package domain

data class AirspaceSector(
    val id: String,
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val minAltitudeFeet: Int,
    val maxAltitudeFeet: Int,
) {
    init {
        require(id.isNotBlank()) { "Airspace sector id must not be blank." }
        require(maxX > minX) { "Airspace sector maxX must be greater than minX." }
        require(maxY > minY) { "Airspace sector maxY must be greater than minY." }
        require(minAltitudeFeet >= 0) { "Airspace sector min altitude must be non-negative." }
        require(maxAltitudeFeet > minAltitudeFeet) {
            "Airspace sector max altitude must be greater than min altitude."
        }
    }

    companion object {
        fun default(): AirspaceSector =
            AirspaceSector(
                id = "DEFAULT",
                minX = -100.0,
                maxX = 100.0,
                minY = -100.0,
                maxY = 100.0,
                minAltitudeFeet = 0,
                maxAltitudeFeet = 45000,
            )
    }
}
