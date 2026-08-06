import assert from 'node:assert/strict';
import test from 'node:test';

import { buildMarkdown, composition, reconciliationText } from './reporting.js';

test('renders chronological combat history with additions', () => {
  const markdown = buildMarkdown({
    focusPlayer: 'Lulu',
    map: 'Test Map',
    gameSeconds: 600,
    coachFeed: { headline: 'Готово.', cards: [], nextGameRecommendations: [] },
    combats: [
      {
        ordinalLabel: 'Бой 1',
        startedAt: 'PT95S',
        endedAt: 'PT145S',
        initiator: 'Frontdoor',
        opponent: 'Lulu',
        participants: [{
          player: 'Lulu',
          armyBefore: { Zergling: 2 },
          additions: { Zergling: 16 },
          unitsLost: { Zergling: 3 },
          workersLost: {},
          structuresLost: {},
          staticDefenseLost: {},
          armyAfter: { Zergling: 15 },
          armyValueBefore: 575,
          armyValueAfter: 900,
          reconciliationStatus: 'EXACT',
        }],
      },
    ],
  });

  assert.match(markdown, /## История боёв/);
  assert.match(markdown, /### Бой 1 · 1:35–2:25/);
  assert.match(markdown, /Новые юниты в интервале: 16 × Zergling/);
  assert.match(markdown, /Сверка: точная/);
});

test('renders backend-owned narrative analysis without strategic inference', () => {
  const markdown = buildMarkdown({
    focusPlayer: 'dragonDriver',
    map: 'Test Map',
    gameSeconds: 900,
    coachFeed: { headline: 'Готово.', cards: [], nextGameRecommendations: [] },
    narrativeAnalysis: {
      officialReplayResult: 'Win',
      status: 'PRELIMINARY',
      strategicResultStatus: 'NOT_EVALUATED',
      focusTeamPlayers: ['Lulu', 'dragonDriver'],
      summary: { verdict: 'Официальный результат есть, стратегический итог не вычисляется.' },
      timeline: {
        phases: [{
          startedAt: 'PT0S',
          endedAt: 'PT420S',
          title: 'Раннее давление',
          summary: 'Контекст показывает спад.',
        }],
        causalLinks: [{
          kind: 'PRECEDED',
          statement: 'Предыдущая фаза предшествует следующей, но не доказывает причинность.',
        }],
      },
      chart: {
        series: [{
          label: 'Стоимость армии',
          completeness: 'COMPLETE',
          points: [{ at: 'PT0S', value: 0 }, { at: 'PT420S', value: 900 }],
        }],
      },
      matchFlow: {
        matchStartedAt: 'PT0S',
        matchEndedAt: 'PT10M',
        intervals: [
          {
            id: 'match-flow-000',
            kind: 'COMBAT',
            title: 'Боевой интервал',
            startedAt: 'PT6M13S',
            endedAt: 'PT7M3S',
            confidence: 0.78,
            completeness: 'PARTIAL',
            summary: 'Бой и развитие.',
            drilldown: {
              combat: { combatIds: ['combat-2'], emptyStates: [] },
              development: {
                macro: { metrics: [{ metric: 'armyValue', startValue: 100, endValue: 180, delta: 80 }] },
                production: { observations: ['Lulu: new combat units became available during the interval: +16 Zergling.'] },
              },
            },
          },
          {
            id: 'match-flow-001',
            kind: 'LOW_EVIDENCE',
            title: 'Низкая доказательность',
            startedAt: 'PT7M3S',
            endedAt: 'PT10M',
            confidence: 0.38,
            completeness: 'PARTIAL',
            summary: 'Нет уверенных событий.',
            drilldown: {
              combat: { combatIds: [], emptyStates: ['Боёв в этом интервале не обнаружено.'] },
              development: { emptyStates: ['Экономических, производственных, технологических или разведывательных событий в этом интервале не обнаружено.'] },
            },
          },
        ],
      },
      evidence: {
        participants: [
          { id: 'participant-dragondriver', displayName: 'dragonDriver', relationship: 'SELECTED' },
          { id: 'participant-lulu', displayName: 'Lulu', relationship: 'TEAMMATE' },
        ],
        metricComparisons: [{
          label: 'Стоимость армии',
          completeness: 'COMPLETE',
          series: [
            {
              participantId: 'participant-dragondriver',
              completeness: 'COMPLETE',
              lineStyle: 'solid',
              points: [{ at: 'PT0S', value: 100 }, { at: 'PT420S', value: 900 }],
            },
            {
              participantId: 'participant-lulu',
              completeness: 'PARTIAL',
              lineStyle: 'dashed',
              points: [{ at: 'PT0S', value: 80 }, { at: 'PT420S', value: 700 }],
            },
          ],
        }],
        combats: [{
          label: 'Бой 2',
          startedAt: 'PT373S',
          endedAt: 'PT423S',
          sides: [{
            label: 'Команда фокуса',
            completeness: 'COMPLETE',
            totalRows: [{
              unit: 'Zergling',
              startCount: 2,
              additions: 16,
              losses: 3,
              endCount: 15,
              creditedKills: { value: null, completeness: 'UNAVAILABLE' },
              reconciliationStatus: 'EXACT',
            }],
            participants: [{
              player: 'Lulu',
              reconciliationStatus: 'EXACT',
              completeness: 'COMPLETE',
              rows: [{
                unit: 'Zergling',
                startCount: 2,
                additions: 16,
                losses: 3,
                endCount: 15,
                creditedKills: { value: null, completeness: 'UNAVAILABLE' },
                reconciliationStatus: 'EXACT',
              }],
              workerLosses: { Drone: 1 },
              structureLosses: {},
              staticDefenseLosses: {},
            }],
          }],
          notes: ['Kill attribution is unavailable.'],
        }],
      },
      limitations: ['Replay does not prove intent.'],
    },
    combats: [],
  });

  assert.match(markdown, /## Narrative Analysis/);
  assert.match(markdown, /Strategic result: NOT_EVALUATED/);
  assert.match(markdown, /Раннее давление/);
  assert.match(markdown, /### Непрерывный ход матча/);
  assert.match(markdown, /Покрытие: 0:00–10:00, интервалов: 2/);
  assert.match(markdown, /Бои: combat-2/);
  assert.match(markdown, /Развитие: Стоимость армии 100 → 180 \(\+80\)/);
  assert.match(markdown, /Боёв в этом интервале не обнаружено/);
  assert.match(markdown, /Экономических, производственных, технологических или разведывательных событий/);
  assert.match(markdown, /Стоимость армии: COMPLETE, 2 точек/);
  assert.match(markdown, /dragonDriver \(фокус\): COMPLETE, solid, 2 точек/);
  assert.match(markdown, /Lulu \(союзник\): PARTIAL, dashed, 2 точек/);
  assert.match(markdown, /Zergling: старт 2, новые 16, потери 3, финиш 15, kills нет данных, EXACT/);
  assert.match(markdown, /Рабочие: 1 × Drone/);
  assert.doesNotMatch(markdown, /guaranteed win|caused/i);
});

test('distinguishes empty and unavailable values', () => {
  assert.equal(composition({}), 'нет');
  assert.equal(composition(null, true), 'нет данных');
});

test('renders partial reconciliation evidence without hiding the card', () => {
  assert.equal(reconciliationText({
    reconciliationStatus: 'PARTIAL',
    reconciliationIssues: [{
      unit: 'Marine',
      startCount: 1,
      additions: 0,
      losses: 2,
      expectedEndCount: -1,
      actualEndCount: 0,
    }],
  }), 'неполные данные: Marine: 1 + 0 - 2 = -1, в конце 0');
});
