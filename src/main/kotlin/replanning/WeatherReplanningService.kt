package replanning

import domain.DynamicScenarioEventType
import domain.Maneuver
import domain.ManeuverType
import domain.Position
import domain.ResolutionPlan
import domain.Scenario
import domain.SimulationState
import domain.Waypoint
import domain.WeatherZone
import events.BeliefUpdatedEvent
import events.ManeuverSelectedEvent
import events.PlanGeneratedEvent
import events.ReplanningTriggeredEvent
import events.SimulationEvent
import events.WeatherZoneActivatedEvent
import planning.formatAsPlannerAction
import reasoning.SafetyReasoner
import simulation.SimulationRunResult
import kotlin.math.hypot

data class WeatherReplanningDecision(
    val tick: Int,
    val aircraftId: String,
    val zone: WeatherZone,
    val resolutionPlan: ResolutionPlan,
    val events: List<SimulationEvent>,
)

class WeatherReplanningService(
    private val safetyReasoner: SafetyReasoner,
) {
    fun planWeatherReplanning(
        scenario: Scenario,
        runResult: SimulationRunResult,
    ): List<WeatherReplanningDecision> {
        val weatherActivations =
            scenario.dynamicEvents
                .filter { event -> event.type == DynamicScenarioEventType.ACTIVATE_WEATHER_ZONE }

        if (weatherActivations.isEmpty()) {
            return emptyList()
        }

        val decisions = mutableListOf<WeatherReplanningDecision>()

        weatherActivations.forEach { event ->
            val zone =
                scenario.weatherZones.firstOrNull { weatherZone ->
                    weatherZone.id == event.targetId
                } ?: return@forEach

            val stateAtActivation =
                runResult.states.firstOrNull { state ->
                    state.tick == event.tick
                } ?: runResult.finalState

            decisions +=
                planWeatherReplanningForZone(
                    scenario = scenario,
                    state = stateAtActivation,
                    zone = zone,
                    tick = event.tick,
                )
        }

        return decisions
    }

    fun planWeatherReplanningForTick(
        scenario: Scenario,
        state: SimulationState,
    ): List<WeatherReplanningDecision> {
        val weatherActivations =
            scenario.dynamicEvents
                .filter { event ->
                    event.type == DynamicScenarioEventType.ACTIVATE_WEATHER_ZONE &&
                        event.tick == state.tick
                }

        if (weatherActivations.isEmpty()) {
            return emptyList()
        }

        val decisions = mutableListOf<WeatherReplanningDecision>()

        weatherActivations.forEach { event ->
            val zone =
                scenario.weatherZones.firstOrNull { weatherZone ->
                    weatherZone.id == event.targetId
                } ?: return@forEach

            decisions +=
                planWeatherReplanningForZone(
                    scenario = scenario,
                    state = state,
                    zone = zone,
                    tick = event.tick,
                )
        }

        return decisions
    }

    private fun planWeatherReplanningForZone(
        scenario: Scenario,
        state: SimulationState,
        zone: WeatherZone,
        tick: Int,
    ): List<WeatherReplanningDecision> =
        state.aircraft.values
            .filter { aircraft ->
                val remainingRoute =
                    aircraft.route.waypoints
                        .drop(aircraft.activeWaypointIndex)

                routeSegmentIntersectsWeatherZone(
                    aircraftPosition = aircraft.position,
                    routeWaypoints = remainingRoute,
                    zone = zone,
                    tick = tick,
                )
            }.mapNotNull { aircraft ->
                buildDecision(
                    tick = tick,
                    aircraftId = aircraft.id,
                    aircraftPosition = aircraft.position,
                    zone = zone,
                    state = state,
                )
            }.filter { decision ->
                scenario.aircraft.any { aircraft -> aircraft.id == decision.aircraftId }
            }

    private fun buildDecision(
        tick: Int,
        aircraftId: String,
        aircraftPosition: Position,
        zone: WeatherZone,
        state: SimulationState,
    ): WeatherReplanningDecision? {
        val safeWaypoint =
            buildSafeBypassWaypoint(
                aircraftId = aircraftId,
                aircraftPosition = aircraftPosition,
                zone = zone,
            )

        val candidateManeuvers =
            listOf(
                Maneuver(
                    aircraftId = aircraftId,
                    type = ManeuverType.AVOID_WEATHER_ZONE,
                    reason = "Weather zone ${zone.id} blocks the nominal route",
                ),
                Maneuver(
                    aircraftId = aircraftId,
                    type = ManeuverType.REROUTE_TO_WAYPOINT,
                    targetWaypoint = safeWaypoint,
                    reason = "Reroute around weather zone ${zone.id}",
                ),
            )

        val maneuvers =
            candidateManeuvers.filter { maneuver ->
                safetyReasoner.isManeuverAllowed(
                    aircraftId = aircraftId,
                    maneuver = maneuver,
                    state = state,
                )
            }

        if (maneuvers.isEmpty()) {
            return null
        }

        val plan =
            ResolutionPlan(
                id = "weather-${zone.id}-$aircraftId",
                conflictId = "weather-${zone.id}",
                maneuvers = maneuvers,
                explanation = "Weather replanning selected ${
                    maneuvers.joinToString { maneuver -> maneuver.formatAsPlannerAction() }
                } because zone ${zone.id} became active near the route.",
            )

        val actions =
            maneuvers.map { maneuver ->
                maneuver.formatAsPlannerAction()
            }

        val events =
            buildList {
                add(
                    WeatherZoneActivatedEvent(
                        tick = tick,
                        zone = zone.id,
                        x = zone.center.x,
                        y = zone.center.y,
                        radius = zone.radius,
                    ),
                )

                add(
                    ReplanningTriggeredEvent(
                        tick = tick,
                        aircraft = aircraftId,
                        reason = "Weather zone ${zone.id} blocks the nominal route",
                    ),
                )

                add(
                    PlanGeneratedEvent(
                        tick = tick,
                        planner = "strips",
                        aircraft = aircraftId,
                        actions = actions,
                    ),
                )

                maneuvers.forEach { maneuver ->
                    add(
                        ManeuverSelectedEvent(
                            tick = tick,
                            aircraft = aircraftId,
                            maneuver = maneuver.type.name.lowercase(),
                            reason = maneuver.reason ?: plan.explanation,
                        ),
                    )
                }

                add(
                    BeliefUpdatedEvent(
                        tick = tick,
                        agent = "sector_controller",
                        belief = "weather_zone_active(${zone.id})",
                    ),
                )

                add(
                    BeliefUpdatedEvent(
                        tick = tick,
                        agent = "resolution_planner",
                        belief = "weather_replanning($aircraftId,${zone.id})",
                    ),
                )
            }

        return WeatherReplanningDecision(
            tick = tick,
            aircraftId = aircraftId,
            zone = zone,
            resolutionPlan = plan,
            events = events,
        )
    }

    private fun buildSafeBypassWaypoint(
        aircraftId: String,
        aircraftPosition: Position,
        zone: WeatherZone,
    ): Waypoint {
        val vectorToZoneX = zone.center.x - aircraftPosition.x
        val vectorToZoneY = zone.center.y - aircraftPosition.y
        val length = hypot(vectorToZoneX, vectorToZoneY).takeIf { it > 0.0 } ?: 1.0

        val perpendicularX = vectorToZoneY / length
        val perpendicularY = -vectorToZoneX / length

        val bypassDistance = zone.radius * 2.0 + 2.0

        val safePosition =
            Position(
                x = aircraftPosition.x + perpendicularX * bypassDistance,
                y = aircraftPosition.y + perpendicularY * bypassDistance,
            )

        return Waypoint(
            name = "SAFE_${zone.id}_$aircraftId",
            position = safePosition,
        )
    }

    private fun routeSegmentIntersectsWeatherZone(
        aircraftPosition: Position,
        routeWaypoints: List<Waypoint>,
        zone: WeatherZone,
        tick: Int,
    ): Boolean {
        if (!zone.isActiveAt(tick)) {
            return false
        }

        if (routeWaypoints.isEmpty()) {
            return false
        }

        val points =
            listOf(aircraftPosition) +
                routeWaypoints.map { waypoint ->
                    waypoint.position
                }

        return points.zipWithNext().any { (from, to) ->
            distanceFromPointToSegment(
                point = zone.center,
                segmentStart = from,
                segmentEnd = to,
            ) <= zone.radius
        }
    }

    private fun distanceFromPointToSegment(
        point: Position,
        segmentStart: Position,
        segmentEnd: Position,
    ): Double {
        val segmentDx = segmentEnd.x - segmentStart.x
        val segmentDy = segmentEnd.y - segmentStart.y
        val segmentLengthSquared = segmentDx * segmentDx + segmentDy * segmentDy

        if (segmentLengthSquared == 0.0) {
            return point.distanceTo(segmentStart)
        }

        val rawProjection =
            ((point.x - segmentStart.x) * segmentDx + (point.y - segmentStart.y) * segmentDy) /
                segmentLengthSquared

        val projection = rawProjection.coerceIn(0.0, 1.0)

        val closestPoint =
            Position(
                x = segmentStart.x + projection * segmentDx,
                y = segmentStart.y + projection * segmentDy,
            )

        return point.distanceTo(closestPoint)
    }
}
