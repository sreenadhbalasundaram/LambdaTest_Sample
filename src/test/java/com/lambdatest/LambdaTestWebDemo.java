package com.lambdatest;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** One Chrome session showcasing compatible LambdaTest web features with Selenium 3. */
public class LambdaTestWebDemo {
    private static final String TODO = "https://lambdatest.github.io/sample-todo-app/";
    private static final String FILE_SITE = "https://the-internet.herokuapp.com";
    private RemoteWebDriver driver;
    private WebDriverWait wait;
    private Path artifacts;

    @BeforeMethod
    public void setUp() throws Exception {
        String username = requiredEnvironment("LT_USERNAME");
        String accessKey = requiredEnvironment("LT_ACCESS_KEY");
        artifacts = Paths.get("target", "lambdatest-web-demo", String.valueOf(System.currentTimeMillis()));
        Files.createDirectories(artifacts);

        // Flat capabilities match the Selenium 3 dependency already used by this repository.
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("browserName", "Chrome");
        caps.setCapability("version", System.getProperty("demo.browserVersion", "latest"));
        caps.setCapability("platform", System.getProperty("demo.platform", "Windows 11"));
        caps.setCapability("user", username);
        caps.setCapability("accessKey", accessKey);
        caps.setCapability("build", System.getProperty("demo.build", "Web feature demo " + Instant.now()));
        caps.setCapability("name", "Todo + accessibility + Lighthouse + files + network");
        caps.setCapability("projectName", "LambdaTest Web Demo");
        caps.setCapability("tags", Arrays.asList("demo", "testng", "accessibility", "lighthouse"));
        caps.setCapability("buildTags", Collections.singletonList("web-feature-demo"));
        caps.setCapability("resolution", "1920x1080");
        caps.setCapability("video", true);
        caps.setCapability("visual", true); // Command screenshots; does not enable SmartUI.
        caps.setCapability("console", true);
        caps.setCapability("network", true);
        caps.setCapability("network.full.har", true);
        caps.setCapability("commandLog", true);
        caps.setCapability("systemLog", true);
        caps.setCapability("performance", false); // Explicit Lighthouse hooks below.
        caps.setCapability("accessibility", enabled("accessibility"));
        if (enabled("accessibility")) {
            caps.setCapability("accessibility.wcagVersion", "wcag21aa");
            caps.setCapability("accessibility.bestPractice", true);
            caps.setCapability("accessibility.needsReview", true);
        }
        ChromeOptions chrome = new ChromeOptions();
        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("download.prompt_for_download", false);
        preferences.put("profile.default_content_setting_values.automatic_downloads", 1);
        chrome.setExperimentalOption("prefs", preferences);
        caps.setCapability(ChromeOptions.CAPABILITY, chrome);

        // Keep credentials out of the URL and console output.
        driver = new RemoteWebDriver(new URL("https://hub.lambdatest.com/wd/hub"), caps);
        driver.setFileDetector(new LocalFileDetector());
        driver.manage().timeouts().pageLoadTimeout(90, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(180, TimeUnit.SECONDS);
        wait = new WebDriverWait(driver, 45);
        log("Session: " + driver.getSessionId());
        log("Dashboard: https://automation.lambdatest.com/test?testID=" + driver.getSessionId());
        log("Artifacts: " + artifacts.toAbsolutePath());
    }

    @Test
    public void demonstrateWebAutomationFeatures() throws Exception {
        stage("Todo functional assertions", () -> {
            driver.get(TODO);
            WebElement first = wait.until(ExpectedConditions.elementToBeClickable(By.name("li1")));
            first.click();
            Assert.assertTrue(first.isSelected(), "First todo should be completed");
            int originalCount = driver.findElements(By.cssSelector("ul li")).size();
            String task = "Demo LambdaTest web automation";
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sampletodotext"))).sendKeys(task);
            driver.findElement(By.id("addbutton")).click();
            wait.until(d -> d.findElements(By.cssSelector("ul li")).size() == originalCount + 1);
            WebElement added = driver.findElements(By.cssSelector("ul li")).get(originalCount);
            Assert.assertEquals(added.findElement(By.tagName("span")).getText(), task);
            added.findElement(By.tagName("input")).click();
            Assert.assertTrue(added.findElement(By.tagName("input")).isSelected());
            driver.executeScript("console.info('LambdaTest demo: todo assertions passed')");
            screenshot("01-todo");
        });
        if (enabled("accessibility")) {
            stage("WCAG 2.1 AA accessibility scan", () -> {
                Object result = driver.executeScript("lambdatest_executor:{\"action\":\"lambda-accessibility-scan\"}");
                saveJson("accessibility-response.json", result);
                rejectReportedFailure(result, "Accessibility scan");
                log("Scan requested. Review issues in the dashboard Accessibility tab; this is not a compliance gate.");
            });
        }
        if (enabled("lighthouse")) {
            stage("Lighthouse audit on normal network", () -> {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("action", "generateLighthouseReport");
                payload.put("arguments", Collections.singletonMap("url", TODO));
                Object response = driver.executeScript("lambdatest_executor:" + JSONValue.toJSONString(payload));
                saveJson("lighthouse-response.json", response);
                rejectReportedFailure(response, "Lighthouse");
                Object parsed = response instanceof String ? new JSONParser().parse((String) response) : response;
                Assert.assertTrue(parsed instanceof Map, "Lighthouse must return a structured report response");
                Object data = ((Map<?, ?>) parsed).get("data");
                Assert.assertNotNull(data, "Lighthouse returned no report data; inspect lighthouse-response.json");
                Object report = data instanceof String ? new JSONParser().parse((String) data) : data;
                Assert.assertTrue(report instanceof Map && ((Map<?, ?>) report).containsKey("categories"),
                        "Lighthouse response must contain audit categories");
                saveJson("lighthouse-report.json", report);
            });
        }
        if (enabled("throttling")) {
            stage("Todo navigation under Regular 4G", () -> {
                try {
                    driver.executeScript("networkProfile", Collections.singletonMap("condition", "Regular 4G"));
                    driver.navigate().refresh();
                    wait.until(ExpectedConditions.elementToBeClickable(By.id("sampletodotext")))
                            .sendKeys("Works under simulated 4G");
                    driver.findElement(By.id("addbutton")).click();
                    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("body"),
                            "Works under simulated 4G"));
                    screenshot("02-throttled");
                } finally {
                    driver.executeScript("networkProfile", Collections.singletonMap("condition", "Reset"));
                }
            });
        }
        if (enabled("files")) {
            stage("Upload a local file to the cloud browser", () -> {
                Path upload = artifacts.resolve("lambdatest-demo-upload.txt");
                Files.write(upload, "LambdaTest cloud upload demo\n".getBytes(StandardCharsets.UTF_8));
                driver.get(FILE_SITE + "/upload");
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("file-upload")))
                        .sendKeys(upload.toAbsolutePath().toString());
                driver.findElement(By.id("file-submit")).click();
                wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("uploaded-files"),
                        upload.getFileName().toString()));
                Assert.assertEquals(driver.findElement(By.tagName("h3")).getText(), "File Uploaded!");
                screenshot("03-upload");
            });
            stage("Download and verify a file on the cloud machine", () -> {
                driver.get(FILE_SITE + "/download");
                WebElement link = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector(".example a[href$='.txt']")));
                String fileName = link.getText().trim();
                link.click();
                wait.until(d -> Boolean.TRUE.equals(driver.executeScript("lambda-file-exists=" + fileName)));
                saveJson("download-stats.json", driver.executeScript("lambda-file-stats=" + fileName));
                Object content = driver.executeScript("lambda-file-content=" + fileName);
                Assert.assertTrue(content instanceof String, "Expected base64 download content");
                byte[] bytes = Base64.getMimeDecoder().decode((String) content);
                Assert.assertTrue(bytes.length > 0, "Downloaded sample should not be empty");
                Files.write(artifacts.resolve("downloaded-sample.txt"), bytes);
                log("Verified cloud download: " + fileName + " (" + bytes.length + " bytes)");
            });
        }
        stage("Export complete network HAR", () -> {
            Object har = driver.executeScript("lambda:network=full.har");
            Assert.assertNotNull(har, "Expected a full HAR response");
            saveJson("network-har-response.json", har);
        });
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver == null) { return; }
        if (!result.isSuccess()) {
            try { screenshot("failure"); }
            catch (Exception e) { log("Could not capture failure screenshot: " + e.getClass().getSimpleName()); }
        }
        // Reporting failure must still release the cloud session. Never turn a failed test green.
        try {
            driver.executeScript("lambda-status=" + (result.isSuccess() ? "passed" : "failed"));
        } finally {
            try { driver.quit(); } finally { driver = null; }
        }
    }

    private void stage(String name, DemoStep step) throws Exception {
        log(name);
        driver.executeScript("lambda-testCase-start=" + name);
        Throwable failure = null;
        try { step.run(); }
        catch (Exception | AssertionError e) { failure = e; throw e; }
        finally {
            try { driver.executeScript("lambda-testCase-end=" + name); }
            catch (RuntimeException e) {
                if (failure != null) { failure.addSuppressed(e); }
                else { throw e; }
            }
        }
    }

    private void screenshot(String name) throws Exception {
        Files.write(artifacts.resolve(name + ".png"), driver.getScreenshotAs(OutputType.BYTES));
    }

    private void saveJson(String name, Object value) throws Exception {
        String text = value instanceof String ? (String) value : JSONValue.toJSONString(value);
        Files.write(artifacts.resolve(name), text.getBytes(StandardCharsets.UTF_8));
    }

    private void rejectReportedFailure(Object response, String feature) {
        String text = String.valueOf(response).toLowerCase(java.util.Locale.ROOT);
        Assert.assertFalse(text.contains("failed to") || text.contains("not enabled") || text.contains("not supported"),
                feature + " reported a failure; inspect the saved response and account entitlements");
    }

    private static boolean enabled(String feature) {
        return Boolean.parseBoolean(System.getProperty("demo." + feature, "true"));
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Set " + name + " before running the demo");
        }
        return value;
    }

    private static void log(String message) { Reporter.log(message, true); }

    @FunctionalInterface
    private interface DemoStep { void run() throws Exception; }
}
