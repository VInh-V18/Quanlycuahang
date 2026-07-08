import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Category {
  id: number;
  name: string;
  parentId: number | null;
  displayOrder: number;
}

export interface CategoryRequest {
  name: string;
  parentId: number | null;
  displayOrder: number;
}

export async function listCategories(): Promise<Category[]> {
  const response = await apiClient.get<ApiSuccess<Category[]>>("/categories");
  return response.data.data;
}

export async function createCategory(request: CategoryRequest): Promise<Category> {
  const response = await apiClient.post<ApiSuccess<Category>>("/categories", request);
  return response.data.data;
}

export async function updateCategory(id: number, request: CategoryRequest): Promise<Category> {
  const response = await apiClient.put<ApiSuccess<Category>>(`/categories/${id}`, request);
  return response.data.data;
}

export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/categories/${id}`);
}
