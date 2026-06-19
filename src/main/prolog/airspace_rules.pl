% True when the horizontal distance between two aircraft is below the configured
% minimum horizontal separation threshold.
below_horizontal_separation(Distance, Threshold) :-
    Distance < Threshold.

% True when the vertical distance between two aircraft is below the configured
% minimum vertical separation threshold.
below_vertical_separation(DistanceFeet, ThresholdFeet) :-
    DistanceFeet < ThresholdFeet.

% True when a pair of aircraft is unsafe.
% In this simplified model, a pair is unsafe only if BOTH conditions hold:
% - horizontal separation is below threshold;
% - vertical separation is below threshold.
unsafe_pair(_AircraftA, _AircraftB, HorizontalDistance, VerticalDistanceFeet, HorizontalThreshold, VerticalThresholdFeet) :-
    below_horizontal_separation(HorizontalDistance, HorizontalThreshold),
    below_vertical_separation(VerticalDistanceFeet, VerticalThresholdFeet).

% Assigns a numeric priority score to an aircraft.
priority_score(general, _DeclaredPriority, 100).
priority_score(low_fuel, _DeclaredPriority, 90).
priority_score(none, emergency, 100).
priority_score(none, high, 50).
priority_score(none, normal, 10).

% True when aircraft A has higher symbolic priority than aircraft B.
higher_priority(AStatus, APriority, BStatus, BPriority) :-
    priority_score(AStatus, APriority, AScore),
    priority_score(BStatus, BPriority, BScore),
    AScore > BScore.

% True when an altitude is inside the sector altitude limits.
valid_altitude(AltitudeFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    AltitudeFeet >= MinAltitudeFeet,
    AltitudeFeet =< MaxAltitudeFeet.

% Allowed altitude deltas for a single maneuver.
% This is true for general aviation aircraft
allowed_altitude_delta(1000).
allowed_altitude_delta(2000).

% True when the difference between current altitude and target altitude is an allowed climb amount.
valid_altitude_change(CurrentFeet, TargetFeet) :-
    TargetFeet > CurrentFeet,
    Delta is TargetFeet - CurrentFeet,
    allowed_altitude_delta(Delta).

valid_altitude_change(CurrentFeet, TargetFeet) :-
    CurrentFeet > TargetFeet,
    Delta is CurrentFeet - TargetFeet,
    allowed_altitude_delta(Delta).

% True when a climb maneuver is allowed.
maneuver_allowed(climb, CurrentFeet, TargetFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    TargetFeet > CurrentFeet,
    valid_altitude(TargetFeet, MinAltitudeFeet, MaxAltitudeFeet),
    valid_altitude_change(CurrentFeet, TargetFeet).

maneuver_allowed(descend, CurrentFeet, TargetFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    TargetFeet < CurrentFeet,
    valid_altitude(TargetFeet, MinAltitudeFeet, MaxAltitudeFeet),
    valid_altitude_change(CurrentFeet, TargetFeet).

maneuver_allowed(slow_down, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(resume_speed, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(enter_holding, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(continue_route, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(turn_left, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(turn_right, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(avoid_weather_zone, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

maneuver_allowed(reroute_to_waypoint, CurrentFeet, CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    valid_altitude(CurrentFeet, MinAltitudeFeet, MaxAltitudeFeet).

% Explanation facts are deliberately short natural-language messages.
explanation(unsafe_pair, 'Both horizontal and vertical separation are below configured thresholds.').
explanation(priority_low_fuel, 'Low fuel aircraft receive higher symbolic priority than normal traffic.').
explanation(priority_emergency, 'Aircraft in emergency receive maximum symbolic priority.').
explanation(maneuver_climb_allowed, 'A climb is allowed when the target altitude is inside the sector and the altitude delta is controlled.').
explanation(maneuver_rejected, 'A maneuver is rejected when it violates altitude bounds or allowed altitude-change increments.').
