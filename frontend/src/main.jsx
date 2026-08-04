import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
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
      const response = await fetch('/api/v1/analyses', { method: 'POST', body });
      if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        throw new Error(problem.detail || 'Не удалось проанализировать реплей');
      }
      setAnalysis(await response.json());
      setStatus('Анализ готов');
    } catch (error) {
      setStatus(error.message);
    } finally {
      setLoading(false);
    }
  }

  function downloadMarkdown() {
    if (!analysis) return;
    const feed = analysis.coachFeed || {};
    const lines = [
      '# SC2 Coach Report', '',
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
    const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'sc2-coach-report.md';
    link.click();
    URL.revokeObjectURL(link.href);
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
          <input type="file" accept=".SC2Replay" onChange={e => setFile(e.target.files?.[0] || null)} />
          <strong>{file ? file.name : 'Выберите .SC2Replay'}</strong>
          <span>Реплей хранится только во временной директории</span>
        </label>
        <button disabled={!file || loading}>{loading ? 'Анализируем…' : 'Запустить анализ'}</button>
      </form>
      {status && <p className={status === 'Анализ готов' ? 'status success' : 'status'}>{status}</p>}
    </section>

    {analysis && <Report analysis={analysis} onDownload={downloadMarkdown} />}
  </main>;
}

function Report({ analysis, onDownload }) {
  const summary = analysis.matchContext?.summary || {};
  const feed = analysis.coachFeed || {};
  const ranking = analysis.comparison?.ranking || [];
  return <>
    <section className="summary-grid">
      <Metric label="Карта" value={analysis.map || '—'} />
      <Metric label="Длительность" value={clock(analysis.gameSeconds)} />
      <Metric label="Лидер в конце" value={summary.finalLeaderName || analysis.comparison?.leader || '—'} />
      <Metric label="Уверенность" value={summary.confidence || analysis.comparison?.confidence || '—'} />
    </section>

    <section className="panel feed">
      <div className="section-heading"><div><span>Главное за матч</span><h2>{feed.headline || 'Coach Feed'}</h2></div><button className="secondary" onClick={onDownload}>Скачать Markdown</button></div>
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

function Metric({ label, value }) { return <div className="metric"><span>{label}</span><strong>{value}</strong></div>; }
function durationSeconds(value) { const m = String(value || '').match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/); return m ? Number(m[1] || 0) * 3600 + Number(m[2] || 0) * 60 + Number(m[3] || 0) : Number(value || 0); }
function clock(value) { const seconds = Math.round(Number(value || 0)); return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`; }
function round(value) { return Math.round(Number(value || 0) * 10) / 10; }
function signed(value) { const result = round(value); return `${result > 0 ? '+' : ''}${result}%`; }
function component(value) { return ({ army: 'Армия', economy: 'Экономика', supply: 'Снабжение' })[value] || value; }

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
