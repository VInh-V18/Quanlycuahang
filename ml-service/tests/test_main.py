"""Tests for the FruitHouse ML service."""

from datetime import date, timedelta

import sys
from pathlib import Path

# Allow running `pytest` from the ml-service/ directory without needing
# to install the package.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from fastapi.testclient import TestClient

from main import _build_daily_series, _compute_velocity, _detect_outliers, app

client = TestClient(app)


def _dates_back(n_days: int):
    """Helper: list of date objects for the last n_days, oldest first."""
    today = date.today()
    return [today - timedelta(days=i) for i in range(n_days - 1, -1, -1)]


# ---------------------------------------------------------------------------
# /health
# ---------------------------------------------------------------------------

def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body.get("status") == "ok"


# ---------------------------------------------------------------------------
# /forecast - basic contract
# ---------------------------------------------------------------------------

def test_forecast_empty_products_returns_empty_results():
    resp = client.post("/forecast", json={"branchId": 1, "lookbackDays": 90, "products": []})
    assert resp.status_code == 200
    assert resp.json() == {"results": []}


def test_forecast_product_with_empty_daily_sales_does_not_crash():
    payload = {
        "branchId": 1,
        "lookbackDays": 90,
        "products": [
            {
                "productId": 42,
                "productName": "New Product",
                "sku": "SKU-NEW",
                "currentStock": 10.0,
                "minStock": 5.0,
                "dailySales": [],
            }
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    results = resp.json()["results"]
    assert len(results) == 1
    r = results[0]
    assert r["productId"] == 42
    assert r["forecastDailyVelocity"] == 0.0
    assert r["outliersRemoved"] == 0
    # velocity 0 -> target stock = minStock -> suggestedQty = max(0, minStock - currentStock)
    assert r["suggestedQty"] == 0.0  # currentStock (10) already >= minStock (5)


def test_forecast_all_zero_sales_velocity_zero_and_suggested_from_min_stock():
    dates = _dates_back(30)
    daily_sales = [{"date": d.isoformat(), "quantity": 0.0} for d in dates]
    payload = {
        "branchId": 1,
        "lookbackDays": 30,
        "products": [
            {
                "productId": 7,
                "productName": "Dead Stock Fruit",
                "sku": "SKU-DEAD",
                "currentStock": 2.0,
                "minStock": 20.0,
                "dailySales": daily_sales,
            }
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    r = resp.json()["results"][0]
    assert r["forecastDailyVelocity"] == 0.0
    assert r["outliersRemoved"] == 0
    # target stock = max(minStock, 0) = 20 -> suggestedQty = 20 - 2 = 18
    assert r["suggestedQty"] == 18.0
    assert r["daysOfStockRemaining"] > 1000  # sentinel for "won't run out"


def test_forecast_steady_sales_arithmetic():
    # 20 days of steady sales at 5.0/day, no zeros, no outliers.
    dates = _dates_back(20)
    daily_sales = [{"date": d.isoformat(), "quantity": 5.0} for d in dates]
    payload = {
        "branchId": 1,
        "lookbackDays": 20,
        "products": [
            {
                "productId": 99,
                "productName": "Steady Seller",
                "sku": "SKU-STEADY",
                "currentStock": 15.0,
                "minStock": 10.0,
                "dailySales": daily_sales,
            }
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    r = resp.json()["results"][0]

    # Constant series -> IsolationForest skipped (no variance) -> 0 outliers.
    assert r["outliersRemoved"] == 0
    # Velocity = mean of last 14 days of constant 5.0 series = 5.0
    assert r["forecastDailyVelocity"] == 5.0
    # daysOfStockRemaining = 15 / 5 = 3.0
    assert r["daysOfStockRemaining"] == 3.0
    # target stock = max(10, 5 * 30 * 1.2) = max(10, 180) = 180
    # suggestedQty = 180 - 15 = 165.0
    assert r["suggestedQty"] == 165.0
    assert 0.0 <= r["confidence"] <= 1.0


def test_forecast_outlier_day_is_excluded_from_velocity():
    # 20 days of steady 2.0/day sales, with one huge spike day (bulk sale).
    dates = _dates_back(20)
    daily_sales = []
    for i, d in enumerate(dates):
        qty = 2.0
        if i == 10:
            qty = 500.0  # obvious one-off bulk sale / data entry spike
        daily_sales.append({"date": d.isoformat(), "quantity": qty})

    payload = {
        "branchId": 1,
        "lookbackDays": 20,
        "products": [
            {
                "productId": 55,
                "productName": "Spiky Product",
                "sku": "SKU-SPIKE",
                "currentStock": 50.0,
                "minStock": 5.0,
                "dailySales": daily_sales,
            }
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    r = resp.json()["results"][0]

    assert r["outliersRemoved"] >= 1
    # Velocity should be close to 2.0 (the outlier excluded), not skewed
    # up towards the 500 spike.
    assert r["forecastDailyVelocity"] < 10.0
    assert r["forecastDailyVelocity"] > 0.0


def test_forecast_multiple_products_matched_by_id():
    dates = _dates_back(5)
    payload = {
        "branchId": 1,
        "lookbackDays": 5,
        "products": [
            {
                "productId": 1,
                "currentStock": 10.0,
                "minStock": 5.0,
                "dailySales": [{"date": dates[0].isoformat(), "quantity": 1.0}],
            },
            {
                "productId": 2,
                "currentStock": 20.0,
                "minStock": 5.0,
                "dailySales": [{"date": dates[1].isoformat(), "quantity": 2.0}],
            },
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    results = resp.json()["results"]
    ids = {r["productId"] for r in results}
    assert ids == {1, 2}


def test_forecast_lookback_days_zero_does_not_crash():
    payload = {
        "branchId": 1,
        "lookbackDays": 0,
        "products": [
            {
                "productId": 3,
                "currentStock": 5.0,
                "minStock": 2.0,
                "dailySales": [],
            }
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    r = resp.json()["results"][0]
    assert r["forecastDailyVelocity"] == 0.0


def test_forecast_decimal_quantities_handled():
    dates = _dates_back(15)
    daily_sales = [{"date": d.isoformat(), "quantity": 2.5} for d in dates]
    payload = {
        "branchId": 1,
        "lookbackDays": 15,
        "products": [
            {
                "productId": 8,
                "currentStock": 5.0,
                "minStock": 1.0,
                "dailySales": daily_sales,
            }
        ],
    }
    resp = client.post("/forecast", json=payload)
    assert resp.status_code == 200
    r = resp.json()["results"][0]
    assert r["forecastDailyVelocity"] == 2.5


# ---------------------------------------------------------------------------
# Unit-level tests for helper functions
# ---------------------------------------------------------------------------

def test_build_daily_series_fills_missing_dates_with_zero():
    today = date.today()
    from main import DailySale

    sparse = [
        DailySale(date=today, quantity=3.0),
        DailySale(date=today - timedelta(days=2), quantity=4.0),
    ]
    series = _build_daily_series(sparse, 3)
    assert len(series) == 3
    assert series == [4.0, 0.0, 3.0]  # oldest -> newest


def test_detect_outliers_skips_when_insufficient_data():
    series = [1.0, 2.0, 0.0, 3.0]  # fewer than MIN_POINTS_FOR_OUTLIER_DETECTION
    mask = _detect_outliers(series)
    assert mask == [False] * len(series)


def test_compute_velocity_empty_returns_zero():
    assert _compute_velocity([], []) == 0.0
