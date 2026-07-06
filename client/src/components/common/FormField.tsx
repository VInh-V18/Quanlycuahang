import type { Control, FieldPath, FieldValues } from "react-hook-form";
import { Input } from "@/components/ui/input";
import {
  FormControl,
  FormDescription,
  FormField as RHFFormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

export interface FormFieldProps<TFieldValues extends FieldValues> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  description?: string;
  type?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
}

/** Wrapper gọn cho trường hợp phổ biến nhất: 1 Label + 1 Input text + thông báo lỗi Zod
 * (React Hook Form + Zod, đúng Part C) — dùng trực tiếp `ui/form.tsx` khi cần control tùy biến hơn
 * (Select, DatePicker...). */
export function FormField<TFieldValues extends FieldValues>({
  control,
  name,
  label,
  description,
  type = "text",
  placeholder,
  disabled,
  required,
}: FormFieldProps<TFieldValues>) {
  return (
    <RHFFormField
      control={control}
      name={name}
      render={({ field }) => (
        <FormItem>
          <FormLabel>
            {label}
            {required && <span className="text-destructive"> *</span>}
          </FormLabel>
          <FormControl>
            <Input type={type} placeholder={placeholder} disabled={disabled} {...field} />
          </FormControl>
          {description && <FormDescription>{description}</FormDescription>}
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
