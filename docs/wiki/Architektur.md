# Architektur

## Stack

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01 / Compose 1.7.6), Material 3
- PDFBox-Android (`com.tom-roush:pdfbox-android:2.0.27.0`, basiert auf PDFBox 2.0.27)
- DataStore (Preferences) für „Zuletzt geöffnet", DocumentFile/SAF für Dateizugriff
- minSdk 26, targetSdk 35, Java 17

## Wichtige Dateien

| Datei | Zweck |
|---|---|
| `app/src/main/java/dev/momro/fosspdf/MainActivity.kt` | Einstieg, Intent-Handling (Launcher + `ACTION_VIEW` von außen), Crash-Handler + Crash-Screen, Screen-Navigation (Home ↔ Viewer) |
| `app/src/main/java/dev/momro/fosspdf/ui/home/HomeScreen.kt` | Home mit Dateipicker (SAF `OpenDocument`, `*/*`) und „Zuletzt geöffnet"-Liste |
| `app/src/main/java/dev/momro/fosspdf/ui/viewer/PdfViewerScreen.kt` | Viewer: Top-Bar (Suche, Text-Auswahl, Teilen, Umbenennen), `HorizontalPager`, Seiten-Slider, Auswahlmodus |
| `app/src/main/java/dev/momro/fosspdf/ui/viewer/ZoomablePage.kt` | Eine Seite: Bitmap + transparente Textebene, Tap/Doppel-Tap/Pinch/Pan-Gesten (ein eigener Pointer-Loop) |
| `app/src/main/java/dev/momro/fosspdf/pdf/PdfViewerViewModel.kt` | Dokument laden (mit `file://`-Fallback), Seiten rendern (Bitmap-Cache), Volltextsuche, Textzeilen pro Seite, Teilen, Umbenennen |
| `app/src/main/java/dev/momro/fosspdf/pdf/PdfSearch.kt` | Suche mit Positionen (`PositionStripper` extends `PDFTextStripper`), Treffer-Rechtecke |
| `app/src/main/java/dev/momro/fosspdf/pdf/PdfTextLayer.kt` | Textextraktion pro Seite → Zeilen (`TextRun`), rein testbar via `buildTextRuns` (siehe `TextLayerTest`) |
| `app/src/main/java/dev/momro/fosspdf/data/RecentRepository.kt` | DataStore für Zuletzt geöffnet |
| `app/src/main/AndroidManifest.xml` | `MainActivity` mit `launchMode="singleTop"` und mehreren `VIEW`-Intent-Filtern (siehe Funktionen) |
| `app/build.gradle.kts` | `versionName`/`versionCode`, APK-Ausgabe `Foss-PDF.apk`, F-Droid-konform (`dependenciesInfo` aus) |
| `.github/workflows/release.yml` | Build & Release (siehe Releases-und-CI) |

## Datenfluss

1. `MainActivity.onCreate`/`onNewIntent`: `ACTION_VIEW`-URI → `Screen.Viewer(uri)`, sonst `Screen.Home`.
2. `PdfViewerScreen` erzeugt pro URI einen `PdfViewerViewModel` (Key = URI). Der ViewModel lädt das `PDDocument` auf IO-Thread, liest Seitenanzahl/-größen.
3. `PageItem` (pro Pager-Seite) rendert die Seite lazy via `vm.renderPage(...)` und lädt die Textzeilen via `vm.textRunsForPage(...)`.
4. `ZoomablePage` zeigt Bitmap + Textebene, verwaltet Zoom/Pan-Zustand lokal.

## Hinweise für Erweiterungen

- Neue UI-Features im Viewer immer in `PdfViewerScreen`/`ZoomablePage` verankern; Dokument-Logik gehört in den ViewModel (IO-Thread, Ergebnisse per `withContext(Dispatchers.Main)`).
- PDFBox-Koordinaten: `TextPosition.getXDirAdj()/getYDirAdj()/getHeightDir()` sind bereits „y von oben" im gerenderten Bitmap-Koordinatensystem (gleiche Basis wie die Such-Highlights in `PdfSearch.kt`).
