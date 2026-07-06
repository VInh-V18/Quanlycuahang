import { format } from "date-fns";
import { CalendarIcon } from "lucide-react";
import type { DateRange } from "react-day-picker";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";

export interface DateRangePickerProps {
  value?: DateRange;
  onChange?: (range: DateRange | undefined) => void;
  className?: string;
  placeholder?: string;
}

/** Bộ lọc khoảng ngày dùng chung cho báo cáo/danh sách (Phase 5.3) — trả về
 * { from, to } kiểu Date, nơi gọi tự format sang tham số API phù hợp. */
export function DateRangePicker({
  value,
  onChange,
  className,
  placeholder = "Chọn khoảng ngày",
}: DateRangePickerProps) {
  return (
    <div className={cn("grid gap-2", className)}>
      <Popover>
        <PopoverTrigger asChild>
          <Button
            id="date-range"
            variant="outline"
            className={cn(
              "w-[260px] justify-start text-left font-normal",
              !value?.from && "text-muted-foreground",
            )}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {value?.from ? (
              value.to ? (
                <>
                  {format(value.from, "dd/MM/yyyy")} – {format(value.to, "dd/MM/yyyy")}
                </>
              ) : (
                format(value.from, "dd/MM/yyyy")
              )
            ) : (
              <span>{placeholder}</span>
            )}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            initialFocus
            mode="range"
            defaultMonth={value?.from}
            selected={value}
            onSelect={onChange}
            numberOfMonths={2}
          />
        </PopoverContent>
      </Popover>
    </div>
  );
}
