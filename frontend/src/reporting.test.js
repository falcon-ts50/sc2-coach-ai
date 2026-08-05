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
