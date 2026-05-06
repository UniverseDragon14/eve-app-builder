import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  allowedDevOrigins: [
    "192.168.70.117",
    "192.168.70.119",
    "nova-pi.local",
    "localhost",
  ],
};

export default nextConfig;
