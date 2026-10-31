package de.formerdehne.wordprest.ui

/** Abschnitt eines Rechtstexts: Überschrift (optional) + Absätze. */
data class LegalSection(val heading: String?, val body: List<String>)

object LegalTexts {

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
