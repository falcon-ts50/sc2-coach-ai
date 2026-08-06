import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import JSZip from 'jszip';
import { buildMarkdown, clock, composition, durationSeconds, listText, reconciliationText } from './reporting.js';
import './styles.css';

function App() {
  const [file, setFile] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [focusPlayer, setFocusPlayer] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);

  async function analyze(event, requestedFocus = focusPlayer) {
    event?.preventDefault();
    if (!file) return;
    setLoading(true);
    setStatus(requestedFocus ? `Собираем разбор для ${requestedFocus}…` : 'Декодируем реплей…');
    try {
      const body = new FormData();
      body.append('replay', file);
      const query = requestedFocus ? `?focusPlayer=${encodeURIComponent(requestedFocus)}` : '';
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 120000);
      const response = await fetch(`/api/v1/analyses${query}`, { method: 'POST', body, signal: controller.signal });
      clearTimeout(timeout);
      if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        throw new Error(problem.detail || 'Не удалось проанализировать реплей');
      }
      const result = await response.json();
      setAnalysis(result);
      setFocusPlayer(result.focusPlayer || result.players?.[0]?.name || '');
      setStatus('Анализ готов');
    } catch (error) {
      setStatus(error.name === 'AbortError' ? 'Анализ занял больше двух минут. Попробуйте ещё раз.' : error.message);
    } finally {
      setLoading(false);
    }
  }

  function downloadMarkdown() { downloadText(buildMarkdown(analysis), 'sc2-match-review.md'); }
  function downloadTranscript() { downloadText(analysis.transcriptMarkdown, 'sc2-replay-transcript.md'); }
  async function downloadSupportBundle() {
    const zip = new JSZip();
    zip.file('report.md', buildMarkdown(analysis));
    zip.file('transcript.md', analysis.transcriptMarkdown || '# Transcript unavailable\n');
    zip.file('analysis-response.json', JSON.stringify(analysis, null, 2));
    zip.file('metadata.json', JSON.stringify(analysis.diagnostics || {}, null, 2));
    downloadBlob(await zip.generateAsync({ type: 'blob', compression: 'DEFLATE' }), `sc2-match-review-${analysis.diagnostics?.analysisId || 'unknown'}.zip`);
  }

  return <main>
    <header className="hero"><div className="eyebrow">REPLAY INTELLIGENCE</div><h1>SC2 Match Review</h1><p>Послематчевый разбор решений, боёв и переломных моментов.</p></header>
    <section className="panel upload-panel">
      <form onSubmit={analyze}>
        <label className="dropzone"><input type="file" accept=".SC2Replay" disabled={loading} onChange={e => setFile(e.target.files?.[0] || null)} /><strong>{file ? file.name : 'Выберите .SC2Replay'}</strong><span>Файл удаляется после анализа</span></label>
        <button disabled={!file || loading}>{loading ? 'Анализируем…' : 'Запустить анализ'}</button>
      </form>
      {status && <p className="status">{status}</p>}
    </section>
    {analysis && <Report analysis={analysis} focusPlayer={focusPlayer} setFocusPlayer={setFocusPlayer} rebuild={() => analyze(null, focusPlayer)} loading={loading} onDownload={downloadMarkdown} onTranscript={downloadTranscript} onSupportBundle={downloadSupportBundle} />}
  </main>;
}

