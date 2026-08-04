# SC2 Coach Java

The Java side owns product logic after replay decoding.

## Modules

- `coach-domain` — typed representation of `replay_analysis.json` and shared analysis contracts.
- `portal` — Spring Boot stateless upload API and, later, orchestration of decoder, analysis and report generation.

The Python decoder remains the adapter for `.SC2Replay`, `sc2reader`, `s2protocol` and MPQ details.

```text
.SC2Replay
    -> Python decoder
    -> replay_analysis.json
    -> Java coach-domain
    -> strategy / knowledge / comparison engines
    -> report model
    -> Spring Boot portal
```

## Requirements

- JDK 25
- Maven 3.9+

## Build

```bash
cd java
mvn verify
```

## Contract boundary

`../contracts/replay-analysis.schema.json` is the language-neutral boundary. Python must produce it; Java consumes it. Contract changes require a schema-version change and fixtures for both implementations.

## Current portal boundary

```http
POST /api/v1/analyses
Content-Type: multipart/form-data
replay=<file.SC2Replay>
```

The endpoint currently validates the upload boundary and returns `202 Accepted`. Decoder orchestration and temporary workspace lifecycle are the next implementation step.
