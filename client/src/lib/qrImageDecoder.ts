import jsQR from "jsqr";

/** Giải mã QR trong 1 file ảnh (upload từ máy) thành chuỗi payload thô — dùng canvas để đọc pixel
 * vì jsQR chỉ nhận ImageData, không tự đọc File/Blob. Trả về null nếu ảnh không chứa QR đọc được
 * (ảnh mờ, không phải mã QR...) — gọi nơi dùng phải tự xử lý trường hợp null, không phải lỗi. */
export async function decodeQrFromFile(file: File): Promise<string | null> {
  const imageUrl = URL.createObjectURL(file);
  try {
    const image = await loadImage(imageUrl);
    const canvas = document.createElement("canvas");
    canvas.width = image.naturalWidth;
    canvas.height = image.naturalHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return null;
    ctx.drawImage(image, 0, 0);
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const result = jsQR(imageData.data, imageData.width, imageData.height);
    return result?.data ?? null;
  } finally {
    URL.revokeObjectURL(imageUrl);
  }
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("Không đọc được ảnh"));
    image.src = src;
  });
}
