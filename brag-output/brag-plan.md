# Ether booth video — plan

**What:** Android app where students swipe to find hackathon teammates inside one competition's deck. A mutual right swipe forms the team.
**For:** Students entering GEMASTIK and campus hackathons without a full team.
**Different:** Per-competition decks, two-sided consent, XP for outcomes, not swipes.
**Hook:** The group chat everyone knows: "anyone need a designer?" buried under a flood of the same messages, then swiped away.
**Tone:** default, playful and punchy, in the app's own Arcade style (cream, ink outlines, solid offset shadows, Nunito Black, Etha the mascot).
**References:** the Printerino and fomo launch videos: one idea per beat, words blur in, a floating phone running the real UI, ends on the logo.
**Format:** 1920×1080, 30 fps, 30 s, made to loop on a booth screen. Outro holds a QR code to the APK.

| # | Time | Scene | On screen |
|---|------|-------|-----------|
| 1 | 0.0–3.5 | Hook | "anyone need a designer?" gets typed, the chat floods, Etha runs through and knocks every bubble away |
| 2 | 3.5–6.5 | Reveal | ether mark pops and winds up, Etha bursts out of it in a sparkle ring, spins through an arc, lands with a squash and dust puffs, then waves, "Swipe to find your hackathon team." |
| 3 | 6.5–8.5 | Meet Etha | Big waving Etha, "hi!" bubble, "Meet Etha. Your team-finding sidekick." |
| 4 | 8.5–12.5 | Compete | Real Competitions screen (live countdown), Etha peeking. Chip: Compete |
| 5 | 12.5–16.5 | Swipe | Real deck: NOPE tease, then LIKE. Chip: Swipe |
| 6 | 16.5–21.0 | Match | Grape burst, real IT'S A MATCH screen, Etha celebrates. Chip: Match |
| 7 | 21.0–24.0 | Level up | Real profile, XP counting up, Etha levels up. Chip: Level up |
| 8 | 24.0–30.0 | Outro | Logo, tagline, QR to the APK. Etha peeks over the QR card, jumps on top, lands with dust, waves; "scan me! 👇" pops up |

UI footage: the current build running on fake demo data (`applicationIdSuffix .demo`) on the `ether` emulator, so no real accounts were touched.

The swipe → match take was recorded with `animator_duration_scale 3` through the emulator's 60 fps host recorder, then sped up 3×, so the drag stays smooth.

**Etha as the transition:** from "Meet Etha" to the outro, Etha never fades or pops. Every scene change is Etha travelling: a hop down onto the phone (Meet → Compete), a run left leading the phone (Compete → Swipe), a spinning leap where the purple burst grows out of Etha (Swipe → Match), a run right while the purple shrinks back into Etha (Match → Level up), and a run off-screen (Level up → Outro).
