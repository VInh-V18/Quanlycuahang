import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test("thu ngân bán 1 sản phẩm ghi nợ khách hàng và nhận thông báo tạo đơn thành công", async ({
  page,
}) => {
  const accessToken = await login(page);
  const headers = { Authorization: `Bearer ${accessToken}` };

  // Tu tao san pham/khach hang CO SAN rieng cho test nay qua API, khong con doan vao du lieu demo
  // co san ("Ca phe hoa tan G7 hop 20 goi") - V12__remove_demo_seed_data.sql xoa toan bo san pham/
  // NCC/khach hang demo tren MOI lan cai dat (chay tren ca database moi tinh), nen test cu tim
  // dung ten do LUON THAT BAI tren moi moi truong hien tai (phat hien khi rieng soat). Ten random
  // hoa theo timestamp de khong trung khi chay lai/chay song song.
  const suffix = Date.now();
  const productName = `E2E test coffee ${suffix}`;
  const customerName = `E2E test customer ${suffix}`;

  const branchesRes = await page.request.get("/api/v1/branches", { headers });
  const [branch] = (await branchesRes.json()).data as Array<{ id: number }>;

  const productRes = await page.request.post("/api/v1/products", {
    headers,
    data: {
      name: productName,
      unit: "hộp",
      sellPrice: 55000,
      priceIncludesVat: true,
      vatRate: 0,
      minStock: 0,
    },
  });
  const product = (await productRes.json()).data as { id: number };

  const supplierRes = await page.request.post("/api/v1/suppliers", {
    headers,
    data: { name: `E2E test supplier ${suffix}` },
  });
  const supplier = (await supplierRes.json()).data as { id: number };

  // Nhap kho qua don mua hang - tao san pham xong ton kho van la 0, chua ban duoc (phai co it nhat
  // 1 phieu nhap/kiem ke de co ton, giong nghiep vu that).
  await page.request.post("/api/v1/purchase-orders", {
    headers,
    data: {
      supplierId: supplier.id,
      branchId: branch.id,
      items: [{ productId: product.id, quantity: 10, unitPrice: 30000 }],
      paidAmount: 300000,
    },
  });

  // POS khong con thu tien tai quay (bo tien mat/CK) — moi don deu ghi thanh cong no phai thu cua
  // khach hang (xem comment "Khong con thu tien tai POS" trong PosPage.tsx), nen bat buoc phai co
  // khach hang truoc khi checkout, khac voi luong "Thanh toán & In" + dien "Khách đưa" cua test cu.
  await page.request.post("/api/v1/customers", {
    headers,
    data: { name: customerName },
  });

  await page.goto("/pos");

  await page.getByText(productName).first().click();

  await page.getByPlaceholder(/Chọn khách hàng/i).fill(customerName);
  await page.getByRole("button", { name: new RegExp(customerName) }).click();

  await page.getByRole("button", { name: /In hóa đơn/i }).click();

  // Toast Radix render 2 node cung text (div hien thi + span aria-live cho screen reader).
  await expect(page.getByText(/Đã tạo đơn/i).first()).toBeVisible();
});
