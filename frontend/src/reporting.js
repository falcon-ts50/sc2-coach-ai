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
    const losses = combatHistoryLosses(combat);
    if (losses.length) lines.push(`Потери: ${losses.join('. ')}.`);
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
    `- Официальный результат реплея: ${resultText(narrative.officialReplayResult)}`,
    `- Статус анализа: ${analysisStatusText(narrative.status)}`,
    `- Стратегический результат: ${strategicResultText(narrative.strategicResultStatus)}`,
    `- Команда фокуса: ${(narrative.focusTeamPlayers || []).join(', ') || '—'}`, '',
    narrative.summary?.verdict || 'Недостаточно данных для связного сценария.', '',
    '### Фазы', ''
  ];
  (narrative.timeline?.phases || []).forEach(phase => {
    lines.push(`- ${clock(durationSeconds(phase.startedAt))}–${clock(durationSeconds(phase.endedAt))}: ${phase.title}. ${phase.summary}`);
  });
  pushMatchFlow(lines, narrative.matchFlow);
  lines.push('', '### Сценарная цепочка', '');
  (narrative.timeline?.causalLinks || []).forEach(link => {
    lines.push(`- ${link.kind}: ${link.statement}`);
  });
  lines.push('', '### Данные графика', '');
  (narrative.chart?.series || []).forEach(series => {
    const points = series.points || [];
    const first = points[0];
    const last = points[points.length - 1];
    lines.push(`- ${series.label}: ${completenessText(series.completeness)}, ${points.length} точек${first && last ? `, ${clock(durationSeconds(first.at))}=${Math.round(first.value)}, ${clock(durationSeconds(last.at))}=${Math.round(last.value)}` : ''}`);
  });
  if (narrative.evidence) {
    lines.push('', '### Сравнение участников', '');
    (narrative.evidence.metricComparisons || []).forEach(metric => {
      lines.push(`- ${metric.label}: ${completenessText(metric.completeness)}, ${(metric.series || []).length} серий`);
      (metric.series || []).forEach(series => {
        const participant = participantById(narrative.evidence, series.participantId);
        const points = series.points || [];
        const first = points[0];
        const last = points[points.length - 1];
        lines.push(`  - ${participantName(participant)} (${relationshipText(participant)}): ${completenessText(series.completeness)}, ${lineStyleText(series.lineStyle)}, ${points.length} точек${first && last ? `, ${clock(durationSeconds(first.at))}=${Math.round(first.value)}, ${clock(durationSeconds(last.at))}=${Math.round(last.value)}` : ''}`);
      });
    });
  }
  if ((narrative.limitations || []).length) {
    lines.push('', '### Ограничения', '');
    visibleLimitations(narrative.limitations).forEach(item => lines.push(`- ${item}`));
  }
  lines.push('');
  return lines;
}

function pushUnitRows(lines, label, rows) {
  if (!rows.length) {
    lines.push(`${label}: нет`);
    return;
  }
  const showKills = rows.some(row => row.creditedKills && row.creditedKills.value != null);
  rows.forEach(row => {
    const kills = showKills ? `, убийства ${countEvidence(row.creditedKills)}` : '';
    const quality = row.reconciliationStatus && row.reconciliationStatus !== 'EXACT'
      ? `, ${reconciliationStatusText(row.reconciliationStatus)}`
      : row.completeness && row.completeness !== 'COMPLETE'
        ? `, ${completenessText(row.completeness)}`
        : '';
    lines.push(`${label}: ${row.unit}: старт ${row.startCount}, новые ${row.additions}, потери ${row.losses}, финиш ${row.endCount}${kills}${quality}`);
  });
}

function pushCombatEvidence(lines, combats, indent = '') {
  combats.forEach(combat => {
    lines.push(`${indent}- Таблица боя: ${combat.label} · ${clock(durationSeconds(combat.startedAt))}–${clock(durationSeconds(combat.endedAt))}`);
    (combat.sides || []).forEach(side => {
      lines.push(`${indent}  - ${side.label}${side.completeness && side.completeness !== 'COMPLETE' ? `: ${completenessText(side.completeness)}` : ''}`);
      pushUnitRows(lines, `${indent}    - Итого`, side.totalRows || []);
      (side.participants || []).forEach(player => {
        const quality = player.reconciliationStatus && player.reconciliationStatus !== 'EXACT'
          ? `: ${reconciliationStatusText(player.reconciliationStatus)}`
          : player.completeness && player.completeness !== 'COMPLETE'
            ? `: ${completenessText(player.completeness)}`
            : '';
        lines.push(`${indent}    - ${player.player}${quality}`);
        pushUnitRows(lines, `${indent}      - Боевые юниты`, player.rows || []);
        pushOptionalComposition(lines, `${indent}      - Рабочие`, player.workerLosses);
        pushOptionalComposition(lines, `${indent}      - Здания`, player.structureLosses);
        pushOptionalComposition(lines, `${indent}      - Потери статичной обороны`, player.staticDefenseLosses);
      });
    });
  });
}

function pushOptionalComposition(lines, label, value) {
  if (hasValues(value)) lines.push(`${label}: ${composition(value)}`);
}