function Report({ analysis, focusPlayer, setFocusPlayer, rebuild, loading, onDownload, onTranscript, onSupportBundle }) {
  const feed = analysis.coachFeed || {};
  const narrativeCard = (feed.cards || []).find(card => card.title === 'Как развивался матч');
  const events = (feed.cards || []).filter(card => card !== narrativeCard);
  return <article className="report">
    <section className="report-header panel"><div><span className="eyebrow">РАЗБОР ДЛЯ ИГРОКА</span><h2>{analysis.focusPlayer || focusPlayer}</h2><p>{feed.headline}</p></div><div className="perspective"><label>Для кого сделать отчёт</label><select value={focusPlayer} onChange={event => setFocusPlayer(event.target.value)}>{(analysis.players || []).map(player => <option key={player.pid} value={player.name}>{player.name} · {player.race}</option>)}</select><button disabled={loading || focusPlayer === analysis.focusPlayer} onClick={rebuild}>Перестроить отчёт</button></div></section>
    <NarrativeAnalysisSection narrative={analysis.narrativeAnalysis} />
    <section className="report-section"><span className="section-number">02</span><div className="wide"><h2>История боёв</h2><div className="combat-list">{(analysis.combats || []).length ? analysis.combats.map((combat, index) => <CombatBlock combat={combat} index={index} key={combat.id || `${combat.startedAt}-${index}`} />) : <p className="muted">Не удалось надёжно восстановить отдельные боевые эпизоды.</p>}</div></div></section>
    <section className="report-section"><span className="section-number">03</span><div className="wide"><h2>Переломные моменты</h2><div className="story-list">{events.map((card, index) => <div className="story-row" key={`${card.at}-${index}`}><time>{clock(durationSeconds(card.at))}</time><div><h3>{card.title}</h3><p>{card.explanation}</p><small>Уверенность {Math.round((card.confidence || 0) * 100)}%</small></div></div>)}</div></div></section>
    <section className="report-section"><span className="section-number">04</span><div className="wide"><h2>Что изменить в следующей игре</h2><ol className="next-actions">{(feed.nextGameRecommendations || []).map(item => <li key={item}>{item}</li>)}</ol></div></section>
    <footer className="report-footer panel"><div className="actions"><button onClick={onDownload}>Скачать отчёт</button><button onClick={onTranscript}>Расшифровка для ИИ</button><button onClick={onSupportBundle}>Support bundle</button></div><small>Analysis ID: {analysis.diagnostics?.analysisId || '—'} · {analysis.diagnostics?.applicationVersion || '—'} · {analysis.diagnostics?.totalTimeMs || 0} мс</small></footer>
  </article>;
}

function NarrativeAnalysisSection({ narrative }) {
  const [selectedIntervalId, setSelectedIntervalId] = useState('');
  const [hoverAt, setHoverAt] = useState(null);
  if (!narrative) return null;
  const matchFlow = narrative.matchFlow;
  const intervals = matchFlow?.intervals?.length ? matchFlow.intervals : (narrative.timeline?.phases || []).map(phase => ({
    id: phase.id,
    kind: phase.kind,
    title: phase.title,
    startedAt: phase.startedAt,
    endedAt: phase.endedAt,
    confidence: phase.confidence,
    completeness: 'PARTIAL',
    summary: phase.summary,
    drilldown: null,
  }));
  const evidence = narrative.evidence || {};
  const focuses = evidence.focuses || [];
  const selectedInterval = intervals.find(interval => interval.id === selectedIntervalId) || null;
  const selectedRange = selectedInterval ? { id: selectedInterval.id, from: selectedInterval.startedAt, to: selectedInterval.endedAt } : null;
  const metrics = evidence.metricComparisons?.length
    ? evidence.metricComparisons
    : legacyMetricComparisons(withOverallScoreSeries(narrative.chart, narrative.timeline?.snapshots || []), narrative);
  const participants = evidence.participants || [];
  const team = (narrative.focusTeamPlayers || []).join(', ') || '—';
  const selectIntervalAt = at => {
    const seconds = durationSeconds(at);
    const interval = intervals.find(item => seconds >= durationSeconds(item.startedAt) && seconds < durationSeconds(item.endedAt));
    if (interval) setSelectedIntervalId(interval.id);
  };

  return <section className="report-section"><span className="section-number">01</span><div className="wide">
    <h2>Ход матча</h2>
    <div className="narrative-summary">
      <p className="lead">Официальный результат для {narrative.focusPlayer || 'игрока'}: <strong>{narrative.officialReplayResult || 'не определён'}</strong>.</p>
      <dl className="summary-facts"><dt>Команда</dt><dd>{team}</dd></dl>
    </div>
    <div className="phase-list">{intervals.map(interval => {
      const selected = interval.id === selectedInterval?.id;
      return <button className={selected ? 'phase selected' : 'phase'} key={interval.id} onClick={() => setSelectedIntervalId(selected ? '' : interval.id)}>
      <time>{clock(durationSeconds(interval.startedAt))}–{clock(durationSeconds(interval.endedAt))}</time>
      <strong>{interval.title}</strong>
      <span>{interval.summary}</span>
      <small>{kindText(interval.kind)} · {completenessText(interval.completeness)} · {Math.round((interval.confidence || 0) * 100)}%</small>
    </button>;
    })}</div>
    <div className="evidence-legend">{participants.map((participant, index) => <span className={`legend-item ${participant.relationship?.toLowerCase() || 'unknown'}`} key={participant.id}><i style={{ background: participantColor(index) }} />{participant.displayName}<small>{relationshipText(participant)}</small></span>)}</div>
    <div className="charts-stack">{metrics.map(metric => <MetricComparisonChart key={metric.id} metric={metric} participants={participants} focuses={focuses} selected={selectedRange} hoverAt={hoverAt} onHover={setHoverAt} onFocusAt={selectIntervalAt} />)}</div>
    <IntervalDrilldown interval={selectedInterval} />
  </div></section>;
}

