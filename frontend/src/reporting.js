export function buildMarkdown(analysis) {
  if (!analysis) return '';
  const lines = [
    '# SC2 Coach Report', '',
    `**Разбор для:** ${analysis.focusPlayer || '—'}`,
    `**Карта:** ${analysis.map || '—'}`,
    `**Длительность:** ${clock(analysis.gameSeconds)}`, '',
    '## Итог', '', analysis.coachFeed?.headline || 'Недостаточно данных.', '',
    ...narrativeAnalysisMarkdown(analysis),
    '## Как развивался матч', '', narrativeText(analysis), '',
    '## История боёв', ''
  ];
  (analysis.combats || []).forEach((combat, index) => {
    lines.push(`### ${combat.ordinalLabel || `Бой ${index + 1}`} · ${clock(durationSeconds(combat.startedAt))}–${clock(durationSeconds(combat.endedAt))}`);
    lines.push(`${combat.initiator || 'Игрок'} атакует ${combat.opponent || 'соперника'}.`);
    if (combat.location) lines.push(`Координаты команды: ${combat.location}.`);
    (combat.participants || []).forEach(player => {
      lines.push('', `**${player.player}**`,
        `- Армия в начале: ${composition(player.armyBefore)}`,
        `- Новые юниты в интервале: ${composition(player.additions)}`,
        `- Боевые потери: ${composition(player.unitsLost)}`,
        `- Рабочие: ${composition(player.workersLost)}`,
        `- Здания: ${composition(player.structuresLost)}`,
        `- Оборона: ${composition(player.staticDefenseLost)}`,
        `- Армия в конце: ${composition(player.armyAfter)}`,
        `- Грейды: ${listText(player.upgrades)}`,
        `- Ключевые технологии: ${listText(player.technologies)}`,
        `- Стоимость армии: ${Math.round(player.armyValueBefore || 0)} → ${Math.round(player.armyValueAfter || 0)}`,
        `- Сверка: ${reconciliationText(player)}`);
    });
    lines.push('');
  });
  lines.push('## Что сделать в следующей игре', '');
  (analysis.coachFeed?.nextGameRecommendations || []).forEach((item, index) => lines.push(`${index + 1}. ${item}`));
  return lines.join('\n');
}

export function narrativeAnalysisMarkdown(analysis) {
  const narrative = analysis?.narrativeAnalysis;
  if (!narrative) return [];
  const lines = [
    '## Narrative Analysis', '',
    `- Официальный результат реплея: ${narrative.officialReplayResult || '—'}`,
    `- Статус анализа: ${narrative.status || '—'}`,
    `- Strategic result: ${narrative.strategicResultStatus || 'NOT_EVALUATED'}`,
    `- Команда фокуса: ${(narrative.focusTeamPlayers || []).join(', ') || '—'}`, '',
    narrative.summary?.verdict || 'Недостаточно данных для связного сценария.', '',
    '### Фазы', ''
  ];
  (narrative.timeline?.phases || []).forEach(phase => {
    lines.push(`- ${clock(durationSeconds(phase.startedAt))}–${clock(durationSeconds(phase.endedAt))}: ${phase.title}. ${phase.summary}`);
  });
  lines.push('', '### Сценарная цепочка', '');
  (narrative.timeline?.causalLinks || []).forEach(link => {
    lines.push(`- ${link.kind}: ${link.statement}`);
  });
  lines.push('', '### Данные графика', '');
  (narrative.chart?.series || []).forEach(series => {
    const points = series.points || [];
    const first = points[0];
    const last = points[points.length - 1];
    lines.push(`- ${series.label}: ${series.completeness || '—'}, ${points.length} точек${first && last ? `, ${clock(durationSeconds(first.at))}=${Math.round(first.value)}, ${clock(durationSeconds(last.at))}=${Math.round(last.value)}` : ''}`);
  });
  if (narrative.evidence) {
    lines.push('', '### Сравнение участников', '');
    (narrative.evidence.metricComparisons || []).forEach(metric => {
      lines.push(`- ${metric.label}: ${metric.completeness || '—'}, ${(metric.series || []).length} серий`);
      (metric.series || []).forEach(series => {
        const participant = participantById(narrative.evidence, series.participantId);
        const points = series.points || [];
        const first = points[0];
        const last = points[points.length - 1];
        lines.push(`  - ${participantName(participant)} (${relationshipText(participant)}): ${series.completeness || '—'}, ${series.lineStyle || 'solid'}, ${points.length} точек${first && last ? `, ${clock(durationSeconds(first.at))}=${Math.round(first.value)}, ${clock(durationSeconds(last.at))}=${Math.round(last.value)}` : ''}`);
      });
    });
    lines.push('', '### Боевые evidence-таблицы', '');
    (narrative.evidence.combats || []).forEach(combat => {
      lines.push(`#### ${combat.label} · ${clock(durationSeconds(combat.startedAt))}–${clock(durationSeconds(combat.endedAt))}`);
      (combat.sides || []).forEach(side => {
        lines.push(`- ${side.label}: ${side.completeness || '—'}`);
        pushUnitRows(lines, '  - Итого', side.totalRows || []);
        (side.participants || []).forEach(player => {
          lines.push(`  - ${player.player}: ${player.reconciliationStatus || '—'}, ${player.completeness || '—'}`);
          pushUnitRows(lines, '    - Боевые юниты', player.rows || []);
          lines.push(`    - Рабочие: ${composition(player.workerLosses)}`);
          lines.push(`    - Здания: ${composition(player.structureLosses)}`);
          lines.push(`    - Оборона: ${composition(player.staticDefenseLosses)}`);
        });
      });
      (combat.notes || []).forEach(note => lines.push(`- Примечание: ${note}`));
    });
  }
  if ((narrative.limitations || []).length) {
    lines.push('', '### Ограничения', '');
    narrative.limitations.forEach(item => lines.push(`- ${item}`));
  }
  lines.push('');
  return lines;
}

