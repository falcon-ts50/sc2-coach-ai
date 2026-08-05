export function buildMarkdown(analysis) {
  if (!analysis) return '';
  const lines = [
    '# SC2 Coach Report', '',
    `**Разбор для:** ${analysis.focusPlayer || '—'}`,
    `**Карта:** ${analysis.map || '—'}`,
    `**Длительность:** ${clock(analysis.gameSeconds)}`, '',
    '## Итог', '', analysis.coachFeed?.headline || 'Недостаточно данных.', '',
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
