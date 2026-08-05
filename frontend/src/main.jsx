import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import JSZip from 'jszip';
import { buildMarkdown, clock, composition, durationSeconds, listText, narrativeText, reconciliationText } from './reporting.js';
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

  function downloadMarkdown() { downloadText(buildMarkdown(analysis), 'sc2-coach-report.md'); }
  function downloadTranscript() { downloadText(analysis.transcriptMarkdown, 'sc2-coach-replay-transcript.md'); }
  async function downloadSupportBundle() {
    const zip = new JSZip();
    zip.file('report.md', buildMarkdown(analysis));
    zip.file('transcript.md', analysis.transcriptMarkdown || '# Transcript unavailable\n');
    zip.file('analysis-response.json', JSON.stringify(analysis, null, 2));
    zip.file('metadata.json', JSON.stringify(analysis.diagnostics || {}, null, 2));
    downloadBlob(await zip.generateAsync({ type: 'blob', compression: 'DEFLATE' }), `sc2-coach-support-${analysis.diagnostics?.analysisId || 'unknown'}.zip`);
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
  const narrative = (feed.cards || []).find(card => card.title === 'Как развивался матч');
  const events = (feed.cards || []).filter(card => card !== narrative);
  return <article className="report">
    <section className="report-header panel"><div><span className="eyebrow">РАЗБОР ДЛЯ ИГРОКА</span><h2>{analysis.focusPlayer || focusPlayer}</h2><p>{feed.headline}</p></div><div className="perspective"><label>Для кого сделать отчёт</label><select value={focusPlayer} onChange={event => setFocusPlayer(event.target.value)}>{(analysis.players || []).map(player => <option key={player.pid} value={player.name}>{player.name} · {player.race}</option>)}</select><button disabled={loading || focusPlayer === analysis.focusPlayer} onClick={rebuild}>Перестроить отчёт</button></div></section>
    <NarrativeAnalysisSection narrative={analysis.narrativeAnalysis} />
    <section className="report-section"><span className="section-number">02</span><div><h2>Как развивался матч</h2><p className="lead">{narrative?.explanation || 'Недостаточно данных для связного сценария.'}</p></div></section>
    <section className="report-section"><span className="section-number">03</span><div className="wide"><h2>История боёв</h2><div className="combat-list">{(analysis.combats || []).length ? analysis.combats.map((combat, index) => <CombatBlock combat={combat} index={index} key={combat.id || `${combat.startedAt}-${index}`} />) : <p className="muted">Не удалось надёжно восстановить отдельные боевые эпизоды.</p>}</div></div></section>
    <section className="report-section"><span className="section-number">04</span><div className="wide"><h2>Переломные моменты</h2><div className="story-list">{events.map((card, index) => <div className="story-row" key={`${card.at}-${index}`}><time>{clock(durationSeconds(card.at))}</time><div><h3>{card.title}</h3><p>{card.explanation}</p><small>Уверенность {Math.round((card.confidence || 0) * 100)}%</small></div></div>)}</div></div></section>
    <section className="report-section"><span className="section-number">05</span><div className="wide"><h2>Что изменить в следующей игре</h2><ol className="next-actions">{(feed.nextGameRecommendations || []).map(item => <li key={item}>{item}</li>)}</ol></div></section>
    <footer className="report-footer panel"><div className="actions"><button onClick={onDownload}>Скачать отчёт</button><button onClick={onTranscript}>Расшифровка для ИИ</button><button onClick={onSupportBundle}>Support bundle</button></div><small>Analysis ID: {analysis.diagnostics?.analysisId || '—'} · {analysis.diagnostics?.applicationVersion || '—'} · {analysis.diagnostics?.totalTimeMs || 0} мс</small></footer>
  </article>;
}

