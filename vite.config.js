import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    // 개발 중에는 Vite(5173)가 /api 요청만 Spring Boot(8080)로 넘긴다.
    // 운영에서는 Spring Boot가 정적 파일까지 같이 서빙하므로 프록시가 필요 없다.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
