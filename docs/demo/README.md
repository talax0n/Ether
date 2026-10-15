# Demo screenshots

Captured on the `ether` emulator (Android 16) running the debug build (Arcade UI) against local Supabase (`supabase db reset`, then `scripts/e2e-setup.sh`), driven by Maestro `takeScreenshot` flows. OTP codes were read from Mailpit. Each PNG is downscaled to 1000 px tall.

| File | What it shows |
|------|---------------|
| `01-sign-in-email.png` | Sign-in with a new university address, `rani@student.example.ac.id` |
| `02-otp-code.png` | The 6-digit code step |
| `03-profile-setup.png` | Profile setup for the new account, with the 7-field progress meter and the +50 XP sticker |
| `04-competitions.png` | Seeded competitions as event cards: ENDED and OPEN stickers, mono countdown to the registration deadline |
| `05-competition-detail.png` | Campus Hackathon 2026 before joining |
| `05-create-team.png` | The create-team dialog (demo5, who has no listing in Campus Hackathon) |
| `06-deck.png`, `06-deck-team.png` | Swipe deck with the 30-tick energy meter: a solo card and the Byte Club team card |
| `07-match.png` | The full-screen match celebration between Rani and Byte Club, with what the match did and the WhatsApp button |
| `08-matches.png` | Matches tab with Byte Club's contact link |
| `09-teams.png` | Rani's new team with member contacts |
| `10-endorse-report.png` | demo1 endorses Budi in the ended Data Challenge and reports Winner |
| `11-confirm-before.png`, `11-result-confirmed.png` | demo2 confirms the result |
| `12-profile-xp-badges.png` | demo2's profile after the endorsement and the confirmed result: Level 4 Builder, 530 XP (300 seed + 30 + 200), First Team and Winner badges unlocked, the rest locked |
| `13-matches-empty.png` | Empty state for matches |
| `14-error-retry.png` | Error state with Retry on the Teams tab, with the emulator's Wi-Fi and data turned off |
| `15-swipe-cap.png` | The `swipe_cap_reached` error mapped to the daily-cap screen |
| `16-error-email-domain.png` | A non-university email rejected at sign-up |
| `17-error-registration-closed.png` | `registration_closed` from the database: Campus Hackathon's deadline was moved into the past after the screen loaded, then Rani tapped Join solo |
| `18-dark-competitions.png` | The dark Arcade variant (system night mode) |
| `19-dark-profile-reduced-motion.png` | Dark profile with animations off (`animator_duration_scale 0`), captured 0.6 s after opening the tab: the XP bar and count are already final. The account is the one `signin_and_onboard` created |
| `ether-demo.mp4` | 86-second screen recording (720×1600) of the happy path as demo3: sign in with a Mailpit code, competitions, deck skeleton, drag two cards left (NOPE stamp), pass to Byte Club, drag right, match celebration with confetti, Keep swiping, Matches, Teams, and Profile with the XP bar filling. Sign-in is typed with `adb shell input`, the rest is driven by Maestro after `scripts/e2e-setup.sh` |

**How the match was set up.** Byte Club's right swipe on Rani was created beforehand in SQL, by calling `swipe()` as demo7 (the team owner) under an `authenticated` JWT claim. Rani then swiped right on Byte Club in the app, and the database created the match.

**Other SQL setup.** For `05-create-team.png`, `scripts/e2e-setup.sh` already deletes demo5's seeded listing in Campus Hackathon. For `15-swipe-cap.png`, 29 left swipes by demo4 (between other listings) were inserted before the deck opened, and one more was inserted after it opened, so the app's next swipe hit the database cap.
