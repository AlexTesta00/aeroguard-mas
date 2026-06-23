from pathlib import Path

import pytest

from validate_events import EventValidationError, group_events_by_type, load_jsonl_events, validate_events


def test_load_jsonl_reads_valid_events(tmp_path: Path) -> None:
    events_file = tmp_path / "events.jsonl"
    events_file.write_text(
        "\n".join(
            [
                '{"tick":0,"type":"aircraft_state","aircraft":"AZA123","x":0.0,"y":0.0,"altitude":30000,"speed":1.0,"status":"normal","priority":"NORMAL"}',
                '{"tick":1,"type":"explanation","agent":"explanation_agent","message":"Test explanation."}',
            ]
        ),
        encoding="utf-8",
    )

    events = load_jsonl_events(events_file)

    assert len(events) == 2
    assert events[0]["type"] == "aircraft_state"
    assert events[1]["type"] == "explanation"


def test_validate_events_rejects_missing_required_fields() -> None:
    events = [
        {
            "tick": 0,
            "type": "aircraft_state",
            "aircraft": "AZA123",
            "x": 0.0,
            "y": 0.0,
            "altitude": 30000,
            "speed": 1.0,
            "status": "normal",
        }
    ]

    result = validate_events(events)

    assert not result.valid
    assert any("priority" in error for error in result.errors)


def test_validate_events_groups_events_by_type() -> None:
    events = [
        {"tick": 0, "type": "aircraft_state", "aircraft": "AZA123", "x": 0.0, "y": 0.0, "altitude": 30000, "speed": 1.0, "status": "normal", "priority": "NORMAL"},
        {"tick": 1, "type": "aircraft_state", "aircraft": "AZA123", "x": 1.0, "y": 1.0, "altitude": 30000, "speed": 1.0, "status": "normal", "priority": "NORMAL"},
        {"tick": 1, "type": "explanation", "agent": "explanation_agent", "message": "OK"},
    ]

    grouped = group_events_by_type(events)

    assert len(grouped["aircraft_state"]) == 2
    assert len(grouped["explanation"]) == 1


def test_load_jsonl_handles_malformed_json(tmp_path: Path) -> None:
    events_file = tmp_path / "bad_events.jsonl"
    events_file.write_text('{"tick":0,"type":"aircraft_state"}\n{bad json}', encoding="utf-8")

    with pytest.raises(EventValidationError):
        load_jsonl_events(events_file)
