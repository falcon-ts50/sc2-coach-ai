import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import JSZip from 'jszip';
import './styles.css';

const impactLabels = {
  GAME_CHANGING: 'Меняет игру',
  HIGH: 'Высокое',
  MEDIUM: 'Среднее',
  LOW: 'Низкое'
};

function App() {
  const [file, setFile] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    if (!file) return;
    setLoading(true);
    setStatus('Декодируем реплей и собираем отчёт…');
    setAnalysis(null);
    try {
      const body = new FormData();
      body.append('replay', file);
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 120000);
      const response = await fetch('/api/v1/analyses', { method: 'POST', body, signal: controller.signal });
      clearTimeout(timeout);
      if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        throw new Error(problem.detail || 'Не удалось проанализировать реплей');
      }
      setAnalysis(await response.json());
      setStatus('Анализ готов');
    } catch (error) {
      setStatus(error.name === 'AbortError'
        ? 'Анализ занял больше двух минут. Попробуйте ещё раз или сообщите об ошибке.'
        : error.message);
    } finally {
      setLoading(false);
    }
  }

  function buildMarkdown() {
    if (!analysis) return '';
    const feed = analysis.coachFeed || {};
    const diagnostics = analysis.diagnostics || {};
    const lines = [
      '# SC2 Coach Report', '',
      `**Analysis ID:** ${diagnostics.analysisId || '—'}`,
      `**Версия:** ${diagnostics.applicationVersion || '—'} (${diagnostics.gitCommit || 'unknown'})`,
      `**Карта:** ${analysis.map || '—'}`,
      `**Длительность:** ${clock(analysis.gameSeconds)}`, '',
      '## Итог', '', feed.headline || 'Недостаточно данных.', '',
      '## Coach Feed', ''
    ];
    (feed.cards || []).forEach(card => {
      lines.push(`### ${clock(durationSeconds(card.at))} — ${card.title}`);
      lines.push(`- Влияние: ${impactLabels[card.impact] || card.impact}`);
      lines.push(`- Уверенность: ${Math.round((card.confidence || 0) * 100)}%`);
      lines.push('', card.explanation, '');
    });
    lines.push('## На следующую игру', '');
    (feed.nextGameRecommendations || []).forEach((item, index) => lines.push(`${index + 1}. ${item}`));
    return lines.join('\n');
  }

  function downloadMarkdown() {
    if (!analysis) return;
    downloadText(buildMarkdown(), 'sc2-coach-report.md');
  }

  function downloadTranscript() {
    if (!analysis?.transcriptMarkdown) return;
    downloadText(analysis.transcriptMarkdown, 'sc2-coach-replay-transcript.md');
  }

  async function downloadSupportBundle() {
    if (!analysis) return;
    const zip = new JSZip();
    zip.file('report.md', buildMarkdown());
    zip.file('transcript.md', analysis.transcriptMarkdown || '# Transcript unavailable\n');
    zip.file('analysis-response.json', JSON.stringify(analysis, null, 2));
    zip.file('metadata.json', JSON.stringify(analysis.diagnostics || {}, null, 2));
    const blob = await zip.generateAsync({ type: 'blob', compression: 'DEFLATE' });
    downloadBlob(blob, `sc2-coach-support-${analysis.diagnostics?.analysisId || 'unknown'}.zip`);
  }

  return <main>
    <header className="hero">
      <div className="eyebrow">REPLAY INTELLIGENCE</div>
      <h1>SC2 Coach</h1>
      <p>Загрузите реплей. Получите сравнение игроков, переломы матча и практические советы.</p>
    </header>

    <section className="panel upload-panel">
      <form onSubmit={submit}>
        <label className="dropzone">
          <input type="file" accept=".SC2Replay" disabled={loading} onChange={e => setFile(e.target.files?.[0] || null)} />
          <strong>{file ? file.name : 'Выберите .SC2Replay'}</strong>
          <span>Реплей хранится только во временной директории</span>
        </label>
        <button disabled={!file || loading}>{loading ? 'Анализируем…' : 'Запустить анализ'}</button>
      </form>
      {status && <p className={status === 'Анализ готов' ? 'status success' : 'status'}>{status}</p>}
    </section>

    {analysis && <Report analysis={analysis} onDownload={downloadMarkdown} onTranscript={downloadTranscript} onSupportBundle={downloadSupportBundle} />}
  </main>;
}

