# Trading Engine

The Trading Engine is a modular machine learning-powered system designed to deliver real-time stock trade recommendations based on a combination of live market data and news sentiment. It is architected as a scalable, microservice-oriented platform with dedicated modules for scraping, training, serving, and consuming ML-driven insights.

---

## 🧠 High-Level Overview

- **Goal**: To correlate financial market fluctuations with real-time and historical news data to produce actionable trade recommendations.
- **Architecture**:
    - WebFlux API for consuming recommendations
    - Headless browser scraping of financial news
    - Java-based ML training pipeline (DJL)
    - ONNX model served via a lightweight model-sidecar service
    - Shared POJOs and DTOs for consistent contract enforcement across services

---

## 📦 Component Breakdown

### `api-java-webflux`
> **Purpose**: Exposes REST endpoints to serve ML-based trade recommendations

- **Tech**: Spring WebFlux, Reactive WebClient
- **Endpoints**:
    - `GET /recommendations/?symbol={symbol}` – fetches a live recommendation for a given stock symbol
- **Swagger UI**:
    - Available at: `http://localhost:8080/swagger-ui.html`
- **Dependencies**:
    - `shared-models` for DTOs
    - Connects to `model-sidecar` for inference

---

### `shared-models`
> **Purpose**: Centralized data models shared across services

- Contains:
    - `StockFeatures`: feature input class for ML model
    - `TradeRecommendation`: output prediction structure
- No runtime logic
- Used by all modules (training, API, inference)

---

### `model-training`
> **Purpose**: Responsible for ML training pipeline using DJL (Deep Java Library)

- Trains a model on feature vectors + labeled recommendations
- Saves model in ONNX format to shared-models path
- Includes:
    - `TrainModel.java`: training entrypoint
    - `InMemoryDataset.java`: builds training dataset from POJOs
    - `StockFeatureTranslator.java`: converts features to NDArray
- Next steps:
    - Integrate with scraped news + stock data pipeline

---

### `model-sidecar`
> **Purpose**: Lightweight REST service to load & serve the ONNX-trained ML model

- Accepts `StockFeatures` via REST POST
- Uses DJL’s ONNX runtime for inference
- Returns `TradeRecommendation`
- Includes model reload endpoint for dynamic model refreshing

---

### `scraper-playwright-java`
> **Purpose**: Scrapes financial headlines periodically using Playwright

- `NewsScraper.java`: extracts latest headlines from a financial site (via Wayback Machine)
- `HeadlinesScraperApp.java`: runnable scheduler to scrape every 10 minutes
- Planned Enhancements:
    - Save scraped news to MongoDB
    - Integrate with sentiment analysis & feature enrichment

---

## 🚀 Getting Started

1. **Start MongoDB (Docker)**:
   ```bash
   docker run -d -p 27017:27017 --name mongo mongo
   ```
2. **Train your model**:
   ```bash
   ./gradlew :model-training:run
   ```
3. **Run the model-sidecar**:
   ```bash
   ./gradlew :model-sidecar:bootRun
   ```
4. **Run the WebFlux API**:
   ```bash
   ./gradlew :api-java-webflux:bootRun
   ```
5. **Scrape News**:
   ```bash
   ./gradlew :scraper-playwright-java:run
   ```

---

## 🧼 Code Style
- Spotless plugin is included and will auto-format code before build.
- To apply manually:
  ```bash
  ./gradlew spotlessApply
  ```

---

## 📂 Future Components (planned)
- `frontend-dashboard`: Real-time web UI for trades
- `dotnet-sdk`: Reusable SDK for MAUI consumers (Android, iOS, Mac)
- `maui-app`: Cross-platform mobile app

---

## 🧠 Contributors
 - jtortiz27
---

For more info or integration help, open an issue or contact the maintainers.
