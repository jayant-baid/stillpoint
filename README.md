# Stillpoint

Daily app limits, shared limits across groups of apps, and grayscale that switches on only for the
apps you choose. For Android, open source, and with no internet access at all.

<p>
  <img src="docs/stop-screen.png" width="260" alt="Stop screen shown when an app's limit runs out">
  <img src="docs/settings.png" width="260" alt="Settings tab with setup status, reset time and lock">
</p>

## What it does

- **App limits:** give an app a daily limit. When the time runs out, a full-screen stop page covers
  it until limits reset.
- **Groups:** apps in a group share one limit. Time in any of them counts against the same total,
  and when it runs out they all stop.
- **Per-app grayscale:** pick apps from a list. The screen turns gray while one of them is open and
  goes back to normal colours about a second after you switch away.
- **Your own reset time:** limits reset at midnight by default, or any minute you choose. With a
  6:00 AM reset, an app you used up at 11 PM stays blocked until 6:00 AM.
- **Optional lock:** once a limit runs out, you can't change or remove it until limits reset.
- **Works alongside UPI apps:** Stillpoint doesn't use an accessibility service, which is the
  permission that made Paytm refuse to run in earlier testing.

Inspired by [Mindful](https://github.com/akaMrNagar/Mindful). Stillpoint is a separate app with its
own code, and isn't affiliated with that project.

## Project status

Early, and looking for feedback. It has been tested on **one phone: a OnePlus on Android 16, in dark
mode**, installed over USB, with Paytm as the only payment app tried. Other brands, light mode,
tap-to-install, other payment apps and long-term battery use are untested, and reports on any of
them are the most useful thing you can send.

## Install

1. Download `app-release.apk` from [Releases](../../releases) and tap it to install. Android may warn
   you about installing an app from outside the Play Store.
2. Open Stillpoint and go to **Settings**.
3. Allow **Usage access**, then **Display over other apps**.

That's all you need for limits and groups. No computer is required.

If a permission is grayed out, go to **App info > ⋮ > Allow restricted settings** and try again.

### Optional: grayscale

Android doesn't let an app ask for the permission that controls the colour filter, so this needs a
computer with [adb](https://developer.android.com/tools/releases/platform-tools), once per install:

1. Turn on **Developer options > USB debugging** and connect your phone.
2. Run:

   ```bash
   adb shell pm grant com.stillpoint.app android.permission.WRITE_SECURE_SETTINGS
   ```

   On OnePlus, Oppo and Realme this can fail with a `SecurityException`. Turn on **Developer options
   > Disable permission monitoring**, restart the phone and run it again.

3. Turn Developer options off, then check that the steps in the Settings tab still say Done.

## Permissions and privacy

This app asks for trust, so here is exactly what it does with it.

| Permission | Why | Limit on it |
|---|---|---|
| Usage access (`PACKAGE_USAGE_STATS`) | Shows which app is in front, about once a second, so it can count time and apply your rules | Reveals which apps you open and when, but not what's inside them |
| Display over other apps (`SYSTEM_ALERT_WINDOW`) | Lets the stop screen open from the background | Used only to open Stillpoint's own stop screen |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED` | Keep counting running, including after a restart | The restart receiver does nothing except start the counting service |
| `WRITE_SECURE_SETTINGS` (optional) | Switches Android's colour filter | Powerful in general, but the code writes only the two colour filter settings, in [`GrayscaleController`](app/src/main/java/com/stillpoint/app/service/GrayscaleController.kt) |
| `QUERY_ALL_PACKAGES` | Lists your apps so you can pick them | Stays on the phone |

- **No internet.** The [manifest](app/src/main/AndroidManifest.xml) removes the `INTERNET` permission,
  so Android itself blocks the app from sending or receiving anything. There's no analytics, no
  accounts and no ads.
- **No accessibility service.** It can't read your screen, what you type or your messages.
- **Your data stays on the phone.** Limits and usage are stored only in the app's private storage,
  and are excluded from cloud backup and device transfer.
- **Almost nothing to call into.** Other apps can open the launcher screen and nothing else. The
  restart receiver is exported only because Android requires it, for system broadcasts other apps
  can't send.

To take the grayscale permission back without uninstalling:

```bash
adb shell pm revoke com.stillpoint.app android.permission.WRITE_SECURE_SETTINGS
```

## Good to know

- Time is counted only while the phone is unlocked with the app in front. Background audio isn't
  counted.
- A blocked app or a grayscale change can take about a second to apply after you switch apps.
- This is a tool for self-control, not a parental control. Removing Usage access stops all limits,
  even with the lock on.
- Changing the reset time keeps time already used, and it can't be changed while a limit is locked.
- If you already use Android's colour correction, your setting is saved and put back when a grayscale
  app closes.
- If you uninstall while the screen is gray, turn it off in **Settings > Accessibility > Colour
  correction**.
- If limits stop counting after a while, your phone's battery manager may be stopping the app. Set
  Stillpoint's battery use to unrestricted.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Contributing

Suggestions and pull requests are welcome, including ideas for what to remove. See
[CONTRIBUTING.md](CONTRIBUTING.md). Please report security problems privately, as described in
[SECURITY.md](SECURITY.md).

## License

[GPL-3.0](LICENSE). The Archivo font is under the
[SIL Open Font License](app/src/main/assets/fonts/OFL-Archivo.txt).
