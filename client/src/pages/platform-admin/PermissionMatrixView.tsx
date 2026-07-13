import { Fragment, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
  listPlatformAdminPermissions,
  listPlatformAdminRoles,
  updatePlatformAdminRolePermissions,
} from "@/lib/api/platformAdmin";

/**
 * Ma trận phân quyền TOÀN CỤC (vai trò × quyền) — nơi SỬA duy nhất trên hệ thống; phía cửa hàng
 * (EmployeesPage) chỉ còn xem. Thay đổi ở đây áp dụng NGAY cho mọi cửa hàng vì Role/Permission là
 * dữ liệu dùng chung. Lưu bằng Promise.allSettled: vai trò nào lưu lỗi vẫn giữ nguyên chỉnh sửa
 * để lưu lại, vai trò đã lưu thành công không bị coi là "chưa lưu" — tránh đúng lỗi mất thay đổi
 * hàng loạt của bản sửa ma trận cũ phía tenant (đã gỡ bỏ).
 */
export function PermissionMatrixView() {
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const rolesQuery = useQuery({
    queryKey: ["platform-admin-roles"],
    queryFn: listPlatformAdminRoles,
  });
  const permissionsQuery = useQuery({
    queryKey: ["platform-admin-permissions"],
    queryFn: listPlatformAdminPermissions,
  });

  // Bản chỉnh sửa tay theo roleId — chỉ chứa vai trò người dùng đã đụng vào; giá trị hiển thị ưu
  // tiên bản sửa, không có thì lấy từ server. Không cần useEffect đồng bộ lại sau refetch.
  const [edits, setEdits] = useState<Record<number, Set<string>>>({});

  const serverSets = useMemo(() => {
    const map: Record<number, Set<string>> = {};
    (rolesQuery.data ?? []).forEach((role) => {
      map[role.id] = new Set(role.permissionCodes);
    });
    return map;
  }, [rolesQuery.data]);

  const dirtyRoleIds = useMemo(
    () =>
      Object.keys(edits)
        .map(Number)
        .filter((roleId) => {
          const edited = edits[roleId];
          const server = serverSets[roleId];
          if (!server) return false;
          if (edited.size !== server.size) return true;
          for (const code of edited) if (!server.has(code)) return true;
          return false;
        }),
    [edits, serverSets],
  );

  function cellChecked(roleId: number, code: string): boolean {
    return (edits[roleId] ?? serverSets[roleId])?.has(code) ?? false;
  }

  function toggleCell(roleId: number, code: string) {
    setEdits((prev) => {
      const base = new Set(prev[roleId] ?? serverSets[roleId] ?? []);
      if (base.has(code)) {
        base.delete(code);
      } else {
        base.add(code);
      }
      return { ...prev, [roleId]: base };
    });
  }

  const saveMutation = useMutation({
    mutationFn: async (roleIds: number[]) => {
      const results = await Promise.allSettled(
        roleIds.map((roleId) =>
          updatePlatformAdminRolePermissions(roleId, Array.from(edits[roleId] ?? [])).then(
            () => roleId,
          ),
        ),
      );
      return { roleIds, results };
    },
    onSuccess: ({ roleIds, results }) => {
      const savedIds = results
        .filter((r): r is PromiseFulfilledResult<number> => r.status === "fulfilled")
        .map((r) => r.value);
      const failedIds = roleIds.filter((id) => !savedIds.includes(id));
      setEdits((prev) => {
        const next = { ...prev };
        savedIds.forEach((id) => delete next[id]);
        return next;
      });
      queryClient.invalidateQueries({ queryKey: ["platform-admin-roles"] });
      if (failedIds.length === 0) {
        toast({ title: "Đã lưu ma trận phân quyền", description: `${savedIds.length} vai trò` });
      } else {
        const names = (rolesQuery.data ?? [])
          .filter((r) => failedIds.includes(r.id))
          .map((r) => r.displayName)
          .join(", ");
        toast({
          variant: "destructive",
          title: "Một số vai trò chưa lưu được",
          description: `Chỉnh sửa của: ${names} vẫn được giữ — bấm Lưu để thử lại`,
        });
      }
    },
  });

  const rowGroups = useMemo(() => {
    const map = new Map<string, { code: string; description: string }[]>();
    (permissionsQuery.data ?? []).forEach((p) => {
      const resource = p.code.split(":")[0];
      map.set(resource, [...(map.get(resource) ?? []), p]);
    });
    return Array.from(map.entries());
  }, [permissionsQuery.data]);

  const roles = rolesQuery.data ?? [];

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <div>
          <CardTitle>Ma trận phân quyền</CardTitle>
          <p className="mt-1 text-sm text-destructive">
            Thay đổi áp dụng cho TẤT CẢ cửa hàng trên hệ thống ngay khi lưu.
          </p>
        </div>
        {dirtyRoleIds.length > 0 && (
          <Button
            disabled={saveMutation.isPending}
            onClick={() => saveMutation.mutate(dirtyRoleIds)}
          >
            {saveMutation.isPending ? "Đang lưu..." : `Lưu thay đổi (${dirtyRoleIds.length})`}
          </Button>
        )}
      </CardHeader>
      <CardContent className="max-h-[65svh] overflow-auto p-0">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="sticky left-0 top-0 z-30 bg-background">Quyền</TableHead>
              {roles.map((role) => (
                <TableHead key={role.id} className="sticky top-0 z-20 bg-background text-center">
                  {role.displayName}
                  {dirtyRoleIds.includes(role.id) && <span className="text-destructive"> *</span>}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {(rolesQuery.isLoading || permissionsQuery.isLoading) && (
              <TableRow>
                <TableCell colSpan={roles.length + 1} className="py-8 text-center text-muted-foreground">
                  Đang tải...
                </TableCell>
              </TableRow>
            )}
            {rowGroups.map(([resource, permissions]) => (
              <Fragment key={resource}>
                <TableRow className="bg-muted/50 hover:bg-muted/50">
                  <TableCell
                    colSpan={roles.length + 1}
                    className="py-2 text-xs font-semibold uppercase text-muted-foreground"
                  >
                    {resource}
                  </TableCell>
                </TableRow>
                {permissions.map((perm) => (
                  <TableRow key={perm.code}>
                    <TableCell className="sticky left-0 z-10 bg-background">
                      <div>{perm.description}</div>
                      <div className="text-xs text-muted-foreground">{perm.code}</div>
                    </TableCell>
                    {roles.map((role) => (
                      <TableCell key={role.id} className="text-center">
                        <input
                          type="checkbox"
                          className="h-4 w-4 cursor-pointer accent-primary"
                          checked={cellChecked(role.id, perm.code)}
                          onChange={() => toggleCell(role.id, perm.code)}
                        />
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </Fragment>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
