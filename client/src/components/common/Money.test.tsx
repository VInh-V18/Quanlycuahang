import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Money } from "@/components/common/Money";

describe("Money", () => {
  it("formats VND with Vietnamese thousands separator and currency suffix", () => {
    render(<Money value={139000} />);
    expect(screen.getByText(/139\.000/)).toBeInTheDocument();
    expect(screen.getByText(/₫/)).toBeInTheDocument();
  });

  it("shows a + sign for positive change amounts when showSign is set", () => {
    render(<Money value={11000} showSign />);
    expect(screen.getByText(/\+11\.000/)).toBeInTheDocument();
  });
});
