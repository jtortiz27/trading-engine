package com.trading.scraper;

import com.microsoft.playwright.*;

import java.util.List;
import java.util.stream.Collectors;

public class NewsScraper {
    public List<String> getLatestHeadlines() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate("https://web.archive.org/web/*/https://www.cnbc.com/latest-news/");
            page.waitForSelector(".LatestNews-headline");

            return page.querySelectorAll(".LatestNews-headline")
                       .stream()
                       .map(ElementHandle::innerText)
                       .collect(Collectors.toList());
        }
    }
}
