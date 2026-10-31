# PLAN — Clookie
Spec: SPEC.md (acceptance lists per stage). Design rules: design/DESIGN.md.

- [x] 1 Foundation + design: toolchain, signing, release.ps1, design system, logo, showcase (sample data)
- [x] 2 Alarm engine (Room v1, setAlarmClock, receivers, ringing FGS + lock-screen activity, ring state machine), editor sheets, classic mission via `WakeMission` registry, permission screen + Độ tin cậy, updater, 6 synthesized sounds, v0.2.0
- [x] 3 Cookie Code (k = 4, 40 mm) + QR/barcode (ZXing), pure-Kotlin detector + synthetic set, code studio, v0.3.0
- [ ] 4 Other missions, Crumbs game, sleeper profiles, safeguards, awake check
- [ ] 5 Studio, clock styles, timer/stopwatch, wake log, bedtime, widget, backup
- [ ] 6 Review, fixes, performance pass, targetSdk 37 review, v1.0.0

Key decisions: Fraunces display + Be Vietnam Pro UI; logo "bitten clock"; targetSdk 36 (37 in Stage 6);
engine decisions in SPEC §3 "Alarm engine" (USE_EXACT_ALARM, systemExempted FGS, direct-boot storage,
15-min late grace, 30-min auto-silence); Room schema v1 frozen, migrations only from now on.
Sounds: `python -E -P scripts\sounds.py` -> res/raw. Missions: add an object + one MissionRegistry line.

Stage 3: detector `model/CookieDetector` (report: app/build/reports/cookie-detector.txt after the tests);
missions `mission/ScanMissions` + `CodeEditors`; contract gained `EditorExtras`. Codes use the v1 tables.
Next step: owner runs the Stage 3 device test list (photos of real drawn codes -> add as JVM samples);
then Stage 4 (MathBackup/MathProblem are ready to grow into the Math mission).
