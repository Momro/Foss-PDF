# Foss PDF – Wiki

Einfache Open-Source-PDF-Reader-App für Android (Kotlin + Jetpack Compose, Apache PDFBox-Android, GPL-3.0).
Repo: https://github.com/Momro/Foss-PDF

## Inhalt

- [Architektur](Architektur) – Aufbau, wichtige Dateien, Datenfluss
- [Funktionen](Funktionen) – wie die einzelnen Features umgesetzt sind (high level, mit Code-Stellen)
- [Releases & CI](Releases-und-CI) – Test-Releases bei jedem Push, v-Tags für echte Releases, Signierung, F-Droid

## Kurzfassung

- App-Start: `MainActivity` entscheidet anhand des Intents zwischen Home (`HomeScreen`) und Viewer (`PdfViewerScreen`).
- PDFs rendern: PDFBox (`PDFRenderer.renderImage`) als Bitmap pro Seite; Seiten im `HorizontalPager`.
- Text: pro Seite extrahierte, transparente selektierbare Textebene (`PdfTextLayer.kt`) + Auswahlmodus ohne Pager.
- Releases: jeder Branch-Push → Pre-Release `test-<run>` mit `Foss-PDF.apk`; Tag `vX.Y.Z` → normaler Release.