function legacyMetricComparisons(chart, narrative) {
  const participant = {
    id: 'participant-focus',
    displayName: narrative.focusPlayer || 'Фокус',
    relationship: 'SELECTED',
    selected: true,
  };
  return (chart?.series || []).map(series => ({
    id: series.id,
    label: series.label,
    unit: series.unit,
    completeness: series.completeness,
    series: [{ id: `${series.id}-focus`, participantId: participant.id, completeness: series.completeness, lineStyle: 'solid', strokeWeight: 5, points: series.points || [] }],
  }));
}

function withOverallScoreSeries(chart, snapshots) {
  if (!chart) return chart;
  const existing = chart.series || [];
  if (existing.some(series => series.id === 'overallScore')) return chart;
  const points = snapshots.map(snapshot => ({ at: snapshot.at, value: Number(snapshot.metrics?.overallScore || 0) }));
  return { ...chart, series: [...existing, { id: 'overallScore', label: 'Общее преимущество', unit: 'баллы', points }] };
}

function MetricComparisonChart({ metric, participants, focuses, selected, hoverAt, onHover, onFocusAt }) {
  const visibleSeries = metric.series || [];
  const participantById = new Map((participants || []).map((participant, index) => [participant.id, { ...participant, color: participantColor(index) }]));
  const allPoints = visibleSeries.flatMap(series => series.points || []);
  const start = Math.min(...allPoints.map(point => durationSeconds(point.at)), durationSeconds(selected?.from), 0);
  const end = Math.max(...allPoints.map(point => durationSeconds(point.at)), durationSeconds(selected?.to), 1);
  const duration = Math.max(1, end - start);
  const values = allPoints.map(point => Number(point.value || 0));
  const rawMin = Math.min(0, ...values);
  const rawMax = Math.max(1, ...values);
  const span = Math.max(1, rawMax - rawMin);
  const minValue = rawMin - span * .08;
  const maxValue = rawMax + span * .08;
  const width = 900;
  const height = 360;
  const left = 76;
  const right = 28;
  const top = 42;
  const bottom = 58;
  const plotWidth = width - left - right;
  const plotHeight = height - top - bottom;
  const x = value => left + (durationSeconds(value) - start) / duration * plotWidth;
  const y = value => top + (maxValue - Number(value || 0)) / (maxValue - minValue) * plotHeight;
  const dash = style => style === 'dashed' ? '12 8' : style === 'dotted' ? '3 8' : style === 'dashdot' ? '12 6 3 6' : undefined;
  const xTicks = [0, .25, .5, .75, 1];
  const yTicks = [0, .25, .5, .75, 1];
  const focusFrom = selected ? x(selected.from) : null;
  const focusTo = selected ? x(selected.to) : null;
  const hoverX = hoverAt == null ? null : left + (Number(hoverAt) - start) / duration * plotWidth;
  const pointerMove = event => {
    const rect = event.currentTarget.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
    onHover(Math.round(start + ratio * duration));
  };

  return <section className="chart-card">
    <header className="chart-title"><h3>{metric.label}</h3><span>{metric.unit || ''} · {completenessText(metric.completeness)}</span></header>
    <div className="chart-scroll">
      <svg className="narrative-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`${metric.label} по времени матча`} onPointerMove={pointerMove} onPointerLeave={() => onHover(null)}>
        {selected && <rect className="phase-band active" x={Math.min(focusFrom, focusTo)} y={top} width={Math.max(2, Math.abs(focusTo - focusFrom))} height={plotHeight} />}
        {yTicks.map(step => {
          const value = maxValue - step * (maxValue - minValue);
          const yy = top + step * plotHeight;
          return <g key={`y-${step}`}><line className="grid-line" x1={left} x2={width - right} y1={yy} y2={yy} /><text className="axis-label" x={left - 12} y={yy + 4} textAnchor="end">{formatAxisValue(value)}</text></g>;
        })}
        {xTicks.map(step => {
          const seconds = start + step * duration;
          const xx = left + step * plotWidth;
          return <g key={`x-${step}`}><line className="grid-line vertical" x1={xx} x2={xx} y1={top} y2={height - bottom} /><text className="axis-label" x={xx} y={height - 28} textAnchor="middle">{clock(seconds)}</text></g>;
        })}
        <line className="axis-line" x1={left} x2={left} y1={top} y2={height - bottom} />
        <line className="axis-line" x1={left} x2={width - right} y1={height - bottom} y2={height - bottom} />
        {visibleSeries.map(series => {
          const participant = participantById.get(series.participantId) || {};
          const points = series.points || [];
          const clipped = selected ? clippedPoints(points, selected.from, selected.to) : points;
          return <g key={series.id}>
            <polyline className={selected ? 'series-line muted-range' : 'series-line'} fill="none" stroke={selected ? '#9aa8b8' : participant.color || '#dbe7f5'} strokeWidth={selected ? Math.max(1.5, (series.strokeWeight || 2) - 1) : series.strokeWeight || 2} strokeLinejoin="round" strokeLinecap="round" strokeDasharray={dash(series.lineStyle)} points={points.map(point => `${x(point.at)},${y(point.value)}`).join(' ')} />
            {selected && clipped.length > 1 && <polyline className="series-line active-range" fill="none" stroke={participant.color || '#dbe7f5'} strokeWidth={Math.max(4, series.strokeWeight || 2)} strokeLinejoin="round" strokeLinecap="round" strokeDasharray={dash(series.lineStyle)} points={clipped.map(point => `${x(point.at)},${y(point.value)}`).join(' ')} />}
          </g>;
        })}
        {focuses.filter(focus => focus.kind === 'COMBAT' || focus.kind === 'TURNING_POINT').map(focus => <line key={focus.id} className="marker-line" x1={x(focus.at)} x2={x(focus.at)} y1={top} y2={height - bottom} onClick={() => onFocusAt(focus.at)} />)}
        {hoverX != null && hoverX >= left && hoverX <= width - right && <g><line className="crosshair-line" x1={hoverX} x2={hoverX} y1={top} y2={height - bottom} /><text className="axis-label crosshair-label" x={hoverX + 8} y={top + 16}>{clock(hoverAt)}</text></g>}
        <text className="axis-title" x={18} y={top + plotHeight / 2} transform={`rotate(-90 18 ${top + plotHeight / 2})`} textAnchor="middle">{metric.unit || metric.label}</text>
        <text className="axis-title" x={left + plotWidth / 2} y={height - 6} textAnchor="middle">Время матча</text>
      </svg>
    </div>
  </section>;
}

