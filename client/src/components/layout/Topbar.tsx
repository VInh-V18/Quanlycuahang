import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Bell, Check, ChevronDown, KeyRound, LogOut, Search } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { ChangePasswordDialog } from "@/components/layout/ChangePasswordDialog";
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
import { listMyBranches } from "@/lib/api/settings";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { clearCredentials } from "@/store/slices/authSlice";
import { setCurrentBranchId } from "@/store/slices/uiSlice";

export function Topbar() {
  const user = useAppSelector((state) => state.auth.user);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const currentBranchId = useCurrentBranchId();
  const [showChangePassword, setShowChangePassword] = useState(false);

  // /branches/mine (khong phai /branches) — tra dung chi nhanh USER NAY duoc phep chuyen, khong
  // doi hoi quyen branch:view (owner/manager thay toan chuoi, con lai chi thay chi nhanh duoc gan).
  const branchesQuery = useQuery({ queryKey: ["branches", "mine"], queryFn: listMyBranches });
  const branches = branchesQuery.data ?? [];

  useEffect(() => {
    if (branches.length === 0) return;
    if (!branches.some((b) => b.id === currentBranchId)) {
      dispatch(setCurrentBranchId(branches[0].id));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branches]);

  async function handleLogout() {
    try {
      await logoutApi();
    } finally {
      dispatch(clearCredentials());
      navigate("/login", { replace: true });
    }
  }

  const initials = user?.fullName?.slice(0, 2).toUpperCase() ?? "??";
  const currentBranch = branches.find((b) => b.id === currentBranchId);

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b bg-background px-4">
      {branches.length > 1 ? (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="gap-1.5 font-medium">
              {currentBranch?.name ?? "Chọn chi nhánh"}
              <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            <DropdownMenuLabel>Chọn chi nhánh</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {branches.map((b) => (
              <DropdownMenuItem
                key={b.id}
                className="gap-2"
                onClick={() => dispatch(setCurrentBranchId(b.id))}
              >
                <span className="flex-1">{b.name}</span>
                {b.id === currentBranchId && <Check className="h-4 w-4" />}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      ) : (
        currentBranch && (
          <span className="rounded-md border px-3 py-1.5 text-sm font-medium">
            {currentBranch.name}
          </span>
        )
      )}

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
            <DropdownMenuItem onClick={() => setShowChangePassword(true)}>
              <KeyRound className="mr-2 h-4 w-4" />
              Đổi mật khẩu
            </DropdownMenuItem>
            <DropdownMenuItem onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Đăng xuất
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {showChangePassword && (
        <ChangePasswordDialog onClose={() => setShowChangePassword(false)} />
      )}
    </header>
  );
}
