# Design — версия 0.9.0

## Current state

Текущий React-отчёт линейный: ход матча, графики, детализация интервала, история боёв, поворотные моменты и рекомендации расположены последовательно. Backend уже владеет participant identity, metric comparisons, MatchFlow, combat evidence, reconciliation и completeness.

## Target information architecture

```text
Match header
  -> summary KPI row
  -> desktop dashboard grid
       -> left episode/navigation rail
       -> central primary metric workspace
            -> metric tabs
            -> synchronized chart
            -> episode markers
       -> right insight column
            -> army composition
            -> categorized losses
            -> base/economy status
       -> selected episode workspace
            -> episode summary and metric deltas
            -> related combat evidence
            -> force table
            -> related events
  -> unified timeline
  -> secondary report sections / exports
```

## Design decisions

### 1. Evidence episode is the selection unit

Интерфейс хранит один `selectedEpisodeId`, а не независимые выбранные phase, combat и turning point.

Backend-owned episode должен содержать или однозначно ссылаться на:

- stable ID;
- start/end time;
- title and category;
- importance/confidence/completeness;
- metric deltas;
- related MatchFlow interval IDs;
- related combat IDs;
- related turning-point IDs;
- participant state references before and after.

Frontend только отображает и синхронизирует ссылки.

### 2. Summary metrics belong to backend

Верхние KPI вычисляются в domain layer и сериализуются как visualization-ready facts. React не ищет максимум, крупнейший бой или момент максимального преимущества по сырым сериям.

Первая версия KPI:

- official result;
- match duration;
- maximum army value by side/player;
- economy peak or representative economy comparison;
- total categorized losses;
- maximum occupied supply;
- largest reconstructed combat;
- strongest measured positive and negative swing.

Каждая метрика содержит label, value, comparison value, unit, evidence timestamp/range, completeness и limitations.

### 3. One primary chart with metric tabs

На desktop одновременно показывается один крупный comparative chart. Вкладки переключают army value, economy proxy, occupied supply и другие доступные метрики. Participant identity, colours, line styles, hover time и selected range сохраняются между вкладками.

### 4. Turning points become navigation

Ключевые эпизоды отображаются компактной горизонтальной лентой. Карточка содержит время, короткий backend-owned заголовок, категорию, основную delta и importance. Нажатие выбирает episode и синхронизирует workspace.

Лента не должна быть механической проекцией каждого боя или каждого MatchFlow interval. Для обычной полной игры целевой результат — 4–6 человекочитаемых эпизодов. Короткий бой или короткий всплеск метрики становится evidence внутри более крупного эпизода, если только это не единственное решающее событие короткой партии.

### 5. Selected episode workspace

Панель показывает только доказательства выбранного эпизода:

- краткое описание наблюдаемого изменения;
- значения до/после;
- связанные бои;
- team-aware force table;
- потери combat units, workers, infrastructure и static defence;
- completeness/reconciliation warnings;
- события на временном отрезке.

### 6. No unsupported claims

0.9.0 не вводит strategic result inference. Официальный результат и измеренное конечное состояние остаются разными понятиями, но inferred/factual result не выводится. Kill credit конкретного юнита не показывается без stable killer-unit identity. Мини-карта боя не отображается без валидированных координат и production spatial clustering.

### 7. Desktop-first visual contract

StarCraft II replay review is primarily a desktop workflow. The 0.9.0 implementation optimizes the report for wide monitors first, then provides a readable narrow-screen fallback.

Desktop acceptance is part of the feature, not a styling afterthought:

- at 1440px and 1920px widths the first viewport shows the match header, KPI row, primary chart workspace, episode navigation and useful selected-episode context;
- the main chart is the visual anchor and is not split into several competing large graphs;
- the right insight column shows compact secondary facts such as army composition, categorized losses and base/economy status;
- selected-episode details align to the top of their grid area, even when one side has little or no evidence;
- empty states are compact and do not create large centered blank spaces;
- user-facing text excludes internal ADR references, raw enum names, build-contract jargon and duplicated completeness labels;
- the dark theme preserves readable contrast for chart grid, labels, tooltips, tabs, timelines and selected ranges.

Mobile is not the primary product surface for 0.9.0. Narrow screens must remain usable enough to inspect an analysis, but they do not drive layout density, information ordering or feature scope.

### 8. Coarse episode segmentation

Evidence episodes aggregate MatchFlow intervals, combats and metric changes into larger story units. The assembly should prefer regime changes over event boundaries:

- smooth noisy metric series before choosing episode boundaries;
- use army value, economy proxy, occupied supply, combat intensity, losses and development evidence as a multi-signal basis;
- target 4–6 episodes for a normal-length match;
- enforce minimum episode duration unless the whole replay is short or the final decisive ending is itself short;
- merge combats shorter than roughly 20 seconds into a neighbouring episode as combat evidence;
- merge intervals shorter than roughly 45–60 seconds unless their before/after metric state shows a strong durable regime change;
- place boundaries around preparation, engagement and consequence, not tightly around the death-event window alone.

This segmentation is deterministic and backend-owned. React may choose display density, but it must not re-segment the match.

## Responsive behaviour

Desktop использует dashboard grid с главным графиком, левой/нижней навигацией эпизодов, правой insight-колонкой и selected episode workspace. На tablet и mobile блоки складываются вертикально:

1. match header;
2. KPI carousel/grid;
3. metric tabs and chart;
4. episode cards;
5. selected episode details;
6. timeline.

Таблицы допускают локальный horizontal scroll только когда упрощение уничтожит смысл.

Narrow-screen fallback должен сохранять доступ к данным, но не обязан повторять desktop dashboard как равноправный first-class сценарий.

## Migration strategy

1. Добавить новые additive backend contracts, сохранив текущие поля.
2. Ввести новый dashboard shell поверх существующих данных.
3. Перенести выбор интервала в episode selection model.
4. Сохранить старые секции как fallback до завершения parity.
5. Удалять дубли только после benchmark и visual acceptance.

## Risks

- чрезмерная плотность dashboard;
- дублирование MatchFlow и Episode concepts;
- frontend-derived metrics при неполном контракте;
- ложное ощущение точности из-за красивой визуализации;
- деградация mobile usability;
- перегрузка верхней части страницы второстепенными KPI.

## Mitigations

- ограничить первый экран 4–6 KPI;
- сделать episode агрегирующим view model, а не новым analytical engine;
- все выводимые значения снабжать evidence references;
- явно отображать completeness и limitations;
- проверять desktop и mobile на одном эталонном support bundle.
- фиксировать visual acceptance screenshots на 1440px и 1920px до merge.
