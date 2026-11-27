# Funktionen – wie umgesetzt (high level)

## PDF öffnen

- In-App: `HomeScreen` nutzt SAF (`ActivityResultContracts.OpenDocument`, MIME `*/*`), nimmt persistierbare Lese-Berechtigung (`takePersistableUriPermission`) und öffnet den Viewer.
- Von außen (Dateimanager/Browser): `AndroidManifest.xml` hat mehrere `VIEW`-Intent-Filter:
  1. nur MIME-Typen (`application/pdf`, `application/x-pdf`) – matcht getypte `content:`/`file:`-URIs,
  2. `scheme content/file + host * + pathPattern .*\.pdf` **ohne** MIME – matcht ungetypte `.pdf`-URIs,
  3. `scheme + mimeType application/octet-stream + pathPattern .*\.pdf` – falsch deklarierte PDFs.
  Dazu `launchMode="singleTop"` (damit `onNewIntent` feuert), `takePersistableUriPermission`-Versuch in `MainActivity.externalViewUri()` und in `PdfViewerViewModel.openStream()` ein `file://`-Fallback (direkter `FileInputStream`), falls der ContentResolver die URI nicht öffnen kann.

## Rendern & Blättern

- Pro Seite `PDFRenderer.renderImage(page, scale)` (scale = `fit * BASE_RENDER_SCALE`, `BASE_RENDER_SCALE = 2f` in `PdfViewerScreen.kt`), Bitmap-Cache (`LruCache`, 6 Einträge) im ViewModel.
- Seitenwechsel über `HorizontalPager` + `rememberPagerState`; Seiten-Slider unten; Tap blendet Chrome aus/ein.

## Pinch-Zoom & Pan (zwei Finger)

- `ZoomablePage.kt`: ein eigener Pointer-Loop (`awaitPointerEventScope`) statt mehrerer konkurrierender Detektoren.
  - 2+ Finger: Pinch skaliert kontinuierlich 1x–5x um den Finger-Mittelpunkt (Offset-Mathe um den Layer-Pivot), Pan des Centroids inklusive.
  - 1 Finger: Pan nur wenn `scale > 1` (sonst Events nicht konsumieren → Pager-Swipe funktioniert weiter).
  - Events werden nur konsumiert, wenn gezoomt/gepincht wird; Doppel-Tap toggelt 2,5x (`ZOOM_SCALE`).
- Wichtig: `PagerState.userScrollEnabled` existiert in Compose 1.7.6 **nicht** – nicht verwenden.

## Text markieren & kopieren (direkt im PDF)

- `PdfTextLayer.kt`: `SpanStripper` (extends `PDFTextStripper`, `sortByPosition = true`) sammelt pro Wort `TextPosition`s; `buildTextRuns()` gruppiert sie zu Zeilen (`TextRun`: text/x/y/width/height in PDF-Punkten, y von oben). PDFBox ruft `writeString(word, positions)` pro Wort auf (`writeLine` in `PDFTextStripper`).
- `ZoomablePage.PageTextLayer`: pro Zeile ein `Text(color = Color.Transparent)` in einer `SelectionContainer`, positioniert per `Modifier.offset { IntOffset }` mit `fit`-Skalierung (letterbox wie `ContentScale.Fit`), fontSize = `heightDir * fit`. Textebene liegt über dem Bitmap und skaliert/pannt mit (`graphicsLayer`).
- Auswahlmodus: Top-Bar-Button („Text markieren“, `Icons.Filled.SelectAll`) schaltet in `PdfViewerScreen` in einen Modus, der die Seite **ohne `HorizontalPager`** rendert (nur statische Seite, Seitenwahl über Slider). Dadurch hat kein scrollable-Vorfahre die Chance, das Pointer-Down in der Initial-Pass zu konsumieren – Long-press-Selection direkt auf der Seite funktioniert zuverlässig. (Hintergrund: Compose-Selection bricht ab, wenn das Down bereits konsumiert ist; im Pager kann das passieren.)
- Long-press → System-Toolbar mit Kopieren. Für reine Scan-PDFs (kein Text) gibt es nichts zu selektieren.

## Volltextsuche

- `PdfSearch.kt`: `PositionStripper` + `findMatchIndices` (case-insensitive), Treffer mit Rechtecken (`PdfRect`) aus `xDirAdj/yDirAdj/widthDirAdj/heightDir`; Rechtecke werden direkt ins Bitmap gezeichnet (`PdfViewerViewModel.renderPage`), Treffer-Navigation setzt `currentPage`.

## Teilen & Umbenennen

- `shareIntent()`: `ACTION_SEND` mit `EXTRA_STREAM` + `FLAG_GRANT_READ_URI_PERMISSION`.
- Umbenennen: `DocumentFile.fromSingleUri(...).renameTo(...)`; „Zuletzt geöffnet" wird mit umbenannt.

## Crash-Screen

- `MainActivity.installCrashHandler()` schreibt `last_crash.txt` in `cacheDir`; beim nächsten Start wird der Report in einer `SelectionContainer` (kopierbar) angezeigt.

## Tests

- `app/src/test/.../TextMatchTest.kt`, `TextLayerTest.kt` (reine Funktionen, z. B. `buildTextRuns`). CI: `./gradlew :app:testDebugUnitTest`.
- Kein lokaler SDK nötig; Build/APK kommen aus CI.
