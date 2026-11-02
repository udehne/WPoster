package de.formerdehne.wposter.ui

import java.util.Locale

/** Abschnitt eines Rechtstexts: Überschrift (optional) + Absätze. */
data class LegalSection(val heading: String?, val body: List<String>)

object LegalTexts {

    private val isGerman get() = Locale.getDefault().language == "de"

    /** Texte in der Gerätesprache: Deutsch für "de", sonst Englisch. */
    fun help(): List<LegalSection> = if (isGerman) hilfe else helpEn
    fun legalNotice(): List<LegalSection> = if (isGerman) impressum else impressumEn
    fun privacy(): List<LegalSection> = if (isGerman) datenschutz else privacyEn

    val hilfe = listOf(
        LegalSection(
            "Neuen Beitrag schreiben",
            listOf(
                "Auf der Startseite auf das große + tippen. Fotos und Videos hinzufügen – sie werden sofort " +
                    "hochgeladen –, eine Überschrift und optional Text vor und nach den Fotos eingeben, dann „Veröffentlichen“.",
                "Verlässt du den Editor, bleibt der Entwurf erhalten. Er erscheint oben in der Beitragsliste, " +
                    "wo du ihn fortsetzen oder verwerfen kannst."
            )
        ),
        LegalSection(
            "Direkt aus der Galerie teilen",
            listOf(
                "In Google Fotos, der Galerie o. Ä. Fotos/Videos auswählen, „Teilen“ antippen und WPoster wählen. " +
                    "Die App öffnet den Editor und lädt sofort hoch."
            )
        ),
        LegalSection(
            "Beiträge ansehen",
            listOf(
                "Die Startseite zeigt die neuesten Beiträge mit Beitragsbild, Autor und Datum. Zum Aktualisieren " +
                    "die Liste nach unten ziehen; beim Scrollen werden ältere Beiträge nachgeladen.",
                "Ein Tipp auf einen Beitrag zeigt ihn vollständig. Unten wechselst du zur Übersicht, zum älteren " +
                    "oder neueren Beitrag oder in den Editor."
            )
        ),
        LegalSection(
            "Suchen",
            listOf(
                "Menü ☰ → Suche. Ab zwei Buchstaben werden Überschriften und Texte aller Beiträge durchsucht; " +
                    "die Treffer erscheinen wie auf der Startseite und lassen sich antippen."
            )
        ),
        LegalSection(
            "Kalender",
            listOf(
                "Menü ☰ → Kalender. Tage mit Beiträgen sind farbig markiert, bei mehreren Beiträgen steht die " +
                    "Anzahl darunter. Mit den Pfeilen blätterst du durch die Monate.",
                "Unter dem Kalender stehen alle Beiträge des Monats. Ein Tipp auf einen markierten Tag zeigt nur " +
                    "dessen Beiträge, ein zweiter Tipp (oder „Ganzer Monat“) wieder alle."
            )
        ),
        LegalSection(
            "Beitrag bearbeiten",
            listOf(
                "Im Editor kannst du Überschrift, Texte und Zwischenüberschriften ändern, Fotos und Videos " +
                    "hinzufügen, mit ▲▼ verschieben und mit ✕ entfernen.",
                "Der Stern ☆ an einem Foto macht es zum Beitragsbild (wird in der Übersicht angezeigt).",
                "Das Veröffentlichungsdatum bestimmt die Reihenfolge der Beiträge. Liegt es in der Zukunft, " +
                    "wird der Beitrag geplant und erst dann sichtbar.",
                "Inhalte, die die App nicht bearbeiten kann (z. B. Galerien oder Spalten aus dem Web-Editor), " +
                    "bleiben unverändert erhalten und lassen sich verschieben oder entfernen.",
                "Texte mit Formatierungen (z. B. Links) bleiben unverändert, solange du sie nicht änderst. " +
                    "Änderst du den Text, wird er als einfacher Text gespeichert.",
                "Aus dem Beitrag entfernte Fotos bleiben in der WordPress-Mediathek erhalten.",
                "Eigene Beiträge darf jeder Autor bearbeiten. Für Beiträge anderer Personen braucht dein " +
                    "WordPress-Benutzer die Rolle „Redakteur“ oder „Administrator“."
            )
        ),
        LegalSection(
            "Datenschutz bei Fotos",
            listOf(
                "Vor dem Hochladen entfernt die App den Aufnahmeort (GPS) und andere Metadaten aus Fotos und Videos. " +
                    "Fotos werden außerdem auf die in den Einstellungen gewählte Größe verkleinert."
            )
        ),
        LegalSection(
            "Probleme?",
            listOf(
                "„Die Anmeldung kommt beim Server nicht an“: Der Webserver entfernt den Anmelde-Header. In die " +
                    ".htaccess des Blogs gehört oberhalb von „# BEGIN WordPress“ die Zeile:\n" +
                    "SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=${'$'}1",
                "„Benutzername oder Anwendungspasswort ist falsch“: In WordPress unter Benutzer → Profil ein neues " +
                    "Anwendungspasswort erzeugen und in den Einstellungen eintragen.",
                "„Datei ist zu groß“: Video kürzen oder das Upload-Limit beim Hoster erhöhen und in den Einstellungen anpassen.",
                "Bilder werden nicht angezeigt: Manche Schutz-Plugins sperren auch die Bilddateien für Apps. " +
                    "Dann im Plugin den Ordner wp-content/uploads freigeben.",
                "Weitere Hilfe: kontakt@former-dehne.de"
            )
        ),
    )

