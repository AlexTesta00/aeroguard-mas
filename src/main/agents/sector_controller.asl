// This agent coordinates detection, planning and explanation.

agent_role(sector_controller).
sector(sector_alpha).
belief_source(simulation_core).
controller_mode(supervisory).

!coordinate_sector.
!request_conflict_scan.

// Initial coordination goal.
+!coordinate_sector
    <- .send(conflict_detector, achieve, scan_sector);
       .send(explanation_agent, tell, controller_started(sector_alpha)).

// Explicit scan request goal.
+!request_conflict_scan
    <- .send(conflict_detector, achieve, scan_sector).

// When the conflict detector reports a conflict, store the belief and delegate.
+conflict_detected(AircraftA, AircraftB, Tick)
    <- +active_conflict(AircraftA, AircraftB, Tick);
       .send(resolution_planner, achieve, resolve_conflict(AircraftA, AircraftB, Tick)).

// When a resolution plan is received, send the maneuver to the affected aircraft
// and ask the explanation agent to explain the choice.
+resolution_plan(Aircraft, Maneuver, Reason)
    <- +selected_plan(Aircraft, Maneuver, Reason);
       .send(Aircraft, tell, assigned_maneuver(Maneuver));
       .send(explanation_agent, achieve, explain_resolution(Aircraft, Maneuver, Reason)).

// Emergency reports update priority beliefs and inform the resolution planner.
+emergency_declared(Aircraft)
    <- +priority(Aircraft, emergency);
       .send(resolution_planner, tell, priority(Aircraft, emergency)).
