import type { Control, FieldPath, FieldValues } from "react-hook-form";
import { NumberInput } from "@/components/common/NumberInput";
import {
  FormControl,
  FormDescription,
  FormField as RHFFormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

export interface NumberFieldProps<TFieldValues extends FieldValues> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  description?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  allowDecimal?: boolean;
  min?: number;
  suffix?: string;
}

/** Ban sao cua FormField.tsx nhung dung NumberInput (khong phai <input type="number"> tran) cho
 * cac truong so trong form React Hook Form - xem NumberInput.tsx ve ly do. */
export function NumberField<TFieldValues extends FieldValues>({
  control,
  name,
  label,
  description,
  placeholder,
  disabled,
  required,
  allowDecimal = true,
  min,
  suffix,
}: NumberFieldProps<TFieldValues>) {
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
            <NumberInput
              value={field.value}
              onValueChange={(v) => field.onChange(v ?? 0)}
              onBlur={field.onBlur}
              allowDecimal={allowDecimal}
              min={min}
              disabled={disabled}
              placeholder={placeholder}
              suffix={suffix}
            />
          </FormControl>
          {description && <FormDescription>{description}</FormDescription>}
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
