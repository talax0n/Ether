# Ether UI References

Swipe decks, gamified progress, and game-HUD patterns gathered to steer Ether's redesign into three directions: Arcade, Neon Night, and Editorial Clean.

Status key: `opened` = fetched with WebFetch, `search result only` = seen in search results but not fetched, `blocked` = fetch returned 403 or an empty body. Dribbble, Behance, Mobbin, and Pinterest block automated fetches or render client-side, so those entries rely on search-result titles and snippets.

## 1. Swipe and match (card deck)

1. **New TINDER Swipe Animation (Daniel Leth, Dribbble)**: https://dribbble.com/shots/14520450-New-TINDER-Swipe-Animation
   Status: `blocked` · [Editorial] [Arcade]
   Steal: Keep each motion small and staggered. When a card leaves, the action buttons do a quick close-and-collapse, and each element gets its own timing offset.

2. **Dating app: New match (Igor Shadko, Dribbble)**: https://dribbble.com/shots/4083570-Dating-app-New-match
   Status: `blocked` · [Neon] [Editorial]
   Steal: Skip the usual two-circle avatar layout for the match screen. Use a full-screen reveal with one large photo and the "message them" CTA right below it.

3. **Tinder Swiper component (Framer Marketplace)**: https://www.framer.com/marketplace/components/tinder-swiper/
   Status: `opened` · [Arcade]
   Steal: Make "Stack Gap" and "Stack Shrink" tunable, so cards behind the top card sit offset and scaled down. Directional LIKE/NOPE stamps fade in as the drag grows, and action buttons get a gradient pressed state.

4. **Design a Tinder-Style Swipe Deck (techinterview.org)**: https://www.techinterview.org/post/3233474992/design-tinder-swipe-deck-mobile/
   Status: `opened` · [Arcade] [Neon] [Editorial]
   Steal: Release past about 100pt flings the card off-screen, anything short springs back. Prefetch photos for the next 5 cards. Fire the match modal within about 1s of the mutual like. A server-enforced daily quota maps to a friendly client "out of swipes" state.

5. **Bumble iOS Profile Card (Mobbin)**: https://mobbin.com/explore/screens/c6185a4c-b1a5-4604-ae11-856c3045a945
   Status: `blocked` · [Editorial]
   Steal: Use one bold brand accent on an otherwise neutral card. Interests render as rounded chips under the photo, and the expanded profile scrolls inside the card (bio, "About me", "My interests") with like/dislike pinned.

