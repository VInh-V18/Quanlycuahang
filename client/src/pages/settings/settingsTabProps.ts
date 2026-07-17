export type FormState = Record<string, string>;

/** Props chung cho 4 tab cua SettingsPage.tsx (tach rieng qua audit production readiness
 * 2026-07-17) — form/set/saveMutation van do SettingsPage() so huu qua useSettingsForm(), moi tab
 * chi la component hien thi thuan tuy nhan props xuong. */
export interface SettingsTabProps {
  form: FormState;
  set: (key: string, value: string) => void;
  canEdit: boolean;
  loading: boolean;
  saving: boolean;
  onSave: (keys: string[]) => void;
}