function Report({ analysis, onDownload, onTranscript, onSupportBundle }) {
  const summary = analysis.matchContext?.summary || {};
  const feed = analysis.coachFeed || {};
  const ranking = analysis.comparison?.ranking || [];
  const diagnostics = analysis.diagnostics || {};
  return <>
    <section className="summary-grid">
      <Metric label="Карта" value={analysis.map || '—'} />
      <Metric label="Длительность" value={clock(analysis.gameSeconds)} />
      <Metric label="Лидер в конце" value={summary.finalLeaderName || analysis.comparison?.leader || '—'} />
      <Metric label="Время анализа" value={`${diagnostics.totalTimeMs || 0} мс`} />
    </section>

    <section className="panel feed">
      <div className="section-heading"><div><span>Главное за матч</span><h2>{feed.headline || 'Coach Feed'}</h2></div><div className="actions"><button className="secondary" onClick={onDownload}>Скачать отчёт</button><button className="secondary" disabled={!analysis.transcriptMarkdown} onClick={onTranscript}>Скачать расшифровку для ИИ</button><button className="secondary" onClick={onSupportBundle}>Скачать support bundle</button></div></div>
      <p className="status">Analysis ID: <strong>{diagnostics.analysisId || '—'}</strong> · версия {diagnostics.applicationVersion || '—'} · commit {diagnostics.gitCommit || 'unknown'}</p>
      <div className="feed-grid">
        {(feed.cards || []).map((card, index) => <article className={`feed-card kind-${String(card.kind).toLowerCase()}`} key={`${card.at}-${index}`}>
          <div className="card-meta"><span>{clock(durationSeconds(card.at))}</span><span>{impactLabels[card.impact] || card.impact}</span></div>
          <h3>{card.title}</h3><p>{card.explanation}</p>
          <div className="confidence"><i style={{ width: `${Math.round((card.confidence || 0) * 100)}%` }} />Уверенность {Math.round((card.confidence || 0) * 100)}%</div>
        </article>)}
      </div>
      <h3 className="subheading">На следующую игру</h3>
      <ol className="recommendations">{(feed.nextGameRecommendations || []).map(item => <li key={item}>{item}</li>)}</ol>
    </section>

    <section className="panel">
      <div className="section-heading"><div><span>Контекст</span><h2>Переломные моменты</h2></div></div>
      <div className="timeline">{(analysis.turningPoints || []).map((point, index) => <article key={`${point.at}-${index}`}>
        <time>{clock(durationSeconds(point.at))}</time><div><h3>{point.previousLeaderName || '—'} → {point.newLeaderName || '—'}</h3><p>Сдвиг преимущества: {round(point.scoreSwing)} · {point.severity}</p><small>{(point.reasons || []).map(r => `${component(r.component)} ${signed(r.change)}`).join(' · ')}</small></div>
      </article>)}</div>
    </section>

    <section className="panel table-panel"><div className="section-heading"><div><span>Сравнение</span><h2>Игроки</h2></div></div>
      <div className="table-wrap"><table><thead><tr><th>#</th><th>Игрок</th><th>Раса</th><th>Итог</th><th>Экономика</th><th>Армия</th><th>Эффективность</th></tr></thead><tbody>
        {ranking.map((player, index) => <tr key={`${player.name}-${index}`}><td>{index + 1}</td><td><strong>{player.name}</strong></td><td>{player.race}</td><td>{round(player.score)}</td><td>{round(player.economy)}</td><td>{round(player.army)}</td><td>{round(player.efficiency)}</td></tr>)}
      </tbody></table></div>
    </section>
  </>;
}

function downloadText(text, filename) { downloadBlob(new Blob([text], { type: 'text/markdown;charset=utf-8' }), filename); }
function downloadBlob(blob, filename) { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = filename; link.click(); URL.revokeObjectURL(link.href); }
function Metric({ label, value }) { return <div className="metric"><span>{label}</span><strong>{value}</strong></div>; }
function durationSeconds(value) { const m = String(value || '').match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/); return m ? Number(m[1] || 0) * 3600 + Number(m[2] || 0) * 60 + Number(m[3] || 0) : Number(value || 0); }
function clock(value) { const seconds = Math.round(Number(value || 0)); return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`; }
function round(value) { return Math.round(Number(value || 0) * 10) / 10; }
function signed(value) { const result = round(value); return `${result > 0 ? '+' : ''}${result}%`; }
function component(value) { return ({ army: 'Армия', economy: 'Экономика', supply: 'Снабжение' })[value] || value; }

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
