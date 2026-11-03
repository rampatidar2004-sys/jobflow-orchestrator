# Security Policy

## Supported versions

This is a demonstration/portfolio project, not a maintained production
library. There are no guaranteed supported versions or backports.

## Reporting a vulnerability

If you find a security issue (e.g. an authentication bypass, an injection
vector, or a way to escalate privileges through the API), please open a
private security advisory on GitHub rather than a public issue, or contact
the maintainers listed in CODEOWNERS directly. Please include:

- A description of the vulnerability and its impact
- Steps to reproduce
- Any relevant logs or request/response examples (with secrets redacted)

## Handling secrets

- Never commit real credentials, JWT signing secrets, or database passwords.
  `application.yml` reads all secrets from environment variables with
  non-production placeholder defaults.
- Rotate the `JWT_SECRET` and database credentials before any real deployment
  — the defaults in this repository are for local `docker compose` use only.
- The demo `TaskHandler` cannot execute arbitrary shell commands, and there is
  no built-in handler that does; adding one is a deliberate architectural
  decision an operator would have to make explicitly.
