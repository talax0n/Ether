# Pip

Pip is Ether’s front-facing coral heart mascot. The taller left lobe, dot eyes, small smile, and yellow boots give Pip a recognizable silhouette at 96 px. The flat fills, chunky Ink outline, and solid offset edge follow Ether’s Arcade style.

## States

| State | Use | Frames | FPS | Atlas playback |
| --- | --- | ---: | ---: | --- |
| idle | Gentle breathing loop | 8 | 4 | Loop |
| celebrate | Jump and cheer on a mutual match | 6 | 9 | Once |
| wave | One-handed onboarding greeting | 6 | 7 | Once |
| sleep | Closed-eye breathing for empty screens | 4 | 4 | Loop |

Idle uses sprite-gen’s Smooth Breathe layer on a single approved pose. The GIFs repeat for review. App playback follows the manifest.

## Files

- `base.png` contains the approved identity reference.
- `concepts/` contains the heart, spark, and robot stills.
- `sprite-sheet-alpha.png` and `manifest.json` are the runtime atlas and frame metadata. Use the rectangles in `frame_layout`.
- `idle.gif`, `celebrate.gif`, `wave.gif`, and `sleep.gif` are transparent previews.
- `run/` contains sprite-gen inputs, generated rows, extracted frames, reports, and QA previews.

<details>
<summary>Full asset and run file paths</summary>

```text
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/.run.sg-rwlock
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/README.md
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/base.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/celebrate.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/concepts/heart.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/concepts/robot.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/concepts/spark.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/idle.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/manifest.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/base-source.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/curation.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/celebrate/frame-0.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/celebrate/frame-1.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/celebrate/frame-2.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/celebrate/frame-3.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/celebrate/frame-4.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/celebrate/frame-5.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/frames-manifest.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/idle/frame-0.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/idle/frame-1.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/idle/frame-2.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/idle/frame-3.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/sleep/frame-0.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/sleep/frame-1.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/sleep/frame-2.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/sleep/frame-3.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/wave/frame-0.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/wave/frame-1.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/wave/frame-2.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/wave/frame-3.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/wave/frame-4.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/frames/wave/frame-5.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/manifest.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/previews/celebrate.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/previews/gif-manifest.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/previews/idle.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/previews/sleep.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/previews/wave.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/prompts/celebrate.txt
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/prompts/idle.txt
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/prompts/sleep.txt
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/prompts/wave.txt
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/all-contact.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/celebrate-contact.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/celebrate.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/idle-contact.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/idle.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/sleep-contact.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/sleep.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/wave-contact.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/qa/wave.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/celebrate.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/celebrate.png.raw.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/idle.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/idle.png.raw.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/sleep.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/sleep.png.raw.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/wave.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/raw/wave.png.raw.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/references/layout-guides/celebrate.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/references/layout-guides/idle.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/references/layout-guides/sleep.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/references/layout-guides/wave.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/celebrate.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/celebrate.log
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/idle.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/idle.log
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/set.report.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/sleep.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/sleep.log
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/table.md
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/wave.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/reports/gen-set/wave.log
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/sprite-request.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/sprite-sheet-alpha.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/run/sprite-sheet-alpha.report.json
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/sleep.gif
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/sprite-sheet-alpha.png
/Users/theo/Documents/PROJECT/ether/docs/brand/mascot/wave.gif
```
</details>
