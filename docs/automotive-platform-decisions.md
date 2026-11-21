# Automotive platform decisions (roadmap branch)

Date: 2026-11-21. Primary-source research: developer.android.com / source.android.com / Google Play policy.
This document records why some roadmap candidates are implemented, gated, or not shippable.

## Implemented in `feat/automotive-roadmap`
- Back safety: player first Back hides controls without stopping; main default double-Back with 400ms debounce / 1500ms window, localized prompt.
- Live tiles: Continue watching row from recent channels + viewing time, existing ChannelCard touch/D-pad/long-press parity, parental lock preserved.
- User-controlled cleanup: Settings → Backup & Network → Clear cache, confirmation dialog, only `cacheDir` contents.
- Car screen preset policy: 64dp targets / 32dp icons / 24dp gaps / 24sp minimum; WCAG contrast helpers; dark scrim for LiquidGlass.

## Not defensible as shippable safe features
1. **Arbitrary phone-screen projection / launching arbitrary apps on Android Auto head unit.**
   Official Android Auto supports category apps (media audio, navigation, messaging, POI/IoT/weather) and limited parked categories. There is no public official mechanism for arbitrary screen mirroring/app launch.
   Sources: https://developer.android.com/training/cars, https://developer.android.com/training/cars/parked, https://developer.android.com/training/cars/parked/auto
   Repo legacy `CATEGORY_PROJECTION`, `aauto.aar`, Xposed DO/speed/install-source hooks are outside the official model and must not be promoted as a safe product feature.

2. **Video/UI on NavigationTemplate surface.**
   Navigation category expects a map + NavigationManager metadata/TBT. Rendering IPTV video on that surface is category abuse and violates distraction requirements.
   Sources: https://developer.android.com/training/cars/apps/navigation, https://developer.android.com/reference/androidx/car/app/navigation/model/NavigationTemplate, https://developer.android.com/docs/quality-guidelines/car-app-quality

3. **Accessibility touch injection for remote control.**
   Play restricts AccessibilityAPI to disability tools or narrow disclosed automation with declaration. Current `TouchInjectionService` reads window content and injects gestures; not a defensible Play feature.
   Source: https://support.google.com/googleplay/android-developer/answer/10964491

4. **Mandatory Wi-Fi ADB setup.**
   Wireless debugging is a developer tool requiring pairing/authorizations and granting shell-level access. It must not be a mandatory consumer setup path. Optional dev documentation only after separate security review.
   Sources: https://developer.android.com/tools/adb, https://developer.android.com/studio/debug/dev-options

## Gated / needs hardware or program access
- Video on Android Auto: beta EAP, Android 15+ head unit; public parked category currently Games only.
  Sources: https://developer.android.com/training/cars/parked/video, https://developer.android.com/training/cars/parked/auto
- Templated media: Early Access Program, internal/closed testing tracks only.
- Real navigation integration: requires NavigationManager, TBT, map surface, cluster behavior; not just a tile.
- Physical head-unit validation: DHU/emulator presets are useful but OEM hosts differ; `SurfaceCallback.onClick` may not be called on some systems.
  Sources: https://developer.android.com/training/cars/testing/dhu, https://developer.android.com/reference/androidx/car/app/SurfaceCallback

## Known repo defects if experimental mirror is kept
TargetSdk 35 means Android 14+ MediaProjection rules apply:
- consent before each projection session; token used once; one `createVirtualDisplay` per token;
- foreground service type mediaProjection started before `getMediaProjection`;
- current `MirrorProjectionService` caches/reuses token and `OpenIptvMirrorActivity` recreates VirtualDisplay on same token; order is wrong.
Sources: https://developer.android.com/media/grow/media-projection, https://developer.android.com/reference/android/media/projection/MediaProjectionManager

## Recommended next solution path
1. Stabilize official audio path (`PlaybackService` MediaLibraryService) and test with DHU 800x480 / 1920x1080 presets.
2. Separate release vs experimental flavor: release only official components; legacy/Xposed/mirror/accessibility behind explicit experimental build, not Play release.
3. If navigation is desired, implement real navigation category or remove NAVIGATION category/surface video from release.
4. For parked video, target AAOS parked category and/or apply to Android Auto video EAP; validate on physical head unit.
5. Add network-security-config and remove global cleartext allowance except documented scoped exceptions.