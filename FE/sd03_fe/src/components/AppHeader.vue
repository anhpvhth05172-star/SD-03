<template>
  <header class="app-header">
    <div class="header-actions">
      <button class="icon-btn" type="button" aria-label="Giao diện tối">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
        </svg>
      </button>

      <div class="avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7.5" r="4" />
        </svg>
      </div>

      <button class="icon-btn" type="button" aria-label="Mở rộng">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 9.5l6 6 6-6" />
        </svg>
      </button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 56px;
  background: var(--white);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 18px;
  border-bottom: 1.5px solid var(--pink-line);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e7e7ec;
  border-radius: 10px;
  padding: 5px 12px;
  background: var(--white);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #3f3f47;
}

.icon-btn svg {
  width: 19px;
  height: 19px;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f4f4f6;
  color: #4b4b53;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar svg {
  width: 17px;
  height: 17px;
}
</style>
