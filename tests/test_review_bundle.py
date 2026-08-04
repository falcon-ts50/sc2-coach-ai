from review_bundle import diagnostics, margin


def engagement(**overrides):
    base = {
        "id": 1,
        "start": 100.0,
        "end": 120.0,
        "duration": 20.0,
        "engagement_type": "battle",
        "classification": "lost",
        "team_loss_delta": {"1": 500.0, "2": 1000.0},
    }
    base.update(overrides)
    return base


def test_margin_uses_focus_team_perspective() -> None:
    assert margin(engagement(), "2") == 500.0
    assert margin(engagement(), "1") == -500.0


def test_diagnostics_accept_clean_windows() -> None:
    report = {"battles": [engagement(), engagement(id=2, start=120.0, end=140.0)]}
    result = diagnostics(report)
    assert result["status"] == "ok"
    assert result["warning_count"] == 0


def test_diagnostics_reports_overlap_and_unknown_type() -> None:
    report = {
        "battles": [
            engagement(),
            engagement(id=2, start=119.0, end=220.0, duration=101.0, engagement_type="mystery"),
        ]
    }
    codes = {warning["code"] for warning in diagnostics(report)["warnings"]}
    assert {"overlap", "duration_limit", "unknown_type"}.issubset(codes)


def test_zero_delta_army_engagement_is_suspicious() -> None:
    report = {"battles": [engagement(team_loss_delta={"1": 0.0, "2": 0.0})]}
    codes = {warning["code"] for warning in diagnostics(report)["warnings"]}
    assert "zero_army_delta" in codes
