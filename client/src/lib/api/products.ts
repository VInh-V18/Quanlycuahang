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
  active: boolean;
}

export interface ProductSearchParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export async function searchProducts(
  params: ProductSearchParams,
): Promise<ApiSuccess<Product[]>> {
  const response = await apiClient.get<ApiSuccess<Product[]>>("/products", { params });
  return response.data;
}
