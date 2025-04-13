package com.trading.scraper;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeadlinesScraperApp {

  public static void main(String[] args) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    Runnable scrapeTask =
        () -> {
          NewsScraper scraper = new NewsScraper();
          List<String> headlines = scraper.getLatestHeadlines();
          System.out.println("Scraped headlines: ");
          headlines.forEach(System.out::println);
        };

    // Schedule to run every 10 minutes
    scheduler.scheduleAtFixedRate(scrapeTask, 0, 10, TimeUnit.MINUTES);
  }
}
