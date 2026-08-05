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
  const [selectedPhaseId, setSelectedPhaseId] = useState(narrative?.timeline?.phases?.[0]?.id || '');
  if (!narrative) return null;
  const phases = narrative.timeline?.phases || [];
  const selected = phases.find(phase => phase.id === selectedPhaseId) || phases[0];
  const chart = withOverallScoreSeries(narrative.chart, narrative.timeline?.snapshots || []);
  const team = (narrative.focusTeamPlayers || []).join(', ') || '—';

  return <section className="report-section"><span className="section-number">01</span><div className="wide">
    <h2>Ход матча</h2>
    <div className="narrative-summary">
      <p className="lead">Официальный результат для {narrative.focusPlayer || 'игрока'}: <strong>{narrative.officialReplayResult || 'не определён'}</strong>.</p>
      <dl className="summary-facts"><dt>Команда</dt><dd>{team}</dd></dl>
    </div>
    <div className="phase-list">{phases.map(phase => <button className={phase.id === selected?.id ? 'phase selected' : 'phase'} key={phase.id} onClick={() => setSelectedPhaseId(phase.id)}>
      <time>{clock(durationSeconds(phase.startedAt))}–{clock(durationSeconds(phase.endedAt))}</time>
      <strong>{phase.title}</strong>
      <span>{phase.summary}</span>
    </button>)}</div>
    <div className="charts-stack">{(chart?.series || []).map((series, index) => <NarrativeChart key={series.id} chart={chart} series={series} selectedPhaseId={selected?.id} colorIndex={index} />)}</div>
  </div></section>;
}

function withOverallScoreSeries(chart, snapshots) {
  if (!chart) return chart;
  const existing = chart.series || [];
  if (existing.some(series => series.id === 'overallScore')) return chart;
  const points = snapshots.map(snapshot => ({ at: snapshot.at, value: Number(snapshot.metrics?.overallScore || 0) }));
  return { ...chart, series: [...existing, { id: 'overallScore', label: 'Общее преимущество', unit: 'баллы', points }] };
}

function NarrativeChart({ chart, series, selectedPhaseId, colorIndex }) {
  const points = series.points || [];
  const start = durationSeconds(chart?.startedAt);
  const end = durationSeconds(chart?.endedAt);
  const duration = Math.max(1, end - start);
  const values = points.map(point => Number(point.value || 0));
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
  const colors = ['#74d7ff', '#ffd166', '#9bf6a5', '#c69cff'];
  const color = colors[colorIndex % colors.length];
  const xTicks = [0, .25, .5, .75, 1];
  const yTicks = [0, .25, .5, .75, 1];

  return <section className="chart-card">
    <header className="chart-title"><h3>{series.label}</h3><span>{series.unit || ''}</span></header>
    <div className="chart-scroll">
      <svg className="narrative-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`${series.label} по времени матча`}>
        {(chart?.phaseIntervals || []).map(interval => <rect key={interval.id} className={interval.phaseId === selectedPhaseId ? 'phase-band active' : 'phase-band'} x={x(interval.from)} y={top} width={Math.max(2, x(interval.to) - x(interval.from))} height={plotHeight} />)}
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
        <polyline fill="none" stroke={color} strokeWidth="4" strokeLinejoin="round" strokeLinecap="round" points={points.map(point => `${x(point.at)},${y(point.value)}`).join(' ')} />
        {(chart?.markers || []).map(marker => <line key={marker.id} className="marker-line" x1={x(marker.at)} x2={x(marker.at)} y1={top} y2={height - bottom} />)}
        <text className="axis-title" x={18} y={top + plotHeight / 2} transform={`rotate(-90 18 ${top + plotHeight / 2})`} textAnchor="middle">{series.unit || series.label}</text>
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

function CombatBlock({ combat, index }) {
  return <section className="combat-block">
    <div className="combat-title"><time>{clock(durationSeconds(combat.startedAt))}</time><div><h3>{combat.ordinalLabel || `Бой ${index + 1}`} · {combat.initiator || 'Игрок'} атакует {combat.opponent || 'соперника'}</h3><p>{clock(durationSeconds(combat.startedAt))}–{clock(durationSeconds(combat.endedAt))}. {combat.location ? `Координаты команды: ${combat.location}.` : ''}</p></div></div>
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

function downloadText(text, filename) { downloadBlob(new Blob([text || ''], { type: 'text/markdown;charset=utf-8' }), filename); }
function downloadBlob(blob, filename) { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = filename; link.click(); URL.revokeObjectURL(link.href); }

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
