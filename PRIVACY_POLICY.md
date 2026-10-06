# Privacy Policy – MeiHome

MeiHome is an Android dashboard application for personal and household use.

## Data processing

MeiHome does not contain advertising and does not use Firebase Analytics. Personal data is not sold.

The application uses several optional online services to provide its configured dashboard and front-door functions.

## Firebase and MeiLists

MeiHome uses:

- **Firebase Authentication / Google Sign-In** to identify the signed-in user;
- **Cloud Firestore** to read and update MeiLists categories, lists, entries and catalog information shared with that user;
- **Firebase Crashlytics** to receive technical crash reports and diagnose app errors.

Depending on the features used, Firebase data can include account identifiers and email address, household/list content and sharing information. Crash reports can contain technical information such as app version, device and operating-system information, stack traces, app state and diagnostic identifiers.

## Calendar

MeiHome can read calendar data already synchronized on the Android device in order to display upcoming appointments. Calendar data is read through Android's local calendar provider. MeiHome does not upload those appointments to Firebase or a MeiHome server.

## Google Nest / Device Access

When the front-door integration is configured, MeiHome communicates with Google's Smart Device Management API to discover the configured Nest Doorbell and establish its WebRTC live stream.

Doorbell chime events are received through a Google Cloud Pub/Sub subscription. Google therefore processes the device identifiers, authorization information, stream signalling and event data required to provide these services.

Google OAuth refresh tokens are stored encrypted on the Android device. Short-lived access tokens are used to access the configured Google APIs.

## Nuki

When Nuki integration is configured, MeiHome communicates with the **Nuki Web API** to read the selected Smart Lock's state and to execute user-requested actions such as unlock, lock, unlatch and Lock ’n’ Go.

The personal Nuki API token and selected device configuration are stored on the Android device; the API token is encrypted using Android Keystore-backed storage. Nuki processes the device/account information required to provide its Web API.

- Nuki Privacy Policy: https://nuki.io/en/legal/privacy/

## Local data

Dashboard selections, night-mode settings, selected ringtone and front-door configuration are stored locally. Sensitive front-door credentials are stored using Android Keystore-backed encryption.

Local app data can be removed by clearing the app data or uninstalling MeiHome.

## Third parties

MeiHome uses Google/Firebase, Google Device Access/Cloud Pub/Sub, Google Play services and optionally Nuki Web API. It does not contain advertising SDKs or Firebase Analytics.

- Google Privacy Policy: https://policies.google.com/privacy
- Firebase Privacy and Security: https://firebase.google.com/support/privacy/
- Nuki Privacy Policy: https://nuki.io/en/legal/privacy/

## Data sharing and deletion

Personal data is not sold. Data is transmitted to Google/Firebase and, when enabled, Nuki only as required for the functions described above.

Local data can be removed by clearing app data or uninstalling the application. Cloud data stored in MeiLists/Firestore or by the connected Google/Nuki services is subject to the corresponding account, sharing and provider controls. Questions or deletion requests concerning MeiHome-managed cloud data can be sent to the contact address below.

## Security

MeiHome uses encrypted local storage for OAuth/Nuki secrets. No method of electronic storage or transmission can guarantee absolute security.

## Children

MeiHome is intended as a household dashboard and is not specifically directed at children.

## Changes

This privacy policy may be updated when the application's functionality or services change. The current version is published in this repository.

## Contact

Email: phaberland@googlemail.com  
GitHub: https://github.com/pehab/MeiHome
