// This agent receives aircraft monitoring beliefs and informs the controller, when a potential loss of separation is detected.

agent_role(conflict_detector).
minimum_horizontal_separation(5).
minimum_vertical_separation(1000).
prediction_horizon(6).
belief_source(simulation_core).

!scan_sector.
!publish_detector_ready.

// Declare readiness to the controller.
+!publish_detector_ready
    <- .send(sector_controller, tell, detector_ready(conflict_detector)).

// Scan goal. In the real Kotlin core, actual geometry is computed by the simulation engine.
+!scan_sector
    <- +intention(scan_for_predicted_conflicts);
       .send(sector_controller, tell, scan_started(conflict_detector)).

// Aircraft monitoring belief.
+monitor_aircraft(Aircraft)
    <- +monitored_aircraft(Aircraft);
       .send(sector_controller, tell, aircraft_under_monitoring(Aircraft)).

// When the Kotlin core injects or mirrors a predicted loss of separation, this agent informs both controller and planner.
+predicted_loss_of_separation(AircraftA, AircraftB, Tick)
    <- +conflict_candidate(AircraftA, AircraftB, Tick);
       .send(sector_controller, tell, conflict_detected(AircraftA, AircraftB, Tick));
       .send(resolution_planner, achieve, resolve_conflict(AircraftA, AircraftB, Tick)).
