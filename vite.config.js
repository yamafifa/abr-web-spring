import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';

// frontend/ の JS と CSS をまとめて、src/main/resources/static/build/ に出す（Spring Boot が /build/ で配る）
// ファイル名にハッシュを付けないので、テンプレートからは /build/app.js・/build/app.css で読める
export default defineConfig({
    plugins: [tailwindcss()],
    build: {
        outDir: 'src/main/resources/static/build',
        emptyOutDir: true,
        rollupOptions: {
            input: 'frontend/app.js',
            output: {
                entryFileNames: 'app.js',
                assetFileNames: 'app[extname]',
            },
        },
    },
});