function combatHistoryLosses(combat) {
  return (combat.participants || [])
    .map(player => {
      const parts = [
        composition(player.unitsLost),
        hasValues(player.workersLost) ? `рабочие: ${composition(player.workersLost)}` : '',
        hasValues(player.structuresLost) ? `здания: ${composition(player.structuresLost)}` : '',
        hasValues(player.staticDefenseLost) ? `статичная оборона: ${composition(player.staticDefenseLost)}` : '',
      ].filter(Boolean).filter(value => value !== 'нет');
      return parts.length ? `${player.player}: ${parts.join('; ')}` : '';
    })
    .filter(Boolean);
}

function visibleLimitations(items) {
  return [...new Set((items || [])
    .map(userFacingLimitation)
    .filter(Boolean))];
}

function userFacingLimitation(item) {
  if (!item) return '';
  return String(item)
    .replace(/ADR-\d+/g, 'правило интерпретации пополнений')
    .replace(/replay response/g, 'данных реплея')
    .replace(/combat evidence/g, 'боевых данных')
    .replace(/NarrativeEvidence/g, 'боевых данных');
}

function pushMatchFlow(lines, matchFlow) {
  if (!matchFlow?.intervals?.length) return;
  lines.push('', '### Непрерывный ход матча', '');
  lines.push(`- Покрытие: ${clock(durationSeconds(matchFlow.matchStartedAt))}–${clock(durationSeconds(matchFlow.matchEndedAt))}, интервалов: ${matchFlow.intervals.length}`);
  (matchFlow.intervals || []).forEach(interval => {
    lines.push(`- ${clock(durationSeconds(interval.startedAt))}–${clock(durationSeconds(interval.endedAt))}: ${interval.title} (${kindLabel(interval.kind)}, ${completenessText(interval.completeness)}, ${Math.round((interval.confidence || 0) * 100)}%). ${interval.summary || ''}`);
    const combat = interval.drilldown?.combat;
    if ((combat?.combatIds || []).length) {
      lines.push(`  - Бои: ${combat.combatIds.join(', ')}`);
      if (combat.summary) lines.push(`  - Описание боя: ${combat.summary}`);
      pushCombatEvidence(lines, combat.combats || [], '  ');
    } else {
      (combat?.emptyStates || ['Боёв в этом интервале не обнаружено.']).forEach(item => lines.push(`  - Бои: ${item}`));
    }
    pushDevelopment(lines, interval.drilldown?.development);
    visibleLimitations(interval.limitations).forEach(item => lines.push(`  - Ограничение: ${item}`));
  });
}

function pushDevelopment(lines, development) {
  if (!development) {
    lines.push('  - Развитие: нет данных');
    return;
  }
  const metrics = development.macro?.metrics || [];
  const observations = [
    ...(development.production?.observations || []),
    ...(development.tech?.observations || []),
    ...(development.scouting?.observations || []),
    ...(development.preparation?.observations || []),
  ];
  if (!metrics.length && !observations.length) {
    (development.emptyStates || ['Экономических/технологических событий в этом интервале не обнаружено.'])
      .forEach(item => lines.push(`  - Развитие: ${item}`));
    return;
  }
  metrics.forEach(row => {
    lines.push(`  - Развитие: ${metricLabel(row.metric)} ${Math.round(row.startValue || 0)} → ${Math.round(row.endValue || 0)} (${formatDelta(row.delta)})`);
  });
  observations.forEach(item => lines.push(`  - Развитие: ${userFacingLimitation(item)}`));
}

function metricLabel(metric) {
  return {
    armyValue: 'Стоимость армии',
    economyProxy: 'Экономика',
    supplyUsed: 'Занятый лимит',
  }[metric] || metric;
}

function kindLabel(kind) {
  return {
    OPENING_BUILDUP: 'открытие',
    ECONOMIC_GROWTH: 'экономика',
    TECH_TRANSITION: 'технологии',
    ARMY_BUILDUP: 'армия',
    MAP_CONTROL_OR_SCOUTING: 'карта/разведка',
    PRESSURE_PREPARATION: 'подготовка',
    COMBAT: 'бой',
    RECOVERY: 'восстановление',
    REGROUPING_OR_LOW_ACTIVITY: 'перегруппировка',
    LOW_EVIDENCE: 'низкая доказательность',
  }[kind] || 'интервал';
}

function completenessText(value) {
  return {
    COMPLETE: 'данные полные',
    PARTIAL: 'частичные данные',
    UNAVAILABLE: 'нет данных',
  }[value] || 'статус неизвестен';
}

function analysisStatusText(value) {
  return {
    PRELIMINARY: 'предварительный',
  }[value] || value || 'статус неизвестен';
}

function lineStyleText(value) {
  return {
    solid: 'сплошная линия',
    dashed: 'пунктир',
    dotted: 'точки',
    dashdot: 'штрих-пунктир',
  }[value] || 'линия';
}

function reconciliationStatusText(value) {
  return {
    EXACT: 'сверка точная',
    PARTIAL: 'сверка частичная',
    UNKNOWN: 'сверка неизвестна',
  }[value] || '';
}

function strategicResultText(value) {
  return {
    NOT_EVALUATED: 'не оценивался',
  }[value] || value || 'не оценивался';
}

function resultText(value) {
  return {
    Win: 'победа',
    Loss: 'поражение',
    Tie: 'ничья',
    Unknown: 'не определён',
  }[value] || value || 'не определён';
}

function hasValues(map) {
  return Object.values(map || {}).some(value => Number(value || 0) > 0);
}

function formatDelta(value) {
  const rounded = Math.round(Number(value || 0));
  return rounded > 0 ? `+${rounded}` : String(rounded);
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
