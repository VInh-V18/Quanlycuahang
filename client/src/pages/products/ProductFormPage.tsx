import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Controller, useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { FolderCog } from "lucide-react";
import { z } from "zod";
import { useAppSelector } from "@/store/hooks";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CategoryManagerDialog } from "@/components/products/CategoryManagerDialog";
import { Form, FormControl, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { FormField } from "@/components/common/FormField";
import { NumberField } from "@/components/common/NumberField";
import { NumberInput } from "@/components/common/NumberInput";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Money } from "@/components/common/Money";
import { useToast } from "@/components/ui/use-toast";
import { listCategories } from "@/lib/api/categories";
import { updateCostPrice } from "@/lib/api/inventory";
import {
  createProduct,
  getPriceHistory,
  getProduct,
  ORIGIN_COUNTRIES,
  updateProduct,
} from "@/lib/api/products";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDate } from "@/lib/utils";

const productSchema = z.object({
  name: z.string().min(1, "Vui lòng nhập tên sản phẩm"),
  sku: z.string().optional(),
  barcode: z.string().optional(),
  categoryId: z.string().min(1, "Vui lòng chọn danh mục"),
  unit: z.string().min(1, "Vui lòng nhập đơn vị tính"),
  originCountry: z.string().optional(),
  originRegion: z.string().optional(),
  sellPrice: z.coerce.number().min(0, "Giá bán phải ≥ 0"),
  priceIncludesVat: z.boolean(),
  vatRate: z.coerce.number(),
  minStock: z.coerce.number().min(0, "Tồn tối thiểu phải ≥ 0"),
});

type ProductFormValues = z.infer<typeof productSchema>;

const numberFormatter = new Intl.NumberFormat("vi-VN");

