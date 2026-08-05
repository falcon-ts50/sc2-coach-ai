import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import JSZip from 'jszip';
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
      setStatus(error.name === 'AbortError'
        ? 'Анализ занял больше двух минут. Попробуйте ещё раз.'
        : error.message);
    } finally {
      setLoading(false);
    }
  }

  function buildMarkdown() {
    if (!analysis) return '';
    const lines = [
      '# SC2 Coach Report', '',
      `**Разбор для:** ${analysis.focusPlayer || '—'}`,
      `**Карта:** ${analysis.map || '—'}`,
      `**Длительность:** ${clock(analysis.gameSeconds)}`, '',
      '## Итог', '', analysis.coachFeed?.headline || 'Недостаточно данных.', '',
      '## Как развивался матч', '', narrativeText(analysis), '',
      '## Ключевые бои', ''
    ];
    (analysis.combats || []).forEach(combat => {
      lines.push(`### ${clock(durationSeconds(combat.startedAt))} — ${combat.initiator || 'Игрок'} атакует ${combat.opponent || 'соперника'}`);
      lines.push(`Победитель эпизода: **${combat.winner || 'не определён'}**.`);
      (combat.participants || []).forEach(player => {
        lines.push('', `**${player.player}**`, `- До боя: ${composition(player.armyBefore)}`, `- После боя: ${composition(player.armyAfter)}`, `- Потери: ${composition(player.unitsLost)}`);
      });
      lines.push('');
    });
    lines.push('## Что сделать в следующей игре', '');
    (analysis.coachFeed?.nextGameRecommendations || []).forEach((item, index) => lines.push(`${index + 1}. ${item}`));
    return lines.join('\n');
  }

  function downloadMarkdown() { downloadText(buildMarkdown(), 'sc2-coach-report.md'); }
  function downloadTranscript() { downloadText(analysis.transcriptMarkdown, 'sc2-coach-replay-transcript.md'); }
  async function downloadSupportBundle() {
    const zip = new JSZip();
    zip.file('report.md', buildMarkdown());
    zip.file('transcript.md', analysis.transcriptMarkdown || '# Transcript unavailable\n');
    zip.file('analysis-response.json', JSON.stringify(analysis, null, 2));
    zip.file('metadata.json', JSON.stringify(analysis.diagnostics || {}, null, 2));
    downloadBlob(await zip.generateAsync({ type: 'blob', compression: 'DEFLATE' }), `sc2-coach-support-${analysis.diagnostics?.analysisId || 'unknown'}.zip`);
  }

  return <main>
    <header className="hero">
      <div className="eyebrow">REPLAY INTELLIGENCE</div>
      <h1>SC2 Coach</h1>
      <p>Послематчевый разбор решений, боёв и переломных моментов.</p>
    </header>

    <section className="panel upload-panel">
      <form onSubmit={analyze}>
        <label className="dropzone">
          <input type="file" accept=".SC2Replay" disabled={loading} onChange={e => setFile(e.target.files?.[0] || null)} />
          <strong>{file ? file.name : 'Выберите .SC2Replay'}</strong>
          <span>Файл удаляется после анализа</span>
        </label>
        <button disabled={!file || loading}>{loading ? 'Анализируем…' : 'Запустить анализ'}</button>
      </form>
      {status && <p className="status">{status}</p>}
    </section>

    {analysis && <Report
      analysis={analysis}
      focusPlayer={focusPlayer}
      setFocusPlayer={setFocusPlayer}
      rebuild={() => analyze(null, focusPlayer)}
      loading={loading}
      onDownload={downloadMarkdown}
      onTranscript={downloadTranscript}
      onSupportBundle={downloadSupportBundle}
    />}
  </main>;
}