    val impressum = listOf(
        LegalSection(
            "Angaben gemäß § 5 DDG",
            listOf("FORMER.DEHNE\nUli Dehne und Simon Former\nWaldstraße 3\n22955 Hoisdorf\nDeutschland")
        ),
        LegalSection("Kontakt", listOf("E-Mail: kontakt@former-dehne.de")),
        LegalSection(
            "Verantwortlich für die App",
            listOf("Uli Dehne und Simon Former, Anschrift wie oben.")
        ),
        LegalSection(
            "Inhalte auf verbundenen WordPress-Seiten",
            listOf(
                "WPoster ist ein Werkzeug, mit dem Nutzerinnen und Nutzer Beiträge auf ihrer eigenen " +
                    "WordPress-Seite veröffentlichen. Für die dort veröffentlichten Inhalte ist allein " +
                    "die jeweilige Betreiberin bzw. der jeweilige Betreiber der WordPress-Seite verantwortlich."
            )
        ),
        LegalSection(
            "Haftung für Links",
            listOf(
                "Die App öffnet auf Wunsch Inhalte externer Webseiten (z. B. Ihre WordPress-Seite). " +
                    "Auf deren Inhalte haben wir keinen Einfluss; dafür ist stets der jeweilige Anbieter verantwortlich."
            )
        ),
        LegalSection(
            "Lizenz",
            listOf(
                "WPoster ist freie Software: Sie können sie unter den Bedingungen der GNU General Public " +
                    "License, Version 3, weitergeben und/oder verändern. Die App wird ohne jede Gewährleistung " +
                    "bereitgestellt. Details: https://www.gnu.org/licenses/gpl-3.0.html"
            )
        ),
        LegalSection(
            "Marken",
            listOf(
                "WordPress ist eine eingetragene Marke der WordPress Foundation. WPoster ist ein " +
                    "unabhängiges Projekt und steht in keiner Verbindung zur WordPress Foundation oder zu Automattic."
            )
        ),
    )

