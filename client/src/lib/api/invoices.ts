import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface InvoiceLine {
  productName: string;
  unit: string;
  unitPrice: number;
  quantity: number;
  discountAmount: number;
  vatRate: number;
  vatAmount: number;
  lineTotal: number;
}

export interface VatBreakdown {
  vatRate: number;
  taxableAmount: number;
  vatAmount: number;
}

export interface InvoicePayment {
  method: string;
  amount: number;
}

export interface InvoiceDetail {
  invoiceNumber: string;
  issuedAt: string;
  lookupCode: string;
  qrPayload: string | null;
  orderNumber: string;
  orderStatus: string;
  storeName: string;
  storeTaxCode: string;
  branchName: string;
  branchAddress: string;
  branchPhone: string;
  cashierName: string;
  customerName: string | null;
  customerPhone: string | null;
  customerEmail: string | null;
  lines: InvoiceLine[];
  vatBreakdown: VatBreakdown[];
  payments: InvoicePayment[];
  subtotalAmount: number;
  discountAmount: number;
  vatAmount: number;
  roundingAdjustment: number;
  totalAmount: number;
  cashReceived: number | null;
  changeAmount: number | null;
}

export async function getInvoiceById(id: number | string): Promise<InvoiceDetail> {
  const response = await apiClient.get<ApiSuccess<InvoiceDetail>>(`/invoices/${id}`);
  return response.data.data;
}

export async function getInvoiceByLookupCode(lookupCode: string): Promise<InvoiceDetail> {
  const response = await apiClient.get<ApiSuccess<InvoiceDetail>>(`/invoices/lookup/${lookupCode}`);
  return response.data.data;
}
