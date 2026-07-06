import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Category {
  id: number;
  name: string;
  parentId: number | null;
  displayOrder: number;
}

export async function listCategories(): Promise<Category[]> {
  const response = await apiClient.get<ApiSuccess<Category[]>>("/categories");
  return response.data.data;
}
