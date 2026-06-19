package integration

import domain.Aircraft
import domain.AircraftPriority
import domain.AirspaceSector
import domain.DynamicScenarioEvent
import domain.DynamicScenarioEventType
import domain.EmergencyStatus
import domain.FlightLevel
import domain.Position
import domain.Route
import domain.Scenario
import domain.SeparationConfiguration
import domain.Velocity
import domain.Waypoint
import domain.WeatherZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class JsonScenarioLoader(
    private val json: Json =
        Json {
            ignoreUnknownKeys = false
            isLenient = false
            prettyPrint = false
        },
) : ScenarioLoader {
    override fun load(path: Path): Scenario {
        val content =
            try {
                Files.readString(path)
            } catch (ex: IOException) {
                throw ScenarioLoadingException("Unable to read scenario file: $path", ex)
            }

        return try {
            json.decodeFromString<ScenarioDto>(content).toDomain()
        } catch (ex: SerializationException) {
            throw ScenarioLoadingException("Invalid JSON scenario format in file: $path", ex)
        } catch (ex: IllegalArgumentException) {
            throw ScenarioLoadingException("Invalid scenario data in file: $path. ${ex.message}", ex)
        } catch (ex: IllegalStateException) {
            throw ScenarioLoadingException("Invalid scenario state in file: $path. ${ex.message}", ex)
        }
    }
}

@Serializable
private data class ScenarioDto(
    val name: String,
    val maxTicks: Int,
    val separation: SeparationDto,
    val aircraft: List<AircraftDto>,
    val sector: SectorDto? = null,
    val weatherZones: List<WeatherZoneDto> = emptyList(),
    val dynamicEvents: List<DynamicEventDto> = emptyList(),
) {
    fun toDomain(): Scenario =
        Scenario(
            name = name,
            maxTicks = maxTicks,
            separation = separation.toDomain(),
            aircraft = aircraft.map { it.toDomain() },
            sector = sector?.toDomain() ?: AirspaceSector.default(),
            weatherZones = weatherZones.map { it.toDomain() },
            dynamicEvents = dynamicEvents.map { it.toDomain() },
        )
}

@Serializable
private data class SeparationDto(
    val horizontal: Double,
    val vertical: Int,
) {
    fun toDomain(): SeparationConfiguration =
        SeparationConfiguration(
            horizontal = horizontal,
            verticalFeet = vertical,
        )
}

@Serializable
private data class AircraftDto(
    val id: String,
    val x: Double,
    val y: Double,
    val altitude: Int,
    val speed: Double,
    val priority: String = "normal",
    val emergency: Boolean = false,
    val emergencyStatus: String? = null,
    val route: List<WaypointDto>,
) {
    fun toDomain(): Aircraft =
        Aircraft(
            id = id,
            position = Position(x, y),
            flightLevel = FlightLevel(altitude),
            velocity = Velocity(speed),
            route = Route(route.map { it.toDomain() }),
            priority = priority.toAircraftPriority(),
            emergencyStatus = emergencyStatus.toEmergencyStatus(emergency),
        )
}

@Serializable
private data class WaypointDto(
    val name: String,
    val x: Double,
    val y: Double,
) {
    fun toDomain(): Waypoint =
        Waypoint(
            name = name,
            position = Position(x, y),
        )
}

@Serializable
private data class SectorDto(
    val id: String = "DEFAULT",
    val minX: Double = -100.0,
    val maxX: Double = 100.0,
    val minY: Double = -100.0,
    val maxY: Double = 100.0,
    val minAltitude: Int = 0,
    val maxAltitude: Int = 45000,
) {
    fun toDomain(): AirspaceSector =
        AirspaceSector(
            id = id,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minAltitudeFeet = minAltitude,
            maxAltitudeFeet = maxAltitude,
        )
}

@Serializable
private data class WeatherZoneDto(
    val id: String,
    val x: Double,
    val y: Double,
    val radius: Double,
    val activeFromTick: Int = 0,
    val activeUntilTick: Int? = null,
) {
    fun toDomain(): WeatherZone =
        WeatherZone(
            id = id,
            center = Position(x, y),
            radius = radius,
            activeFromTick = activeFromTick,
            activeUntilTick = activeUntilTick,
        )
}

@Serializable
private data class DynamicEventDto(
    val tick: Int,
    val type: String,
    val targetId: String,
    val value: String? = null,
) {
    fun toDomain(): DynamicScenarioEvent =
        DynamicScenarioEvent(
            tick = tick,
            type = type.toDynamicScenarioEventType(),
            targetId = targetId,
            value = value,
        )
}

private fun String.toAircraftPriority(): AircraftPriority =
    when (normalizedToken()) {
        "normal" -> AircraftPriority.NORMAL
        "high" -> AircraftPriority.HIGH
        "emergency" -> AircraftPriority.EMERGENCY
        else -> error("Unsupported aircraft priority: '$this'.")
    }

private fun String?.toEmergencyStatus(emergencyFlag: Boolean): EmergencyStatus {
    if (this == null) {
        return if (emergencyFlag) EmergencyStatus.GENERAL else EmergencyStatus.NONE
    }

    return when (normalizedToken()) {
        "none" -> EmergencyStatus.NONE
        "general" -> EmergencyStatus.GENERAL
        "emergency" -> EmergencyStatus.GENERAL
        "low_fuel" -> EmergencyStatus.LOW_FUEL
        else -> error("Unsupported emergency status: '$this'.")
    }
}

private fun String.toDynamicScenarioEventType(): DynamicScenarioEventType =
    when (normalizedToken()) {
        "activate_weather_zone" -> DynamicScenarioEventType.ACTIVATE_WEATHER_ZONE
        "declare_emergency" -> DynamicScenarioEventType.DECLARE_EMERGENCY
        "declare_low_fuel" -> DynamicScenarioEventType.DECLARE_LOW_FUEL
        else -> error("Unsupported dynamic event type: '$this'.")
    }

private fun String.normalizedToken(): String =
    trim()
        .lowercase()
        .replace("-", "_")
        .replace(" ", "_")
