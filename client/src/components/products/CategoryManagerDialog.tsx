import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
  type Category,
} from "@/lib/api/categories";
import { getApiErrorMessage } from "@/lib/http/errors";

const NO_PARENT = "none";

export interface CategoryManagerDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  canManage: boolean;
}

/** Quan ly cay danh muc 2 cap (them/sua/xoa) — mo tu trang San pham, cung 1 danh sach categories
 * dung de loc/gan cho san pham. */
export function CategoryManagerDialog({ open, onOpenChange, canManage }: CategoryManagerDialogProps) {
  const [addingRoot, setAddingRoot] = useState(false);
  const queryClient = useQueryClient();

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: listCategories,
    enabled: open,
  });

  const categories = categoriesQuery.data ?? [];
  const topLevel = categories.filter((c) => c.parentId === null);
  const parentName = (parentId: number | null) =>
    parentId === null ? null : categories.find((c) => c.id === parentId)?.name;

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["categories"] });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Quản lý danh mục</DialogTitle>
          <DialogDescription>Cây danh mục tối đa 2 cấp — dùng để lọc và gán cho sản phẩm</DialogDescription>
        </DialogHeader>

        <div className="max-h-[60vh] overflow-y-auto rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tên danh mục</TableHead>
                <TableHead>Danh mục cha</TableHead>
                <TableHead className="w-20 text-right">Thứ tự</TableHead>
                {canManage && <TableHead className="w-20" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {addingRoot && (
                <CategoryRow
                  category={null}
                  topLevel={topLevel}
                  canManage={canManage}
                  onClose={() => setAddingRoot(false)}
                  onSaved={invalidate}
                />
              )}
              {categories.map((category) => (
                <CategoryRow
                  key={category.id}
                  category={category}
                  topLevel={topLevel.filter((c) => c.id !== category.id)}
                  parentName={parentName(category.parentId) ?? null}
                  canManage={canManage}
                  onSaved={invalidate}
                />
              ))}
              {categories.length === 0 && !addingRoot && (
                <TableRow>
                  <TableCell colSpan={canManage ? 4 : 3} className="py-6 text-center text-muted-foreground">
                    {categoriesQuery.isLoading ? "Đang tải..." : "Chưa có danh mục nào"}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>

        {canManage && !addingRoot && (
          <Button size="sm" variant="outline" onClick={() => setAddingRoot(true)}>
            <Plus className="h-4 w-4" />
            Thêm danh mục
          </Button>
        )}
      </DialogContent>
    </Dialog>
  );
}

function CategoryRow({
  category,
  topLevel,
  parentName,
  canManage,
  onClose,
  onSaved,
}: {
  category: Category | null;
  topLevel: Category[];
  parentName?: string | null;
  canManage: boolean;
  onClose?: () => void;
  onSaved: () => void;
}) {
  const isNew = category === null;
  const [editing, setEditing] = useState(isNew);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [name, setName] = useState(category?.name ?? "");
  const [parentId, setParentId] = useState<string>(
    category?.parentId != null ? String(category.parentId) : NO_PARENT,
  );
  const [displayOrder, setDisplayOrder] = useState(String(category?.displayOrder ?? 0));
  const { toast } = useToast();

  const saveMutation = useMutation({
    mutationFn: () => {
      const request = {
        name,
        parentId: parentId === NO_PARENT ? null : Number(parentId),
        displayOrder: Number(displayOrder) || 0,
      };
      return isNew ? createCategory(request) : updateCategory(category!.id, request);
    },
    onSuccess: () => {
      toast({ title: isNew ? "Đã thêm danh mục" : "Đã lưu danh mục" });
      onSaved();
      if (isNew) {
        setName("");
        onClose?.();
      } else {
        setEditing(false);
      }
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteCategory(category!.id),
    onSuccess: () => {
      toast({ title: "Đã xóa danh mục" });
      onSaved();
      setConfirmingDelete(false);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể xóa", description: getApiErrorMessage(err) });
      setConfirmingDelete(false);
    },
  });

  if (!editing && category) {
    return (
      <TableRow>
        <TableCell className={category.parentId != null ? "pl-6 font-normal" : "font-medium"}>
          {category.name}
        </TableCell>
        <TableCell className="text-muted-foreground">{parentName ?? "—"}</TableCell>
        <TableCell className="text-right">{category.displayOrder}</TableCell>
        {canManage && (
          <TableCell>
            <div className="flex justify-end gap-1">
              <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
                Sửa
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-destructive hover:text-destructive"
                onClick={() => setConfirmingDelete(true)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
            <ConfirmDialog
              open={confirmingDelete}
              onOpenChange={setConfirmingDelete}
              title={`Xóa danh mục "${category.name}"?`}
              description="Chỉ xóa được khi danh mục không còn danh mục con hoặc sản phẩm nào."
              variant="destructive"
              loading={deleteMutation.isPending}
              onConfirm={() => deleteMutation.mutate()}
            />
          </TableCell>
        )}
      </TableRow>
    );
  }

  return (
    <TableRow>
      <TableCell>
        <Input value={name} onChange={(e) => setName(e.target.value)} className="h-8" placeholder="Tên danh mục" />
      </TableCell>
      <TableCell>
        <Select value={parentId} onValueChange={setParentId}>
          <SelectTrigger className="h-8">
            <SelectValue placeholder="Không có (danh mục gốc)" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NO_PARENT}>Không có (danh mục gốc)</SelectItem>
            {topLevel.map((c) => (
              <SelectItem key={c.id} value={String(c.id)}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </TableCell>
      <TableCell>
        <Input
          type="number"
          value={displayOrder}
          onChange={(e) => setDisplayOrder(e.target.value)}
          className="h-8 text-right"
        />
      </TableCell>
      <TableCell>
        <div className="flex justify-end gap-1">
          <Button
            size="sm"
            disabled={!name.trim() || saveMutation.isPending}
            onClick={() => saveMutation.mutate()}
          >
            Lưu
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={() => (isNew ? onClose?.() : setEditing(false))}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
