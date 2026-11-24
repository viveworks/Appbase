import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  // A committed test.only() must fail the CI run, not narrow it silently to one test.
  forbidOnly: !!process.env.CI,
  // Pinned (not left to Playwright's CPU-based default, and never a one-off
  // --workers CLI flag) so the suite's parallelism is deterministic across
  // machines and CI runs. Each worker drives Flutter's CanvasKit renderer
  // with software WebGL, which is CPU/GPU-emulation heavy per worker, on top
  // of whatever else is persistently resident on a shared host (not just an
  // occasional heavy build: a host running many long-lived processes carries
  // real ambient load at all times). 2 keeps real parallelism while keeping
  // this suite's own contribution to that contention bounded; a higher count
  // trades a faster clean-host run for exactly the slow-page-load-then-
  // connection-refused failure mode this suite exists to never produce.
  workers: 2,
  // Retries must never mask a real failure (owner directive): acceptance
  // runs have to pass at retries: 0, so a transient-looking failure is
  // fixed at its root cause (see all-configs.spec.ts's readiness-wait fix)
  // instead of being quietly re-tried away.
  retries: 0,
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
