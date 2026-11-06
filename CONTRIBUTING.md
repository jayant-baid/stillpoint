# Contributing to Stillpoint

Thanks for helping. You don't need to write code to contribute. These are all useful:

- **Test it on your phone** and report what happens, especially on brands other than OnePlus and in
  light mode.
- **Suggest changes,** or tell me what should be removed. Open an issue.
- **Send a pull request.** For anything large, open an issue first so we can agree on the approach.

## Rules that protect users

This app holds powerful permissions, so these aren't up for negotiation in a pull request:

1. **No internet access.** The `INTERNET` permission stays removed, and no feature may need it.
2. **No reading screen content.** `canRetrieveWindowContent` stays off.
3. **`WRITE_SECURE_SETTINGS` is used only for the colour filter.**
4. **Nothing new is exported** without a clear reason explained in the pull request.
5. **New permissions and dependencies need a justification** in the pull request. Fewer is better.

## Building and testing

Requires JDK 17 and the Android SDK (platform 35). You don't need a signing key.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Then install the debug build. It can't be installed over a release build, so uninstall that first.
Installing also needs the grant from the README if you're testing grayscale.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Please add unit tests for changes to the rules in `core/`, which has no Android dependencies. For
anything involving the service, also try it on a real device, and say in your pull request which
phone and Android version you used.

## Project layout

| Path | What's there |
|---|---|
| `core/` | Limit rules, reset time logic, saved format. Pure Kotlin, covered by unit tests |
| `data/` | Saved state and the installed app list |
| `service/` | Accessibility service, grayscale switching and the stop screen |
| `ui/` | Compose screens and the design system |

## Design

Black and white throughout. `colorScheme.error` is the one colour, and it only ever means that time
is running out or has run out. Please keep it that way, and avoid decorative animation.

## Testing pitfalls

- **`uiautomator dump` disconnects accessibility services** each time it runs, so time won't count
  properly if you run it during a timed test. Use `adb exec-out screencap` instead.
- **Screenshots don't show the grayscale filter.** Check it by eye, or read
  `adb shell settings get secure accessibility_display_daltonizer_enabled`.
- **`pm clear` turns the accessibility service off,** so you have to turn it back on afterwards.

By contributing, you agree that your work is released under the [GPL-3.0](LICENSE).
