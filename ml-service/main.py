"""FruitHouse ML Service.

Small, self-contained FastAPI microservice that provides sales-velocity
forecasting and reorder-quantity suggestions for the FruitHouse POS/ERP
system. Called internally by the Spring Boot backend over the Docker
network only -- no authentication is implemented here by design (see
Prompt #12 of the AI Assistant v2 roadmap).
"""

from __future__ import annotations

from datetime import date, timedelta
from typing import List, Optional

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel
from sklearn.ensemble import IsolationForest

app = FastAPI(title="FruitHouse ML Service", version="1.0.0")


# ---------------------------------------------------------------------------
# Request / response models
# ---------------------------------------------------------------------------

class DailySale(BaseModel):
    date: date
    quantity: float = 0.0


class ProductInput(BaseModel):
    productId: int
    productName: Optional[str] = None
    sku: Optional[str] = None
    currentStock: float = 0.0
    minStock: float = 0.0
    dailySales: List[DailySale] = []


class ForecastRequest(BaseModel):
    branchId: Optional[int] = None
    lookbackDays: int = 90
    products: List[ProductInput] = []


class ProductForecast(BaseModel):
    productId: int
    forecastDailyVelocity: float
    daysOfStockRemaining: Optional[float]
    suggestedQty: float
    confidence: float
    outliersRemoved: int


class ForecastResponse(BaseModel):
    results: List[ProductForecast]


# ---------------------------------------------------------------------------
# Constants / tunables
# ---------------------------------------------------------------------------

# Sentinel used for daysOfStockRemaining when velocity is 0 (i.e. stock
# would theoretically never run out). The Java caller is documented to
# decide whether to display a reorder row using `suggestedQty > 0`, not
# this field, so it is informational only. A very large float is used
# instead of null to keep the field type simple on the Java side (a
# nullable Double also works, but a big sentinel avoids null-handling
# entirely if the caller ever forgets a null check).
DAYS_OF_STOCK_SENTINEL = 999999.0

# Minimum number of *non-trivial* daily observations required before we
# bother running IsolationForest. Below this, there simply isn't enough
# data for outlier detection to be statistically meaningful, and running
# it anyway risks flagging legitimate sparse-sales days as "outliers".
MIN_POINTS_FOR_OUTLIER_DETECTION = 14

# Window (days) used to compute the forecast velocity under normal
# conditions.
VELOCITY_WINDOW_DAYS = 14

# Days of cover the suggested reorder quantity should provide, with a
# 20% safety margin on top (i.e. target stock = 30 days * 1.2).
TARGET_COVER_DAYS = 30
SAFETY_FACTOR = 1.2


def _build_daily_series(daily_sales: List[DailySale], lookback_days: int) -> List[float]:
    """Build a complete, zero-filled, date-sorted quantity series covering
    the last `lookback_days` days (today back to lookback_days-1 ago).

    The caller sends a sparse list (zero-sale days are typically omitted,
    and ordering is not guaranteed), so we reconstruct the full window by
    indexing sales by date and filling any missing date with 0.0.
    """
    if lookback_days <= 0:
        return []

    today = date.today()
    start = today - timedelta(days=lookback_days - 1)

    sales_by_date = {}
    for entry in daily_sales:
        # If the same date appears twice (shouldn't normally happen),
        # sum the quantities rather than silently dropping one.
        sales_by_date[entry.date] = sales_by_date.get(entry.date, 0.0) + entry.quantity

    series = []
    current = start
    while current <= today:
        series.append(float(sales_by_date.get(current, 0.0)))
        current += timedelta(days=1)
    return series


def _detect_outliers(series: List[float]) -> List[bool]:
    """Return a boolean mask (same length as `series`) marking which days
    IsolationForest considers outliers. Returns all-False if there isn't
    enough data, or if the series has no variance (all identical values,
    e.g. all zeros -- there is nothing "anomalous" to detect).
    """
    n = len(series)
    non_zero_count = sum(1 for v in series if v > 0)

    if n < MIN_POINTS_FOR_OUTLIER_DETECTION or non_zero_count < MIN_POINTS_FOR_OUTLIER_DETECTION:
        return [False] * n

    arr = np.array(series, dtype=float).reshape(-1, 1)

    if np.allclose(arr, arr[0]):
        # Constant series (including all-zero) -- no meaningful outliers.
        return [False] * n

    # contamination=0.1 -> assume ~10% of days could be anomalous bulk
    # sales / data-entry spikes, which is a reasonable default for daily
    # retail sales data without prior knowledge of the true outlier rate.
    # n_estimators=100 is scikit-learn's default and is stable for series
    # of this size. random_state is fixed for deterministic, reproducible
    # results across requests (important since this feeds a business
    # decision, not just an exploratory analysis).
    clf = IsolationForest(contamination=0.1, random_state=42, n_estimators=100)
    predictions = clf.fit_predict(arr)  # -1 = outlier, 1 = inlier
    return [p == -1 for p in predictions]


