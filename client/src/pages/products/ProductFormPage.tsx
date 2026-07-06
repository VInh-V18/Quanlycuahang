import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Controller, useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormControl, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { FormField } from "@/components/common/FormField";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Money } from "@/components/common/Money";
import { useToast } from "@/components/ui/use-toast";
import { listCategories } from "@/lib/api/categories";
import {
  createProduct,
  getPriceHistory,
  getProduct,
  ORIGIN_COUNTRIES,
  updateProduct,
} from "@/lib/api/products";
import { CURRENT_BRANCH_ID } from "@/lib/constants";
import { apiClient } from "@/lib/http/apiClient";
import { getApiErrorMessage } from "@/lib/http/errors";

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
  imageUrl: z.string().optional(),
});

type ProductFormValues = z.infer<typeof productSchema>;

const numberFormatter = new Intl.NumberFormat("vi-VN");

export function ProductFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [uploading, setUploading] = useState(false);

  const productQuery = useQuery({
    queryKey: ["products", id],
    queryFn: () => getProduct(Number(id), CURRENT_BRANCH_ID),
    enabled: isEdit,
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
      imageUrl: "",
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
      imageUrl: product.imageUrl ?? "",
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
        imageUrl: values.imageUrl || null,
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

  async function handleImageChange(file: File) {
    setUploading(true);
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await apiClient.post<{ data: { url: string } }>("/uploads", body);
      form.setValue("imageUrl", response.data.data.url);
    } catch (err) {
      toast({ variant: "destructive", title: "Tải ảnh thất bại", description: getApiErrorMessage(err) });
    } finally {
      setUploading(false);
    }
  }

  const imageUrl = form.watch("imageUrl");

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
                        <FormLabel>
                          Danh mục<span className="text-destructive"> *</span>
                        </FormLabel>
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
                  <div className="mt-2 flex h-10 items-center rounded-md border bg-muted px-3 text-sm">
                    {productQuery.data?.costPrice != null ? (
                      <Money value={productQuery.data.costPrice} />
                    ) : (
                      "—"
                    )}
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Bình quân gia quyền — chỉ đổi khi nhập hàng
                  </p>
                </div>
                <FormField
                  control={form.control}
                  name="sellPrice"
                  label="Giá bán / đơn vị"
                  type="number"
                  required
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
                <FormField
                  control={form.control}
                  name="minStock"
                  label="Tồn tối thiểu"
                  type="number"
                  required
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
            <Card>
              <CardHeader>
                <CardTitle>Ảnh sản phẩm</CardTitle>
              </CardHeader>
              <CardContent>
                <label className="flex aspect-square cursor-pointer flex-col items-center justify-center gap-2 rounded-md border border-dashed text-center text-sm text-muted-foreground hover:bg-accent">
                  {imageUrl ? (
                    <img src={imageUrl} alt="" className="h-full w-full rounded-md object-cover" />
                  ) : (
                    <>
                      <span>{uploading ? "Đang tải..." : "Kéo thả hoặc bấm để tải ảnh"}</span>
                      <span className="text-xs">JPG/PNG/WebP · tối đa 2MB</span>
                    </>
                  )}
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={(e) => e.target.files?.[0] && handleImageChange(e.target.files[0])}
                  />
                </label>
              </CardContent>
            </Card>

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
                          {new Date(h.createdAt).toLocaleDateString("vi-VN")}
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

            <Card>
              <CardHeader>
                <CardTitle>Cho bán khi hết hàng</CardTitle>
              </CardHeader>
              <CardContent className="flex items-center gap-2">
                <Switch disabled checked={false} />
                <span className="text-sm text-muted-foreground">Theo cài đặt chung: Không</span>
              </CardContent>
            </Card>
          </div>
        </div>
      </form>
    </Form>
  );
}
