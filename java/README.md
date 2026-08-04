# SC2 Coach Java

The Java side owns product logic after replay decoding.

## Modules

- `coach-domain` — typed representation of `replay_analysis.json` and shared analysis contracts.
- `portal` — Spring Boot stateless upload API and orchestration of the Python decoder.

The Python decoder remains the adapter for `.SC2Replay`, `sc2reader`, `s2protocol` and MPQ details.

```text
.SC2Replay
    -> temporary workspace
    -> Python decoder
    -> replay_analysis.json
    -> Java coach-domain
    -> strategy / knowledge / comparison engines
    -> report model
    -> response
    -> workspace deletion
```

## Requirements

- JDK 25
- Maven 3.9+
- Python environment with the root decoder dependencies installed

## Build

```bash
cd java
mvn verify
```

## Run the portal

From the `java/` directory:

```bash
mvn -pl portal spring-boot:run
```

Configuration can be overridden with:

```bash
export SC2_COACH_PYTHON=/path/to/.venv/bin/python
export SC2_COACH_DECODER_SCRIPT=/path/to/sc2-coach-ai/analyze.py
export SC2_COACH_DECODER_TIMEOUT=2m
```

## Stateless upload API

```bash
curl -F 'replay=@match.SC2Replay' http://localhost:8080/api/v1/analyses
```

The request is currently synchronous. The portal creates a unique temporary workspace, runs the decoder, reads `replay_analysis.json` into Java records, returns a compact replay/player summary and deletes the workspace on success or failure.

## Contract boundary

`../contracts/replay-analysis.schema.json` is the language-neutral boundary. Python must produce it; Java consumes it. Contract changes require a schema-version change and fixtures for both implementations.
