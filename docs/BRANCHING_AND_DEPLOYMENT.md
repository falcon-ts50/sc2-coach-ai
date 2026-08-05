# Branching and Deployment

## Branches

- `develop` is the integration branch for feature work.
- Feature branches must open pull requests into `develop`.
- `main` is the production branch.
- Pull requests into `main` must come from `develop`.

The `main-source-guard` GitHub Actions workflow enforces the last rule for pull requests
targeting `main`.

## Production Deploy

Production runs on `nukle.nexus` from the `main` branch.

After a merge into `main`, the `deploy-production` workflow:

1. runs the Python test matrix;
2. builds the React frontend;
3. runs the Java/Maven verification;
4. builds the Docker production image in CI;
5. connects to the production host with the restricted `sc2deploy` user;
6. runs `/usr/local/sbin/sc2-coach-deploy-main`, which rebuilds and restarts the Docker
   container from `origin/main`.

The app container remains bound to `127.0.0.1:18080`; public traffic still goes through
Caddy and HTTPS.
