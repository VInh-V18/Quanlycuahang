/** Bỏ dấu tiếng Việt + thường hóa để so khớp tên danh mục — khóa tra cứu trước đây viết KHÔNG dấu
 * ("Do uong") trong khi tên danh mục thật có dấu đầy đủ ("Đồ uống") nên không bao giờ khớp, mọi
 * danh mục đều rơi về 📦 (phát hiện khi rà soát). Chuẩn hóa cả 2 phía để tên do người dùng tự đặt
 * ("đồ uống", "Đồ Uống"...) vẫn khớp được. */
function normalize(name: string): string {
  return name
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .trim();
}

const CATEGORY_EMOJI: Record<string, string> = {
  "do uong": "🥤",
  "banh keo": "🍬",
  "gia vi & thuc pham kho": "🌾",
  "hoa my pham": "🧴",
  "do gia dung": "🧺",
  "rau cu": "🥬",
  "trai cay": "🍎",
};

export function categoryEmoji(categoryName: string | null | undefined): string {
  if (!categoryName) return "📦";
  return CATEGORY_EMOJI[normalize(categoryName)] ?? "📦";
}
