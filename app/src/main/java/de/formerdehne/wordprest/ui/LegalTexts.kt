package de.formerdehne.wordprest.ui

/** Abschnitt eines Rechtstexts: Überschrift (optional) + Absätze. */
data class LegalSection(val heading: String?, val body: List<String>)

object LegalTexts {

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
                "In Google Fotos, der Galerie o. Ä. Fotos/Videos auswählen, „Teilen“ antippen und WordPREST wählen. " +
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
                "WordPREST ist ein Werkzeug, mit dem Nutzerinnen und Nutzer Beiträge auf ihrer eigenen " +
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
            "Marken",
            listOf(
                "WordPress ist eine eingetragene Marke der WordPress Foundation. WordPREST ist ein " +
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
                "WordPREST überträgt Daten ausschließlich zwischen Ihrem Gerät und der WordPress-Seite, " +
                    "die Sie selbst in der App eintragen. Wir als Anbieter betreiben keinen eigenen Server " +
                    "für die App und erhalten keine Ihrer Daten.",
                "Die App enthält keine Werbung, kein Tracking, keine Analyse-Werkzeuge und keine " +
                    "Absturzberichte an Dritte."
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
            "6. Berechtigungen",
            listOf(
                "Internetzugriff – um Medien und Beiträge an Ihre WordPress-Seite zu senden. " +
                    "Weitere Berechtigungen benötigt die App nicht."
            )
        ),
        LegalSection(
            "7. Abgebildete Personen",
            listOf(
                "Bitte veröffentlichen Sie Fotos und Videos anderer Personen nur mit deren Einverständnis. " +
                    "Bei Kindern ist das Einverständnis der Sorgeberechtigten erforderlich."
            )
        ),
        LegalSection(
            "8. Ihre Rechte",
            listOf(
                "Sie haben nach der DSGVO das Recht auf Auskunft (Art. 15), Berichtigung (Art. 16), " +
                    "Löschung (Art. 17), Einschränkung der Verarbeitung (Art. 18), Datenübertragbarkeit " +
                    "(Art. 20) und Widerspruch (Art. 21). Da wir als Anbieter keine Daten über Sie " +
                    "speichern, richten Sie Anfragen zu Inhalten auf einer WordPress-Seite bitte an deren Betreiber.",
                "Sie können sich außerdem bei einer Datenschutz-Aufsichtsbehörde beschweren, z. B. beim " +
                    "Unabhängigen Landeszentrum für Datenschutz Schleswig-Holstein (ULD), Kiel."
            )
        ),
        LegalSection("Stand", listOf("Oktober 2026")),
    )
}