def _compute_velocity(series: List[float], outlier_mask: List[bool]) -> float:
    """Mean daily quantity over the last VELOCITY_WINDOW_DAYS days of the
    outlier-filtered series. If most of that window got filtered out as
    outliers (leaving too few points to be representative), fall back to
    using the entire filtered history instead, on the theory that a
    slightly stale average is better than an average of 1-2 points.
    """
    filtered = [v for v, is_outlier in zip(series, outlier_mask) if not is_outlier]
    if not filtered:
        return 0.0

    recent_window = filtered[-VELOCITY_WINDOW_DAYS:]

    # If fewer than half the target window survived filtering, treat the
    # recent window as unrepresentative and fall back to the full
    # filtered history.
    if len(recent_window) < VELOCITY_WINDOW_DAYS / 2:
        recent_window = filtered

    if not recent_window:
        return 0.0

    return float(np.mean(recent_window))


def _compute_confidence(series: List[float], velocity: float) -> float:
    """Heuristic confidence score in [0, 1].

    NOTE: this is an unverified heuristic -- it has not been validated
    against real outcomes and should be revisited once we have live
    usage data (e.g. compare predicted vs actual velocity over time).

    It combines two signals:
      1. Data volume: how many days of real (non-zero) sales history we
         have, normalized against VELOCITY_WINDOW_DAYS. More real data
         points => more confidence that the average reflects a genuine
         pattern rather than noise. Capped at 1.0 once we have at least
         a full velocity window of non-zero days.
      2. Stability: coefficient of variation (stdev / mean) of the
         series, inverted and squashed into [0, 1]. A low-variance
         series (steady, predictable sales) yields higher confidence
         than a highly erratic one, even with the same amount of data.

    The two signals are averaged with equal weight. This is a reasonable
    starting point, not a scientifically derived formula.
    """
    non_zero_count = sum(1 for v in series if v > 0)
    volume_score = min(1.0, non_zero_count / VELOCITY_WINDOW_DAYS)

    if velocity <= 0 or len(series) < 2:
        stability_score = 0.0 if velocity <= 0 else 0.5
    else:
        std = float(np.std(series))
        cv = std / velocity if velocity > 0 else float("inf")
        # Squash coefficient of variation into [0, 1]: cv=0 -> 1.0,
        # cv>=2 -> ~0. Chosen so that "typical" retail noise (cv around
        # 0.5-1.0) lands in the middle of the range rather than at the
        # extremes.
        stability_score = max(0.0, 1.0 - cv / 2.0)

    confidence = 0.5 * volume_score + 0.5 * stability_score
    return round(min(1.0, max(0.0, confidence)), 2)


def _forecast_product(product: ProductInput, lookback_days: int) -> ProductForecast:
    series = _build_daily_series(product.dailySales, lookback_days)
    outlier_mask = _detect_outliers(series)
    outliers_removed = sum(outlier_mask)

    velocity = _compute_velocity(series, outlier_mask)

    if velocity > 0:
        days_of_stock_remaining = product.currentStock / velocity
    else:
        days_of_stock_remaining = DAYS_OF_STOCK_SENTINEL

    target_stock = max(product.minStock, velocity * TARGET_COVER_DAYS * SAFETY_FACTOR)
    suggested_qty = max(0.0, target_stock - product.currentStock)

    confidence = _compute_confidence(series, velocity)

    return ProductForecast(
        productId=product.productId,
        forecastDailyVelocity=round(velocity, 2),
        daysOfStockRemaining=round(days_of_stock_remaining, 2),
        # Rounded to 1 decimal: the Java side does its own ceiling-round
        # to an integer afterward, so sub-integer precision here is
        # preserved for that step rather than pre-rounded away.
        suggestedQty=round(suggested_qty, 1),
        confidence=confidence,
        outliersRemoved=int(outliers_removed),
    )


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/forecast", response_model=ForecastResponse)
def forecast(request: ForecastRequest):
    if not request.products:
        return ForecastResponse(results=[])

    results = [
        _forecast_product(product, request.lookbackDays)
        for product in request.products
    ]
    return ForecastResponse(results=results)
