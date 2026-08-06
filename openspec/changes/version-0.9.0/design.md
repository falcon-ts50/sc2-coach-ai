# Design — версия 0.9.0

## Current state

Текущий React-отчёт линейный: ход матча, графики, детализация интервала, история боёв, поворотные моменты и рекомендации расположены последовательно. Backend уже владеет participant identity, metric comparisons, MatchFlow, combat evidence, reconciliation и completeness.

## Target information architecture

```text
Match header
  -> summary KPI row
  -> primary metric workspace
       -> metric tabs
       -> synchronized chart
       -> turning-point episode strip
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

0.9.0 не вводит strategic result inference. Официальный результат и измеренное конечное состояние остаются разными понятиями. Kill credit конкретного юнита не показывается без stable killer-unit identity. Мини-карта боя не отображается без валидированных координат и production spatial clustering.

## Responsive behaviour

Desktop использует dashboard grid с главным графиком и боковой/нижней панелью эпизода. На tablet и mobile блоки складываются вертикально:

1. match header;
2. KPI carousel/grid;
3. metric tabs and chart;
4. episode cards;
5. selected episode details;
6. timeline.

Таблицы допускают локальный horizontal scroll только когда упрощение уничтожит смысл.

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
