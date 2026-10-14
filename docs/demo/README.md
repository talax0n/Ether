# Demo screenshots

Captured on the `ether` emulator (Android 16) running the debug build against local Supabase (`supabase db reset`, seed data), driven with `adb shell input`. OTP codes were read from Mailpit.

| File | What it shows |
|------|---------------|
| `01-sign-in-email.png` | Sign-in with a new university address, `rani@student.example.ac.id` |
| `02-otp-code.png` | The 6-digit code step |
| `03-profile-setup.png` | Profile setup for the new account, including the contact link |
| `04-competitions.png` | Seeded competitions, with the ended one labelled |
| `05-competition-detail.png` | Campus Hackathon 2026 before joining |
| `05-create-team.png` | Creating a team (demo2), which goes through `create_team()` |
| `06-deck.png`, `06-deck-team.png` | Swipe deck with seeded cards: a solo card and the Byte Club team card |
| `07-match.png` | A mutual match between Rani and Byte Club |
| `08-matches.png` | Matches tab with Byte Club's contact link |
| `09-teams.png` | Rani's new team with member contacts |
| `10-endorse-report.png` | demo1 endorses Budi in the ended Data Challenge and reports Winner |
| `11-confirm-before.png`, `11-result-confirmed.png` | demo2 confirms the result |
| `12-profile-xp-badges.png` | demo2's profile after the endorsement and the confirmed result: 530 XP (300 seed + 30 + 200), First Team and Winner badges |
| `13-matches-empty.png` | Empty state for matches |
| `14-error-retry.png` | Error state with Retry, with the emulator's network turned off |
| `15-swipe-cap.png` | The `swipe_cap_reached` error mapped to the daily-cap screen |
| `16-error-email-domain.png` | A non-university email rejected at sign-up |
| `17-error-registration-closed.png` | `registration_closed` when joining the ended competition |
| `ether-demo.mp4` | 68-second screen recording of the happy path as demo3: sign in with a Mailpit code, open the deck, drag two cards left, pass to Byte Club, drag right, match dialog with the WhatsApp button, then Matches, Teams and Profile. Driven by Maestro after `scripts/e2e-setup.sh` |

**How the match was set up.** Byte Club's right swipe on Rani was created beforehand in SQL, by calling `swipe()` as demo7 (the team owner) under an `authenticated` JWT claim. Rani then swiped right on Byte Club in the app, and the database created the match.

**Other SQL setup.** For `05-create-team.png`, demo2's seeded solo listing in Campus Hackathon was deleted, so the create-team option would show. For `15-swipe-cap.png`, 29 swipes for demo2 were inserted before the deck opened, and one more was inserted after it opened, so the app's next swipe hit the database cap.
