from charts import player_series


def sample_data():
    return {
        "players": [
            {
                "name": "dragonDriver",
                "stats": [
                    {
                        "time": 120,
                        "minerals_used_current_army": 500,
                        "vespene_used_current_army": 250,
                        "workers_active_count": 30,
                        "minerals_current": 400,
                        "vespene_current": 100,
                        "minerals_collection_rate": 900,
                        "vespene_collection_rate": 300,
                        "minerals_lost_army": 100,
                        "vespene_lost_army": 50,
                    }
                ],
            }
        ]
    }


def test_army_value_series():
    assert player_series(sample_data(), "army_value") == {"dragonDriver": [(2.0, 750.0)]}


def test_bank_series():
    assert player_series(sample_data(), "bank") == {"dragonDriver": [(2.0, 500.0)]}


def test_workers_series():
    assert player_series(sample_data(), "workers") == {"dragonDriver": [(2.0, 30.0)]}
