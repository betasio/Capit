# Capit for Android

Free Instagram-only prototype for keeping connection while reducing scrolling.
Use Instagram **inside Capit**; this does not modify or block the installed Instagram app.

## Visual refresh · 0.2

- Native home screen in ivory, forest green, mint and lavender, with vector conversation artwork.
- Original C/conversation logo with adaptive and Android 13 themed launcher icons.
- Bottom navigation and selected states, larger touch targets, scalable text and a scrollable home layout.
- Short 220 ms fade/slide entrances, ripple feedback, and animated loading progress.
- **More → Reduce motion** disables entrance/progress motion; Android’s disabled animation setting is also respected. No looping decorative animation.
- The Instagram filter script is unchanged from the version tested on the owner’s phone.

## Current implementation

- Native Android browser with Instagram login, Messages, Stories/home, profile lookup and an experimental Following shortcut.
- Early document injection hides recognized Reel/Explore links and Reel-containing post cards.
- Native navigation guards, click interception and SPA history guards prevent recognized Reel/Explore routes from opening. History visits to blocked routes show a quiet placeholder, without automatically going back.
- Standard home-feed `article` elements are hidden so that the Stories UI can remain.
- Experimental Following URL preserves regular articles and filters recognized English suggestion/sponsor labels.
- System image/video file picker for web uploads; session reset; reload and network error messaging.
- No Capit backend, analytics, ads, subscription, accessibility permission or JavaScript-to-native bridge.

## What is NOT verified yet

This is a prototype, not a claim that Instagram's current logged-in website is fully supported.
No live Instagram account has been used during development. Instagram may reject embedded login,
change markup, or ignore the Following query parameter. A URL parameter is **not proof** of a following-only feed.
The app warns before opening that view. If unwanted posts appear, use Messages instead and report the screen.
Selectors do not reliably classify every video, localized suggestion or recommendation. Web updates can break them.

Calls, microphone/camera recording, push notifications, Facebook login, downloads, fullscreen video and
YouTube are outside v0.1. Stories and file uploads depend on what Instagram exposes on its mobile website.
Reels shared in DMs have their links hidden; the surrounding conversation is preserved.
Profile Reels tabs are blocked, but Reel thumbnails without identifiable links may need more filters.

## Get the test APK

1. Open this repository's **Actions** tab and choose a successful **Android prototype** run on the prototype branch/PR.
2. Download **Capit-debug-apk** under Artifacts (GitHub sign-in required), unzip it, and install `app-debug.apk` on Android.
3. Android may ask you to allow installation from the browser/file manager you used.
4. Open Capit and sign in directly on Instagram's webpage. Never put passwords or session cookies in issues.

Requires Android 8+ and an up-to-date Android System WebView/Chrome with document-start script support.
The app refuses to load Instagram if that feature is unavailable. Debug APKs are for testing; GitHub-hosted
runners generate temporary signing keys, so later builds may require uninstalling the previous test build.
Uninstalling removes the local session. A stable release signing setup is a later milestone.

## Build locally

Install JDK 17, Android SDK platform 35 and Gradle 8.11.1. Open the folder in Android Studio,
set the SDK path in your untracked `local.properties`, then run:

```sh
gradle :app:assembleDebug :app:lintDebug
node --test tests/*.test.cjs
npm install --no-save playwright@1.51.1
npx playwright install chromium
node tests/dom.cjs
```

The repository deliberately uses an installed, fixed Gradle version in CI; a Gradle wrapper is not bundled yet.
AGP is pinned to 8.9.2 and AndroidX WebKit to 1.12.1.

## Phone acceptance checklist

- Login, 2FA/challenge, close/reopen, and session reset.
- Read/send a DM; shared Reel must not remove other messages.
- View a Story and return to inbox.
- View a normal profile/photo/carousel; upload an image from the system picker.
- Open Following and verify every visible author is followed; note if Instagram falls back to home.
- Try Reel/Explore links, profile Reels tabs, navigation/back and dynamically loaded cards.
- Check that Reel audio stops, normal Story/DM video still works, and filters survive rotation/relaunch.
- Repeat with your language, text size and WebView version; check offline/retry behavior.

Record device/Android/WebView version and the failed screen type. Redact usernames, messages and personal
information from screenshots. Do not attach tokens, cookies, passwords or private page dumps.

Capit is an independent project, not affiliated with Instagram or Meta.
