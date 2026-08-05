from transcript import build_transcript_markdown


def test_transcript_contains_events_coordinates_and_snapshot_deltas():
    data = {
        "replay": {
            "map": "Test Map",
            "type": "2v2",
            "release": "5.0.14",
            "base_build": 99999,
            "game_seconds": 120,
            "winner": ["Alpha"],
        },
        "players": [
            {
                "pid": 1,
                "name": "Alpha",
                "race": "Terran",
                "team": 1,
                "result": "Win",
                "mmr": 3500,
                "apm": 120,
                "stats": [
                    {"time": 0, "clock": "00:00", "workers_active_count": 12, "minerals_current": 50},
                    {"time": 60, "clock": "01:00", "workers_active_count": 20, "minerals_current": 300},
                ],
            }
        ],
        "timeline": [
            {
                "time": 30,
                "clock": "00:30",
                "player": "Alpha",
                "event": "TargetPointCommandEvent",
                "ability": "Attack",
                "target_position": {"x": 42.5, "y": 18.25},
            }
        ],
    }

    transcript = build_transcript_markdown(data)

    assert "AI-readable replay transcript" in transcript
    assert "command: Attack" in transcript
    assert "target (42.50, 18.25)" in transcript
    assert "workers=+8" in transcript
    assert "minerals=+250" in transcript
