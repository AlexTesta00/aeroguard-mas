// This agent represents the BDI side of the STRIPS planner.

agent_role(resolution_planner).
planner(strips).
belief_source(kotlin_strips_planner).
default_maneuver(climb).

!standby.
!publish_planner_ready.

// Initial readiness notification.
+!publish_planner_ready
    <- .send(sector_controller, tell, planner_ready(resolution_planner)).

// Priority beliefs can be received from the sector controller.
+priority(Aircraft, emergency)
    <- +protected_aircraft(Aircraft).

// If AircraftA is protected, prefer maneuvering AircraftB.
+!resolve_conflict(AircraftA, AircraftB, Tick)
    : protected_aircraft(AircraftA)
    <- +goal(resolve_conflict(AircraftA, AircraftB, Tick));
       +intention(maneuver(AircraftB, climb));
       .send(sector_controller, tell, resolution_plan(AircraftB, climb, aircraft_a_has_priority));
       .send(explanation_agent, tell, planning_reason(strips, AircraftA, AircraftB)).

// Default resolution delegation when no explicit protected aircraft belief exists.
+!resolve_conflict(AircraftA, AircraftB, Tick)
    <- +goal(resolve_conflict(AircraftA, AircraftB, Tick));
       +intention(maneuver(AircraftB, climb));
       .send(sector_controller, tell, resolution_plan(AircraftB, climb, default_strips_resolution));
       .send(explanation_agent, tell, planning_reason(strips, AircraftA, AircraftB)).
