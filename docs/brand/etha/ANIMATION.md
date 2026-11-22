# Etha animation

The atlas uses 256 × 256 RGBA cells. Use the pixel rectangles in `manifest.json` under `frame_layout.rows`. The table records the atlas timing. GIF previews round frame delays to hundredths of a second.

| State | Frames before → after | Runtime ms per frame | Loop | Result |
| --- | ---: | ---: | --- | --- |
| Idle | 6 → 12 | 83 | Yes | Denser Breathe bake |
| Wave | 6 → 11 | 68 | No | Five Codex in-betweens |
| Celebrate | 6 → 11 | 68 | No | Five Codex in-betweens |
| Level-up | 6 → 6 | 125 | No | Original poses retained |
| Sleep | 6 → 6 | 250 | Yes | Original loop retained |

Idle samples the same Breathe cycle at twice the frame count. Wave and Celebrate interleave the approved poses with deterministically extracted Codex takes aligned to the neighboring visors. Level-up kept its approved poses after rerolls introduced visible changes in the mitten path and hoodie. Sleep kept the original slow bob.

## Original pose review

- Idle used the approved still and sprite-gen's smooth Breathe layer after the generated row altered the hood and visor. The chest moves while the head stays fixed.
- Wave passed on the first take.
- Celebrate passed after two airborne frames were moved up by 10 and 24 pixels to restore the jump arc. The crouch, landing and happy squint came from the first take.
- Level-up took two rerolls. The first take drew individual fingers and the second introduced a wink. The final take keeps both eyes open and uses plain mittens. Its right antenna tip was corrected to Grape.
- Sleep took one reroll to restore the reference hood shape, blush and chest mark around the closed eyes.

## Files

- `sprite-sheet-alpha.png` is the transparent atlas.
- `manifest.json` records frame rectangles and runtime timing.
- `base.png` is the approved still with its green background removed.
- `idle.gif`, `wave.gif`, `celebrate.gif`, `levelup.gif` and `sleep.gif` are the animation previews.
- `ANIMATION.md` records timing and QA.
- `run/` contains the original sprite-gen run and per-state contact sheets.
