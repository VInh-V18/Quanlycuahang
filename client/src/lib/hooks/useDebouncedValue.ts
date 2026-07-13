import { useEffect, useState } from "react";

/** Tra ve gia tri "tre" theo delayMs sau lan doi cuoi cung - dung cho o tim kiem gan voi useQuery
 * (DataTable co chu dich KHONG tu debounce, xem comment trong DataTable.tsx: "debounce do noi goi
 * tu quyet dinh") de tranh goi API tren MOI phim go (phat hien khi rieng soat hieu nang, nhieu
 * trang tim kiem dang goi thang search vao queryKey khong qua debounce). */
export function useDebouncedValue<T>(value: T, delayMs = 400): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
