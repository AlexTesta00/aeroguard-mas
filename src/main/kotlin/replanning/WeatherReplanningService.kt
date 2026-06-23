package replanning

import domain.DynamicScenarioEventType
import domain.Maneuver
import domain.ManeuverType
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

            scenario.aircraft
                .filter { aircraft ->
                    routeIntersectsWeatherZone(
                        routeWaypoints = aircraft.route.waypoints,
                        zone = zone,
                        tick = event.tick,
                    )
                }.forEach { aircraft ->
                    buildDecision(
                        tick = event.tick,
                        aircraftId = aircraft.id,
                        zone = zone,
                        state = stateAtActivation,
                    )?.let { decision ->
                        decisions += decision
                    }
                }
        }

        return decisions
    }

    private fun buildDecision(
        tick: Int,
        aircraftId: String,
        zone: WeatherZone,
        state: SimulationState,
    ): WeatherReplanningDecision? {
        val safeWaypoint =
            Waypoint(
                name = "SAFE_${zone.id}",
                position =
                    domain.Position(
                        x = zone.center.x + zone.radius + 3.0,
                        y = zone.center.y + zone.radius + 3.0,
                    ),
            )

        val maneuvers =
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
            ).filter { maneuver ->
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
                explanation = "Weather replanning selected ${maneuvers.joinToString {
                    it.formatAsPlannerAction()
                }} because zone ${zone.id} became active near the route.",
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

    private fun routeIntersectsWeatherZone(
        routeWaypoints: List<Waypoint>,
        zone: WeatherZone,
        tick: Int,
    ): Boolean {
        if (!zone.isActiveAt(tick)) {
            return false
        }

        return routeWaypoints.any { waypoint ->
            waypoint.position.distanceTo(zone.center) <= zone.radius
        }
    }
}
