# FruitHouse ML Service

Small, self-contained Python microservice that provides sales-velocity
forecasting and reorder-quantity suggestions for the FruitHouse POS/ERP
system (part of "Prompt #12" / AI Assistant v2). It is called internally
by the Spring Boot backend over the Docker network only; it is never
exposed to end users or the internet, so it intentionally implements no
authentication.

## What it does

- `GET /health` — trivial liveness check used by the Docker healthcheck.
- `POST /forecast` — given a branch, a lookback window, and a list of
  products (each with sparse daily sales history, current stock, and
  min stock), returns for each product:
  - `forecastDailyVelocity` — estimated average units sold per day.
  - `daysOfStockRemaining` — `currentStock / velocity` (a large sentinel
    value if velocity is 0).
  - `suggestedQty` — how much to reorder to cover 30 days of demand with
    a 20% safety margin, respecting `minStock` as a floor.
  - `confidence` — a heuristic 0–1 score combining how much sales history
    is available and how stable/noisy that history is.
  - `outliersRemoved` — how many days were flagged as anomalous (e.g.
    one-off bulk sales or data-entry spikes) and excluded from the
    velocity calculation.

The forecasting algorithm zero-fills the sparse daily sales into a
complete daily series over `lookbackDays`, runs `sklearn.ensemble.
IsolationForest` to detect and exclude outlier days (skipped when there
isn't enough non-trivial data for it to be meaningful), and computes the
velocity as the mean of the last 14 (outlier-filtered) days, falling
back to the full filtered history if most of that window was filtered
out. See the code comments in `main.py` for the exact, documented
heuristics and tunables.

## Running locally

```bash
cd ml-service
python -m venv .venv
source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pip install -r requirements.txt
uvicorn main:app --reload
```

The service listens on `http://localhost:8000` by default. Try:

```bash
curl http://localhost:8000/health
```

## Running tests

```bash
cd ml-service
pip install -r requirements.txt pytest
pytest
```

## Docker

```bash
cd ml-service
docker build -t fruithouse-ml-service .
docker run -p 8000:8000 fruithouse-ml-service
```
