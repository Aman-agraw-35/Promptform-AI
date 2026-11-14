import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Promptform AI",
  description: "Turn plain English into working forms.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
