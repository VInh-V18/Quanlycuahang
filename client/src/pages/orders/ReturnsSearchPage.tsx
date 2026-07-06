import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/use-toast";
import { listOrders } from "@/lib/api/orders";
import { CURRENT_BRANCH_ID } from "@/lib/constants";
import { getApiErrorMessage } from "@/lib/http/errors";

/** Trang tim don de tao phieu tra hang (nav "Trả hàng") — hau het truong hop thuc te se vao thang
 * tu "..." o Đơn hàng, trang nay phuc vu nhu 1 loi vao truc tiep khi da biet ma don/SDT. */
export function ReturnsSearchPage() {
  const [query, setQuery] = useState("");
  const navigate = useNavigate();
  const { toast } = useToast();

  const searchMutation = useMutation({
    mutationFn: () => listOrders({ branchId: CURRENT_BRANCH_ID, search: query, size: 1 }),
    onSuccess: (result) => {
      const order = result.data[0];
      if (!order) {
        toast({ variant: "destructive", title: "Không tìm thấy đơn hàng phù hợp" });
        return;
      }
      navigate(`/orders/${order.id}/return`);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Lỗi tìm kiếm", description: getApiErrorMessage(err) });
    },
  });

  return (
    <div className="mx-auto max-w-lg space-y-4 pt-12">
      <Card>
        <CardHeader>
          <CardTitle>Trả hàng</CardTitle>
          <CardDescription>Nhập mã đơn hoặc SĐT khách hàng để tạo phiếu trả hàng</CardDescription>
        </CardHeader>
        <CardContent className="flex gap-2">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && query.trim() && searchMutation.mutate()}
              placeholder="VD: HD002451 hoặc 0903456789"
              className="pl-8"
            />
          </div>
          <Button disabled={!query.trim() || searchMutation.isPending} onClick={() => searchMutation.mutate()}>
            Tìm đơn
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
