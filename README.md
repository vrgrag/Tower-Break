# Tower Break — Native Android (Kotlin)

A full migration of the **Tower Break** Flutter game to native Android: Kotlin,
Jetpack Compose, Clean Architecture + MVVM/MVI, Hilt, Coroutines/Flow and
DataStore. All gameplay, visuals, animation, audio and economy are preserved;
the architecture and code shape are re-thought to be idiomatically Android.

## Build

Requires **JDK 17** and the **Android SDK** (compileSdk 35). Open the
`android-native/` folder in **Android Studio (Ladybug or newer)** and let it sync,
or from the command line:

```bash
./gradlew :app:assembleDebug      # once the Gradle wrapper jar is present
```

> The wrapper `.properties` is included; run `gradle wrapper` (or open in Android
> Studio) once to generate `gradle-wrapper.jar` if it is missing.

## Architecture at a glance

```
core/         cross-cutting: palette, typography-free helpers, asset manifest, DI qualifiers, clock
data/         DataStore preference store, repository implementations, SoundPool/MediaPlayer audio
domain/       entities, repository interfaces, use cases (pure Kotlin, no Android deps except Color)
presentation/ Compose UI + ViewModels, split per feature: splash / hub / game / webview / common
  game/engine   the physics simulation (TowerRoundEngine) + collaborators — pure, frame-driven
  game/render   Compose Canvas renderer + texture cache
di/           Hilt modules (composition root)
```

- **State**: every store publishes an immutable snapshot via `StateFlow`; the
  round engine adds a per-frame `PlayfieldSnapshot`, a discrete `RoundHudState`
  and a one-shot `RoundEvent` `SharedFlow`.
- **Rendering**: a single Compose `Canvas` draws the scene top-down each frame,
  driven by a `withFrameNanos` loop in `GameScreen`.
- **DI**: Hilt replaces the Flutter `ServiceHub`; repositories are bound to
  interfaces and collected into a `Set<Hydratable>` the splash warms up.
- **Persistence**: DataStore replaces `SharedPreferences`; the on-disk key names
  are preserved.

See the migration report accompanying this project for the module-by-module
mapping from the original Dart.
