import { useEffect, useState, type ReactNode } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { PersistGate } from "redux-persist/integration/react";
import { Toaster } from "@/components/ui/toaster";
import { bootstrapSession } from "@/lib/api/bootstrap";
import { queryClient } from "@/lib/http/queryClient";
import { AppRouter } from "@/routes/router";
import { persistor, store } from "@/store";
import { useAppSelector } from "@/store/hooks";

function ThemeSync() {
  const theme = useAppSelector((state) => state.ui.theme);
  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
  }, [theme]);
  return null;
}

function Bootstrap({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    bootstrapSession().finally(() => setReady(true));
  }, []);

  if (!ready) return null;
  return <>{children}</>;
}

export default function App() {
  return (
    <Provider store={store}>
      <PersistGate loading={null} persistor={persistor}>
        <QueryClientProvider client={queryClient}>
          <ThemeSync />
          <Bootstrap>
            <AppRouter />
          </Bootstrap>
          <Toaster />
        </QueryClientProvider>
      </PersistGate>
    </Provider>
  );
}
