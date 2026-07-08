const CATEGORY_EMOJI: Record<string, string> = {
  "Do uong": "🥤",
  "Banh keo": "🍬",
  "Gia vi & Thuc pham kho": "🌾",
  "Hoa my pham": "🧴",
  "Do gia dung": "🧺",
};

export function categoryEmoji(categoryName: string | null | undefined): string {
  if (!categoryName) return "📦";
  return CATEGORY_EMOJI[categoryName] ?? "📦";
}
