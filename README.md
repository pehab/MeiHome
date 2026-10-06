# MeiHome

Familien-Dashboard für ein Android-Wandtablet mit MeiLists, Kalender und Haustürsteuerung.
Aktueller Stand: **0.6.9**, `versionCode 20`; Paket `de.haberland.meihome`.

## Funktionen

- Google-Anmeldung über Credential Manager und Firebase Authentication.
- Auswahl einer Einkaufs- und Aufgabenliste aus den zugänglichen Firebase-Kategorien von MeiLists.
- Live-Anzeige, Hinzufügen und Abhaken von Listeneinträgen.
- Kataloggestützte Einkaufseingabe inklusive Standardbereich.
- Kalenderanzeige für heute und die folgenden zwei Tage über den lokalen Android-Kalenderanbieter.
- Google-Play-In-App-Updates und Firebase Crashlytics.
- **Google Nest Doorbell**: WebRTC-Livestream im Haustürdialog.
- **Klingelereignisse** über Google Smart Device Management / Cloud Pub/Sub; ein Klingeln kann den Haustürdialog automatisch öffnen.
- Wählbarer Android-Klingelton für Klingelereignisse mit Begrenzung und Event-Deduplizierung.
- **Nuki Web API**: Schlossstatus, Aufsperren, Zusperren, Falle ziehen und Lock ’n’ Go.
- Einstellbarer **Nachtmodus**: Klingelereignisse bleiben lautlos, das Display wird abgedunkelt und darf in den Android-Standby wechseln.
- OAuth-Client-Secret, Refresh-Tokens und Nuki-API-Token werden lokal über den Android Keystore verschlüsselt gespeichert.

MeiHome ist für ein dauerhaft betriebenes Wandtablet gedacht. Die Klingelereignisse werden vom laufenden App-Prozess verarbeitet; die aktuelle Implementierung ist kein Push-Dienst, der eine vollständig beendete App im Hintergrund neu startet.

## Berechtigungen

- `INTERNET` für Firebase, Google Device Access/Pub/Sub, Nuki und Play-Dienste.
- `READ_CALENDAR` für die lokale Kalenderanzeige.

Kalenderdaten werden über den Android-Kalenderanbieter gelesen; MeiHome besitzt keine direkte Google-Calendar-API-Anbindung.

## Firebase / MeiLists

`app/google-services.json` muss die App `de.haberland.meihome` im Firebase-Projekt mit den gewünschten MeiLists-Daten enthalten. Google-Anmeldung muss aktiviert sein und die Fingerprints der tatsächlich verwendeten Signaturzertifikate müssen registriert werden.

MeiHome nutzt die Firestore-Collections von MeiLists für zugängliche Kategorien, Listen, Einträge und Katalogprodukte. Lokale MeiLists-Kategorien stehen in MeiHome nicht zur Verfügung.

## Google Nest Doorbell

Die Integration verwendet Googles Smart Device Management API und WebRTC. Für Klingelereignisse wird zusätzlich Google Cloud Pub/Sub verwendet. OAuth läuft über den konfigurierten Device-Access-/Google-Cloud-Client; Refresh-Tokens werden verschlüsselt auf dem Tablet gespeichert.

Die OAuth-Callback-Seite liegt unter `docs/oauth-callback.html` und leitet den Autorisierungscode per `meihome://nest-auth` zurück an die App. Geheimnisse und Tokens gehören nicht ins Repository.

## Nuki

Für den privaten Betrieb wird ein persönlicher Nuki Web API Token verwendet. MeiHome benötigt nur Zugriff zum Anzeigen des Smart Locks und zum Ausführen von Schlossaktionen. Der Token wird verschlüsselt im Android Keystore gespeichert und darf nicht ins Repository eingecheckt werden.

## Entwicklung und Build

Voraussetzungen: JDK 17, Android SDK 37 und Android 8 (API 26) oder neuer.

```bash
git clone https://github.com/pehab/MeiHome.git
cd MeiHome
bash gradlew :app:testDebugUnitTest :app:assembleDebug
bash gradlew :app:lintDebug
```

GitHub Actions führt Tests und Builds für Änderungen auf `main` aus. Funktionen mit echten Firebase-, Nest-, Pub/Sub- oder Nuki-Konten benötigen zusätzlich einen Integrationstest auf dem vorgesehenen Tablet.

## Sicherheit

OAuth-Client-Secret, Google-Refresh-Tokens und der Nuki-API-Token werden nicht im normalen Preferences-Speicher abgelegt, sondern über Android Keystore/AES-GCM geschützt. Kurzlebige Access-Tokens werden nur zur Laufzeit verwendet.

Das schützt die Zugangsdaten auf dem Gerät, ersetzt aber keine serverseitige OAuth-Architektur. Die lokale OAuth-Lösung ist bewusst für dieses persönliche Wandtablet ausgelegt.

## Datenschutz

Siehe [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
