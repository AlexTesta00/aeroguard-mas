package events

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun SimulationEvent.toJsonObject(): JsonObject =
    when (this) {
        is AircraftStateEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("aircraft", aircraft)
                put("x", x)
                put("y", y)
                put("altitude", altitude)
                put("speed", speed)
                put("status", status)
                put("priority", priority)
            }

        is ConflictDetectedEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                putJsonArray("aircraft") {
                    aircraft.forEach { aircraftId ->
                        add(JsonPrimitive(aircraftId))
                    }
                }
                put("severity", severity)
                put("horizontalDistance", horizontalDistance)
                put("verticalDistance", verticalDistance)
                predictedAtTick?.let { value ->
                    put("predictedAtTick", value)
                }
            }

        is PlanGeneratedEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("planner", planner)
                put("aircraft", aircraft)
                putJsonArray("actions") {
                    actions.forEach { action ->
                        add(JsonPrimitive(action))
                    }
                }
            }

        is ManeuverSelectedEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("aircraft", aircraft)
                put("maneuver", maneuver)
                targetAltitude?.let { value ->
                    put("targetAltitude", value)
                }
                put("reason", reason)
            }

        is BeliefUpdatedEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("agent", agent)
                put("belief", belief)
            }

        is ExplanationEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("agent", agent)
                put("message", message)
            }

        is WeatherZoneActivatedEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("zone", zone)
                put("x", x)
                put("y", y)
                put("radius", radius)
            }

        is ReplanningTriggeredEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("aircraft", aircraft)
                put("reason", reason)
            }

        is RouteSnapshotEvent ->
            buildJsonObject {
                put("tick", tick)
                put("type", type)
                put("aircraft", aircraft)
                putJsonArray("waypoints") {
                    waypoints.forEach { waypoint ->
                        add(
                            buildJsonObject {
                                put("name", waypoint.name)
                                put("x", waypoint.x)
                                put("y", waypoint.y)
                            },
                        )
                    }
                }
            }
    }

fun SimulationEvent.toJsonLine(): String = toJsonObject().toString()
