import { useNavigate } from "react-router-dom";
import { Bell, ChevronDown, LogOut, Search, User as UserIcon } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { logout as logoutApi } from "@/lib/api/auth";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { clearCredentials } from "@/store/slices/authSlice";

export function Topbar() {
  const user = useAppSelector((state) => state.auth.user);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await logoutApi();
    } finally {
      dispatch(clearCredentials());
      navigate("/login", { replace: true });
    }
  }

  const initials = user?.fullName?.slice(0, 2).toUpperCase() ?? "??";

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b bg-background px-4">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm" className="gap-1.5 font-medium">
            CN Quận 1
            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          <DropdownMenuLabel>Chọn chi nhánh</DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem>CN Quận 1</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <div className="relative ml-1 max-w-md flex-1">
        <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input placeholder="Tìm nhanh... (Ctrl+K)" className="pl-8" />
      </div>

      <div className="ml-auto flex items-center gap-1">
        <Button variant="ghost" size="icon" className="relative" aria-label="Thông báo">
          <Bell className="h-5 w-5" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-destructive" />
        </Button>
        <ThemeToggle />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2 px-2">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-primary text-primary-foreground">{initials}</AvatarFallback>
              </Avatar>
              <span className="hidden flex-col items-start text-left sm:flex">
                <span className="text-sm font-medium leading-tight">{user?.fullName}</span>
                <span className="text-xs leading-tight text-muted-foreground">@{user?.username}</span>
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>{user?.username}</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <UserIcon className="mr-2 h-4 w-4" />
              Tài khoản
            </DropdownMenuItem>
            <DropdownMenuItem onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Đăng xuất
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
