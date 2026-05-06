import "./globals.css";

export const metadata = {
  title: "EVE App Builder",
  description: "Universal Dragon AI App Builder by Aslam",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
