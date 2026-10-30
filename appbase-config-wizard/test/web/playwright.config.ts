import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  // A committed test.only() must fail the CI run, not narrow it silently to one test.
  forbidOnly: !!process.env.CI,
  retries: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.CONFIG_WIZARD_URL ?? 'http://localhost:8080',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    // Flutter CanvasKit builds accessibility (flt-semantics) elements only when the
    // browser accessibility tree is active.  --force-renderer-accessibility enables
    // it in headless Chromium so that flt-semantics nodes are present in the DOM.
    launchOptions: {
      args: ['--force-renderer-accessibility'],
    },
  },
});