function Report({ analysis, focusPlayer, setFocusPlayer, rebuild, loading, onDownload, onTranscript, onSupportBundle }) {
  const feed = analysis.coachFeed || {};
  const narrative = (feed.cards || []).find(card => card.title === 'Как развивался матч');
  const events = (feed.cards || []).filter(card => card !== narrative);

  return <article className="report">
    <section className="report-header panel">
      <div>
        <span className="eyebrow">РАЗБОР ДЛЯ ИГРОКА</span>
        <h2>{analysis.focusPlayer || focusPlayer}</h2>
        <p>{feed.headline}</p>
      </div>
      <div className="perspective">
        <label>Для кого сделать отчёт</label>
        <select value={focusPlayer} onChange={event => setFocusPlayer(event.target.value)}>
          {(analysis.players || []).map(player => <option key={player.pid} value={player.name}>{player.name} · {player.race}</option>)}
        </select>
        <button disabled={loading || focusPlayer === analysis.focusPlayer} onClick={rebuild}>Перестроить отчёт</button>
      </div>
    </section>

    <section className="report-section">
      <span className="section-number">01</span>
      <div><h2>Как развивался матч</h2><p className="lead">{narrative?.explanation || 'Недостаточно данных для связного сценария.'}</p></div>
    </section>

    <section className="report-section">
      <span className="section-number">02</span>
      <div className="wide"><h2>Ключевые бои</h2>
        <div className="combat-list">{(analysis.combats || []).length
          ? analysis.combats.map((combat, index) => <CombatBlock combat={combat} key={`${combat.startedAt}-${index}`} />)
          : <p className="muted">Не удалось надёжно восстановить отдельные боевые эпизоды.</p>}
        </div>
      </div>
    </section>

    <section className="report-section">
      <span className="section-number">03</span>
      <div className="wide"><h2>Переломные моменты</h2>
        <div className="story-list">{events.map((card, index) => <div className="story-row" key={`${card.at}-${index}`}>
          <time>{clock(durationSeconds(card.at))}</time>
          <div><h3>{card.title}</h3><p>{card.explanation}</p><small>Уверенность {Math.round((card.confidence || 0) * 100)}%</small></div>
        </div>)}</div>
      </div>
    </section>

    <section className="report-section">
      <span className="section-number">04</span>
      <div className="wide"><h2>Что изменить в следующей игре</h2>
        <ol className="next-actions">{(feed.nextGameRecommendations || []).map(item => <li key={item}>{item}</li>)}</ol>
      </div>
    </section>

    <footer className="report-footer panel">
      <div className="actions"><button onClick={onDownload}>Скачать отчёт</button><button onClick={onTranscript}>Расшифровка для ИИ</button><button onClick={onSupportBundle}>Support bundle</button></div>
      <small>Analysis ID: {analysis.diagnostics?.analysisId || '—'} · {analysis.diagnostics?.applicationVersion || '—'} · {analysis.diagnostics?.totalTimeMs || 0} мс</small>
    </footer>
  </article>;
}

function CombatBlock({ combat }) {
  return <section className="combat-block">
    <div className="combat-title"><time>{clock(durationSeconds(combat.startedAt))}</time><div><h3>{combat.initiator || 'Игрок'} атакует {combat.opponent || 'соперника'}</h3><p>{combat.winner ? `Бой выиграл ${combat.winner}.` : 'Победитель эпизода не определён.'}{combat.location ? ` Координаты: ${combat.location}.` : ''}</p></div></div>
    <div className="combat-participants">{(combat.participants || []).map(player => <div key={player.player}>
      <h4>{player.player}</h4>
      <dl><dt>До боя</dt><dd>{composition(player.armyBefore)}</dd><dt>После боя</dt><dd>{composition(player.armyAfter)}</dd><dt>Потери</dt><dd>{composition(player.unitsLost)}</dd><dt>Стоимость армии</dt><dd>{Math.round(player.armyValueBefore)} → {Math.round(player.armyValueAfter)}</dd></dl>
    </div>)}</div>
  </section>;
}

function narrativeText(analysis) { return (analysis.coachFeed?.cards || []).find(card => card.title === 'Как развивался матч')?.explanation || ''; }
function composition(value) { const entries = Object.entries(value || {}); return entries.length ? entries.map(([unit, count]) => `${count} × ${unit}`).join(', ') : 'данных недостаточно'; }
function downloadText(text, filename) { downloadBlob(new Blob([text || ''], { type: 'text/markdown;charset=utf-8' }), filename); }
function downloadBlob(blob, filename) { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = filename; link.click(); URL.revokeObjectURL(link.href); }
function durationSeconds(value) { const m = String(value || '').match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/); return m ? Number(m[1] || 0) * 3600 + Number(m[2] || 0) * 60 + Number(m[3] || 0) : Number(value || 0); }
function clock(value) { const seconds = Math.round(Number(value || 0)); return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`; }

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
