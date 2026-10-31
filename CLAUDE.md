# Clookie — working notes for Claude
Android alarm clock, Kotlin + Compose, single `:app` module. Read SPEC.md (brief, versions, acceptance per stage),
PLAN.md (status, next step) and design/DESIGN.md (tokens and component rules) before changing anything.

## Commands (PowerShell, project root; JDK 17 on PATH, SDK via local.properties)
- Unit + screenshot tests: `.\gradlew.bat :app:recordRoborazziDebug` (PNGs -> design/screens, design/logo)
- Pure unit tests only: `.\gradlew.bat :app:testDebugUnitTest --tests "*Test"`
- Signed release APK: `.\gradlew.bat :app:assembleRelease`
- Publish: `powershell -ExecutionPolicy Bypass -File scripts\release.ps1 <x.y.z> [-Notes "..."]` — bumps
  version.properties, builds + verifies the signed APK, copies dist/Clookie-v<x.y.z>.apk, commits, pushes main,
  creates GitHub release v<x.y.z>, prints the download URL. Run it yourself; the owner never uses a terminal.
- Fonts: `python -E -P scripts\fonts.py <google-fonts-download-dir>` (pip install -r scripts\requirements.txt).

## Test budget (every stage)
- JVM unit tests for pure logic; the screenshots listed in the stage; one release build per fix round.
- No emulator, no instrumented tests, no exhaustive matrices, no repeated clean builds.
- Review new PNGs yourself against the forbidden list in SPEC.md and for clipping; refine at most twice.

## Rules
- Git only to commit to `main` and push; gh only for releases. No branches, worktrees or PRs.
- Secrets: keystore.properties (git-ignored) points to %USERPROFILE%\.clookie-keys\clookie-release.jks. Never commit it.
- UI text only from strings.xml (values = Vietnamese default, values-en). Short labels, no paragraphs.
- Use Clookie components and `Clookie.colors/type/shapes/motion`; never MaterialTheme, stock Material
  components or Material icons on main screens. New icons go in design/icons/ClookieIcons.kt (24 grid, 2 px stroke).
- Time/animation state is read in draw/layout lambdas; clock text recomposes at most once per second (LocalNow).
- Robolectric runs on SDK 34 (src/test/resources/robolectric.properties); SDK 35+ needs Java 21.
- Infinite animations must be skipped when `Clookie.motion.reduced` (screenshot tests use reduced motion).

## Layout
- design/ tokens, theme, shapes, haptics, icons · ui/components signature composables · ui/screens screens
- model/ pure domain logic (unit-tested) · data/ DataStore settings, Room schema (data/db)
