# Ether

> Swipe to find your hackathon team. A campus-only Android app where students swipe right on teammates or teams for a specific competition, and a mutual right swipe becomes a match.

**Status:** 📐 Pre-build spec. See [SPEC.md](SPEC.md) and [PLAN.md](PLAN.md).

| | |
|---|---|
| Scope | One university (MVP) |
| Platform | Android only |
| Stack | Kotlin · Jetpack Compose · Material 3 · Supabase (Postgres, Auth, RLS) |
| Post-match | Contact handoff to WhatsApp / Telegram (no in-app chat) |

## How it works

1. Sign in with your university email.
2. Pick a competition.
3. Swipe through students and teams entered in that competition. Right means interested. Left means pass.
4. Both sides swipe right and you match. The app shows each other's contact link.
5. After the event, teammates endorse each other. Endorsements build your reliability score.

## Docs

- [SPEC.md](SPEC.md) covers the problem, MVP scope, data model, architecture, and risks.
- [PLAN.md](PLAN.md) covers the build order and definition of done.
- [CLAUDE.md](CLAUDE.md) gives coding agents the project conventions.

## Notes

- No secrets in source. `.env.example` lists the variable names. Real values stay in a gitignored `.env` or `local.properties`.

## License

MIT. See [LICENSE](LICENSE).
