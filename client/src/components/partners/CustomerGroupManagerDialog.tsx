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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import {
  createCustomerGroup,
  deleteCustomerGroup,
  listCustomerGroups,
  updateCustomerGroup,
  type CustomerGroup,
} from "@/lib/api/customerGroups";
import { getApiErrorMessage } from "@/lib/http/errors";

export interface CustomerGroupManagerDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  canManage: boolean;
}

/** Quan ly nhom khach hang (VIP/Than thiet/Doanh nghiep...) — mo tu trang Khach hang, dung de loc
 * + gan cho khach hang. */
export function CustomerGroupManagerDialog({
  open,
  onOpenChange,
  canManage,
}: CustomerGroupManagerDialogProps) {
  const [adding, setAdding] = useState(false);
  const queryClient = useQueryClient();

  const groupsQuery = useQuery({
    queryKey: ["customer-groups"],
    queryFn: listCustomerGroups,
    enabled: open,
  });

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["customer-groups"] });
  }

  const groups = groupsQuery.data ?? [];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Quản lý nhóm khách hàng</DialogTitle>
          <DialogDescription>Dùng để lọc và gán cho khách hàng (VD: VIP, Thân thiết, Doanh nghiệp)</DialogDescription>
        </DialogHeader>

        <div className="max-h-[60vh] overflow-y-auto rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tên nhóm</TableHead>
                {canManage && <TableHead className="w-20" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {adding && (
                <GroupRow group={null} canManage={canManage} onClose={() => setAdding(false)} onSaved={invalidate} />
              )}
              {groups.map((group) => (
                <GroupRow key={group.id} group={group} canManage={canManage} onSaved={invalidate} />
              ))}
              {groups.length === 0 && !adding && (
                <TableRow>
                  <TableCell colSpan={canManage ? 2 : 1} className="py-6 text-center text-muted-foreground">
                    {groupsQuery.isLoading ? "Đang tải..." : "Chưa có nhóm khách hàng nào"}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>

        {canManage && !adding && (
          <Button size="sm" variant="outline" onClick={() => setAdding(true)}>
            <Plus className="h-4 w-4" />
            Thêm nhóm
          </Button>
        )}
      </DialogContent>
    </Dialog>
  );
}

function GroupRow({
  group,
  canManage,
  onClose,
  onSaved,
}: {
  group: CustomerGroup | null;
  canManage: boolean;
  onClose?: () => void;
  onSaved: () => void;
}) {
  const isNew = group === null;
  const [editing, setEditing] = useState(isNew);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [name, setName] = useState(group?.name ?? "");
  const { toast } = useToast();

  const saveMutation = useMutation({
    mutationFn: () =>
      isNew ? createCustomerGroup({ name }) : updateCustomerGroup(group!.id, { name }),
    onSuccess: () => {
      toast({ title: isNew ? "Đã thêm nhóm" : "Đã lưu nhóm" });
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
    mutationFn: () => deleteCustomerGroup(group!.id),
    onSuccess: () => {
      toast({ title: "Đã xóa nhóm" });
      onSaved();
      setConfirmingDelete(false);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể xóa", description: getApiErrorMessage(err) });
      setConfirmingDelete(false);
    },
  });

  if (!editing && group) {
    return (
      <TableRow>
        <TableCell className="font-medium">{group.name}</TableCell>
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
              title={`Xóa nhóm "${group.name}"?`}
              description="Chỉ xóa được khi không còn khách hàng nào thuộc nhóm này."
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
        <Input value={name} onChange={(e) => setName(e.target.value)} className="h-8" placeholder="Tên nhóm" />
      </TableCell>
      <TableCell>
        <div className="flex justify-end gap-1">
          <Button size="sm" disabled={!name.trim() || saveMutation.isPending} onClick={() => saveMutation.mutate()}>
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