    val datenschutz = listOf(
        LegalSection(
            "1. Verantwortliche",
            listOf(
                "FORMER.DEHNE, Uli Dehne und Simon Former, Waldstraße 3, 22955 Hoisdorf, " +
                    "E-Mail: kontakt@former-dehne.de"
            )
        ),
        LegalSection(
            "2. Das Wichtigste in Kürze",
            listOf(
                "WPoster überträgt Daten ausschließlich zwischen Ihrem Gerät und der WordPress-Seite, " +
                    "die Sie selbst in der App eintragen. Wir als Anbieter betreiben keinen eigenen Server " +
                    "für die App und erhalten keine Ihrer Daten.",
                "Die App enthält keine Werbung, kein Tracking, keine Analyse-Werkzeuge und keine " +
                    "Absturzberichte an Dritte. Einzige Ausnahme ist die abschaltbare Update-Prüfung bei GitHub (Abschnitt 6)."
            )
        ),
        LegalSection(
            "3. Zugangsdaten",
            listOf(
                "Adresse der WordPress-Seite, Benutzername und Anwendungspasswort werden nur auf Ihrem " +
                    "Gerät gespeichert. Das Anwendungspasswort wird mit einem Schlüssel aus dem " +
                    "Android-Keystore verschlüsselt. Die Daten sind von Android-Sicherungen ausgenommen.",
                "Die Zugangsdaten werden bei jeder Anfrage verschlüsselt (HTTPS) an Ihre WordPress-Seite " +
                    "gesendet, damit diese Sie als berechtigten Benutzer erkennt.",
                "Sie können die Zugangsdaten jederzeit in den Einstellungen löschen. Beim Deinstallieren " +
                    "der App werden sie ebenfalls entfernt. Ein Anwendungspasswort können Sie außerdem " +
                    "jederzeit in Ihrem WordPress-Profil widerrufen."
            )
        ),
        LegalSection(
            "4. Fotos, Videos und Texte",
            listOf(
                "Die App greift nur auf Fotos und Videos zu, die Sie ausdrücklich auswählen – über die " +
                    "Android-Fotoauswahl oder über „Teilen“ aus einer anderen App. Eine Berechtigung für " +
                    "Ihre gesamte Galerie ist nicht nötig.",
                "Vor dem Hochladen werden Fotos auf dem Gerät verkleinert und neu gespeichert. Dabei " +
                    "werden alle eingebetteten Metadaten entfernt, insbesondere der Aufnahmeort (GPS), " +
                    "Kameradaten und Aufnahmezeitpunkt. Bei Videos werden Bild- und Tonspur ohne " +
                    "Metadaten wie den Aufnahmeort in eine neue Datei übernommen.",
                "Die aufbereiteten Dateien liegen nur kurz im App-Zwischenspeicher und werden nach dem " +
                    "Hochladen bzw. beim nächsten App-Start gelöscht.",
                "Fotos, Videos, Überschrift und Texte werden an Ihre WordPress-Seite übertragen und dort " +
                    "als Beitrag veröffentlicht. Für die weitere Verarbeitung auf der WordPress-Seite ist " +
                    "deren Betreiber verantwortlich; es gilt die dortige Datenschutzerklärung."
            )
        ),
        LegalSection(
            "5. Verbindungsdaten",
            listOf(
                "Beim Verbindungsaufbau übermittelt Ihr Gerät technisch notwendige Daten (z. B. IP-Adresse, " +
                    "Zeitpunkt, App-Kennung im User-Agent) an den Server Ihrer WordPress-Seite. Diese Daten " +
                    "verarbeitet allein der Betreiber dieses Servers bzw. dessen Hoster."
            )
        ),
        LegalSection(
            "6. Update-Prüfung",
            listOf(
                "Ist „Nach Updates suchen“ eingeschaltet (Standard), fragt die App höchstens alle sechs Stunden " +
                    "bei GitHub (GitHub Inc., ein Unternehmen von Microsoft) nach der neuesten Version. Dabei werden " +
                    "technisch notwendige Verbindungsdaten wie Ihre IP-Adresse an GitHub übertragen; weitere Daten " +
                    "werden nicht gesendet. Es gilt die Datenschutzerklärung von GitHub. Sie können die Prüfung " +
                    "jederzeit in den Einstellungen abschalten."
            )
        ),
        LegalSection(
            "7. Berechtigungen",
            listOf(
                "Internetzugriff – um Medien und Beiträge an Ihre WordPress-Seite zu senden.",
                "Benachrichtigungen – um den Fortschritt laufender Uploads anzuzeigen, auch wenn die App im " +
                    "Hintergrund ist. Diese Berechtigung ist optional.",
                "Weitere Berechtigungen benötigt die App nicht."
            )
        ),
        LegalSection(
            "8. Abgebildete Personen",
            listOf(
                "Bitte veröffentlichen Sie Fotos und Videos anderer Personen nur mit deren Einverständnis. " +
                    "Bei Kindern ist das Einverständnis der Sorgeberechtigten erforderlich."
            )
        ),
        LegalSection(
            "9. Ihre Rechte",
            listOf(
                "Sie haben nach der DSGVO das Recht auf Auskunft (Art. 15), Berichtigung (Art. 16), " +
                    "Löschung (Art. 17), Einschränkung der Verarbeitung (Art. 18), Datenübertragbarkeit " +
                    "(Art. 20) und Widerspruch (Art. 21). Da wir als Anbieter keine Daten über Sie " +
                    "speichern, richten Sie Anfragen zu Inhalten auf einer WordPress-Seite bitte an deren Betreiber.",
                "Sie können sich außerdem bei einer Datenschutz-Aufsichtsbehörde beschweren, z. B. beim " +
                    "Unabhängigen Landeszentrum für Datenschutz Schleswig-Holstein (ULD), Kiel."
            )
        ),
        LegalSection("Stand", listOf("November 2026")),
    )