function clippedPoints(points, from, to) {
  const start = durationSeconds(from);
  const end = durationSeconds(to);
  const ordered = [...(points || [])].sort((left, right) => durationSeconds(left.at) - durationSeconds(right.at));
  if (!ordered.length || end <= start) return [];
  const result = [];
  for (let index = 0; index < ordered.length - 1; index++) {
    const left = ordered[index];
    const right = ordered[index + 1];
    const leftAt = durationSeconds(left.at);
    const rightAt = durationSeconds(right.at);
    if (rightAt < start || leftAt > end || rightAt === leftAt) continue;
    const segmentStart = Math.max(start, leftAt);
    const segmentEnd = Math.min(end, rightAt);
    if (segmentEnd < segmentStart) continue;
    pushUniquePoint(result, interpolatePoint(left, right, segmentStart));
    pushUniquePoint(result, interpolatePoint(left, right, segmentEnd));
  }
  for (const point of ordered) {
    const at = durationSeconds(point.at);
    if (at >= start && at <= end) pushUniquePoint(result, point);
  }
  return result.sort((left, right) => durationSeconds(left.at) - durationSeconds(right.at));
}

function interpolatePoint(left, right, at) {
  const leftAt = durationSeconds(left.at);
  const rightAt = durationSeconds(right.at);
  const ratio = rightAt === leftAt ? 0 : (at - leftAt) / (rightAt - leftAt);
  return {
    at,
    value: Number(left.value || 0) + (Number(right.value || 0) - Number(left.value || 0)) * ratio,
  };
}

