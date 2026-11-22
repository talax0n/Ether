# Etha animation

The atlas uses 256 × 256 RGBA cells. Use the pixel rectangles in `manifest.json` under `frame_layout.rows`. The table records the atlas timing. GIF previews round frame delays to hundredths of a second.

| State | Frames | FPS | Loop | Visual QA |
| --- | ---: | ---: | --- | --- |
| Idle | 6 | 6 | Yes | Pass |
| Wave | 6 | 8 | No | Pass |
| Celebrate | 6 | 8 | No | Pass |
| Level-up | 6 | 8 | No | Pass |
| Sleep | 6 | 4 | Yes | Pass |

## Frame review

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
- `run/` contains the sprite-gen working files and per-state contact sheets.
