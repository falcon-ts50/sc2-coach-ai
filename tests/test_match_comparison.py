from __future__ import annotations

from match_comparison import compare_match


def stat(time: int, workers: int, income: int, army: int, losses: int, used: int, made: int) -> dict:
    return {
        "time": time,
        "workers_active_count": workers,
        "minerals_collection_rate": income,
        "vespene_collection_rate": 0,
        "minerals_used_current_army": army,
        "vespene_used_current_army": 0,
        "minerals_lost_army": losses,
        "vespene_lost_army": 0,
        "food_used": used,
        "food_made": made,
    }


def fixture() -> dict:
    return {
        "replay": {"game_seconds": 180, "map": "Test"},
        "players": [
            {
                "name": "TerranLead",
                "race": "Terran",
                "team": 1,
                "result": "Win",
                "stats": [
                    stat(60, 24, 1800, 1200, 100, 45, 55),
                    stat(120, 38, 2600, 2600, 350, 80, 95),
                    stat(180, 52, 3400, 4300, 700, 125, 145),
                ],
            },
            {
                "name": "ZergSecond",
                "race": "Zerg",
                "team": 2,
                "result": "Loss",
                "stats": [
                    stat(60, 22, 1600, 900, 150, 42, 55),
                    stat(120, 34, 2300, 1900, 600, 73, 90),
                    stat(180, 46, 2900, 3000, 1200, 108, 135),
                ],
            },
        ],
    }


def test_identifies_measured_leader_across_races() -> None:
    model = compare_match(fixture())
    assert model["leader"]["player"] == "TerranLead"
    assert model["ranking"][0]["race"] == "Terran"
    assert model["ranking"][1]["race"] == "Zerg"
    assert model["leader"]["gap_to_second"] > 0


def test_scores_are_bounded_and_checkpoints_are_synchronized() -> None:
    model = compare_match(fixture())
    for player in model["ranking"]:
        assert 0 <= player["score"] <= 100
        assert player["checkpoint_count"] == 3
        assert all(0 <= value <= 100 for value in player["dimensions"].values())
    assert [item["clock"] for item in model["timelines"]["TerranLead"]] == ["01:00", "02:00", "03:00"]


def test_close_match_has_low_or_medium_confidence() -> None:
    data = fixture()
    data["players"][1]["stats"] = list(data["players"][0]["stats"])
    model = compare_match(data)
    assert model["leader"]["gap_to_second"] == 0
    assert model["leader"]["confidence"] == "low"
