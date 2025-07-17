package ebridge.automation;


import org.openqa.selenium.*;


import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


	
	@Service
	public class EbridgeService  {

	    private static final String DOWNLOAD_DIR = "C:\\Users\\Gokul K\\Desktop\\pdf";
	    private static final String REPORT_DIR = "C:\\Users\\Gokul K\\Desktop\\status ebridge";
	    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy");
	    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyy");
	    private static final String[] CLIENTS = {
	        "EVERYDAY", "WELL", "Eisenberg", "Raso", "GILLION (Weintraub)",
	        "PCM", "PCM - SI", "PCM - LI", "PCM - PP", "FEITELL",
	        "WPFMC (Vitale)", "CRSLI (Colon)", "KATZ"
	    };

	    private Workbook workbook;
	    private Sheet billingSheet;
	    private Sheet postingSheet;
	    private Sheet downloadSheet;
	    private String reportFileName;

	    public String startAutomation() {
	        StringBuilder log = new StringBuilder();
	        try {
	            log(log, "Initializing Excel...");
	            initExcel();

	            WebDriver driver = setupDriver();
	            login(driver);
	            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

	            for (String client : CLIENTS) {
	                boolean downloaded = false;
	                log(log, "🔹 Processing client: " + client);
	                try {
	                    navigateToClient(driver, wait, client);
	                    List<WebElement> rows = driver.findElements(By.xpath("//table[@id='Table1']//tr[td]"));
	                    int max = Math.min(rows.size(), 10);

	                    for (int i = 0; i < max; i++) {
	                        WebElement row = rows.get(i);
	                        String status = row.findElement(By.xpath("./td[8]")).getText().trim();
	                        String dateText = row.findElement(By.xpath("./td[9]")).getText().trim().split(" ")[0];
	                        String batchType = row.findElement(By.xpath("./td[6]")).getText().trim();
	                        String location = row.findElement(By.xpath("./td[5]")).getText().trim();
	                        String pages = row.findElement(By.xpath("./td[11]")).getText().trim();

	                        boolean shouldDownload = status.equals("NOT YET STARTED") ||
	                                (status.equals("COMPLETED") && !dateText.isEmpty() &&
	                                        LocalDate.parse(dateText, DATE_FORMAT).equals(LocalDate.now().minusDays(1)));
	                        if (!shouldDownload) continue;

	                        WebElement icon = row.findElement(By.xpath("./td[2]//img"));
	                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", icon);
	                        icon.click();
	                        Thread.sleep(2000);

	                        String mainWindow = driver.getWindowHandle();
	                        Set<String> handles = driver.getWindowHandles();
	                        handles.remove(mainWindow);

	                        if (!handles.isEmpty()) {
	                            String popup = handles.iterator().next();
	                            driver.switchTo().window(popup);
	                            wait.until(ExpectedConditions.elementToBeClickable(By.id("btnDownload"))).click();
	                            try {
	                                driver.findElement(By.xpath("//span[text()='Download PDF']")).click();
	                            } catch (Exception ignored) {}

	                            String fileName = getFileName(client, batchType, location, dateText);
	                            renameLatestDownload(fileName);
	                            logSheets(client, fileName, batchType, location, status, dateText, pages);
	                            downloaded = true;

	                            driver.close();
	                            driver.switchTo().window(mainWindow);
	                            Thread.sleep(1000);
	                            driver.switchTo().defaultContent();
	                            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("ifMainOuter")));
	                            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("ifMain")));
	                        }
	                    }
	                } catch (Exception e) {
	                    log(log, "⚠️ Error for client " + client + ": " + e.getMessage());
	                }
	                if (!downloaded) {
	                    logSheets(client, "-", "-", "-", "-", "-", "-");
	                }
	            }

	            Thread.sleep(4000);
	            saveExcel();
	            driver.quit();
	            log(log, "✅ Automation completed.");

	        } catch (Exception e) {
	            log(log, "❌ Unexpected error: " + e.getMessage());
	        }

	        return log.toString().replace("\n", "<br>");
	    }

	    private void log(StringBuilder log, String message) {
	        log.append(message).append("\n");
	        System.out.println(message);
	    }

	    private void initExcel() throws IOException {
	        File folder = new File(REPORT_DIR);
	        if (!folder.exists()) folder.mkdirs();

	        reportFileName = "billing_report_" + LocalDate.now().format(FILE_DATE_FORMAT) + "_" +
	                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".xlsx";

	        workbook = new XSSFWorkbook();
	        billingSheet = createSheet("Billing", new String[]{"Client Name", "File Name", "Location", "Pages"});
	        postingSheet = createSheet("Posting", new String[]{"Client Name", "File Name", "Location", "Pages"});
	        downloadSheet = createSheet("Downloads", new String[]{"Client Name", "File Name", "Batch Type", "Location", "Status", "Date", "Pages"});
	    }

	    private Sheet createSheet(String name, String[] headers) {
	        Sheet sheet = workbook.createSheet(name);
	        Row header = sheet.createRow(0);
	        for (int i = 0; i < headers.length; i++) {
	            header.createCell(i).setCellValue(headers[i]);
	        }
	        return sheet;
	    }

	    private WebDriver setupDriver() {
	        ChromeOptions options = new ChromeOptions();
	        Map<String, Object> prefs = new HashMap<>();
	        prefs.put("download.default_directory", DOWNLOAD_DIR);
	        prefs.put("download.prompt_for_download", false);
	        prefs.put("plugins.always_open_pdf_externally", true);
	        options.setExperimentalOption("prefs", prefs);
	        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080");
	        return new ChromeDriver(options);
	    }

	    private void login(WebDriver driver) {
	        driver.get("https://s1.ebridge.com/ebridge/3.0/default.aspx");
	        driver.findElement(By.id("tbUserName")).sendKeys("deeban");
	        driver.findElement(By.id("tbPassword")).sendKeys("Billing2020");
	        driver.findElement(By.id("tbFileCabinet")).sendKeys("NWMEDBILL");
	        driver.findElement(By.id("btnLogin")).click();
	    }

	    private void navigateToClient(WebDriver driver, WebDriverWait wait, String client) throws InterruptedException {
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("ifMainOuter")));
	        wait.until(ExpectedConditions.elementToBeClickable(By.id("btnNavRetrieve"))).click();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("ifMain")));
	        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//input[@type='text'])[1]")));
	        input.click();
	        Thread.sleep(500);
	        driver.findElement(By.xpath("//div[text()='" + client + "']")).click();
	        driver.findElement(By.id("btnSearchF")).click();
	        Thread.sleep(2000);
	    }

	    private String getFileName(String client, String batchType, String location, String date) {
	        String formattedDate = LocalDate.parse(date, DATE_FORMAT).format(FILE_DATE_FORMAT);
	        if (batchType.equalsIgnoreCase("Billing")) {
	            if (client.equals("Raso")) return client + "-Billing-" + location + "-" + formattedDate;
	            if (client.contains("PCM - SI")) return "BILLING-OFFICEVISITS-" + formattedDate + "-SI";
	            if (client.contains("PCM - LI")) return "BILLING-OFFICEVISITS-" + formattedDate + "-LI";
	            if (client.contains("PCM - PP")) return "BILLING-OFFICEVISITS-" + formattedDate + "-PP";
	            return client + "-Billing-" + formattedDate;
	        } else if (batchType.equalsIgnoreCase("Posting")) {
	            if (client.contains("PCM - SI")) return "PCM_MISC_SI_" + formattedDate;
	            if (client.contains("PCM - LI")) return "PCM_MISC_LI_" + formattedDate;
	            if (client.contains("PCM - PP")) return "PCM_MISC_PP_" + formattedDate;
	            return client + "-Posting-" + formattedDate;
	        }
	        return client + "-" + batchType + "-" + formattedDate;
	    }

	    private void renameLatestDownload(String newName) throws InterruptedException {
	        File dir = new File(DOWNLOAD_DIR);
	        Thread.sleep(3000);
	        File[] files = dir.listFiles((d, name) -> name.endsWith(".pdf"));
	        if (files == null || files.length == 0) return;

	        File latest = Arrays.stream(files).filter(f -> !f.getName().contains(newName))
	                .max(Comparator.comparingLong(File::lastModified)).orElse(null);
	        if (latest == null) return;

	        File renamed = new File(DOWNLOAD_DIR, newName + ".pdf");
	        int count = 1;
	        while (renamed.exists()) {
	            renamed = new File(DOWNLOAD_DIR, newName + "_" + count + ".pdf");
	            count++;
	        }

	        latest.renameTo(renamed);
	    }

	    private void logSheets(String client, String fileName, String batchType, String location, String status, String date, String pages) {
	        Row dRow = downloadSheet.createRow(downloadSheet.getLastRowNum() + 1);
	        dRow.createCell(0).setCellValue(client);
	        dRow.createCell(1).setCellValue(fileName);
	        dRow.createCell(2).setCellValue(batchType);
	        dRow.createCell(3).setCellValue(location);
	        dRow.createCell(4).setCellValue(status);
	        dRow.createCell(5).setCellValue(date);
	        dRow.createCell(6).setCellValue(pages);

	        if (batchType.equalsIgnoreCase("Billing")) {
	            Row row = billingSheet.createRow(billingSheet.getLastRowNum() + 1);
	            row.createCell(0).setCellValue(client);
	            row.createCell(1).setCellValue(fileName);
	            row.createCell(2).setCellValue(client.equals("Raso") ? location : "");
	            row.createCell(3).setCellValue(pages);
	        } else if (batchType.equalsIgnoreCase("Posting")) {
	            Row row = postingSheet.createRow(postingSheet.getLastRowNum() + 1);
	            row.createCell(0).setCellValue(client);
	            row.createCell(1).setCellValue(fileName);
	            row.createCell(2).setCellValue("");
	            row.createCell(3).setCellValue(pages);
	        }
	    }

	    private void saveExcel() throws IOException {
	        try (FileOutputStream out = new FileOutputStream(REPORT_DIR + File.separator + reportFileName)) {
	            workbook.write(out);
	        }
	        workbook.close();
	    }
	}


