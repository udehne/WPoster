# WordPREST

**Fotos und Videos vom Handy direkt als Beitrag auf deinem WordPress-Blog veröffentlichen.**

WordPREST ist eine schlanke Android-App für private (Familien-)Blogs. Du wählst
Fotos oder Videos aus, gibst eine Überschrift und optional Texte ein, und mit einem
Tipp ist der Beitrag online. Die App funktioniert mit jeder WordPress-Seite.
Adresse und Zugangsdaten gibst du nach der Installation in der App ein.

---

## Inhalt

1. [Was die App kann](#1-was-die-app-kann)
2. [Voraussetzungen](#2-voraussetzungen)
3. [App installieren](#3-app-installieren)
4. [App einrichten](#4-app-einrichten)
5. [Beitrag veröffentlichen](#5-beitrag-veröffentlichen)
6. [Einstellungen](#6-einstellungen)
7. [Hilfe bei Problemen](#7-hilfe-bei-problemen)
8. [Datenschutz & Sicherheit](#8-datenschutz--sicherheit)
9. [Für Entwickler: Bauen & Signieren](#9-für-entwickler-bauen--signieren)
10. [Impressum](#10-impressum)

---

## 1. Was die App kann

- **Startseite:** Titel und Untertitel deines Blogs, darunter die neuesten Beiträge
  mit Beitragsbild, Titel, Autor und Datum. Zum Aktualisieren nach unten ziehen;
  ältere Beiträge werden beim Scrollen nachgeladen.
- **Beitragsansicht:** Der komplette Beitrag mit allen Texten, Fotos und Videos.
  Unten: **Übersicht**, **Älterer**, **Neuerer** Beitrag und **Bearbeiten**.
- **Beiträge bearbeiten:** Überschrift, Texte und Zwischenüberschriften ändern,
  Fotos/Videos hinzufügen, verschieben und entfernen, Beitragsbild wählen (⭐)
  und das **Veröffentlichungsdatum** ändern (bestimmt die Reihenfolge).
- **Menü ☰:** Hilfe, Einstellungen, Impressum, Datenschutz.
- **Teilen aus der Galerie:** Fotos/Videos in Google Fotos, der Samsung-Galerie o. Ä.
  auswählen, dann **Teilen → WordPREST**. Der Upload startet sofort, und du tippst
  währenddessen Überschrift und Texte ein.
- **Beitrag in der App anlegen:** App öffnen → **Neuer Beitrag** → Fotos/Videos hinzufügen.
- **Aufbau jedes Beitrags** (Block-Editor):
  1. Überschrift
  2. Text vor den Fotos (optional)
  3. Fotos und Videos untereinander, in der gewählten Reihenfolge
  4. Text nach den Fotos (optional)
- Das **erste Foto wird automatisch Beitragsbild**.
- Reihenfolge per Pfeil ändern und einzelne Medien entfernen. Entfernte Medien
  werden auch auf dem Server wieder gelöscht.
- **Sofort veröffentlichen.** Danach kannst du den Beitrag **ansehen** oder gleich
  einen **weiteren Beitrag anlegen**.
- **Entwürfe:** Verlässt du den Editor, bleibt der Entwurf erhalten, bis du ihn
  veröffentlichst oder verwirfst.
- **Standort wird entfernt:** Fotos und Videos werden vor dem Hochladen von
  GPS-Daten und anderen Metadaten befreit.

## 2. Voraussetzungen

| Was | Details |
|---|---|
| Handy | Android 9 oder neuer |
| Blog | WordPress 5.6 oder neuer, erreichbar über **https://** |
| WordPress-Benutzer | mindestens Rolle **Autor** (darf hochladen und veröffentlichen) |
| Anwendungspasswort | wird in WordPress erzeugt, siehe [Abschnitt 4](#4-app-einrichten) |

Jede Person (z. B. du und deine Partnerin oder dein Partner) nutzt **ihren eigenen
WordPress-Benutzer** und **ihr eigenes Anwendungspasswort**. So steht bei jedem
Beitrag der richtige Name als Autor.

## 3. App installieren

Die App gibt es nicht im Play Store. Sie wird hier auf GitHub gebaut und als
APK-Datei bereitgestellt.

### Variante A: Release (empfohlen, am einfachsten auf dem Handy)

1. Auf dem Handy die GitHub-Seite des Projekts öffnen → **Releases**.
2. Beim neuesten Release die Datei **`WordPREST-1.0.x.apk`** antippen und herunterladen.
3. Die heruntergeladene Datei öffnen. Beim ersten Mal fragt Android, ob der Browser
   „Apps aus unbekannten Quellen installieren“ darf. Das einmal erlauben.
4. **Installieren** antippen.

### Variante B: Artefakt aus einem Build

1. GitHub → **Actions** → Workflow **„Android-APK bauen“** → neuesten grünen Lauf öffnen.
2. Unten unter **Artifacts** die Datei **`WordPREST-1.0.x`** herunterladen
   (dafür muss man bei GitHub angemeldet sein).
3. Es ist ein **ZIP**. Entpacken (z. B. mit der App „Dateien“), dann die APK
   öffnen und installieren wie oben.

### Updates

Eine neue Version einfach genauso herunterladen und installieren. Sie ersetzt die
alte Version, und Zugangsdaten und Einstellungen bleiben erhalten. Voraussetzung ist,
dass der Signierschlüssel hinterlegt ist, siehe [Abschnitt 9](#9-für-entwickler-bauen--signieren).

## 4. App einrichten

Beim ersten Start öffnen sich automatisch die **Einstellungen**. Neben jedem Feld
findest du ein **ⓘ**. Ein Tipp darauf erklärt, was dort hingehört und wo du es findest.

### Schritt 1: Anwendungspasswort in WordPress erstellen

Das Anwendungspasswort ist **nicht** dein normales WordPress-Passwort. Es ist ein
eigenes Passwort nur für diese App und lässt sich jederzeit widerrufen.

1. Im Browser (gern am Computer) bei WordPress anmelden: `https://deine-seite.de/wp-admin`
2. Links im Menü **Benutzer → Profil**.
3. Ganz nach unten scrollen bis **„Anwendungspasswörter“**.
4. Bei **„Name des neuen Anwendungspassworts“** z. B. `WordPREST Handy Uli` eingeben.
5. **„Neues Anwendungspasswort hinzufügen“** klicken.
6. Das angezeigte Passwort (24 Zeichen, z. B. `abcd EFGH 1234 ijkl MNOP 5678`) kopieren.
   **Es wird nur dieses eine Mal angezeigt.** Du kannst es dir z. B. per Mail selbst
   schicken oder direkt am Handy erzeugen.

> Fehlt der Abschnitt „Anwendungspasswörter“, ist die Funktion auf deiner Seite
> deaktiviert, meist durch ein Sicherheits-Plugin. Dort kann man sie wieder einschalten.

### Schritt 2: Daten in der App eintragen

| Feld | Was eintragen | Wo finde ich das? |
|---|---|---|
| **Adresse** | z. B. `https://udehne.de` | Adresszeile im Browser, wenn du deinen Blog öffnest (ohne `/wp-admin`) |
| **Benutzername** | dein Anmeldename | WordPress → Benutzer → Profil → „Benutzername“ (ganz oben, grau) |
| **Anwendungspasswort** | das Passwort aus Schritt 1 | WordPress → Benutzer → Profil → Anwendungspasswörter |

Dann **„Verbindung prüfen & speichern“** antippen. Bei Erfolg erscheint der Name
deines Blogs und deines Benutzers, und die App ist bereit.

## 5. Beitrag veröffentlichen

### Weg 1: Aus der Galerie teilen

1. In deiner Foto-App ein oder mehrere Fotos/Videos auswählen.
2. **Teilen** → **WordPREST** antippen.
3. Die App öffnet den Editor und **lädt sofort hoch**. Den Fortschritt siehst du an jedem Bild.
4. Währenddessen **Überschrift** und optional **Text vor** und **Text nach den Fotos** eingeben.
5. Sobald alles hochgeladen ist: **Veröffentlichen**.
6. **Beitrag ansehen** öffnet ihn im Browser.

### Weg 2: In der App

1. App öffnen → großes **+** unten rechts.
2. **Hinzufügen** → Fotos/Videos auswählen (mehrere möglich, auch nachträglich weitere).
3. Überschrift und Texte eingeben → **Veröffentlichen**.
4. Danach: **Beitrag ansehen** oder **Weiteren Beitrag anlegen**.

### Beitrag ansehen und bearbeiten

1. Auf der Startseite einen Beitrag antippen. Er wird vollständig angezeigt.
2. Mit **Älterer** und **Neuerer** blätterst du durch die Beiträge, **Übersicht** führt zurück zur Liste.
3. **Bearbeiten** öffnet den Editor:
   - **Überschrift** und **Veröffentlichungsdatum** (Tippen auf das Datum → Tag und Uhrzeit wählen).
     Ein Datum in der Zukunft plant den Beitrag, er ist dann erst ab diesem Zeitpunkt sichtbar.
   - **Inhalt:** Jeder Absatz, jede Zwischenüberschrift und jedes Foto/Video ist ein eigener
     Block. Mit ▲▼ verschieben, mit ✕ entfernen.
   - **Hinzufügen:** Text, Titel (Zwischenüberschrift) oder Medien (Fotos/Videos werden sofort hochgeladen).
   - **⭐** an einem Foto macht es zum Beitragsbild.
4. **Speichern**. Danach siehst du den aktualisierten Beitrag.

Gut zu wissen:
- Inhalte, die die App nicht bearbeiten kann (z. B. Galerien oder Spalten aus dem
  Web-Editor), bleiben unverändert erhalten und lassen sich nur verschieben oder entfernen.
- Absätze mit Formatierungen (z. B. Links, fett) bleiben erhalten, solange du sie nicht
  änderst. Änderst du den Text, wird er als einfacher Text gespeichert.
- Aus einem Beitrag entfernte Fotos bleiben in der WordPress-Mediathek.
- Eigene Beiträge darf jeder **Autor** bearbeiten. Für Beiträge anderer Personen braucht
  der WordPress-Benutzer die Rolle **Redakteur** oder **Administrator**.

### Tipps

- **Absätze:** Eine Leerzeile im Text erzeugt einen neuen Absatz, ein einfacher
  Zeilenumbruch bleibt ein Zeilenumbruch.
- **Reihenfolge:** Mit den Pfeilen ▲▼ neben jedem Bild verschieben.
- **Fehler beim Upload:** Bei dem Bild auf **„Erneut versuchen“** tippen oder es mit ✕ entfernen.
- Die App sollte geöffnet bleiben, bis alle Uploads fertig sind. Besonders bei
  Videos über mobile Daten kann das etwas dauern.

## 6. Einstellungen

Über das Menü **☰** oben links → **Einstellungen**.

| Einstellung | Bedeutung |
|---|---|
| **Fotos: 1920 / 2560 / 3840 px** | Längste Bildkante nach dem Verkleinern. **2560 px** (Standard) reicht für Blogs völlig. |
| **Maximale Dateigröße (MB)** | Größte Datei, die dein Server annimmt (Standard 128 MB). Steht in WordPress unter **Medien → Datei hinzufügen**. |
| **Zugangsdaten löschen** | Entfernt Adresse, Benutzer und Passwort vom Handy. |
| **Impressum / Datenschutz** | Rechtliche Hinweise der App. |

## 7. Hilfe bei Problemen

| Meldung / Problem | Lösung |
|---|---|
| **„Benutzername oder Anwendungspasswort ist falsch“** | Benutzername prüfen (Anmeldename, nicht Anzeigename). Neues Anwendungspasswort erzeugen und neu einfügen. |
| **„Die Anmeldung kommt beim Server nicht an“** | Der Webserver entfernt den Anmelde-Header. Lösung siehe unten (.htaccess). |
| **„Keine WordPress-REST-API gefunden“** | Adresse prüfen. Im Browser `https://deine-seite.de/wp-json/` aufrufen: Dort sollte Text in geschweiften Klammern `{…}` erscheinen. Sicherheits-Plugins können die REST-API sperren. |
| **„Keine Berechtigung …“** | Der WordPress-Benutzer braucht mindestens die Rolle **Autor**. |
| **„Datei ist zu groß“** | Video kürzen oder das Upload-Limit beim Hoster erhöhen und in den Einstellungen anpassen. |
| **„Beitrag ansehen“ zeigt die Anmeldeseite** | Wenn dein Blog nur für angemeldete Benutzer sichtbar ist: im Browser einmal anmelden, danach bleibt die Anmeldung meist erhalten. |
| **„Du darfst diesen Beitrag nicht bearbeiten“** | Der Beitrag stammt von einer anderen Person. Dafür braucht dein Benutzer die Rolle **Redakteur** (WordPress → Benutzer → Rolle ändern). |
| **Bilder in der Liste fehlen** | Manche Schutz-Plugins sperren auch Bilddateien für Apps. Im Plugin den Ordner `wp-content/uploads` freigeben. |
| **Update lässt sich nicht installieren** („Konflikt mit vorhandenem Paket“) | Die Versionen wurden mit unterschiedlichen Schlüsseln signiert. App einmal deinstallieren und neu installieren, dann Zugangsdaten neu eingeben. Dauerhafte Lösung: [Abschnitt 9](#9-für-entwickler-bauen--signieren). |

### .htaccess-Anpassung (z. B. bei IONOS)

Per FTP/SFTP oder im Dateimanager des Hosters die Datei `.htaccess` im
WordPress-Hauptverzeichnis öffnen und **oberhalb** von `# BEGIN WordPress` einfügen:

```apache
SetEnvIf Authorization "(.*)" HTTP_AUTHORIZATION=$1
```

## 8. Datenschutz & Sicherheit

- Daten gehen **nur** zwischen deinem Handy und deinem Blog hin und her. Es gibt
  keinen Server der App-Anbieter, kein Tracking und keine Werbung.
- Das Anwendungspasswort wird **verschlüsselt** gespeichert (Android-Keystore) und
  ist von Android-Backups ausgenommen.
- Die App sieht **nur die Fotos, die du auswählst**. Zugriff auf die ganze Galerie braucht sie nicht.
- **Fotos** werden neu gespeichert, ohne GPS-Standort, Kameradaten und Zeitstempel.
  **Videos** werden ohne Metadaten wie den Aufnahmeort umkopiert. Bild und Ton bleiben unverändert.
- Ein verlorenes Handy? In WordPress unter **Benutzer → Profil → Anwendungspasswörter**
  das Passwort **widerrufen**. Danach hat die App keinen Zugriff mehr.

Vollständiger Text: [docs/DATENSCHUTZ.md](docs/DATENSCHUTZ.md) (auch in der App).

## 9. Für Entwickler: Bauen & Signieren

### Automatischer Build

Jeder Push baut über GitHub Actions (`.github/workflows/android.yml`) eine
Release-APK und lädt sie als **Artefakt** hoch (90 Tage verfügbar). Die
Versionsnummer ergibt sich aus der Build-Nummer.

Ein **Release** mit direkt herunterladbarer APK entsteht durch einen Tag:

```bash
git tag v1.0
git push origin v1.0
```

Oder ohne Kommandozeile: Actions → „Android-APK bauen“ → **Run workflow** →
bei *release_tag* z. B. `v1.1` eintragen → **Run workflow**. Ohne Eintrag entsteht nur ein Artefakt.

### Signierschlüssel (wichtig für Updates)

Android installiert ein Update nur, wenn es mit **demselben Schlüssel** signiert ist.
Ohne hinterlegten Schlüssel signiert die Pipeline mit einem bei jedem Build neuen
Debug-Schlüssel. Dann muss die App vor jedem Update deinstalliert werden.

Einmalig einen Schlüssel erzeugen:

```bash
keytool -genkeypair -keystore wordprest.jks -storetype PKCS12 -alias wordprest \
  -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 wordprest.jks      # Ausgabe ist der Wert für KEYSTORE_BASE64
```

Dann unter GitHub → **Settings → Secrets and variables → Actions → New repository secret** anlegen:

| Secret | Inhalt |
|---|---|
| `KEYSTORE_BASE64` | Base64-Text der `.jks`-Datei |
| `SIGNING_STORE_PASSWORD` | Keystore-Passwort |
| `SIGNING_KEY_ALIAS` | `wordprest` |
| `SIGNING_KEY_PASSWORD` | Schlüssel-Passwort (bei PKCS12 identisch mit dem Keystore-Passwort) |

`.jks`-Datei und Passwörter sicher aufbewahren (z. B. im Passwortmanager) und
**niemals committen**. Geht der Schlüssel verloren, müssen alle einmal neu installieren.

### Lokal bauen

Voraussetzung: JDK 17 und Android SDK (z. B. über Android Studio).

```bash
./gradlew testReleaseUnitTest   # Unit-Tests (Block-Parser/-Builder)
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

### Technik

- Kotlin, Jetpack Compose (Material 3), OkHttp, Coil
- WordPress-REST-API (`/wp-json/wp/v2/…`, Fallback `?rest_route=`) mit Anwendungspasswort (HTTP Basic Auth)
- minSdk 28 (Android 9), targetSdk 35

```
app/src/main/java/de/formerdehne/wordprest/
├── data/
│   ├── WpClient.kt            REST-API: Verbindung prüfen, Medien hochladen, Beitrag anlegen
│   ├── MediaPreparer.kt       Fotos verkleinern/Metadaten entfernen, Videos umkopieren
│   ├── BlockBuilder.kt        Beitragsinhalt im Block-Editor-Format erzeugen
│   ├── BlockParser.kt         bestehende Beiträge in Blöcke zerlegen (für den Editor)
│   ├── MediaUploader.kt       gemeinsamer Upload für Erstellen und Bearbeiten
│   ├── SettingsRepository.kt  Einstellungen
│   └── SecretBox.kt           Verschlüsselung über Android-Keystore
└── ui/
    ├── screens/               Start, Beitrag, Editoren, Einstellungen, Hilfe, Rechtstexte
    ├── ComposeViewModel.kt    neuer Beitrag: Entwurf, Uploads, Veröffentlichen
    ├── HomeViewModel.kt       Beitragsliste
    ├── PostViewModel.kt       Beitragsansicht inkl. älterer/neuerer Beitrag
    ├── EditViewModel.kt       Beitrag bearbeiten
    ├── NavViewModel.kt        Navigation
    └── theme/                 Farben & Typografie
```

## 10. Impressum

FORMER.DEHNE · Uli Dehne und Simon Former · Waldstraße 3 · 22955 Hoisdorf ·
kontakt@former-dehne.de. Vollständig: [docs/IMPRESSUM.md](docs/IMPRESSUM.md)

*WordPress ist eine eingetragene Marke der WordPress Foundation. WordPREST ist ein
unabhängiges Projekt ohne Verbindung zur WordPress Foundation oder zu Automattic.*
