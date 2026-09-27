# MeiHome

Familien-Dashboard für ein Android-Wandtablet mit MeiLists-Listen und Kalender.
Aktueller Stand: **0.5.0**, `versionCode 8`; Paket `de.haberland.meihome`.

## Verfügbare Funktionen

- Google-Anmeldung über Credential Manager und Firebase Authentication.
- Auswahl je einer Einkaufs- und Aufgabenliste aus den zugänglichen Firebase-Kategorien von MeiLists.
- Live-Anzeige, Hinzufügen und Abhaken von Listeneinträgen.
- Einkaufsprodukte aus dem Katalog der ausgewählten Listenkategorie wählen: Suche, Produktauswahl und Übernahme des Standardbereichs.
- Freie Einkaufseingabe nur, wenn der erfolgreich geladene Produktkatalog leer ist. Todos bleiben frei eingebbar.
- Erhaltene Listenauswahl auch bei zeitversetzt eintreffenden Kategorie-Daten.
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

Die App nutzt die Collections `categories`, `shopping_lists`, `list_items` und `catalog_products`; zugängliche Kategorien werden über `allowedUsers` bestimmt. Firestore-Regeln müssen diese Berechtigungen serverseitig durchsetzen. Deploybare Regeln sind nicht Bestandteil dieses Repositories.

## Einkauf aus dem Katalog

Beim Hinzufügen lädt MeiHome den Produktkatalog aus `catalog_products`, gefiltert nach der `categoryId` der Einkaufsliste. Bei vorhandenem Katalog muss ein Produkt ausgewählt werden; bloßes Tippen eines Namens reicht nicht. Der Produktname und `defaultArea` werden als `text` und `area` am Listeneintrag gespeichert. Katalogpflege erfolgt weiterhin in MeiLists.

Zum Prüfen des Katalogs wird eine Serververbindung benötigt: Ein leerer Offline-Cache gilt nicht als leerer Katalog. Bei Ladefehlern bleibt die Eingabe gesperrt und kann erneut versucht werden. Speicherfehler werden im geöffneten Dialog angezeigt; wiederholtes Tippen während des Speicherns erzeugt keine zusätzlichen Schreibaufträge.

## Architektur und Qualität

- `data/auth/`: Google-/Firebase-Anmeldung und Auth-Zustand.
- `data/lists/`: Firestore-Listener und Listenoperationen.
- `data/calendar/`: Android-Kalenderzugriff.
- `data/preferences/`: lokal gespeicherte Dashboard-Auswahl.
- `domain/model/`: Listenmodelle.
- `ui/dashboard/`: ViewModel, Zustand und Darstellung.

GitHub Actions baut die Debug-APK und führt `testDebugUnitTest` sowie `lintDebug` aus. Die JVM-Tests prüfen Katalogpflicht, freien Eintrag bei leerem Katalog, Kategoriezuordnung, Bereichsübernahme, Lade-/Speicherfehler, doppelte Klicks, Abbruch sowie die Wiederherstellung der Listenauswahl bei zeitversetzten Daten. Firebase-Regeln und die Bedienung auf dem Wandtablet benötigen zusätzlich einen Integrationstest.

```bash
bash gradlew :app:testDebugUnitTest :app:assembleDebug
```

Firestore-Listenerfehler werden angezeigt, Coroutine-Abbrüche nicht als fachliche Fehler behandelt. Der Einkaufsdialog hat ein eigenes ViewModel mit injizierbarem Repository. Nächste technische Schritte: Kalenderabfragen vom Hauptthread lösen und den Tageswechsel ohne Kalenderänderung berücksichtigen; weitere Dashboard-Abhängigkeiten für Tests injizierbar machen.

## Datenschutz

Siehe [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
