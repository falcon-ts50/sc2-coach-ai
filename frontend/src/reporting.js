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
        `- Армия в конце: ${composition(player.armyAfter)}`,
        `- Грейды: ${listText(player.upgrades)}`,
        `- Ключевые технологии: ${listText(player.technologies)}`,
        `- Стоимость армии: ${Math.round(player.armyValueBefore || 0)} → ${Math.round(player.armyValueAfter || 0)}`,
        `- Сверка: ${reconciliationText(player)}`);
      pushOptionalComposition(lines, '- Рабочие', player.workersLost);
      pushOptionalComposition(lines, '- Здания', player.structuresLost);
      pushOptionalComposition(lines, '- Потери статичной обороны', player.staticDefenseLost);
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
    lines.push('', '### Боевые таблицы доказательств', '');
    (narrative.evidence.combats || []).forEach(combat => {
      lines.push(`#### ${combat.label} · ${clock(durationSeconds(combat.startedAt))}–${clock(durationSeconds(combat.endedAt))}`);
      (combat.sides || []).forEach(side => {
        lines.push(`- ${side.label}: ${completenessText(side.completeness)}`);
        pushUnitRows(lines, '  - Итого', side.totalRows || []);
        (side.participants || []).forEach(player => {
          lines.push(`  - ${player.player}: ${reconciliationStatusText(player.reconciliationStatus)}, ${completenessText(player.completeness)}`);
          pushUnitRows(lines, '    - Боевые юниты', player.rows || []);
          pushOptionalComposition(lines, '    - Рабочие', player.workerLosses);
          pushOptionalComposition(lines, '    - Здания', player.structureLosses);
          pushOptionalComposition(lines, '    - Потери статичной обороны', player.staticDefenseLosses);
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
  rows.forEach(row => lines.push(`${label}: ${row.unit}: старт ${row.startCount}, новые ${row.additions}, потери ${row.losses}, финиш ${row.endCount}, убийства ${countEvidence(row.creditedKills)}, ${reconciliationStatusText(row.reconciliationStatus) || completenessText(row.completeness)}`));
}

function pushOptionalComposition(lines, label, value) {
  if (hasValues(value)) lines.push(`${label}: ${composition(value)}`);
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
    } else {
      (combat?.emptyStates || ['Боёв в этом интервале не обнаружено.']).forEach(item => lines.push(`  - Бои: ${item}`));
    }
    pushDevelopment(lines, interval.drilldown?.development);
    (interval.limitations || []).forEach(item => lines.push(`  - Ограничение: ${item}`));
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
  observations.forEach(item => lines.push(`  - Развитие: ${item}`));
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