6. **Hinge iOS Detailed profile (Mobbin)**: https://mobbin.com/explore/screens/a2572471-b8a5-4e60-802d-4d7ca54c0a8e
   Status: `search result only` · [Editorial]
   Steal: Build the profile as a vertical feed of photo and prompt blocks, each with its own like affordance. Use a near-monochrome palette (Mobbin lists #1A1A1A / #FFFFFF / #666666) so the photos and prompt text do the work. This maps to liking a specific skill or project on an Ether card.

## 2. Gamified progress (XP, streaks, badges, celebrations)

7. **Duolingo design guide (blakecrosley.com)**: https://blakecrosley.com/guides/design/duolingo
   Status: `opened` · [Arcade]
   Steal: A chunky 3D button made with a 4px darker bottom edge. On press, the edge drops to 0 and the button moves down 4px in 100ms. Each color has a fixed meaning (green = success, yellow #ffc800 = XP, orange #ff9600 = streak, purple = league). The progress bar still nudges forward on a "miss", so progress never looks stalled.

8. **Streak milestone design (Duolingo Blog)**: https://blog.duolingo.com/streak-milestone-design-animation
   Status: `opened` · [Arcade] [Neon]
   Steal: Save a bigger celebration for milestone days only, framed as a video-game power-up (Duo turns into a fire phoenix). Follow it with a square share card ("I just reached a … streak!") the user can post without leaving the app.

9. **How the Duolingo streak builds habit (Duolingo Blog)**: https://blog.duolingo.com/how-duolingo-streak-builds-habit
   Status: `opened` · [Arcade]
   Steal: Extension beat: the count appears, the mascot spins, then it bursts into flame. The freeze inventory shows as outlined empty slots ("0 of 2 equipped"), which works for Ether's daily-swipe-cap meter.

10. **Duolingo streak mechanics (Deconstructor of Fun)**: https://duolingo.deconstructoroffun.com/mechanics/streaks
    Status: `opened` · [Arcade] [Editorial]
    Steal: Sequence: XP total → small celebration → streak counter tick-up. Calendar days get checkmarks or snowflake "saved" markers. A "perfect week" turns 7 day dots into one continuous orange bar with a yellow halo on the flame.

11. **Shape language: Duolingo's art style (Duolingo Blog)**: https://blog.duolingo.com/shape-language-duolingos-art-style/
    Status: `opened` · [Arcade]
    Steal: Rounded, bouncy, bright vector shapes, using the fewest details that still read. Every key object gets a clear silhouette with white space around it. Use this as the rule for badge and rank icon art.

12. **Start earning Achievements (Duolingo Blog)**: https://blog.duolingo.com/achievement-badges/
    Status: `opened` · [Arcade] [Editorial]
    Steal: Badges "collect on your profile like trophies on a shelf". Mix tiered difficulty badges with a separate "personal records" row (most XP in a day, longest streak, best league finish) plus a few surprise badges (night owl).

13. **Elevating craft: core tabs redesign (Duolingo Blog)**: https://blog.duolingo.com/core-tabs-redesign/
    Status: `opened` · [Editorial] [Arcade]
    Steal: Every tab title sits in the same left-aligned spot at the same size. Whitespace separates sections, not boxed containers. The team tested "Punchy" (saturated solid headers per tab) against "Soft" (subtle gradients), and the shipped version paired bold headers with flat cards.

14. **E-commerce Gamification app (Yogesh Madharam, Dribbble)**: https://dribbble.com/shots/7739492-E-commerce-Gamification-app
    Status: `blocked` · [Arcade] [Editorial]
    Steal: Show earned and unearned badges in one grid. Unearned ones are faded, so the faded edge marks how far the user has progressed.

15. **Confetti #2 (LottieFiles)**: https://lottiefiles.com/fr/free-animation/confetti-2-cDGLzwPOrG
    Status: `search result only` · [Arcade]
    Steal: A confetti layer built specifically to sit behind a badge, rank, or medal (Lottie Simple license, .aep included for recoloring to brand colors). Play it behind the badge on unlock instead of as a full-screen burst.

## 3. Game HUD and dark neon

16. **HP Bar (franciscoguadarrama, Rive Community)**: https://rive.app/community/files/17640-33080-hp-bar/
    Status: `opened` · [Neon]
    Steal: Damage "chipping": the main bar drops at once and a lighter ghost bar drains after it. Reverse it for XP gain, where the ghost fills first and the solid bar catches up with spring overshoot.

17. **Progress bar (pixel-point, Rive Community)**: https://rive.app/community/files/6965-13392-progress-bar/
    Status: `opened` · [Neon]
    Steal: A glowing progress bar (CC BY). Use a bloom on the leading edge of the fill as the reliability-score or XP meter on dark surfaces.

18. **Doom Eternal HUD (hello.tonirebello, Rive Community)**: https://rive.app/community/files/16595-31250-doom-eternal-hud/
    Status: `opened` · [Neon]
    Steal: A bottom-centered HUD cluster driven by a state machine. Use it as the model for a persistent bottom bar on the swipe screen showing swipes left, XP, and streak as segmented gauges.

19. **Lumen: Forms that feel like glass (Superdesign)**: https://app.superdesign.dev/library/lumen-forms-that-feel-like-glass
    Status: `blocked` (redirected, then the page body was empty; tokens come from the search snippet) · [Neon]
    Steal: Near-black ink background, frosted translucent surfaces (backdrop blur) with a 1px white hairline border, a neon aqua-to-magenta "aurora" gradient behind them, Space Grotesk headings over Inter body.

20. **250 Neon UI ideas (Pinterest board)**: https://ca.pinterest.com/jocelynbbg/neon-ui/
    Status: `search result only` · [Neon]
    Steal: The "Neon Player Card" pattern: a profile or stat card on a dark background with a glowing outline and a rarity-tinted rim. This fits a teammate card's "rank" frame.

21. **Minigames & Playful Interactions (Awwwards collection)**: https://www.awwwards.com/awwwards/collections/minigames-playful-interactions/
    Status: `opened` · [Arcade] [Neon]
    Steal: Playful micro-games inside utility UI, for example Mailchimp's "Hole Golf" and Analogue's "Arcade Game". Ether could add a small tap-to-play moment on the empty or "out of swipes" screen.

## 4. Editorial stats and refined motion

22. **Strava Year in Sport 2021 (Manual)**: https://manual.studio/work/strava-year-in-sport-2021
    Status: `opened` · [Editorial]
    Steal: Big stats shown as an animated story, with overlapping colored shapes ("bright collisions") marking peak moments and one bold diagonal crop reused across titles and transitions.

23. **Strava Year in Sport 2023 (Hello Monday)**: https://hellomonday.com/work/strava-year-in-sport-2023
    Status: `opened` · [Editorial] [Neon]
    Steal: One expressive polyline plus a highlighter-stroke motif carries the athlete's data. Ether could draw an XP-over-time line with a single vivid highlight stroke on a profile.

24. **Airbnb app redesign 2025 (It's Nice That)**: https://www.itsnicethat.com/articles/airbnb-app-redesign-140525
    Status: `opened` · [Editorial] [Arcade]
    Steal: A clean layout with softly rounded corners plus small, detailed 3D pictograph icons that animate on tap. The rest stays calm and only the icons get personality.

25. **Customize animations: spring (Android Developers, Compose)**: https://developer.android.com/develop/ui/compose/animation/customize
    Status: `opened` · [Arcade] [Neon] [Editorial]
    Steal: Use `spring()` for everything finger-driven because it can be interrupted and keeps velocity when the target changes. Use `DampingRatioMediumBouncy` (0.5) / `StiffnessMediumLow` (400) for XP bar overshoot, and no-bounce for card snap-back.

## Synthesis

1. **Spring the swipe card, never tween it.** Drag-follow with about ±12° tilt mapped to x-offset, about 100pt (or ~30% width) fling threshold, spring snap-back. Use Compose `spring()` so a mid-gesture re-grab keeps velocity (refs 3, 4, 25).
2. **Stamps and stack depth for feedback.** Directional stamps fade in with drag distance, back cards scale and offset up as the top card leaves, and action buttons collapse in a staggered way (refs 1, 3).
3. **XP gain plays in two layers.** A ghost bar fills first, the solid bar springs in after it with overshoot, then the number ticks up. Glow on the fill's leading edge for Neon, flat yellow for Arcade (refs 7, 16, 17).
4. **Tier the celebrations.** Small inline +XP on every swipe, a mid celebration on a match, and full-screen "power-up" reveals only at milestones (first match, level up, 7-day streak). Each milestone ends in a shareable square card (refs 8, 10, 12).
5. **Show the swipe cap as inventory, not as a wall.** Show 30 swipes as segmented slots or a HUD gauge. At zero, show a warm "come back tomorrow" state with a countdown, which can host a tiny playful moment instead of a paywall feel (refs 4, 9, 18, 21).
6. **Profile as a trophy shelf.** Earned and faded-unearned badges in one grid, a "personal records" row (best streak, matches made, endorsements), and the reliability score as one hero gauge (refs 12, 14, 20).
7. **Fixed color semantics across every direction.** Pick one color each for XP, streak, endorsement, and reliability and never reuse them for decoration. Arcade pushes them saturated, Neon makes them glow, and Editorial uses them as the only color on a neutral page (refs 7, 13, 24).
8. **Consistent structure under the effects.** Same title position on every tab, whitespace instead of boxes, and one press interaction (the 4px drop button in Arcade, a scale-down in Editorial and Neon) used everywhere (refs 7, 13).

> **Scope note.** Streaks, levels and level names (used in the mockups as "Level 4 · Builder") are not in SPEC §4. Levels can be a pure display function of `xp`, but thresholds and names need a SPEC change before they ship. Streaks need new data and are out of scope for now.

## Could not open

- https://dribbble.com/shots/14520450-New-TINDER-Swipe-Animation (empty body, client-rendered)
- https://dribbble.com/shots/4083570-Dating-app-New-match (empty body)
- https://dribbble.com/shots/21056966-Badge-Award-UI-Gamification (empty body)
- https://dribbble.com/shots/7739492-E-commerce-Gamification-app (empty body)
- https://dribbble.com/search/tinder-swipe (empty body)
- https://dribbble.com/search/gamification-app (empty body)
- https://www.behance.net/gallery/172095577/UIUX-Case-Study-Gamified-E-learning-APP (HTTP 403)
- https://www.behance.net/search/projects/gamified%20%20fitness?locale=en_US (HTTP 403)
- https://mobbin.com/explore/screens/c6185a4c-b1a5-4604-ae11-856c3045a945 (HTTP 403)
- https://mobbin.com/colors/brand/hinge (HTTP 403)
- https://www.pinterest.com/pin/917749230347314077/ (truncated, no content)
- https://in.pinterest.com/thinkaman/gamification-ui/ (truncated, no content)
- https://m3.material.io/styles/motion/overview (title only, client-rendered)
- https://superdesign.dev/library/lumen-forms-that-feel-like-glass (title only after redirect)
