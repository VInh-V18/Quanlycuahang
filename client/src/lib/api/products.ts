import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Product {
  id: number;
  categoryId: number;
  categoryName: string;
  sku: string;
  barcode: string | null;
  name: string;
  unit: string;
  sellPrice: number;
  priceIncludesVat: boolean;
  vatRate: number;
  minStock: number;
  imageUrl: string | null;
  active: boolean;
  originCountry: string | null;
  originRegion: string | null;
  /** Chi co gia tri khi truyen branchId cho searchProducts/getProduct. */
  stock: number | null;
  costPrice: number | null;
}

export interface ProductSearchParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
  categoryId?: number;
  active?: boolean;
  originCountry?: string;
  branchId?: number;
}

export interface ProductRequest {
  categoryId?: number | null;
  sku?: string;
  barcode?: string | null;
  name: string;
  unit: string;
  sellPrice: number;
  priceIncludesVat: boolean;
  vatRate: number;
  minStock: number;
  imageUrl?: string | null;
  originCountry?: string | null;
  originRegion?: string | null;
}

export interface PriceHistoryEntry {
  id: number;
  oldPrice: number;
  newPrice: number;
  changedByName: string | null;
  createdAt: string;
}

/** Danh sach quoc gia xuat xu thuong gap (chuoi trai cay nhap khau) — dung chung cho Select +
 * hien thi flag emoji trong bang San pham. */
export const ORIGIN_COUNTRIES = [
  { value: "Việt Nam", flag: "🇻🇳" },
  { value: "Mỹ", flag: "🇺🇸" },
  { value: "Hàn Quốc", flag: "🇰🇷" },
  { value: "Úc", flag: "🇦🇺" },
  { value: "New Zealand", flag: "🇳🇿" },
  { value: "Nhật Bản", flag: "🇯🇵" },
  { value: "Thái Lan", flag: "🇹🇭" },
  { value: "Trung Quốc", flag: "🇨🇳" },
  { value: "Nam Phi", flag: "🇿🇦" },
  { value: "Chile", flag: "🇨🇱" },
] as const;

export function originFlag(country: string | null | undefined): string {
  return ORIGIN_COUNTRIES.find((c) => c.value === country)?.flag ?? "";
}

export async function searchProducts(
  params: ProductSearchParams,
): Promise<ApiSuccess<Product[]>> {
  const response = await apiClient.get<ApiSuccess<Product[]>>("/products", { params });
  return response.data;
}

export async function getProduct(id: number, branchId?: number): Promise<Product> {
  const response = await apiClient.get<ApiSuccess<Product>>(`/products/${id}`, {
    params: { branchId },
  });
  return response.data.data;
}

export async function createProduct(request: ProductRequest): Promise<Product> {
  const response = await apiClient.post<ApiSuccess<Product>>("/products", request);
  return response.data.data;
}

export async function updateProduct(id: number, request: ProductRequest): Promise<Product> {
  const response = await apiClient.put<ApiSuccess<Product>>(`/products/${id}`, request);
  return response.data.data;
}

export async function deleteProduct(id: number): Promise<void> {
  await apiClient.delete(`/products/${id}`);
}

export async function getPriceHistory(id: number): Promise<PriceHistoryEntry[]> {
  const response = await apiClient.get<ApiSuccess<PriceHistoryEntry[]>>(
    `/products/${id}/price-history`,
  );
  return response.data.data;
}
