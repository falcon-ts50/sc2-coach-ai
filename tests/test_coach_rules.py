from coach_rules import build


def coaching_fixture():
    return {
        "focus_player": "dragonDriver",
        "focus_team": 2,
        "players": [
            {
                "name": "dragonDriver",
                "mineral_float_intervals": [[100, 260]],
                "supply_blocks": [[300, 330]],
                "peak_army_value": 4000,
                "peak_army_time": 900,
                "final_army_value": 500,
            }
        ],
        "team_timeline": [
            {"time": 900, "clock": "15:00", "teams": {"1": {"army_value": 3000}, "2": {"army_value": 5000}}},
            {"time": 960, "clock": "16:00", "teams": {"1": {"army_value": 4500}, "2": {"army_value": 3500}}},
        ],
    }


def battle_fixture():
    return {
        "focus_player": "dragonDriver",
        "focus_team": "2",
        "battles": [
            {
                "id": 1,
                "start": 1000,
                "end": 1060,
                "start_clock": "16:40",
                "end_clock": "17:40",
                "engagement_type": "battle",
                "classification": "catastrophic_loss",
                "team_loss_delta": {"1": 500, "2": 2500},
                "units_lost": {"dragonDriver": {"Viking": 7}},
                "loss_categories": {"dragonDriver": {"army": 7}},
            },
            {
                "id": 2,
                "start": 1100,
                "end": 1160,
                "start_clock": "18:20",
                "end_clock": "19:20",
                "engagement_type": "battle",
                "classification": "catastrophic_loss",
                "team_loss_delta": {"1": 700, "2": 2300},
                "units_lost": {"dragonDriver": {"Thor": 2}},
                "loss_categories": {"dragonDriver": {"army": 2}},
            },
        ],
    }


def test_build_emits_explainable_findings():
    report = build(coaching_fixture(), battle_fixture())
    ids = {item["rule_id"] for item in report["findings"]}
    assert report["schema_version"] == "0.4.0"
    assert "macro.mineral_float" in ids
    assert "macro.supply_block" in ids
    assert "macro.army_not_recovered" in ids
    assert "combat.cascade" in ids
    assert "teamwork.lead_conversion" in ids
    assert all(item["evidence"] for item in report["findings"])
    assert all(item["recommendation"] for item in report["findings"])


def test_findings_are_sorted_by_severity():
    report = build(coaching_fixture(), battle_fixture())
    order = {"critical": 0, "high": 1, "medium": 2, "low": 3}
    severities = [order[item["severity"]] for item in report["findings"]]
    assert severities == sorted(severities)
