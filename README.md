# Foss PDF

Einfache Open-Source-PDF-Reader-App für Android (Kotlin + Jetpack Compose, PDFBox-Android / Apache-2.0).

## Funktionen

- PDF über den System-Dateipicker öffnen (Liste „Zuletzt geöffnet")
- Blättern per Wischgesten
- Doppel-Tipp zoomt die aktuelle Seite (einfacher Tap blendet die Bedienung aus/ein)
- Volltextsuche mit Trefferzähler, Vor/Zurück und Hervorhebung im Dokument
- PDF über das Android-Share-Sheet teilen
- PDF direkt umbenennen

Die App ist zweisprachig (Deutsch/Englisch, je nach Systemsprache).

## Lokaler Build

Voraussetzung: JDK 17.

```bash
./gradlew :app:assembleDebug
```

## Releases (automatisch bei jedem Push)

GitHub Actions (`.github/workflows/release.yml`):

- **Jeder Push auf einen Branch** → Test-Release als Pre-Release (Tag `test-<run-number>`)
- **Push eines Tags `vX.Y.Z`** → normaler Release (Tag `vX.Y.Z`, kein Pre-Release) mit APK zum Herunterladen

```bash
# versionName in app/build.gradle.kts an den Tag anpassen, dann:
git tag v0.1.0
git push origin v0.1.0
```

**Signiertes Release:** Sobald die folgenden 4 Repository-Secrets existieren
(GitHub → Settings → Secrets and variables → Actions), wird das Release-APK damit signiert:

- `KEYSTORE_BASE64` – Base64-kodiertes PKCS12-Keystore
- `KEYSTORE_PASSWORD` – Store-Passwort
- `KEY_ALIAS` – z. B. `fosspdf`
- `KEY_PASSWORD` – Key-Passwort

Keystore lokal erzeugen (PKCS12, Android-signiertauglich):

```bash
keytool -genkeypair -v -keystore release.p12 -storetype PKCS12 -alias fosspdf \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Foss PDF"
base64 -w0 release.p12   # macOS: base64 -i release.p12
```

Solange die Secrets fehlen, wird stattdessen ein debug-signiertes (installierbares) APK veröffentlicht.

## F-Droid

Ziel: Aufnahme in den offiziellen F-Droid-Katalog.

Bereits vorbereitet in diesem Repo:

- Store-Texte (EN/DE) und 512px-Icon unter `fastlane/metadata/android/` (F-Droid liest die Fastlane-Struktur aus dem App-Repo)
- Entwurf der fdroiddata-Metadata unter `fdroid/metadata.yml`
- Reproducible-Builds-Voraussetzungen: gepinnte Versionen, `dependenciesInfo` deaktiviert (in `app/build.gradle.kts`)

Vorgehen:

1. Erstes Release taggen: `git tag v0.1.0 && git push origin v0.1.0` (muss zu `versionName` in `app/build.gradle.kts` passen; `UpdateCheckMode: Tags ^v[0-9.]+$` ignoriert die `test-*`-Tags)
2. Fork von [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata) erstellen, `fdroid/metadata.yml` als `metadata/dev.momro.fosspdf.yml` übernehmen (Commit-Hash des Tags eintragen), Merge Request mit Label „New App" öffnen
3. F-Droid baut die App aus dem Source und signiert sie mit dem eigenen F-Droid-Key (unser Keystore ist nur für die GitHub-Releases). Nach dem Merge ist die App nach ca. 24–48 h im Katalog; Updates laufen danach automatisch über neue `v*`-Tags

Lokal prüfen: `pip install fdroidserver` → `fdroid lint metadata/dev.momro.fosspdf.yml` (die fdroiddata-CI prüft es beim MR ebenfalls).

## Lizenz

[GPL-3.0](LICENSE)
