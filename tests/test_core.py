from analyze import clock, json_safe, normalize_entity_name, seconds_from_frame
from coach import find_turning_points, intervals


def test_game_time_uses_replay_frames() -> None:
    assert seconds_from_frame(22259) == 1391.19
    assert clock(seconds_from_frame(22259)) == "23:11"


def test_tuple_keys_are_json_safe() -> None:
    assert json_safe({("SCV", 79): {("x", "y"): 1}}) == {
        "(SCV,79)": {"(x,y)": 1}
    }


def test_entity_names_are_normalized() -> None:
    assert normalize_entity_name("(Battlecruiser,11)") == "Battlecruiser"
    assert normalize_entity_name("VikingFighter") == "VikingFighter"


def test_intervals_observe_minimum_duration() -> None:
    stats = [
        {"time": 0, "minerals_current": 1200},
        {"time": 10, "minerals_current": 1200},
        {"time": 20, "minerals_current": 1200},
        {"time": 30, "minerals_current": 100},
    ]
    assert intervals(stats, lambda stat: stat["minerals_current"] >= 1000, 20) == [(0.0, 20.0)]


def test_turning_point_detection() -> None:
    rows = [
        {"time": 900, "teams": {"1": {"army_value": 3000}, "2": {"army_value": 2500}}},
        {"time": 960, "teams": {"1": {"army_value": 1500}, "2": {"army_value": 3000}}},
    ]
    points = find_turning_points(rows, 1)
    assert points[0]["type"] == "army_lead_lost"
