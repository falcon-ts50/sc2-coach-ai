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
  const combatEvidenceById = new Map((analysis.narrativeAnalysis?.evidence?.combats || []).map(combat => [combat.id, combat]));
  return <article className="report">
    <section className="report-header panel"><div><span className="eyebrow">РАЗБОР ДЛЯ ИГРОКА</span><h2>{analysis.focusPlayer || focusPlayer}</h2><p>{feed.headline}</p></div><div className="perspective"><label>Для кого сделать отчёт</label><select value={focusPlayer} onChange={event => setFocusPlayer(event.target.value)}>{(analysis.players || []).map(player => <option key={player.pid} value={player.name}>{player.name} · {player.race}</option>)}</select><button disabled={loading || focusPlayer === analysis.focusPlayer} onClick={rebuild}>Перестроить отчёт</button></div></section>
    <NarrativeAnalysisSection narrative={analysis.narrativeAnalysis} />
    <section className="report-section"><span className="section-number">02</span><div className="wide"><h2>История боёв</h2><div className="combat-list">{(analysis.combats || []).length ? analysis.combats.map((combat, index) => <CombatBlock combat={combat} evidence={combatEvidenceById.get(combat.id)} index={index} key={combat.id || `${combat.startedAt}-${index}`} />) : <p className="muted">Не удалось надёжно восстановить отдельные боевые эпизоды.</p>}</div></div></section>
    <section className="report-section"><span className="section-number">03</span><div className="wide"><h2>Переломные моменты</h2><div className="story-list">{events.map((card, index) => <div className="story-row" key={`${card.at}-${index}`}><time>{clock(durationSeconds(card.at))}</time><div><h3>{card.title}</h3><p>{card.explanation}</p><small>Уверенность {Math.round((card.confidence || 0) * 100)}%</small></div></div>)}</div></div></section>
    <section className="report-section"><span className="section-number">04</span><div className="wide"><h2>Что изменить в следующей игре</h2><ol className="next-actions">{(feed.nextGameRecommendations || []).map(item => <li key={item}>{item}</li>)}</ol></div></section>
    <footer className="report-footer panel"><div className="actions"><button onClick={onDownload}>Скачать отчёт</button><button onClick={onTranscript}>Расшифровка для ИИ</button><button onClick={onSupportBundle}>Support bundle</button></div><small>Analysis ID: {analysis.diagnostics?.analysisId || '—'} · {analysis.diagnostics?.applicationVersion || '—'} · {analysis.diagnostics?.totalTimeMs || 0} мс</small></footer>
  </article>;
}

function NarrativeAnalysisSection({ narrative }) {
  const [selectedFocusId, setSelectedFocusId] = useState(narrative?.evidence?.focuses?.[0]?.id || '');
  const [hoverAt, setHoverAt] = useState(null);
  if (!narrative) return null;
  const phases = narrative.timeline?.phases || [];
  const evidence = narrative.evidence || {};
  const focuses = evidence.focuses || [];
  const selected = focuses.find(focus => focus.id === selectedFocusId) || focuses[0];
  const metrics = evidence.metricComparisons?.length
    ? evidence.metricComparisons
    : legacyMetricComparisons(withOverallScoreSeries(narrative.chart, narrative.timeline?.snapshots || []), narrative);
  const participants = evidence.participants || [];
  const team = (narrative.focusTeamPlayers || []).join(', ') || '—';

  return <section className="report-section"><span className="section-number">01</span><div className="wide">
    <h2>Ход матча</h2>
    <div className="narrative-summary">
      <p className="lead">Официальный результат для {narrative.focusPlayer || 'игрока'}: <strong>{narrative.officialReplayResult || 'не определён'}</strong>.</p>
      <dl className="summary-facts"><dt>Команда</dt><dd>{team}</dd></dl>
    </div>
    <div className="phase-list">{phases.map(phase => {
      const focus = focuses.find(item => item.sourceId === phase.id);
      return <button className={focus?.id === selected?.id ? 'phase selected' : 'phase'} key={phase.id} onClick={() => setSelectedFocusId(focus?.id || '')}>
      <time>{clock(durationSeconds(phase.startedAt))}–{clock(durationSeconds(phase.endedAt))}</time>
      <strong>{phase.title}</strong>
      <span>{phase.summary}</span>
    </button>;
    })}</div>
    <div className="evidence-legend">{participants.map((participant, index) => <span className={`legend-item ${participant.relationship?.toLowerCase() || 'unknown'}`} key={participant.id}><i style={{ background: participantColor(index) }} />{participant.displayName}<small>{relationshipText(participant)}</small></span>)}</div>
    <div className="charts-stack">{metrics.map(metric => <MetricComparisonChart key={metric.id} metric={metric} participants={participants} focuses={focuses} selected={selected} hoverAt={hoverAt} onHover={setHoverAt} onFocus={setSelectedFocusId} />)}</div>
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

function MetricComparisonChart({ metric, participants, focuses, selected, hoverAt, onHover, onFocus }) {
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
    <header className="chart-title"><h3>{metric.label}</h3><span>{metric.unit || ''} · {metric.completeness || '—'}</span></header>
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
          return <polyline key={series.id} fill="none" stroke={participant.color || '#dbe7f5'} strokeWidth={series.strokeWeight || 2} strokeLinejoin="round" strokeLinecap="round" strokeDasharray={dash(series.lineStyle)} points={(series.points || []).map(point => `${x(point.at)},${y(point.value)}`).join(' ')} />;
        })}
        {focuses.filter(focus => focus.kind === 'COMBAT' || focus.kind === 'TURNING_POINT').map(focus => <line key={focus.id} className={focus.id === selected?.id ? 'marker-line active' : 'marker-line'} x1={x(focus.at)} x2={x(focus.at)} y1={top} y2={height - bottom} onClick={() => onFocus(focus.id)} />)}
        {hoverX != null && hoverX >= left && hoverX <= width - right && <g><line className="crosshair-line" x1={hoverX} x2={hoverX} y1={top} y2={height - bottom} /><text className="axis-label crosshair-label" x={hoverX + 8} y={top + 16}>{clock(hoverAt)}</text></g>}
        <text className="axis-title" x={18} y={top + plotHeight / 2} transform={`rotate(-90 18 ${top + plotHeight / 2})`} textAnchor="middle">{metric.unit || metric.label}</text>
        <text className="axis-title" x={left + plotWidth / 2} y={height - 6} textAnchor="middle">Время матча</text>
      </svg>
    </div>
  </section>;
}

