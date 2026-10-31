# WordPREST

Schlanke Android-App, die Fotos und Videos über die WordPress-REST-API hochlädt
und daraus direkt einen Beitrag veröffentlicht. Sie ist für private Familienblogs
gedacht und funktioniert mit jeder WordPress-Seite. Adresse und Zugangsdaten
werden in der App eingegeben.

## Funktionen

- **Teilen aus der Galerie:** Fotos oder Videos markieren, „Teilen“ antippen und
  *WordPREST* wählen. Der Upload startet sofort, während du Überschrift und Texte eingibst.
- **Neuer Beitrag in der App:** Medien über die Android-Fotoauswahl hinzufügen.
- Aufbau des Beitrags (Block-Editor): *Text vor den Fotos → Fotos/Videos untereinander → Text nach den Fotos*.
- Das erste Foto wird automatisch zum Beitragsbild.
- Reihenfolge ändern, einzelne Medien entfernen (sie werden dann auch auf der Seite gelöscht).
- Wird sofort veröffentlicht. Danach kannst du den Beitrag ansehen oder gleich einen weiteren anlegen.
- **Datenschutz:** Fotos werden verkleinert (Standard 2560 px) und ohne Metadaten
  neu gespeichert, also ohne GPS-Standort, Kamera und Zeitstempel. Videos werden
  ohne Neukodierung in einen neuen Container ohne Metadaten umkopiert.
- Das Anwendungspasswort wird mit dem Android-Keystore verschlüsselt gespeichert.
- Ohne Tracking und ohne Werbung. Impressum und Datenschutzhinweis sind in der App (siehe `docs/`).

Voraussetzung: Android 9 oder neuer.

## Installation

1. In GitHub unter **Actions → „Android-APK bauen“** den neuesten erfolgreichen Lauf öffnen.
2. Unten bei **Artifacts** die Datei `WordPREST-1.0.x` herunterladen. Das ist ein ZIP,
   darin liegt die APK.
3. Die APK auf dem Handy öffnen und die Installation aus „unbekannten Quellen“ für
   den Browser bzw. Dateimanager erlauben.

Bequemer fürs Handy ist ein **Release**: Ein Tag wie `v1.0` erzeugt automatisch
ein GitHub-Release, an dem die APK direkt hängt (ohne ZIP).

```bash
git tag v1.0 && git push origin v1.0
```

## Einrichtung in der App

1. In WordPress im Browser: **Benutzer → Profil → Anwendungspasswörter**, Namen
   „WordPREST Handy“ eingeben und **Neues Anwendungspasswort hinzufügen**.
2. In der App: Adresse (z. B. `https://udehne.de`), Benutzername und das
   Anwendungspasswort eintragen, dann **Verbindung prüfen & speichern** antippen.

Jede Person nutzt ihren eigenen WordPress-Benutzer und ihr eigenes Anwendungspasswort.
Der Benutzer braucht mindestens die Rolle **Autor**.

### Fehler „Die Anmeldung kommt beim Server nicht an“

Manche Hoster entfernen den `Authorization`-Header. Dann hilft diese Zeile in der
`.htaccess` im WordPress-Verzeichnis, oberhalb von `# BEGIN WordPress`:

```apache
SetEnvIf Authorization "(.*)" HTTP_AUTHORIZATION=$1
```

### „Beitrag ansehen“ bei nur angemeldet sichtbaren Seiten

Die App öffnet den Beitrag im Browser (Chrome Custom Tab). Ist die Seite nur für
angemeldete Benutzer sichtbar, musst du dort einmal angemeldet sein. Die Anmeldung
bleibt danach normalerweise erhalten.

## Signierschlüssel (wichtig für Updates)

Android installiert ein Update nur, wenn es mit **demselben Schlüssel** signiert ist.
Ohne hinterlegten Schlüssel signiert die Pipeline mit einem wechselnden Debug-Schlüssel.
Dann musst du die App vor jedem Update deinstallieren.

Einmalig einen Schlüssel erzeugen und als Secrets hinterlegen
(GitHub → Settings → Secrets and variables → Actions → *New repository secret*):

```bash
keytool -genkeypair -keystore wordprest.jks -alias wordprest \
  -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 wordprest.jks   # Ausgabe = KEYSTORE_BASE64
```

| Secret | Inhalt |
|---|---|
| `KEYSTORE_BASE64` | Base64 der `.jks`-Datei |
| `SIGNING_STORE_PASSWORD` | Keystore-Passwort |
| `SIGNING_KEY_ALIAS` | `wordprest` |
| `SIGNING_KEY_PASSWORD` | Schlüssel-Passwort (bei PKCS12 = Keystore-Passwort) |

Bewahre die `.jks`-Datei und die Passwörter sicher auf und committe sie **nie**.

## Technik

Kotlin, Jetpack Compose (Material 3), OkHttp und Coil. Gebaut wird mit Gradle
über GitHub Actions (`.github/workflows/android.yml`).

```
app/src/main/java/de/formerdehne/wordprest/
├── data/      WordPress-Client, Medienaufbereitung, Block-Erzeugung, Einstellungen
└── ui/        Bildschirme, Navigation, Theme, Rechtstexte
```
