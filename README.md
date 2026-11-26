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

GitHub Actions (`.github/workflows/release.yml`) baut bei **jedem Push** ein APK und
veröffentlicht es als GitHub-Release (Pre-Release, Tag `test-<run-number>`).

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

## Lizenz

[GPL-3.0](LICENSE)