    // ---------- English ----------

    val helpEn = listOf(
        LegalSection(
            "Writing a new post",
            listOf(
                "On the home screen, tap the large +. Add photos and videos – they are uploaded right " +
                    "away –, enter a title and, optionally, text before and after the photos, then tap “Publish”.",
                "If you leave the editor, the draft is kept. It appears at the top of the post list, " +
                    "where you can continue or discard it."
            )
        ),
        LegalSection(
            "Sharing straight from your gallery",
            listOf(
                "In Google Photos, your gallery or similar, select photos/videos, tap “Share” and choose WPoster. " +
                    "The app opens the editor and starts uploading right away."
            )
        ),
        LegalSection(
            "Viewing posts",
            listOf(
                "The home screen shows the latest posts with featured image, author and date. Pull the list " +
                    "down to refresh; older posts are loaded as you scroll.",
                "Tap a post to see it in full. At the bottom you can go to the overview, to the older " +
                    "or newer post, or into the editor."
            )
        ),
        LegalSection(
            "Searching",
            listOf(
                "Menu ☰ → Search. From two letters on, the headings and text of all posts are searched; " +
                    "results appear just like on the home screen and can be tapped."
            )
        ),
        LegalSection(
            "Calendar",
            listOf(
                "Menu ☰ → Calendar. Days with posts are highlighted; if there are several posts, the " +
                    "number is shown below. Use the arrows to move through the months.",
                "Below the calendar you'll find all posts of the month. Tap a highlighted day to see only " +
                    "its posts; tap it again (or “Whole month”) to see all of them."
            )
        ),
        LegalSection(
            "Editing a post",
            listOf(
                "In the editor you can change the title, text and subheadings, add photos and videos, " +
                    "move them with ▲▼ and remove them with ✕.",
                "The star ☆ on a photo makes it the featured image (shown in the overview).",
                "The publish date determines the order of posts. If it is in the future, " +
                    "the post is scheduled and only becomes visible then.",
                "Content the app can't edit (e.g. galleries or columns from the web editor) " +
                    "is kept unchanged and can be moved or removed.",
                "Text with formatting (e.g. links) stays unchanged as long as you don't edit it. " +
                    "If you change the text, it is saved as plain text.",
                "Photos removed from a post remain in the WordPress media library.",
                "Every author may edit their own posts. To edit other people's posts, your " +
                    "WordPress user needs the “Editor” or “Administrator” role."
            )
        ),
        LegalSection(
            "Privacy for photos",
            listOf(
                "Before uploading, the app removes the location (GPS) and other metadata from photos and videos. " +
                    "Photos are also scaled down to the size chosen in Settings."
            )
        ),
        LegalSection(
            "Problems?",
            listOf(
                "“The login isn't reaching the server”: the web server strips the authorization header. Add the " +
                    "following line to your blog's .htaccess, above “# BEGIN WordPress”:\n" +
                    "SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=${'$'}1",
                "“Username or application password is incorrect”: create a new application password in " +
                    "WordPress under Users → Profile and enter it in Settings.",
                "“File is too large”: shorten the video, or raise the upload limit with your host and adjust it in Settings.",
                "Images aren't displayed: some security plugins also block image files for apps. " +
                    "In that case, allow the folder wp-content/uploads in the plugin.",
                "More help: kontakt@former-dehne.de"
            )
        ),
    )

    val impressumEn = listOf(
        LegalSection(
            "Information pursuant to § 5 DDG (German Digital Services Act)",
            listOf("FORMER.DEHNE\nUli Dehne und Simon Former\nWaldstraße 3\n22955 Hoisdorf\nGermany")
        ),
        LegalSection("Contact", listOf("Email: kontakt@former-dehne.de")),
        LegalSection(
            "Responsible for the app",
            listOf("Uli Dehne and Simon Former, address as above.")
        ),
        LegalSection(
            "Content on connected WordPress sites",
            listOf(
                "WPoster is a tool that lets users publish posts on their own WordPress site. " +
                    "The operator of the respective WordPress site is solely responsible for the content published there."
            )
        ),
        LegalSection(
            "Liability for links",
            listOf(
                "On request, the app opens content from external websites (e.g. your WordPress site). " +
                    "We have no influence on their content; the respective provider is always responsible for it."
            )
        ),
        LegalSection(
            "License",
            listOf(
                "WPoster is free software: you can redistribute it and/or modify it under the terms of the " +
                    "GNU General Public License, version 3. The app is provided without any warranty. " +
                    "Details: https://www.gnu.org/licenses/gpl-3.0.html"
            )
        ),
        LegalSection(
            "Trademarks",
            listOf(
                "WordPress is a registered trademark of the WordPress Foundation. WPoster is an " +
                    "independent project and is not affiliated with the WordPress Foundation or Automattic."
            )
        ),
    )