function pushUniquePoint(points, point) {
  const at = durationSeconds(point.at);
  const existing = points.find(item => Math.abs(durationSeconds(item.at) - at) < 0.001);
  if (!existing) points.push(point);
}

function formatAxisValue(value) {
  const absolute = Math.abs(value);
  if (absolute >= 1000) return `${(value / 1000).toFixed(absolute >= 10000 ? 0 : 1)}k`;
  return `${Math.round(value)}`;
}

function CombatBlock({ combat, index }) {
  const participants = combat.participants || [];
  const showUpgrades = participants.some(player => (player.upgrades || []).length);
  const showTechnologies = participants.some(player => (player.technologies || []).length);
  const showWorkers = participants.some(player => hasValues(player.workersLost));
  const showStructures = participants.some(player => hasValues(player.structuresLost));
  const showStaticDefense = participants.some(player => hasValues(player.staticDefenseLost));
  return <section className="combat-block">
    <div className="combat-title"><time>{clock(durationSeconds(combat.startedAt))}</time><div><h3>{combat.ordinalLabel || `Бой ${index + 1}`} · {combat.initiator || 'Игрок'} атакует {combat.opponent || 'соперника'}</h3><p>{clock(durationSeconds(combat.startedAt))}–{clock(durationSeconds(combat.endedAt))}. {combat.location ? `Координаты команды: ${combat.location}.` : ''}</p></div></div>
    <div className="combat-participants">{participants.map(player => <div key={player.player}>
      <h4>{player.player}</h4>
      <dl>
        <dt>Армия в начале</dt><dd>{composition(player.armyBefore)}</dd>
        <dt>Новые юниты в интервале</dt><dd>{composition(player.additions)}</dd>
        {showUpgrades && <><dt>Грейды</dt><dd>{listText(player.upgrades)}</dd></>}
        {showTechnologies && <><dt>Технологии</dt><dd>{listText(player.technologies)}</dd></>}
        <dt>Боевые потери</dt><dd>{composition(player.unitsLost)}</dd>
        {showWorkers && <><dt>Рабочие</dt><dd>{composition(player.workersLost)}</dd></>}
        {showStructures && <><dt>Здания</dt><dd>{composition(player.structuresLost)}</dd></>}
        {showStaticDefense && <><dt>Потери статичной обороны</dt><dd>{composition(player.staticDefenseLost)}</dd></>}
        <dt>Армия в конце</dt><dd>{composition(player.armyAfter)}</dd>
        <dt>Стоимость армии</dt><dd>{Math.round(player.armyValueBefore || 0)} → {Math.round(player.armyValueAfter || 0)}</dd>
        <dt>Сверка</dt><dd>{reconciliationText(player)}</dd>
      </dl>
    </div>)}</div>
  </section>;
}

