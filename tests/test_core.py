from analyze import clock, json_safe, normalize_entity_name, seconds_from_frame
from battles import classify_trade, cluster_deaths
from coach import find_turning_points, intervals


def test_game_time_uses_replay_frames() -> None:
    assert seconds_from_frame(22259) == 1391.19
    assert clock(seconds_from_frame(22259)) == "23:11"


def test_tuple_keys_are_json_safe() -> None:
    assert json_safe({("SCV", 79): {("x", "y"): 1}}) == {"(SCV,79)": {"(x,y)": 1}}


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


def test_deaths_are_clustered_into_battle_windows() -> None:
    events = [{"time": 100}, {"time": 108}, {"time": 117}, {"time": 160}]
    windows = cluster_deaths(events, max_gap=18, padding=8, minimum_deaths=3)
    assert len(windows) == 1
    assert windows[0][0] == 92
    assert windows[0][1] == 125


def test_trade_classification_uses_focus_team_perspective() -> None:
    assert classify_trade({"1": 2500, "2": 600}, "1") == "catastrophic_loss"
    assert classify_trade({"1": 500, "2": 2200}, "1") == "decisive_win"
    assert classify_trade({"1": 1000, "2": 900}, "1") == "even"