function formatAxisValue(value) {
  const absolute = Math.abs(value);
  if (absolute >= 1000) return `${(value / 1000).toFixed(absolute >= 10000 ? 0 : 1)}k`;
  return `${Math.round(value)}`;
}

function CombatBlock({ combat, evidence, index }) {
  return <section className="combat-block">
    <div className="combat-title"><time>{clock(durationSeconds(combat.startedAt))}</time><div><h3>{combat.ordinalLabel || `Бой ${index + 1}`} · {combat.initiator || 'Игрок'} атакует {combat.opponent || 'соперника'}</h3><p>{clock(durationSeconds(combat.startedAt))}–{clock(durationSeconds(combat.endedAt))}. {combat.location ? `Координаты команды: ${combat.location}.` : ''}</p></div></div>
    {evidence && <CombatEvidenceTable evidence={evidence} />}
    <div className="combat-participants">{(combat.participants || []).map(player => <div key={player.player}>
      <h4>{player.player}</h4>
      <dl>
        <dt>Армия в начале</dt><dd>{composition(player.armyBefore)}</dd>
        <dt>Новые юниты в интервале</dt><dd>{composition(player.additions)}</dd>
        <dt>Грейды</dt><dd>{listText(player.upgrades)}</dd>
        <dt>Технологии</dt><dd>{listText(player.technologies)}</dd>
        <dt>Боевые потери</dt><dd>{composition(player.unitsLost)}</dd>
        <dt>Рабочие</dt><dd>{composition(player.workersLost)}</dd>
        <dt>Здания</dt><dd>{composition(player.structuresLost)}</dd>
        <dt>Оборона</dt><dd>{composition(player.staticDefenseLost)}</dd>
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
      <h4>{side.label} <small>{side.completeness || '—'}</small></h4>
      <UnitEvidenceTable rows={side.totalRows || []} caption="Итого по стороне" />
      {(side.participants || []).map(participant => <div className="participant-evidence" key={participant.participantId}>
        <h5>{participant.player} <small>{participant.reconciliationStatus || '—'}</small></h5>
        <UnitEvidenceTable rows={participant.rows || []} caption="Боевые юниты" />
        <dl className="collateral-losses">
          <dt>Рабочие</dt><dd>{composition(participant.workerLosses)}</dd>
          <dt>Здания</dt><dd>{composition(participant.structureLosses)}</dd>
          <dt>Оборона</dt><dd>{composition(participant.staticDefenseLosses)}</dd>
        </dl>
      </div>)}
    </section>)}
    {(evidence.notes || []).length > 0 && <p className="evidence-note">{evidence.notes.join(' ')}</p>}
  </div>;
}

function UnitEvidenceTable({ rows, caption }) {
  if (!rows.length) return <p className="muted">{caption}: нет боевых юнитов.</p>;
  return <div className="unit-table-wrap" role="region" aria-label={caption}>
    <table className="unit-evidence-table">
      <caption>{caption}</caption>
      <thead><tr><th>Юнит</th><th>Старт</th><th>Новые</th><th>Потери</th><th>Финиш</th><th>Kills</th></tr></thead>
      <tbody>{rows.map(row => <tr key={row.unit}>
        <th scope="row">{row.unit}<small>{row.completeness || '—'}</small></th>
        <td>{row.startCount}</td>
        <td>{row.additions}</td>
        <td>{row.losses}</td>
        <td>{row.endCount}</td>
        <td>{countEvidence(row.creditedKills)}</td>
      </tr>)}</tbody>
    </table>
  </div>;
}

function countEvidence(value) {
  if (!value || value.value == null) return 'нет данных';
  return value.value;
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
