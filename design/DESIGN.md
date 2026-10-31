# Clookie design system (rules for every stage)
Concept: a clock that is a cookie. Bakery packaging x Swiss editorial: matte solid color, big numerals, crisp geometry,
one motif used sparingly: the **bite** (circular notch) and **chips** (small dots). Code: `app/.../design/`.

## Never
Gradients, neon, glow, blur, glass, soft/blurred shadows, ripples, Material 3 defaults (purple, FAB, Switch, TimePicker,
list rows with trailing switch), emoji or Material icons on main screens, explanatory paragraphs.

## Tokens
- Color roles (`ClookieColors`): background, surface, slab, ink, inkMuted, accent/onAccent, accentSoft/onAccentSoft,
  outline, shadow, chip, ringing/onRinging, grid/gridMajor/margin (Giấy ô li only), scrim (only alpha use).
- Contrast (PaletteContrastTest): text >= 4.5:1 on background/surface/slab/accent; numerals, icons, strokes >= 3:1.
- Type: display = Fraunces SOFT 800 (`DisplayFamily`), UI = Be Vietnam Pro 400-700. Line height >= 1.4x, LineHeightStyle
  Trim.None so "Ỗ Ữ Ẫ ặ ỹ Ở" never clip. Styles: displayTitle 34, displaySmall 24, title 22, heading 17, body 15,
  label 13, eyebrow 11 caps +12% tracking, caption 11.
- Time numerals: `TimeDisplay` only (fixed-width digit slots, dp-sized, ignore font scale, auto-fit width).
  Sizes (dp): Hero 124, Ringing 136, Dial 64, Slab 56, Compact 34.
- Spacing: 4 dp grid (`Space.s1..s16`), screen gutter 20. Touch targets >= 48 dp.
- Strokes: 1.5 dp (thin), 2 dp (regular). Corners (`CornerStyle`): Soft 14/24/36, Rounded 10/16/26, Crisp 3/5/8.
- Depth: solid layers + optional hard shadow (`Modifier.hardShadow`, 4 dp offset, zero blur, `shadow` color).
- Motion (`ClookieMotion`): snappy ~150 ms, standard ~250 ms, playful (overshoot) ~350 ms, all springs; Reduced
  (setting or system animator scale 0) snaps and skips infinite animations. Never animate off-screen content.
- Haptics (`rememberHaptics()`): tick (minute/digit), strongTick (5 min, hour), press, toggle, confirm, reject.

## Presets (`Presets`)
| id | name | look | corners | hard shadow |
|---|---|---|---|---|
| bo_sua | Bơ sữa | cream, cocoa ink, jam red | Rounded | on |
| socola | Socola | dark chocolate, cream, caramel | Soft | on |
| matcha | Matcha | pale matcha, deep green, adzuki | Soft | on |
| khoai_mon | Khoai môn | taro lilac, plum, purple | Soft | off |
| giay_o_li | Giấy ô li | paper white, ink blue, red pen, dotted grid + margin | Crisp | on |
| dem | Đêm | true black, warm white, ember | Crisp | off |
| he_thong | Màu hệ thống | Material You tones flattened + contrast-corrected | Rounded | off |

## Components (`ui/components`) — reuse, don't re-style
- `Txt` (never Material Text) · `TimeDisplay` · `AlarmSlab` (tall slab: time, label, mission icons, `WeekdayDots`,
  `BiteToggle`; enabled = slab fill + shadow, disabled = outline + muted) · `BiteToggle` (cookie thumb, bite when off).
- `CookieDial` (outer minute ring carries the hour, inner 24-h ring, cookie with chip hands) + `TimeKeypad` in a
  `ClookieSheet` (tap the digits). · `ClookieBottomNav` (floating ink bar, selected = dough pill with label).
- Buttons: `CookieButton` Primary/Secondary/Quiet, `PillChip`, `IconCircleButton`, `BiteAddButton`, `Segmented`.
  Press = slide onto the hard shadow (`Modifier.pressable`), or 4% shrink when shadows are off.
- `RingingLayout` (full-bleed `ringing`, giant time, wobbling mark, mission slot, snooze slot), `SlideToStop`,
  `Modifier.biteAway` (dismissal bite), `ClookieMark`/`ClookieLockup` (logo), `Tile`, `SettingsRow`, `Eyebrow`, `TopBar`.
- Missions plug into `RingingLayout(mission = …)` and must use these tokens; scan screens keep the ringing color frame.
- `PermissionRow` (status badge, "Bỏ qua", "Cấp") in a `RowGroup`; `ChoiceChips` for every editor-sheet choice;
  `UpdateBanner` inline on Home. Editor settings open as `ClookieSheet`s (never dialogs or stock pickers).

## Missions (plug-in contract, `mission/`)
- A mission = one `object : WakeMission` + one line in `MissionRegistry.all`. Nothing else changes: the engine,
  the ringing host (`RingingScreen`) and the editor's mission sheet read the registry.
- `type`: a `MissionType` (name and icon come from `ModelUi`). `settings`: `MissionSetting(key, label, values,
  default)` chips rendered by the mission sheet; values arrive in `session.spec.params`.
- `Content(session, modifier)`: drawn in the mission slot on the `ringing` color with `onRinging` ink, Clookie
  components only. Call `session.interact()` on every touch (ducks the alarm to 30% for 20 s) and
  `session.complete()` once when solved; the host runs the next mission of the chain (max 3) or the bite.
- `quickDismiss`: true only for trivial missions (Classic); allows "Tắt" in the notification.
  `permissions`: runtime permissions needed (camera, activity), asked inline before the mission is added.
- `session.preview` is the editor's "Thử": no sound, completing returns to the editor.
- Unknown or unregistered types run as Classic (`MissionRegistry.resolve`), so a chain can always be stopped.

## Logo
"Bitten clock": cream cookie on jam red, bite forms the C, chocolate-chip hands at 10:10, three chips. One geometry
(`MarkGeometry`) for launcher XML, monochrome (even-odd cut-outs), splash and in-app mark. Concepts: design/logo/.
