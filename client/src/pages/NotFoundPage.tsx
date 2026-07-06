import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function NotFoundPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 py-24 text-center">
      <h1 className="text-4xl font-bold">404</h1>
      <p className="text-muted-foreground">Không tìm thấy trang bạn yêu cầu.</p>
      <Button asChild>
        <Link to="/">Về Dashboard</Link>
      </Button>
    </div>
  );
}
