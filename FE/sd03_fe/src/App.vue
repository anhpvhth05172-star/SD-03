<script setup>
import Sidebar from './components/Sidebar.vue'
import Topbar from './components/Topbar.vue'
</script>

<template>
  <div class="app-layout">
    <Sidebar />
    <div class="main-wrapper">
      <Topbar />
      <main class="content-body">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style>
:root {
  --primary: #d92d20;
  --primary-hover: #b42318;
  --primary-light: #ffe4e6;
  --primary-tint: #fff1f2;
  --primary-border: #fecdd3;

  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --border-color: #e2e8f0;
  --border-input: #e2e8f0;

  --text-main: #0f172a;
  --text-secondary: #334155;
  --text-muted: #64748b;
  --text-placeholder: #94a3b8;

  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 9999px;

  --font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: var(--font-family);
  background-color: var(--bg-page);
  color: var(--text-main);
  font-size: 13.5px;
  -webkit-font-smoothing: antialiased;
}

a, a:hover, a:focus {
  text-decoration: none !important;
}

.app-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
  background-color: var(--bg-page);
}

.content-body {
  padding: 24px 32px 80px;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
}
</style>

