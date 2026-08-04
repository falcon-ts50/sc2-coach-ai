from build_order import extract_player


def fixture_data():
    return {
        "replay": {"map": "Test Map", "release": "5.0", "base_build": 999, "type": "2v2"},
        "players": [{
            "name": "dragonDriver", "race": "Terran",
            "stats": [
                {"time": 60, "workers_active_count": 19, "food_used": 22},
                {"time": 120, "workers_active_count": 21, "food_used": 51},
                {"time": 300, "workers_active_count": 42, "food_used": 101},
            ],
        }],
        "timeline": [
            {"time": 50, "player": "dragonDriver", "event": "UnitInitEvent", "unit": "Barracks"},
            {"time": 96, "player": "dragonDriver", "event": "UnitDoneEvent", "unit": "Barracks"},
            {"time": 180, "player": "dragonDriver", "event": "UnitInitEvent", "unit": "Factory"},
            {"time": 240, "player": "dragonDriver", "event": "UnitDoneEvent", "unit": "Factory"},
            {"time": 420, "player": "dragonDriver", "event": "UnitBornEvent", "unit": "Battlecruiser"},
            {"time": 425, "player": "dragonDriver", "event": "UnitBornEvent", "unit": "Battlecruiser"},
            {"time": 310, "player": "dragonDriver", "event": "UpgradeCompleteEvent", "upgrade": "TerranInfantryWeaponsLevel1"},
            {"time": 20, "player": "dragonDriver", "event": "UnitBornEvent", "unit": "SCV"},
        ],
    }


def test_extracts_canonical_structure_pairs_and_first_key_unit():
    model = extract_player(fixture_data(), "dragonDriver")
    barracks = [event for event in model["events"] if event["name"] == "Barracks"]
    assert [event["phase"] for event in barracks] == ["start", "complete"]
    assert {event["key"] for event in barracks} == {"structure:Barracks:1"}
    battlecruisers = [event for event in model["events"] if event["name"] == "Battlecruiser"]
    assert len(battlecruisers) == 1
    assert battlecruisers[0]["phase"] == "first"


def test_extracts_upgrade_and_economic_milestones():
    model = extract_player(fixture_data(), "dragonDriver")
    assert any(event["category"] == "upgrade" and event["name"] == "TerranInfantryWeaponsLevel1" for event in model["events"])
    milestones = {(item["metric"], item["threshold"]) for item in model["milestones"]}
    assert ("workers", 20) in milestones
    assert ("workers", 40) in milestones
    assert ("supply_used", 50) in milestones
    assert ("supply_used", 100) in milestones


def test_player_lookup_is_case_insensitive():
    model = extract_player(fixture_data(), "DRAGONDRIVER")
    assert model["focus_player"] == "dragonDriver"
