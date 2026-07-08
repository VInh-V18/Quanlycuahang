/** Chèn ngày giờ hiện tại vào tên file (trước phần đuôi mở rộng) — để mỗi lần xuất Excel/tải file
 * ra tên khác nhau, không đè lên file tải trước đó cùng tên trong ngày. */
export function timestampedFileName(baseName: string): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  const stamp = `${pad(now.getDate())}-${pad(now.getMonth() + 1)}-${now.getFullYear()}_${pad(now.getHours())}${pad(now.getMinutes())}`;
  const dotIndex = baseName.lastIndexOf(".");
  if (dotIndex === -1) return `${baseName}_${stamp}`;
  return `${baseName.slice(0, dotIndex)}_${stamp}${baseName.slice(dotIndex)}`;
}

/** Kích hoạt tải file từ Blob đã tải qua Axios (endpoint export cần header Authorization nên
 * không dùng <a href> trực tiếp được) — dùng chung cho mọi nơi xuất Excel/tải file trong app. */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
