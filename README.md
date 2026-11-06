# hakka26-android

Standalone native Android app in Kotlin and Jetpack Compose. This repository is not a fork. It is independent of the Expo and Swift workspaces.

`submissions/aymanissa-dev-android/` is the contest-shaped entry:

- `app/` is the OTP verification sheet. It starts from the public `templates/android-compose` starter and keeps that Gradle setup.
- `PROMPT.md` is the single prompt for the screen.
- `TEAM.md` lists the author.
- `screenshot.png` is a capture of this Compose UI running on an emulator, in the partially filled state.
- `demo.mp4` is a screen recording of that same app: a rejected code, then `123456`.

This repo does not open a pull request against the contest upstream.

From `submissions/aymanissa-dev-android/app`:

```sh
./gradlew assembleDebug testDebugUnitTest
```