export function ProductFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const branchId = useCurrentBranchId();
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canManageCategories =
    permissions.includes("category:create") ||
    permissions.includes("category:update") ||
    permissions.includes("category:delete");
  const canOverrideCostPrice = permissions.includes("inventory:cost-price-override");
  const [showCategoryManager, setShowCategoryManager] = useState(false);
  const [costPriceInput, setCostPriceInput] = useState<number | null>(null);

  const productQuery = useQuery({
    queryKey: ["products", id, branchId],
    queryFn: () => getProduct(Number(id), branchId),
    enabled: isEdit,
  });

  useEffect(() => {
    setCostPriceInput(productQuery.data?.costPrice ?? null);
  }, [productQuery.data?.costPrice]);

  const costPriceMutation = useMutation({
    mutationFn: (newCostPrice: number) => updateCostPrice(Number(id), branchId, newCostPrice),
    onSuccess: () => {
      toast({ title: "Đã cập nhật giá vốn" });
      queryClient.invalidateQueries({ queryKey: ["products", id, branchId] });
    },
    onError: (err) => {
      toast({
        variant: "destructive",
        title: "Không thể cập nhật giá vốn",
        description: getApiErrorMessage(err),
      });
    },
  });

  const priceHistoryQuery = useQuery({
    queryKey: ["products", id, "price-history"],
    queryFn: () => getPriceHistory(Number(id)),
    enabled: isEdit,
  });

  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: listCategories });

  const form = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: "",
      sku: "",
      barcode: "",
      categoryId: "",
      unit: "Cái",
      originCountry: "",
      originRegion: "",
      sellPrice: 0,
      priceIncludesVat: true,
      vatRate: 5,
      minStock: 0,
    },
  });

  useEffect(() => {
    const product = productQuery.data;
    if (!product) return;
    form.reset({
      name: product.name,
      sku: product.sku,
      barcode: product.barcode ?? "",
      categoryId: String(product.categoryId ?? ""),
      unit: product.unit,
      originCountry: product.originCountry ?? "",
      originRegion: product.originRegion ?? "",
      sellPrice: product.sellPrice,
      priceIncludesVat: product.priceIncludesVat,
      vatRate: product.vatRate,
      minStock: product.minStock,
    });
  }, [productQuery.data, form]);

  const saveMutation = useMutation({
    mutationFn: (values: ProductFormValues) => {
      const request = {
        name: values.name,
        sku: values.sku || undefined,
        barcode: values.barcode || null,
        categoryId: values.categoryId ? Number(values.categoryId) : null,
        unit: values.unit,
        originCountry: values.originCountry || null,
        originRegion: values.originRegion || null,
        sellPrice: values.sellPrice,
        priceIncludesVat: values.priceIncludesVat,
        vatRate: values.vatRate,
        minStock: values.minStock,
      };
      return isEdit ? updateProduct(Number(id), request) : createProduct(request);
    },
    onSuccess: () => {
      toast({ title: isEdit ? "Đã lưu thay đổi" : "Đã thêm sản phẩm" });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      navigate("/products");
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  if (isEdit && productQuery.isLoading) {
    return <p className="text-sm text-muted-foreground">Đang tải...</p>;
  }

  if (isEdit && productQuery.isError) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-destructive">{getApiErrorMessage(productQuery.error)}</p>
        <Button variant="outline" onClick={() => navigate("/products")}>
          Quay lại
        </Button>
      </div>
    );
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}
        className="space-y-4"
      >
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">{isEdit ? "Sửa sản phẩm" : "Thêm sản phẩm mới"}</h1>
            <p className="text-sm text-muted-foreground">
              Sản phẩm / {isEdit ? "Sửa" : "Thêm mới"}
            </p>
          </div>
          <div className="flex gap-2">
            <Button type="button" variant="outline" onClick={() => navigate("/products")}>
              Hủy
            </Button>
            <Button type="submit" disabled={saveMutation.isPending}>
              Lưu sản phẩm
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle>Thông tin chung</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <FormField control={form.control} name="name" label="Tên sản phẩm" required />
                <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="sku"
                    label="Mã SKU"
                    description="Để trống sẽ tự sinh"
                  />
                  <FormField control={form.control} name="barcode" label="Mã vạch (barcode)" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <Controller
                    control={form.control}
                    name="categoryId"
                    render={({ field }) => (
                      <FormItem>
                        <div className="flex items-center justify-between">
                          <FormLabel>
                            Danh mục<span className="text-destructive"> *</span>
                          </FormLabel>
                          {canManageCategories && (
                            <button
                              type="button"
                              className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                              onClick={() => setShowCategoryManager(true)}
                            >
                              <FolderCog className="h-3 w-3" />+ Danh mục mới
                            </button>
                          )}
                        </div>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Chọn danh mục" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {categoriesQuery.data?.map((c) => (
                              <SelectItem key={c.id} value={String(c.id)}>
                                {c.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField control={form.control} name="unit" label="Đơn vị tính" required />
                </div>
                <Controller
                  control={form.control}
                  name="originCountry"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Xuất xứ</FormLabel>
                      <div className="grid grid-cols-2 gap-4">
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Quốc gia" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {ORIGIN_COUNTRIES.map((c) => (
                              <SelectItem key={c.value} value={c.value}>
                                {c.flag} {c.value}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <Input
                          placeholder="Vùng (VD: Washington)"
                          {...form.register("originRegion")}
                        />
                      </div>
                    </FormItem>
                  )}
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Giá &amp; thuế</CardTitle>
              </CardHeader>
              <CardContent className="grid grid-cols-3 gap-4">
                <div>
                  <FormLabel>Giá vốn</FormLabel>
                  {isEdit && canOverrideCostPrice ? (
                    <div className="mt-2 flex gap-2">
                      <NumberInput
                        value={costPriceInput}
                        onValueChange={setCostPriceInput}
                        min={0}
                        placeholder="—"
                      />
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={costPriceInput == null || costPriceMutation.isPending}
                        onClick={() => costPriceInput != null && costPriceMutation.mutate(costPriceInput)}
                      >
                        Cập nhật
                      </Button>
                    </div>
                  ) : (
                    <div className="mt-2 flex h-10 items-center rounded-md border bg-muted px-3 text-sm">
                      {productQuery.data?.costPrice != null ? (
                        <Money value={productQuery.data.costPrice} />
                      ) : (
                        "—"
                      )}
                    </div>
                  )}
                  <p className="mt-1 text-xs text-muted-foreground">
                    {canOverrideCostPrice
                      ? "Bình quân gia quyền, tự đổi khi nhập hàng — có thể ghi đè trực tiếp tại đây"
                      : "Bình quân gia quyền — chỉ đổi khi nhập hàng"}
                  </p>
                </div>
                <NumberField
                  control={form.control}
                  name="sellPrice"
                  label="Giá bán / đơn vị"
                  required
                  min={0}
                />
                <Controller
                  control={form.control}
                  name="vatRate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thuế suất VAT</FormLabel>
                      <Select
                        value={String(field.value)}
                        onValueChange={(v) => field.onChange(Number(v))}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="0">0%</SelectItem>
                          <SelectItem value="5">5%</SelectItem>
                          <SelectItem value="8">8%</SelectItem>
                          <SelectItem value="10">10%</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormItem>
                  )}
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Tồn kho</CardTitle>
              </CardHeader>
              <CardContent className="grid grid-cols-2 gap-4">
                <NumberField
                  control={form.control}
                  name="minStock"
                  label="Tồn tối thiểu"
                  required
                  allowDecimal={false}
                  min={0}
                />
                <div>
                  <FormLabel>Tồn hiện tại — Kho hiện tại</FormLabel>
                  <div className="mt-2 flex h-10 items-center rounded-md border bg-muted px-3 text-sm">
                    {productQuery.data?.stock != null
                      ? `${numberFormatter.format(productQuery.data.stock)} ${productQuery.data.unit}`
                      : "—"}
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            {isEdit && (
              <Card>
                <CardHeader>
                  <CardTitle>Lịch sử giá bán</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  {priceHistoryQuery.data?.length ? (
                    priceHistoryQuery.data.map((h) => (
                      <div key={h.id} className="text-sm">
                        <div className="text-muted-foreground">
                          {formatDate(h.createdAt)}
                        </div>
                        <div>
                          <Money value={h.oldPrice} /> → <Money value={h.newPrice} />
                        </div>
                        {h.changedByName && (
                          <div className="text-xs text-muted-foreground">
                            Người sửa: {h.changedByName}
                          </div>
                        )}
                      </div>
                    ))
                  ) : (
                    <p className="text-sm text-muted-foreground">Chưa có thay đổi giá</p>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Thẻ "Cho bán khi hết hàng" (Switch tắt cứng) đã được gỡ bỏ: không có cài đặt riêng
                theo sản phẩm ở Backend — hành vi bán âm kho do cài đặt chung allow_negative_stock
                quyết định (trang Cài đặt → Bán hàng & tiền tệ); UI chết gây hiểu lầm là có tính
                năng nhưng không bấm được (phát hiện khi rà soát). */}
          </div>
        </div>
      </form>
      <CategoryManagerDialog
        open={showCategoryManager}
        onOpenChange={setShowCategoryManager}
        canManage={canManageCategories}
      />
    </Form>
  );
}
