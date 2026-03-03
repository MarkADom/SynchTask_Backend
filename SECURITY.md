# Security Policy

Thanks for helping keep SynchTask Backend safe.

## Reporting a vulnerability

Please **do not open a public issue** for security vulnerabilities.

Send details to: `security@synchtask.local` (replace with your real contact)

Include:
- clear description of the issue
- impact and affected endpoints/components
- reproduction steps or proof of concept
- optional mitigation suggestions

## Response expectations

- Initial acknowledgement: up to **3 business days**
- First triage/update: up to **7 business days**
- Fix timeline depends on severity and complexity

## Scope highlights

This repository includes concerns such as:
- authentication and JWT handling
- OAuth2 login flow
- endpoint authorization rules
- operational endpoints (actuator)
- secret/config handling via environment variables

## Good practices for contributors

- Never commit secrets (`.env`, `.envrc`, keys, tokens)
- Avoid logging sensitive data
- Keep least-privilege authorization in mind
- Document security-relevant changes in PRs
