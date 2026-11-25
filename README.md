# LambdaTest Web and Android Sample

This Maven/TestNG project contains the Selenium web demo and the Appium Android demos in their original Java packages.

## Setup

Install Maven and JDK 8 or 11. Set `LT_USERNAME` and `LT_ACCESS_KEY` in your environment.

For the Proverbial Android test, upload `app/proverbial_android.apk` to your LambdaTest real-device app storage and set `LT_APP_URL` to the returned `lt://` URL. The Android test uses Proverbial resource IDs. For `QAAppCommandLogTest`, upload `app/NewAndroidSampleAppRelease5Jul25(2).apk` and set `LT_QA_APP_URL` to that app’s `lt://` URL.

## Run

```sh
mvn test                           # All tests, using testng.xml
mvn test -Dsuite=web_demo.xml       # Web only
mvn test -Dsuite=android_single.xml # Android only
```

The Proverbial test defaults to Pixel 7 on Android 13. The QA app test defaults to a Galaxy device on Android 15. Pass `-Dqa.performanceLabels=false` to run the QA app flow without performance labels. Edit the device parameters in the suite XML you run to select a different device. Enable Android SmartUI with `-Ddemo.android.smartui=true` or a dedicated proxy with `-Ddemo.android.dedicatedProxy=true` if your account supports those features.

The web demo runs its accessibility, Lighthouse, network throttling, and file stages by default. Each can be disabled with `-Ddemo.accessibility=false`, `-Ddemo.lighthouse=false`, `-Ddemo.throttling=false`, or `-Ddemo.files=false`. Maven reports are in `target/surefire-reports/`.
