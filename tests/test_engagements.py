from battles_v033 import engagement_type


def test_worker_only_losses_are_harassment() -> None:
    assert engagement_type({"player": {"worker": 6}}) == "worker_harass"


def test_large_army_losses_are_battle() -> None:
    assert engagement_type({"a": {"army": 5}, "b": {"army": 4}}) == "battle"


def test_small_army_contact_is_skirmish() -> None:
    assert engagement_type({"a": {"army": 2}}) == "skirmish"


def test_structure_only_losses_are_base_assault() -> None:
    assert engagement_type({"a": {"economy_structure": 1, "static_defense": 1}}) == "base_assault"
