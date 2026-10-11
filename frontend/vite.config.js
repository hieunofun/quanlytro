import {defineConfig} from 'vite';
export default defineConfig({build:{outDir:'../src/main/resources/static',emptyOutDir:true},server:{proxy:{'/api':'http://localhost:8080'}}});
