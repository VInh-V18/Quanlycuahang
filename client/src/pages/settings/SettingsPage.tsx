import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAppSelector } from "@/store/hooks";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/use-toast";
import { getSettings, listBranches, updateSettings } from "@/lib/api/settings";
import { apiClient } from "@/lib/http/apiClient";
import { getApiErrorMessage } from "@/lib/http/errors";
import { decodeQrFromFile } from "@/lib/qrImageDecoder";
import { parseVietQrPayload, VIETQR_BANKS } from "@/lib/vietqr";
import { InvoiceSettingsTab } from "@/pages/settings/InvoiceSettingsTab";
import { SalesSettingsTab } from "@/pages/settings/SalesSettingsTab";
import { SecuritySettingsTab } from "@/pages/settings/SecuritySettingsTab";
import { StoreSettingsTab } from "@/pages/settings/StoreSettingsTab";
import type { FormState } from "@/pages/settings/settingsTabProps";

function useSettingsForm() {
  const query = useQuery({ queryKey: ["settings"], queryFn: getSettings });
  const [form, setForm] = useState<FormState>({});
  const initialized = useRef(false);

  useEffect(() => {
    // Chi ap du lieu server 1 LAN DAU (useRef, khong phai moi lan query.data doi) - trang nay co 4
    // nut "Luu" rieng theo tung tab, moi nut chi gui 1 tap con key; luu xong invalidateQueries lam
    // query nay fetch lai, va neu useEffect ap lai TOAN BO query.data.settings vao form moi lan nhu
    // truoc day, no se GHI DE mat het chinh sua chua luu o CAC TAB KHAC (phat hien khi rieng soat).
    // Sau lan dau, form la du lieu nhap dang cua nguoi dung, khong con dong bo lai tu server nua.
    if (query.data && !initialized.current) {
      setForm(query.data.settings);
      initialized.current = true;
    }
  }, [query.data]);

  return { query, form, setForm };
}

/** Tach 4 tab thanh component rieng (SalesSettingsTab/InvoiceSettingsTab/StoreSettingsTab/
 * SecuritySettingsTab, phat hien qua audit production readiness 2026-07-17) — component nay chi
 * con giu state/mutation dung chung (form/set/saveMutation qua useSettingsForm()) va dieu phoi tab
 * dang chon, moi tab con lai la component hien thi thuan tuy nhan props xuong. */
export function SettingsPage() {
  const [tab, setTab] = useState<"sales" | "invoice" | "store" | "security">("sales");
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canEdit = permissions.includes("settings:update");
  const canManageBranch = permissions.includes("branch:manage");
  const [uploadingQr, setUploadingQr] = useState(false);

  const { query: settingsQuery, form, setForm } = useSettingsForm();

  const branchesQuery = useQuery({
    queryKey: ["branches"],
    queryFn: listBranches,
    enabled: tab === "store",
  });

  const saveMutation = useMutation({
    mutationFn: (keys: string[]) => {
      const payload: FormState = {};
      keys.forEach((k) => {
        payload[k] = form[k] ?? "";
      });
      return updateSettings(payload);
    },
    onSuccess: () => {
      toast({ title: "Đã lưu cài đặt" });
      queryClient.invalidateQueries({ queryKey: ["settings"] });
      queryClient.invalidateQueries({ queryKey: ["branding"] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  function set(key: string, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleQrUpload(file: File) {
    setUploadingQr(true);
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await apiClient.post<{ data: { url: string } }>("/uploads", body);
      set("bank_qr_image_url", response.data.data.url);
    } catch (err) {
      toast({ variant: "destructive", title: "Tải ảnh thất bại", description: getApiErrorMessage(err) });
      setUploadingQr(false);
      return;
    }

    // Doc thu QR trong anh vua upload de tu dien so TK/ngan hang/chu TK — best-effort, khong chan
    // luong upload: anh mo hoac khong phai VietQR thi bo qua, nguoi dung van tu nhap tay duoc.
    try {
      const payload = await decodeQrFromFile(file);
      if (payload) {
        const parsed = parseVietQrPayload(payload);
        if (parsed.bankBin || parsed.accountNumber || parsed.merchantName) {
          if (parsed.bankBin) {
            set("bank_bin", parsed.bankBin);
            const bank = VIETQR_BANKS.find((b) => b.bin === parsed.bankBin);
            if (bank) set("bank_name", bank.shortName);
          }
          if (parsed.accountNumber) set("bank_account_number", parsed.accountNumber);
          if (parsed.merchantName) set("bank_account_name", parsed.merchantName);
          toast({ title: "Đã tự động điền thông tin ngân hàng từ ảnh QR" });
        }
      }
    } catch {
      // Khong doc duoc QR trong anh — im lang, khong phai loi can bao.
    } finally {
      setUploadingQr(false);
    }
  }

  const loading = settingsQuery.isLoading;
  const tabProps = { form, set, canEdit, loading, saving: saveMutation.isPending, onSave: saveMutation.mutate };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">Cài đặt</h1>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as typeof tab)}>
        <TabsList>
          <TabsTrigger value="sales">Bán hàng & tiền tệ</TabsTrigger>
          <TabsTrigger value="invoice">Hóa đơn & thanh toán</TabsTrigger>
          <TabsTrigger value="store">Cửa hàng & chi nhánh</TabsTrigger>
          <TabsTrigger value="security">Bảo mật & hệ thống</TabsTrigger>
        </TabsList>
      </Tabs>

      {tab === "sales" && <SalesSettingsTab {...tabProps} />}
      {tab === "invoice" && <InvoiceSettingsTab {...tabProps} />}
      {tab === "store" && (
        <StoreSettingsTab
          {...tabProps}
          canManageBranch={canManageBranch}
          branches={branchesQuery.data}
          uploadingQr={uploadingQr}
          onQrUpload={handleQrUpload}
        />
      )}
      {tab === "security" && (
        <SecuritySettingsTab {...tabProps} systemInfo={settingsQuery.data?.systemInfo} />
      )}
    </div>
  );
}
