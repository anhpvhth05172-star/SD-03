// Cấu hình ESLint tối thiểu cho PolyShoes FE (GĐ0/P0.5).
// Mục tiêu: nền lint ổn định, không strict quá mức — có thể nâng dần sau.
import js from '@eslint/js';
import pluginVue from 'eslint-plugin-vue';
import globals from 'globals';

export default [
  {
    ignores: [
      'node_modules/**',
      'dist/**',
      'coverage/**',
      '*.local',
    ],
  },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    rules: {
      // Baseline: giữ cảnh báo thay vì lỗi cứng để không ép refactor hàng loạt.
      'no-unused-vars': ['warn', { args: 'none' }],
      'no-console': 'off',
      'vue/multi-word-component-names': 'off',
    },
  },
];
