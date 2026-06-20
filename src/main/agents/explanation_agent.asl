// This agent stores explanation-related beliefs and prepares explanations for the sector controller. It is responsible for explaining the reasoning behind the decisions made by the planning agent.

agent_role(explanation_agent).
explanation_policy(symbolic_trace).
belief_source(reasoning_layer).
language(english).

!publish_explainer_ready.
!explain_system_role.

// Initial readiness notification.
+!publish_explainer_ready
    <- .send(sector_controller, tell, explainer_ready(explanation_agent)).

// Goal describing its responsibility.
+!explain_system_role
    <- +intention(explain_agent_decisions);
       .send(sector_controller, tell, explanation_capability(symbolic_trace)).

// Store planner reasoning facts.
+planning_reason(Planner, AircraftA, AircraftB)
    <- +belief(planner_used(Planner, AircraftA, AircraftB)).

// Explain a concrete resolution.
+!explain_resolution(Aircraft, Maneuver, Reason)
    <- +explanation(Aircraft, Maneuver, Reason);
       .send(sector_controller, tell, explanation_ready(Aircraft, Maneuver, Reason)).
