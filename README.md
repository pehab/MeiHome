# MeiHome

Familien-Dashboard für ein Android-Wandtablet mit MeiLists-Listen und Kalender.
Aktueller Stand: **0.4.3**, `versionCode 7`; Paket `de.haberland.meihome`.

## Verfügbare Funktionen

- Google-Anmeldung über Credential Manager und Firebase Authentication.
- Auswahl je einer Einkaufs- und Aufgabenliste aus den zugänglichen Firebase-Kategorien von MeiLists.
- Live-Anzeige, Hinzufügen und Abhaken von Listeneinträgen.
- Lokale Speicherung der ausgewählten Listen.
- Kalenderanzeige für heute und die folgenden zwei Tage über den Android-Kalenderanbieter, inklusive ganztägiger Termine.
- Google-Play-In-App-Updates und Firebase Crashlytics.

Der Kalender benötigt `READ_CALENDAR` und auf dem Tablet synchronisierte Kalender. Es gibt keine direkte Google-Calendar-API-Anbindung. Lokale MeiLists-Kategorien werden nicht synchronisiert.

**Noch Platzhalter:** Die Schaltflächen „Haustür“ und „Klingel“ haben noch keine Aktion. Nuki, Klingelstream und weitere Smart-Home-Geräte sind nicht integriert.

## Entwicklung und Build

Voraussetzungen: JDK 17, Android SDK 37 und Android 8 (API 26) oder neuer.

```bash
git clone https://github.com/pehab/MeiHome.git
cd MeiHome
bash gradlew :app:assembleDebug
bash gradlew :app:lintDebug
```

Alternativ in Android Studio öffnen und Gradle synchronisieren. Den SDK-Pfad bei Bedarf in der nicht versionierten `local.properties` als `sdk.dir` setzen.

## Firebase einrichten

`app/google-services.json` muss die App `de.haberland.meihome` im selben Firebase-Projekt wie die gewünschten MeiLists-Daten enthalten. Google-Anmeldung aktivieren und die SHA-1-/SHA-256-Fingerprints der verwendeten Signaturzertifikate registrieren. Lokaler Debug-Build und Google-Play-Build können unterschiedliche Zertifikate verwenden. Anschließend die aktualisierte Firebase-Konfiguration herunterladen.

Die App nutzt die Collections `categories`, `shopping_lists` und `list_items`; zugängliche Kategorien werden über `allowedUsers` bestimmt. Firestore-Regeln müssen diese Berechtigungen serverseitig durchsetzen. Deploybare Regeln sind nicht Bestandteil dieses Repositories.

## Architektur und Qualität

- `data/auth/`: Google-/Firebase-Anmeldung und Auth-Zustand.
- `data/lists/`: Firestore-Listener und Listenoperationen.
- `data/calendar/`: Android-Kalenderzugriff.
- `data/preferences/`: lokal gespeicherte Dashboard-Auswahl.
- `domain/model/`: Listenmodelle.
- `ui/dashboard/`: ViewModel, Zustand und Darstellung.

GitHub Actions baut die Debug-APK und ruft `testDebugUnitTest` auf. Derzeit gibt es jedoch keine Unit-Test-Quellen; ein grüner Build ist deshalb kein Nachweis für getestete Dashboard-Logik.

Nächste technische Schritte: Fehler von Firestore-Listenern sichtbar machen; Listenauswahl bei asynchron eintreffenden Kategorien zuverlässig erhalten; Kalenderabfragen vom Hauptthread lösen und den Tageswechsel ohne Kalenderänderung berücksichtigen. Diese Abläufe benötigen gezielte Tests. Das ViewModel konstruiert seine Repositories aktuell direkt; injizierbare Abhängigkeiten würden solche Tests erleichtern.

## Datenschutz

Siehe [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
