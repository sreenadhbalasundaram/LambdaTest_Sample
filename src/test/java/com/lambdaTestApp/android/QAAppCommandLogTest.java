package com.lambdaTestApp.android;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class QAAppCommandLogTest {
    private static final String APP_PACKAGE = "com.example.QAapp";

    private AndroidDriver<MobileElement> driver;
    private WebDriverWait wait;
    private boolean performanceLabelsEnabled;

    @BeforeMethod
    @Parameters({"device", "version"})
    public void setUp(@Optional("OnePlus 11") String device,
                      @Optional("14") String version) throws Exception {
        String username = requiredEnv("LT_USERNAME");
        String accessKey = requiredEnv("LT_ACCESS_KEY");
        String app = requiredEnv("LT_QA_APP_URL");
        if (!app.startsWith("lt://") || app.length() == "lt://".length()) {
            throw new IllegalArgumentException("LT_QA_APP_URL must be the lt:// URL of the uploaded QA app");
        }

        Map<String, Object> options = new HashMap<>();
        options.put("build", "Android QA App");
        options.put("name", "QA App command log replay");
        options.put("deviceName", device);
        options.put("platformVersion", version);
        options.put("platformName", "Android");
        options.put("automationName", "UiAutomator2");
        options.put("isRealMobile", true);
        options.put("app", app);
        options.put("network", true);
//        options.put("appPackage", APP_PACKAGE);
//        options.put("appActivity", APP_PACKAGE + ".SplashScreenAnimation");
        options.put("autoGrantPermissions", true);
        options.put("deviceOrientation", "PORTRAIT");
        performanceLabelsEnabled = Boolean.parseBoolean(System.getProperty("qa.performanceLabels", "true"));
        options.put("appProfiling", performanceLabelsEnabled);
        if (performanceLabelsEnabled) {
            options.put("advancedProfiling", true);
            options.put("resignApp", true);
        }
        options.put("accessibility", true);
        options.put("w3c", true);

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("lt:options", options);
        URI hub = new URI("https", username + ":" + accessKey,
                "mobile-hub.lambdatest.com", -1, "/wd/hub", null, null);
        driver = new AndroidDriver<>(hub.toURL(), capabilities);
        wait = new WebDriverWait(driver, 30);
    }

    @Test
    public void replayQAAppFlow() {
        driver.activateApp(APP_PACKAGE);

        performanceLabel("page_label_youtube", "start", 0.5);
        clickable("hybridView").click();
        clickable("webpage").click();
        openWebsite("https://www.amazon.in");
        openWebsite("https://ifconfig.me");
        driver.executeScript("lambda-accessibility-scan");
        performanceLabel("page_label_youtube", "end", 0.5);

        clickable("buttonPage").click();
        wait.until(ExpectedConditions.elementToBeClickable(MobileBy.AccessibilityId("More options"))).click();
        driver.executeScript("lambda-accessibility-scan");

        performanceLabel("page_label_camera", "start", 0.6);
        clickable("title").click();
        clickable("camera_new").click();
        clickable("btn_capture").click();
        driver.pressKey(new KeyEvent(AndroidKey.BACK));
        clickable("camerax").click();
        driver.executeScript("lambda-accessibility-scan");
        performanceLabel("page_label_camera", "end", 0.6);

        String cameraXActivity = driver.currentActivity();
        clickable("btn_capture").click();
        // The recording starts this label without sending an end hook.
        performanceLabel("page_label_shorts", "start", 0.8);
        driver.pressKey(new KeyEvent(AndroidKey.BACK));
        barcodeAfterCameraX(cameraXActivity).click();
        driver.executeScript("lambda-accessibility-scan");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver == null) {
            return;
        }

        String status = result.isSuccess() ? "passed" : "failed";
        try {
            driver.executeScript("lambda-hook: {\"action\":\"setTestStatus\","
                    + "\"arguments\":{\"status\":\"" + status + "\","
                    + "\"remark\":\"QA App command log replay\"}}");
        } finally {
            driver.quit();
        }
    }

    private WebElement clickable(String resourceId) {
        return wait.until(ExpectedConditions.elementToBeClickable(
                MobileBy.id(APP_PACKAGE + ":id/" + resourceId)));
    }

    private WebElement barcodeAfterCameraX(String cameraXActivity) {
        try {
            return new WebDriverWait(driver, 5).until(ExpectedConditions.elementToBeClickable(
                    MobileBy.id(APP_PACKAGE + ":id/barcode_qr")));
        } catch (TimeoutException transitionTimeout) {
            // Some capture screens consume the first Back to dismiss the preview.
            if (cameraXActivity.equals(driver.currentActivity())
                    && !driver.findElements(MobileBy.id(APP_PACKAGE + ":id/btn_capture")).isEmpty()) {
                driver.pressKey(new KeyEvent(AndroidKey.BACK));
            }
        }

        try {
            return clickable("barcode_qr");
        } catch (TimeoutException timeout) {
            throw new TimeoutException("Barcode control unavailable after CameraX. Current activity: "
                    + driver.currentActivity() + ". Session: " + driver.getSessionId(), timeout);
        }
    }

    private void openWebsite(String url) {
        WebElement address = clickable("websiteName");
        address.clear();
        address.sendKeys(url);
        clickable("findButton").click();
    }

    private void performanceLabel(String name, String action, double sensitivity) {
        if (!performanceLabelsEnabled) {
            return;
        }

        Map<String, Object> label = new HashMap<>();
        label.put("name", name);
        label.put("action", action);
        label.put("label_type", "page-load");
        label.put(action + "Sensitivity", sensitivity);
        try {
            driver.executeScript("lambda-performance-label", label);
        } catch (WebDriverException exception) {
            throw new IllegalStateException("LambdaTest rejected performance label " + name
                    + " (" + action + "). Check Command Logs for session " + driver.getSessionId()
                    + ". To run the UI flow and accessibility scans independently, "
                    + "set -Dqa.performanceLabels=false.", exception);
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Set " + name + " before running this test");
        }
        return value;
    }
}
