#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


def player_series(data: dict[str, Any], metric: str) -> dict[str, list[tuple[float, float]]]:
    result: dict[str, list[tuple[float, float]]] = {}
    for player in data.get("players", []):
        points: list[tuple[float, float]] = []
        for stat in player.get("stats", []):
            time = float(stat.get("time", 0)) / 60.0
            if metric == "army_value":
                value = float(stat.get("minerals_used_current_army", 0)) + float(stat.get("vespene_used_current_army", 0))
            elif metric == "workers":
                value = float(stat.get("workers_active_count", 0))
            elif metric == "bank":
                value = float(stat.get("minerals_current", 0)) + float(stat.get("vespene_current", 0))
            elif metric == "income_rate":
                value = float(stat.get("minerals_collection_rate", 0)) + float(stat.get("vespene_collection_rate", 0))
            elif metric == "army_losses":
                value = float(stat.get("minerals_lost_army", 0)) + float(stat.get("vespene_lost_army", 0))
            else:
                raise ValueError(f"Unsupported metric: {metric}")
            points.append((time, value))
        result[player["name"]] = points
    return result


def battle_markers(report: dict[str, Any] | None) -> list[dict[str, Any]]:
    return list((report or {}).get("battles", []))


def plot_metric(data: dict[str, Any], metric: str, title: str, ylabel: str, path: Path, battles: dict[str, Any] | None = None) -> None:
    series = player_series(data, metric)
    fig, ax = plt.subplots(figsize=(11, 6))
    for name, points in series.items():
        if not points:
            continue
        x, y = zip(*points)
        ax.plot(x, y, label=name, linewidth=2)
    for battle in battle_markers(battles):
        start = float(battle.get("start", 0)) / 60.0
        end = float(battle.get("end", 0)) / 60.0
        ax.axvspan(start, end, alpha=0.08)
        ax.axvline(start, linewidth=0.8, alpha=0.45)
        ax.text(start, 0.98, f"B{battle.get('id')}", transform=ax.get_xaxis_transform(), va="top", fontsize=8)
    ax.set_title(title)
    ax.set_xlabel("Game time, minutes")
    ax.set_ylabel(ylabel)
    ax.grid(True, alpha=0.25)
    ax.legend()
    fig.tight_layout()
    fig.savefig(path, dpi=160)
    plt.close(fig)


def generate_charts(data: dict[str, Any], out: Path, battles: dict[str, Any] | None = None) -> list[Path]:
    charts_dir = out / "charts"
    charts_dir.mkdir(parents=True, exist_ok=True)
    specs = [
        ("army_value", "Army value over time", "Resources", charts_dir / "army_value.png"),
        ("workers", "Active workers over time", "Workers", charts_dir / "workers.png"),
        ("bank", "Unspent resources over time", "Resources", charts_dir / "bank.png"),
        ("income_rate", "Collection rate over time", "Resources per minute", charts_dir / "income_rate.png"),
        ("army_losses", "Cumulative army losses", "Resources", charts_dir / "army_losses.png"),
    ]
    paths: list[Path] = []
    for metric, title, ylabel, path in specs:
        plot_metric(data, metric, title, ylabel, path, battles)
        paths.append(path)
    return paths


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate PNG charts from replay_analysis.json")
    parser.add_argument("json", type=Path)
    parser.add_argument("--battles", type=Path)
    parser.add_argument("--out", type=Path, default=Path("out"))
    args = parser.parse_args()
    data = json.loads(args.json.read_text(encoding="utf-8"))
    battles = json.loads(args.battles.read_text(encoding="utf-8")) if args.battles and args.battles.is_file() else None
    for path in generate_charts(data, args.out, battles):
        print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