    val privacyEn = listOf(
        LegalSection(
            null,
            listOf(
                "This is a translation of the German privacy policy. In case of doubt, the German version is authoritative."
            )
        ),
        LegalSection(
            "1. Controllers",
            listOf(
                "FORMER.DEHNE, Uli Dehne und Simon Former, Waldstraße 3, 22955 Hoisdorf, Germany, " +
                    "email: kontakt@former-dehne.de"
            )
        ),
        LegalSection(
            "2. Summary",
            listOf(
                "WPoster transfers data exclusively between your device and the WordPress site " +
                    "that you enter in the app yourself. As the provider, we do not operate a server of our own " +
                    "for the app and do not receive any of your data.",
                "The app contains no advertising, no tracking, no analytics tools and no crash reports " +
                    "sent to third parties. The only exception is the optional update check via GitHub (section 6)."
            )
        ),
        LegalSection(
            "3. Login details",
            listOf(
                "The WordPress site address, username and application password are stored only on your " +
                    "device. The application password is encrypted with a key from the Android Keystore. " +
                    "The data is excluded from Android backups.",
                "The login details are sent encrypted (HTTPS) to your WordPress site with every request, " +
                    "so that it recognizes you as an authorized user.",
                "You can delete the login details in Settings at any time. They are also removed when " +
                    "you uninstall the app. In addition, you can revoke an application password at any time " +
                    "in your WordPress profile."
            )
        ),
        LegalSection(
            "4. Photos, videos and text",
            listOf(
                "The app only accesses photos and videos that you explicitly select – via the Android " +
                    "photo picker or via “Share” from another app. Permission to access your entire gallery " +
                    "is not required.",
                "Before uploading, photos are scaled down and re-saved on the device. In the process, " +
                    "all embedded metadata is removed, in particular the location (GPS), camera data and " +
                    "time of capture. For videos, the video and audio tracks are copied into a new file " +
                    "without metadata such as the location.",
                "The processed files are kept only briefly in the app's cache and are deleted after " +
                    "uploading or the next time the app starts.",
                "Photos, videos, title and text are transferred to your WordPress site and published " +
                    "there as a post. The operator of the WordPress site is responsible for any further " +
                    "processing there; the privacy policy of that site applies."
            )
        ),
        LegalSection(
            "5. Connection data",
            listOf(
                "When establishing a connection, your device transmits technically necessary data (e.g. IP address, " +
                    "time, app identifier in the user agent) to the server of your WordPress site. This data " +
                    "is processed solely by the operator of that server or its hosting provider."
            )
        ),
        LegalSection(
            "6. Update check",
            listOf(
                "If “Check for updates” is turned on (default), the app asks GitHub (GitHub Inc., a Microsoft " +
                    "company) for the latest version at most every six hours. In doing so, technically necessary " +
                    "connection data such as your IP address is transmitted to GitHub; no other data is sent. " +
                    "GitHub's privacy statement applies. You can turn off the check in Settings at any time."
            )
        ),
        LegalSection(
            "7. Permissions",
            listOf(
                "Internet access – to send media and posts to your WordPress site.",
                "Notifications – to show the progress of running uploads, even when the app is in the " +
                    "background. This permission is optional.",
                "The app requires no other permissions."
            )
        ),
        LegalSection(
            "8. People shown in photos",
            listOf(
                "Please only publish photos and videos of other people with their consent. " +
                    "For children, the consent of their parents or legal guardians is required."
            )
        ),
        LegalSection(
            "9. Your rights",
            listOf(
                "Under the GDPR, you have the right of access (Art. 15), rectification (Art. 16), " +
                    "erasure (Art. 17), restriction of processing (Art. 18), data portability (Art. 20) " +
                    "and objection (Art. 21). As we, the provider, do not store any data about you, please " +
                    "address requests concerning content on a WordPress site to its operator.",
                "You also have the right to lodge a complaint with a data protection supervisory authority, e.g. the " +
                    "Unabhängiges Landeszentrum für Datenschutz Schleswig-Holstein (ULD), Kiel."
            )
        ),
        LegalSection("Last updated", listOf("November 2026")),
    )
}