function CombatEvidenceTable({ evidence }) {
  return <div className="combat-evidence">
    {(evidence.sides || []).map(side => <section className="combat-side" key={side.id}>
      <h4>{side.label} <small>{completenessText(side.completeness)}</small></h4>
      <UnitEvidenceTable rows={side.totalRows || []} caption="Итого по стороне" />
      {(side.participants || []).map(participant => <div className="participant-evidence" key={participant.participantId}>
        <h5>{participant.player} <small>{reconciliationStatusText(participant.reconciliationStatus)}</small></h5>
        <UnitEvidenceTable rows={participant.rows || []} caption="Боевые юниты" />
        {hasAnyValues(participant.workerLosses, participant.structureLosses, participant.staticDefenseLosses) && <dl className="collateral-losses">
          {hasValues(participant.workerLosses) && <><dt>Рабочие</dt><dd>{composition(participant.workerLosses)}</dd></>}
          {hasValues(participant.structureLosses) && <><dt>Здания</dt><dd>{composition(participant.structureLosses)}</dd></>}
          {hasValues(participant.staticDefenseLosses) && <><dt>Статичная оборона</dt><dd>{composition(participant.staticDefenseLosses)}</dd></>}
        </dl>}
      </div>)}
    </section>)}
    {(evidence.notes || []).length > 0 && <p className="evidence-note">{evidence.notes.join(' ')}</p>}
  </div>;
}

function UnitEvidenceTable({ rows, caption }) {
  if (!rows.length) return <p className="muted">{caption}: нет боевых юнитов.</p>;
  const showKills = rows.some(row => row.creditedKills && row.creditedKills.value != null);
  return <div className="unit-table-wrap" role="region" aria-label={caption}>
    <table className="unit-evidence-table">
      <caption>{caption}</caption>
      <thead><tr><th>Юнит</th><th>Старт</th><th>Новые</th><th>Потери</th><th>Финиш</th>{showKills && <th>Убийства</th>}</tr></thead>
      <tbody>{rows.map(row => <tr key={row.unit}>
        <th scope="row">{row.unit}<small>{completenessText(row.completeness)}</small></th>
        <td>{row.startCount}</td>
        <td>{row.additions}</td>
        <td>{row.losses}</td>
        <td>{row.endCount}</td>
        {showKills && <td>{countEvidence(row.creditedKills)}</td>}
      </tr>)}</tbody>
    </table>
  </div>;
}

function countEvidence(value) {
  if (!value || value.value == null) return 'нет данных';
  return value.value;
}

function IntervalDrilldown({ interval }) {
  if (!interval) return <section className="interval-drilldown">
    <p className="muted">Выберите интервал, чтобы увидеть только его боевую и экономическую расшифровку.</p>
  </section>;
  const drilldown = interval.drilldown || {};
  return <section className="interval-drilldown">
    <header>
      <h3>{interval.title}</h3>
      <span>{clock(durationSeconds(interval.startedAt))}–{clock(durationSeconds(interval.endedAt))}</span>
    </header>
    <div className="drilldown-grid">
      <section className="drilldown-section">
        <h4>Бои</h4>
        <CombatDrilldown combat={drilldown.combat} />
      </section>
      <section className="drilldown-section">
        <h4>Экономика и развитие</h4>
        <DevelopmentDrilldown development={drilldown.development} />
      </section>
    </div>
    {[...(drilldown.limitations || []), ...(interval.limitations || [])].length > 0 && <ul className="limitations">{[...(drilldown.limitations || []), ...(interval.limitations || [])].map(item => <li key={item}>{item}</li>)}</ul>}
  </section>;
}

function CombatDrilldown({ combat }) {
  if (!combat) return <p className="muted">Боевых данных для интервала нет.</p>;
  if ((combat.combats || []).length) {
    return <div className="combat-list">
      {combat.summary && <p className="interval-narrative">{combat.summary}</p>}
      {combat.combats.map(item => <section className="combat-block compact" key={item.id}>
      <div className="combat-title"><time>{clock(durationSeconds(item.startedAt))}</time><div><h3>{item.label}</h3><p>{clock(durationSeconds(item.startedAt))}–{clock(durationSeconds(item.endedAt))}</p></div></div>
      <CombatEvidenceTable evidence={item} />
    </section>)}</div>;
  }
  return <EmptyState items={combat.emptyStates} fallback="Боёв в этом интервале не обнаружено." />;
}

