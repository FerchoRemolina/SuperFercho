import type { NextConfig } from "next";

const springOrigin =
  process.env.SUPERFERCHO_API_ORIGIN ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${springOrigin}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
