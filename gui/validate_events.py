from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any


JsonEvent = dict[str, Any]


class EventValidationError(RuntimeError):
    """Raised when a JSONL event file cannot be read or parsed."""


@dataclass(frozen=True)
class EventValidationResult:
    valid: bool
    errors: list[str]
    warnings: list[str]
    event_count: int
    type_counts: dict[str, int]
    min_tick: int | None
    max_tick: int | None


REQUIRED_FIELDS_BY_TYPE: dict[str, set[str]] = {
    "aircraft_state": {
        "tick",
        "type",
        "aircraft",
        "x",
        "y",
        "altitude",
        "speed",
        "status",
        "priority",
    },
    "conflict_detected": {
        "tick",
        "type",
        "aircraft",
        "severity",
        "horizontalDistance",
        "verticalDistance",
    },
    "plan_generated": {
        "tick",
        "type",
        "planner",
        "aircraft",
        "actions",
    },
    "maneuver_selected": {
        "tick",
        "type",
        "aircraft",
        "maneuver",
        "reason",
    },
    "belief_update": {
        "tick",
        "type",
        "agent",
        "belief",
    },
    "explanation": {
        "tick",
        "type",
        "agent",
        "message",
    },
    "weather_zone_activated": {
        "tick",
        "type",
        "zone",
        "x",
        "y",
        "radius",
    },
    "replanning_triggered": {
        "tick",
        "type",
        "aircraft",
        "reason",
    },
}


def load_jsonl_events(path: Path) -> list[JsonEvent]:
    if not path.exists():
        raise EventValidationError(f"Event file does not exist: {path}")

    if not path.is_file():
        raise EventValidationError(f"Event path is not a file: {path}")

    events: list[JsonEvent] = []

    with path.open("r", encoding="utf-8") as file:
        for line_number, raw_line in enumerate(file, start=1):
            line = raw_line.strip()

            if not line:
                continue

            try:
                parsed = json.loads(line)
            except json.JSONDecodeError as exc:
                raise EventValidationError(
                    f"Malformed JSON at line {line_number}: {exc.msg}"
                ) from exc

            if not isinstance(parsed, dict):
                raise EventValidationError(
                    f"Line {line_number} is JSON but not an object."
                )

            events.append(parsed)

    return events


def validate_events(events: list[JsonEvent]) -> EventValidationResult:
    errors: list[str] = []
    warnings: list[str] = []

    type_counter: Counter[str] = Counter()
    ticks: list[int] = []

    if not events:
        errors.append("The event file is empty.")

    for index, event in enumerate(events, start=1):
        event_type = event.get("type")
        tick = event.get("tick")

        if event_type is None:
            errors.append(f"Event #{index} is missing required field 'type'.")
            continue

        if not isinstance(event_type, str) or not event_type.strip():
            errors.append(f"Event #{index} has invalid 'type': {event_type!r}.")
            continue

        type_counter[event_type] += 1

        required_fields = REQUIRED_FIELDS_BY_TYPE.get(event_type)
        if required_fields is None:
            warnings.append(f"Event #{index} has unknown type '{event_type}'.")
            required_fields = {"tick", "type"}

        for field_name in sorted(required_fields):
            if field_name not in event:
                errors.append(
                    f"Event #{index} of type '{event_type}' is missing required field '{field_name}'."
                )

        if not isinstance(tick, int):
            errors.append(f"Event #{index} has non-integer tick: {tick!r}.")
        else:
            if tick < 0:
                errors.append(f"Event #{index} has negative tick: {tick}.")
            ticks.append(tick)

        if event_type == "aircraft_state":
            _validate_aircraft_state(index, event, errors)

        if event_type == "conflict_detected":
            _validate_conflict_event(index, event, errors)

        if event_type == "plan_generated":
            _validate_plan_event(index, event, errors)

    return EventValidationResult(
        valid=not errors,
        errors=errors,
        warnings=warnings,
        event_count=len(events),
        type_counts=dict(type_counter),
        min_tick=min(ticks) if ticks else None,
        max_tick=max(ticks) if ticks else None,
    )


def group_events_by_type(events: list[JsonEvent]) -> dict[str, list[JsonEvent]]:
    grouped: dict[str, list[JsonEvent]] = defaultdict(list)

    for event in events:
        event_type = str(event.get("type", "unknown"))
        grouped[event_type].append(event)

    return dict(grouped)


def _validate_aircraft_state(
        index: int,
        event: JsonEvent,
        errors: list[str],
) -> None:
    aircraft = event.get("aircraft")
    x = event.get("x")
    y = event.get("y")
    altitude = event.get("altitude")
    speed = event.get("speed")

    if not isinstance(aircraft, str) or not aircraft.strip():
        errors.append(f"Aircraft state event #{index} has invalid aircraft id.")

    if not isinstance(x, int | float):
        errors.append(f"Aircraft state event #{index} has invalid x coordinate.")

    if not isinstance(y, int | float):
        errors.append(f"Aircraft state event #{index} has invalid y coordinate.")

    if not isinstance(altitude, int):
        errors.append(f"Aircraft state event #{index} has invalid altitude.")

    if not isinstance(speed, int | float):
        errors.append(f"Aircraft state event #{index} has invalid speed.")


def _validate_conflict_event(
        index: int,
        event: JsonEvent,
        errors: list[str],
) -> None:
    aircraft = event.get("aircraft")

    if not isinstance(aircraft, list) or len(aircraft) < 2:
        errors.append(
            f"Conflict event #{index} must contain at least two aircraft ids."
        )

    if "predictedAtTick" in event:
        predicted_at_tick = event["predictedAtTick"]
        tick = event.get("tick")
        if isinstance(predicted_at_tick, int) and isinstance(tick, int):
            if predicted_at_tick < tick:
                errors.append(
                    f"Conflict event #{index} has predictedAtTick before tick."
                )


def _validate_plan_event(
        index: int,
        event: JsonEvent,
        errors: list[str],
) -> None:
    actions = event.get("actions")

    if not isinstance(actions, list) or not actions:
        errors.append(f"Plan event #{index} must contain at least one action.")


def _format_validation_result(result: EventValidationResult) -> str:
    lines = [
        "AeroGuard event validation",
        f"- valid={result.valid}",
        f"- event_count={result.event_count}",
        f"- min_tick={result.min_tick}",
        f"- max_tick={result.max_tick}",
        f"- type_counts={result.type_counts}",
    ]

    if result.warnings:
        lines.append("- warnings:")
        lines.extend(f"  - {warning}" for warning in result.warnings)

    if result.errors:
        lines.append("- errors:")
        lines.extend(f"  - {error}" for error in result.errors)

    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate AeroGuard-MAS JSONL simulation events."
    )
    parser.add_argument("events_file", type=Path, help="Path to a JSONL events file.")
    args = parser.parse_args()

    try:
        events = load_jsonl_events(args.events_file)
        result = validate_events(events)
    except EventValidationError as exc:
        print(f"AeroGuard event validation\n- valid=False\n- errors:\n  - {exc}")
        return 1

    print(_format_validation_result(result))
    return 0 if result.valid else 1


if __name__ == "__main__":
    raise SystemExit(main())
