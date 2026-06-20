// This agent represents the local intention of one aircraft.

agent_role(aircraft_agent).
aircraft_status(normal).
belief_source(simulation_core).
known_controller(sector_controller).

!publish_state.
!wait_for_maneuver.

// Initial goal: publish aircraft availability to the sector controller.
+!publish_state
    <- .send(sector_controller, tell, aircraft_ready(self));
       .send(conflict_detector, tell, monitor_aircraft(self)).

// Goal: wait for an assigned maneuver from the sector controller.
+!wait_for_maneuver
    <- .send(sector_controller, tell, waiting_for_clearance(self)).

// When a maneuver is assigned, store it as an intended maneuver and acknowledge.
+assigned_maneuver(Maneuver)
    <- +intended_maneuver(Maneuver);
       .send(sector_controller, tell, maneuver_ack(self, Maneuver)).

// Emergency reporting goal.
+!report_emergency
    : aircraft_status(emergency)
    <- .send(sector_controller, tell, emergency_declared(self)).