function NarrativeAnalysisSection({ narrative }) {
  const [selectedPhaseId, setSelectedPhaseId] = useState(narrative?.timeline?.phases?.[0]?.id || '');
  if (!narrative) return null;
  const phases = narrative.timeline?.phases || [];
  const links = narrative.timeline?.causalLinks || [];
  const selected = phases.find(phase => phase.id === selectedPhaseId) || phases[0];
  return <section className="report-section"><span className="section-number">01</span><div className="wide">
    <h2>Narrative Analysis</h2>
    <div className="narrative-grid">
      <div className="narrative-summary">
        <div className="narrative-badges"><span>{narrative.status || 'PRELIMINARY'}</span><span>{narrative.strategicResultStatus || 'NOT_EVALUATED'}</span></div>
        <p className="lead">{narrative.summary?.verdict || 'Недостаточно данных для связного сценария.'}</p>
        <dl className="summary-facts">
          <dt>Официальный результат</dt><dd>{narrative.officialReplayResult || '—'}</dd>
          <dt>Команда</dt><dd>{(narrative.focusTeamPlayers || []).join(', ') || '—'}</dd>
        </dl>
      </div>
      <NarrativeChart chart={narrative.chart} selectedPhaseId={selected?.id} />
    </div>
    <div className="phase-list">{phases.map(phase => <button className={phase.id === selected?.id ? 'phase selected' : 'phase'} key={phase.id} onClick={() => setSelectedPhaseId(phase.id)}>
      <time>{clock(durationSeconds(phase.startedAt))}–{clock(durationSeconds(phase.endedAt))}</time>
      <strong>{phase.title}</strong>
      <span>{phase.summary}</span>
    </button>)}</div>
    {!!links.length && <div className="causal-chain"><h3>Сценарная цепочка</h3>{links.map(link => <p key={link.id}><strong>{link.kind}</strong> · {link.statement}</p>)}</div>}
    {!!(narrative.limitations || []).length && <ul className="limitations">{narrative.limitations.map(item => <li key={item}>{item}</li>)}</ul>}
  </div></section>;
}

function NarrativeChart({ chart, selectedPhaseId }) {
  const series = chart?.series || [];
  const duration = Math.max(1, durationSeconds(chart?.endedAt) - durationSeconds(chart?.startedAt));
  const maxValue = Math.max(1, ...series.flatMap(item => (item.points || []).map(point => Number(point.value || 0))));
  const width = 760;
  const height = 270;
  const pad = 32;
  const x = value => pad + (durationSeconds(value) - durationSeconds(chart?.startedAt)) / duration * (width - pad * 2);
  const y = value => height - pad - (Number(value || 0) / maxValue) * (height - pad * 2);
  const colors = ['#74d7ff', '#ffd166', '#9bf6a5'];
  return <div className="chart-wrap">
    <svg className="narrative-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Match overview chart">
      {(chart?.phaseIntervals || []).map(interval => <rect key={interval.id} className={interval.phaseId === selectedPhaseId ? 'phase-band active' : 'phase-band'} x={x(interval.from)} y="16" width={Math.max(2, x(interval.to) - x(interval.from))} height={height - 36} />)}
      {[0, .25, .5, .75, 1].map(step => <line key={step} className="grid-line" x1={pad} x2={width - pad} y1={pad + step * (height - pad * 2)} y2={pad + step * (height - pad * 2)} />)}
      {series.map((item, index) => <polyline key={item.id} fill="none" stroke={colors[index % colors.length]} strokeWidth="3" points={(item.points || []).map(point => `${x(point.at)},${y(point.value)}`).join(' ')} />)}
      {(chart?.markers || []).map(marker => <g key={marker.id}><line className="marker-line" x1={x(marker.at)} x2={x(marker.at)} y1="20" y2={height - 18} /><text className="marker-label" x={x(marker.at) + 4} y="26">{marker.kind === 'COMBAT' ? 'бой' : 'перелом'}</text></g>)}
    </svg>
    <div className="chart-legend">{series.map((item, index) => <span key={item.id}><i style={{ background: colors[index % colors.length] }} />{item.label}</span>)}</div>
  </div>;
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
