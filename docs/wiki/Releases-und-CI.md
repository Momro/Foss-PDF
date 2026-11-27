# Releases & CI

Workflow: `.github/workflows/release.yml` (läuft bei jedem Push auf einen Branch, bei Tag-Push `v*`, und manuell).

## Release-Arten

- **Branch-Push** → Pre-Release mit Tag `test-<run-number>` und `Foss-PDF.apk` (zum Testen).
- **Tag-Push `vX.Y.Z`** → normaler Release (kein Pre-Release) mit `Foss-PDF.apk`. Vorher `versionName` in `app/build.gradle.kts` passend setzen und `versionCode` erhöhen (F-Droid verlangt steigende versionCode; `UpdateCheckMode: Tags ^v[0-9.]+$` ignoriert `test-*`).

```bash
git tag v0.1.5 && git push origin v0.1.5
```

## CI-Ablauf

1. JDK 17, optionales Keystore aus Secrets (`KEYSTORE_BASE64` → PKCS12; sonst debug-signiertes APK).
2. `./gradlew :app:testDebugUnitTest`, dann `assembleRelease` (mit Keystore) oder `assembleDebug`, 3 Versuche mit Retry (transiente 429 von Maven).
3. APK wird als `Foss-PDF.apk` benannt und via `softprops/action-gh-release` veröffentlicht.

## Signiertes Release

Repo-Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (PKCS12, siehe README).

## F-Droid

- Store-Texte/Icon unter `fastlane/metadata/android/`, Metadata-Entwurf `fdroid/metadata.yml`, `dependenciesInfo` in `app/build.gradle.kts` deaktiviert (reproducible builds). Aufnahme via fdroiddata-MR (siehe README).

## Branches/PRs

- Änderungen landen auf Feature-Branches (`vibe/...`) mit Draft-PR gegen `main`; PR-Push erzeugt automatisch Test-Release. Bestehenden PR-Branch aktualisieren (pushen), nicht ersetzen.
