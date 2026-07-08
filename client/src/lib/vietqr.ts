/** Danh sách ngân hàng nội địa phổ biến kèm mã BIN Napas (thông tin công khai, dùng để sinh QR
 * VietQR/Napas 247) — https://api.vietqr.io/v2/banks liệt kê đầy đủ hơn nếu cần bổ sung. */
export interface VietQrBank {
  bin: string;
  name: string;
  shortName: string;
}

export const VIETQR_BANKS: VietQrBank[] = [
  { bin: "970436", name: "Vietcombank", shortName: "Vietcombank" },
  { bin: "970415", name: "VietinBank", shortName: "VietinBank" },
  { bin: "970418", name: "BIDV", shortName: "BIDV" },
  { bin: "970405", name: "Agribank", shortName: "Agribank" },
  { bin: "970407", name: "Techcombank", shortName: "Techcombank" },
  { bin: "970422", name: "MB Bank", shortName: "MBBank" },
  { bin: "970416", name: "ACB", shortName: "ACB" },
  { bin: "970432", name: "VPBank", shortName: "VPBank" },
  { bin: "970423", name: "TPBank", shortName: "TPBank" },
  { bin: "970403", name: "Sacombank", shortName: "Sacombank" },
  { bin: "970437", name: "HDBank", shortName: "HDBank" },
  { bin: "970443", name: "SHB", shortName: "SHB" },
  { bin: "970441", name: "VIB", shortName: "VIB" },
  { bin: "970426", name: "MSB", shortName: "MSB" },
  { bin: "970448", name: "OCB", shortName: "OCB" },
  { bin: "970440", name: "SeABank", shortName: "SeABank" },
  { bin: "970431", name: "Eximbank", shortName: "Eximbank" },
  { bin: "970429", name: "SCB", shortName: "SCB" },
  { bin: "970449", name: "LPBank", shortName: "LPBank" },
  { bin: "970454", name: "BVBank", shortName: "BVBank" },
];

function tlv(tag: string, value: string): string {
  const length = String(value.length).padStart(2, "0");
  return `${tag}${length}${value}`;
}

function truncate(value: string, maxLength: number): string {
  return value.length <= maxLength ? value : value.slice(0, maxLength);
}

/** CRC16-CCITT (False): polynomial 0x1021, init 0xFFFF, không reflect, không XOR out — cùng thuật
 * toán với VietQrService.java phía Backend (dùng để hiển thị preview phía Client, tự sinh độc lập
 * để không cần gọi round-trip Backend mỗi lần đổi số liệu xem trước). */
function crc16Ccitt(data: string): string {
  let crc = 0xffff;
  for (let i = 0; i < data.length; i++) {
    crc ^= (data.charCodeAt(i) & 0xff) << 8;
    for (let bit = 0; bit < 8; bit++) {
      if ((crc & 0x8000) !== 0) {
        crc = ((crc << 1) ^ 0x1021) & 0xffff;
      } else {
        crc = (crc << 1) & 0xffff;
      }
    }
  }
  return crc.toString(16).toUpperCase().padStart(4, "0");
}

const AID_NAPAS = "A000000727";
const SERVICE_CODE_TRANSFER_TO_ACCOUNT = "QRIBFTTA";

export function generateVietQrPayload(
  bankBin: string,
  accountNumber: string,
  merchantName: string,
  amount: number,
  purpose?: string,
): string {
  const merchantAccountInfo =
    tlv("00", AID_NAPAS) +
    tlv("01", tlv("00", bankBin) + tlv("01", accountNumber)) +
    tlv("02", SERVICE_CODE_TRANSFER_TO_ACCOUNT);

  let payload =
    tlv("00", "01") + // Payload Format Indicator
    tlv("01", "12") + // Point of Initiation Method: 12 = động (có số tiền)
    tlv("38", merchantAccountInfo) +
    tlv("52", "0000") + // Merchant Category Code
    tlv("53", "704") + // Currency: VND = 704
    tlv("54", String(Math.round(amount))) +
    tlv("58", "VN") +
    tlv("59", truncate(merchantName || "CUA HANG", 25)) +
    tlv("60", truncate("HA NOI", 15));

  if (purpose) {
    payload += tlv("62", tlv("08", truncate(purpose, 25)));
  }

  const payloadWithCrcTag = `${payload}6304`;
  return payloadWithCrcTag + crc16Ccitt(payloadWithCrcTag);
}

/** Bóc 1 tầng TLV (tag 2 ký tự, length 2 ký tự thập phân, rồi value) — dùng để đọc ngược payload
 * VietQR đọc được từ ảnh QR upload lên, đối xứng với hàm `tlv()` ở trên dùng để sinh payload. */
function parseTlv(data: string): Record<string, string> {
  const result: Record<string, string> = {};
  let i = 0;
  while (i + 4 <= data.length) {
    const tag = data.slice(i, i + 2);
    const length = Number(data.slice(i + 2, i + 4));
    if (!Number.isInteger(length) || length < 0) break;
    result[tag] = data.slice(i + 4, i + 4 + length);
    i += 4 + length;
  }
  return result;
}

export interface ParsedVietQr {
  bankBin: string | null;
  accountNumber: string | null;
  merchantName: string | null;
}

/** Đọc payload EMVCo/VietQR (giải mã được từ ảnh QR chuyển khoản) ra bin ngân hàng, số tài khoản,
 * tên chủ tài khoản — để tự điền Cài đặt thay vì bắt nhập tay lại thông tin đã có sẵn trong ảnh. */
export function parseVietQrPayload(payload: string): ParsedVietQr {
  const top = parseTlv(payload);
  const merchantName = top["59"]?.trim() || null;

  const merchantAccountInfo = top["38"];
  if (!merchantAccountInfo) {
    return { bankBin: null, accountNumber: null, merchantName };
  }
  const beneficiaryInfo = parseTlv(merchantAccountInfo)["01"];
  if (!beneficiaryInfo) {
    return { bankBin: null, accountNumber: null, merchantName };
  }
  const beneficiary = parseTlv(beneficiaryInfo);
  return {
    bankBin: beneficiary["00"]?.trim() || null,
    accountNumber: beneficiary["01"]?.trim() || null,
    merchantName,
  };
}