function DevelopmentDrilldown({ development }) {
  if (!development) return <p className="muted">Данных по развитию для интервала нет.</p>;
  const rows = development.macro?.metrics || [];
  const production = development.production?.observations || [];
  const tech = development.tech?.observations || [];
  const scouting = development.scouting?.observations || [];
  const preparation = development.preparation?.observations || [];
  const hasEvidence = rows.length || production.length || tech.length || scouting.length || preparation.length;
  if (!hasEvidence) {
    return <EmptyState items={development.emptyStates} fallback="Экономических или технологических событий в этом интервале не обнаружено." />;
  }
  return <div className="development-evidence">
    {rows.length > 0 && <table className="development-table">
      <thead><tr><th>Метрика</th><th>Старт</th><th>Финиш</th><th>Δ</th></tr></thead>
      <tbody>{rows.map(row => <tr key={row.metric}>
        <th scope="row">{metricText(row.metric)}</th>
        <td>{Math.round(row.startValue || 0)}</td>
        <td>{Math.round(row.endValue || 0)}</td>
        <td>{formatDelta(row.delta)}</td>
      </tr>)}</tbody>
    </table>}
    <ObservationList title="Производство" items={production} />
    <ObservationList title="Технологии" items={tech} />
    <ObservationList title="Разведка" items={scouting} />
    <ObservationList title="Подготовка" items={preparation} />
    {(development.limitations || []).length > 0 && <ul className="limitations compact-list">{development.limitations.map(item => <li key={item}>{item}</li>)}</ul>}
  </div>;
}

function EmptyState({ items, fallback }) {
  const values = (items || []).length ? items : [fallback];
  return <div className="empty-state">{values.map(item => <p key={item}>{item}</p>)}</div>;
}

function ObservationList({ title, items }) {
  if (!(items || []).length) return null;
  return <div className="observation-list"><h5>{title}</h5><ul>{items.map(item => <li key={item}>{item}</li>)}</ul></div>;
}

function metricText(metric) {
  return {
    armyValue: 'Стоимость армии',
    economyProxy: 'Экономика',
    supplyUsed: 'Занятый лимит',
  }[metric] || metric;
}

function formatDelta(value) {
  const rounded = Math.round(Number(value || 0));
  return rounded > 0 ? `+${rounded}` : String(rounded);
}

function completenessText(value) {
  return {
    COMPLETE: 'данные полные',
    PARTIAL: 'частичные данные',
    UNAVAILABLE: 'нет данных',
  }[value] || 'статус неизвестен';
}

function reconciliationStatusText(value) {
  return {
    EXACT: 'сверка точная',
    PARTIAL: 'сверка частичная',
    UNKNOWN: 'сверка неизвестна',
  }[value] || completenessText(value);
}

function hasValues(map) {
  return Object.values(map || {}).some(value => Number(value || 0) > 0);
}

function hasAnyValues(...maps) {
  return maps.some(hasValues);
}

function kindText(kind) {
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

function participantColor(index) {
  return ['#8bdcff', '#f6d36f', '#a7f3b3', '#d7b4ff', '#ffb3c7', '#b8c8ff'][index % 6];
}

function relationshipText(participant) {
  return {
    SELECTED: 'фокус',
    TEAMMATE: 'союзник',
    OPPONENT: 'соперник',
    UNKNOWN: 'роль неизвестна',
  }[participant.relationship] || 'роль неизвестна';
}

function downloadText(text, filename) { downloadBlob(new Blob([text || ''], { type: 'text/markdown;charset=utf-8' }), filename); }
function downloadBlob(blob, filename) { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = filename; link.click(); URL.revokeObjectURL(link.href); }

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
