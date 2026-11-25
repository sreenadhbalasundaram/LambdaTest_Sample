package com.lambdaTestApp.android;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;

public class AndroidApp {
    private AppiumDriver<MobileElement> driver;

    @Test
    @Parameters({"device", "version", "platform"})
    public void AndroidApp1(String device, String version, String platform) throws Exception {
        String username = requiredEnvironment("LT_USERNAME");
        String accessKey = requiredEnvironment("LT_ACCESS_KEY");
        DesiredCapabilities capabilities = new DesiredCapabilities();
        HashMap<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("build", "Android demo");
        ltOptions.put("name", platform + " " + device + " " + version);
        ltOptions.put("deviceName", device);
        ltOptions.put("platformVersion", version);
        ltOptions.put("platformName", platform);
        ltOptions.put("isRealMobile", true);
        ltOptions.put("app", requiredEnvironment("LT_APP_URL"));
        ltOptions.put("console", true);
        ltOptions.put("network", true);
        ltOptions.put("visual", true);
        ltOptions.put("devicelog", true);
        ltOptions.put("appProfiling", true);
        ltOptions.put("dedicatedProxy", Boolean.getBoolean("demo.android.dedicatedProxy"));
        HashMap<String, String> locationObj = new HashMap<>();
        locationObj.put("lat", "51.507351");
        locationObj.put("long", "-0.127758");
        ltOptions.put("location", locationObj);
        ltOptions.put("appiumVersion", "3.0.2");
        ltOptions.put("w3c", true);
        capabilities.setCapability("lt:options", ltOptions);
        URL hub = new URI("https", username + ":" + accessKey,
                "mobile-hub.lambdatest.com", -1, "/wd/hub", null, null).toURL();
        driver = new AppiumDriver<>(hub, capabilities);
        WebDriverWait wait = new WebDriverWait(driver, 60);
        WebElement color = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/color")));
        color.click();
        if (Boolean.getBoolean("demo.android.smartui")) {
            driver.executeScript("smartui.takeScreenshot=homepage.png");
        }
        WebElement text = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/Text")));
        text.click();
        WebElement toast = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/toast")));
        toast.click();
        WebElement notification = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/notification")));
        notification.click();
        WebElement speedtest = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/speedTest")));
        speedtest.click();
        WebElement home = wait.until(ExpectedConditions.elementToBeClickable(MobileBy.AccessibilityId("Home")));
        home.click();
        WebElement browser = wait.until(ExpectedConditions.elementToBeClickable(MobileBy.AccessibilityId("Browser")));
        browser.click();
        WebElement url = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/url")));
        url.sendKeys("https://www.lambdatest.com");
        WebElement find = wait.until(ExpectedConditions.elementToBeClickable(By.id("com.lambdatest.proverbial:id/find")));
        find.click();
        // Keep the native browser open so its navigation is captured in the session video.
        Thread.sleep(50000);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Set " + name + " before running the demo");
        }
        return value;
    }
}
