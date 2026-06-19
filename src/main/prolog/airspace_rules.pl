below_horizontal_separation(Distance, Threshold) :-
    Distance < Threshold.

below_vertical_separation(DistanceFeet, ThresholdFeet) :-
    DistanceFeet < ThresholdFeet.

unsafe_pair(_AircraftA, _AircraftB, HorizontalDistance, VerticalDistanceFeet, HorizontalThreshold, VerticalThresholdFeet) :-
    below_horizontal_separation(HorizontalDistance, HorizontalThreshold),
    below_vertical_separation(VerticalDistanceFeet, VerticalThresholdFeet).

priority_score(general, _DeclaredPriority, 100).
priority_score(low_fuel, _DeclaredPriority, 90).
priority_score(none, emergency, 100).
priority_score(none, high, 50).
priority_score(none, normal, 10).

higher_priority(AStatus, APriority, BStatus, BPriority) :-
    priority_score(AStatus, APriority, AScore),
    priority_score(BStatus, BPriority, BScore),
    AScore > BScore.

valid_altitude(AltitudeFeet, MinAltitudeFeet, MaxAltitudeFeet) :-
    AltitudeFeet >= MinAltitudeFeet,
    AltitudeFeet =< MaxAltitudeFeet.

allowed_altitude_delta(1000).
allowed_altitude_delta(2000).

valid_altitude_change(CurrentFeet, TargetFeet) :-
    TargetFeet > CurrentFeet,
    Delta is TargetFeet - CurrentFeet,
    allowed_altitude_delta(Delta).

valid_altitude_change(CurrentFeet, TargetFeet) :-
    CurrentFeet > TargetFeet,
    Delta is CurrentFeet - TargetFeet,
    allowed_altitude_delta(Delta).

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

explanation(unsafe_pair, 'Both horizontal and vertical separation are below configured thresholds.').
explanation(priority_low_fuel, 'Low fuel aircraft receive higher symbolic priority than normal traffic.').
explanation(priority_emergency, 'Aircraft in emergency receive maximum symbolic priority.').
explanation(maneuver_climb_allowed, 'A climb is allowed when the target altitude is inside the sector and the altitude delta is controlled.').
explanation(maneuver_rejected, 'A maneuver is rejected when it violates altitude bounds or allowed altitude-change increments.').