function pushUnitRows(lines, label, rows) {
  if (!rows.length) {
    lines.push(`${label}: нет`);
    return;
  }
  rows.forEach(row => lines.push(`${label}: ${row.unit}: старт ${row.startCount}, новые ${row.additions}, потери ${row.losses}, финиш ${row.endCount}, kills ${countEvidence(row.creditedKills)}, ${row.reconciliationStatus || row.completeness || '—'}`));
}

function participantById(evidence, participantId) {
  return (evidence.participants || []).find(participant => participant.id === participantId);
}

function participantName(participant) {
  return participant?.displayName || 'unknown';
}

function relationshipText(participant) {
  return {
    SELECTED: 'фокус',
    TEAMMATE: 'союзник',
    OPPONENT: 'соперник',
    UNKNOWN: 'роль неизвестна',
  }[participant?.relationship] || 'роль неизвестна';
}

function countEvidence(value) {
  if (!value || value.value == null) return 'нет данных';
  return String(value.value);
}

export function narrativeText(analysis) {
  return (analysis.coachFeed?.cards || []).find(card => card.title === 'Как развивался матч')?.explanation || '';
}

export function composition(value, unavailable = false) {
  if (unavailable) return 'нет данных';
  const entries = Object.entries(value || {});
  return entries.length ? entries.map(([unit, count]) => `${count} × ${unit}`).join(', ') : 'нет';
}

export function listText(value) {
  return (value || []).length ? value.join(', ') : 'нет';
}

export function reconciliationText(player) {
  if ((player.reconciliationStatus || 'EXACT') === 'EXACT') return 'точная';
  const issues = player.reconciliationIssues || [];
  if (!issues.length) return 'неполные данные';
  return `неполные данные: ${issues.map(issue =>
    `${issue.unit}: ${issue.startCount} + ${issue.additions} - ${issue.losses} = ${issue.expectedEndCount}, в конце ${issue.actualEndCount}`
  ).join('; ')}`;
}

export function durationSeconds(value) {
  const match = String(value || '').match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/);
  return match ? Number(match[1] || 0) * 3600 + Number(match[2] || 0) * 60 + Number(match[3] || 0) : Number(value || 0);
}

export function clock(value) {
  const seconds = Math.round(Number(value || 0));
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}
