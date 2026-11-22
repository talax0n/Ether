# Etha animation

The atlas uses 256 × 256 RGBA cells. Use the pixel rectangles in `manifest.json` under `frame_layout.rows`. The table records the atlas timing. GIF previews round frame delays to hundredths of a second.

| State | Frames before → after | Runtime ms per frame | Loop | Result |
| --- | ---: | ---: | --- | --- |
| Idle | 6 → 12 | 83 | Yes | Denser Breathe bake |
| Wave | 6 → 11 | 68 | No | Five Codex in-betweens |
| Celebrate | 6 → 11 | 68 | No | Five Codex in-betweens |
| Level-up | 6 → 6 | 125 | No | Original poses retained |
| Sleep | 6 → 6 | 250 | Yes | Original loop retained |
| Look | 0 → 8 | 100 | No | Left glance, right glance, blink |
| Scratch | 0 → 8 | 100 | No | Mitten scratches the blue antenna |
| Tap | 0 → 8 | 100 | No | Two foot taps with a slight sway |
| React | 0 → 8 | 80 | No | Giggle and squash |
| Shrug | 0 → 7 | 100 | No | Both mittens up, flat mouth at the peak |
| Hop | 0 → 7 | 70 | No | Crouch, lift, apex, descent, landing |
| Run | 0 → 8 | 80 | Yes | Run in place for loading |

Idle samples the same Breathe cycle at twice the frame count. Wave and Celebrate interleave the approved poses with deterministically extracted Codex takes aligned to the neighboring visors. Level-up kept its approved poses after rerolls introduced visible changes in the mitten path and hoodie. Sleep kept the original slow bob.

## Original pose review

- Idle used the approved still and sprite-gen's smooth Breathe layer after the generated row altered the hood and visor. The chest moves while the head stays fixed.
- Wave passed on the first take.
- Celebrate passed after two airborne frames were moved up by 10 and 24 pixels to restore the jump arc. The crouch, landing and happy squint came from the first take.
- Level-up took two rerolls. The first take drew individual fingers and the second introduced a wink. The final take keeps both eyes open and uses plain mittens. Its right antenna tip was corrected to Grape.
- Sleep took one reroll to restore the reference hood shape, blush and chest mark around the closed eyes.

## New pose review

- Look keeps the approved visor and body. The eyes move inside the visor, and both lids close together in the rerolled blink. Scratch uses the higher-contact reroll, a Codex pose between the low reach and antenna contact, and round mittens from the first take for the low poses.
- Tap keeps the support foot planted through both taps. React uses a smooth Codex rebound after the second squash. Shrug keeps the half-lidded, flat-mouth pose at the peak and uses a Codex in-between to release it.
- Hop uses a reroll to keep the right antenna tip Grape. Its three airborne poses rise 11, 21 and 10 px above the 232 px foot baseline. Run uses a Codex transition into its first stride so the last-to-first step carries the gait forward.
- The six one-shots use idle frame 0 verbatim at both ends. Rows 0–4 retain their original pixels and timing in the expanded atlases. The Android sheet reuses the original 256-entry palette.

## Files

- `sprite-sheet-alpha.png` is the transparent atlas.
- `manifest.json` records frame rectangles and runtime timing.
- `base.png` is the approved still with its green background removed.
- Each animation has a GIF named for its manifest key.
- `ANIMATION.md` records timing and QA.
- `run/` contains the original sprite-gen run. `run/extra-rows/` contains the Codex takes, and its `qa/` folder has contact sheets for the new rows.
