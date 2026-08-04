from report_i18n import render_battles
from coach_rules_i18n import localize


def test_battle_report_russian_labels():
    report = {"battles": [{"id": 1, "start_clock": "03:00", "end_clock": "03:20", "engagement_type": "worker_harass", "classification": "economic_damage", "death_count": 4, "duration": 20, "player_loss_delta": {}, "loss_categories": {}}]}
    text = render_battles(report, "ru")
    assert "харас рабочих" in text
    assert "экономический урон" in text


def test_strategic_default_english_translation():
    report = {"findings": [{"rule_id": "macro.supply_block", "title": "ru", "explanation": "ru", "recommendation": "ru", "evidence": [{"metric": "supply_block_seconds", "value": 42}]}]}
    translated = localize(report, "en")
    assert translated["language"] == "en"
    assert translated["findings"][0]["title"] == "Significant supply blocks"
    assert "42 seconds" in translated["findings"][0]["explanation"]


def test_russian_preserves_rule_text():
    report = {"findings": [{"rule_id": "macro.supply_block", "title": "Русский", "explanation": "Текст", "recommendation": "Совет", "evidence": []}]}
    localized = localize(report, "ru")
    assert localized["findings"][0]["title"] == "Русский"
