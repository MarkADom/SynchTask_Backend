# Contributing

Thanks for contributing to SynchTask Backend.

## 1) Local setup

### Prerequisites
- JDK 21+
- MySQL and Redis (local or containers)
- Optional: `direnv`

### Environment

```bash
cp .envrc.example .envrc
# edit values
```

Optional:

```bash
direnv allow
```

## 2) Run locally

```bash
./gradlew bootRun
```

## 3) Minimum checks before PR

```bash
./gradlew test
./gradlew detekt
./gradlew jacocoTestReport
```

## 4) Pull request expectations

- Keep PRs focused and small when possible
- Explain **why** the change is needed
- Include test results/validation commands
- Update docs when behavior/contracts change
- Avoid unrelated refactors in the same PR

## 5) Commit style (recommended)

Use conventional-style prefixes when possible:
- `feat:` new behavior
- `fix:` bug fix
- `refactor:` internal improvement
- `test:` test changes
- `docs:` documentation
- `ci:` CI/CD changes

## 6) Security

For vulnerabilities, follow [`SECURITY.md`](SECURITY.md).
