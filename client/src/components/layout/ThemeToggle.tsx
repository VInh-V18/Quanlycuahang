import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { setTheme } from "@/store/slices/uiSlice";

export function ThemeToggle() {
  const theme = useAppSelector((state) => state.ui.theme);
  const dispatch = useAppDispatch();

  return (
    <Button
      variant="ghost"
      size="icon"
      aria-label="Đổi giao diện sáng/tối"
      onClick={() => dispatch(setTheme(theme === "light" ? "dark" : "light"))}
    >
      {theme === "light" ? <Moon className="h-5 w-5" /> : <Sun className="h-5 w-5" />}
    </Button>
  );
}
