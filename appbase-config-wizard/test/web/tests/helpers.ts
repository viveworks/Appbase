/**
 * Shared test helpers for appbase-config-wizard Playwright tests.
 *
 * Flutter web (CanvasKit) builds its accessibility tree (flt-semantics nodes)
 * lazily — only after the `flt-semantics-placeholder` element receives a click
 * event. Without this click, all flt-semantics selectors return 0 results.
 *
 * Trigger sequence (all wrapped in waitForFlutter):
 *   1. Wait for `flutter-view` (Flutter framework mounted)
 *   2. JS-click the off-screen placeholder: [aria-label="Enable accessibility"]
 *   3. Wait for `flt-semantics` elements to appear in DOM
 *
 * Config list rendering notes (CanvasKit Flutter 3.44.1):
 *   - ListView items render as `flt-semantics[role="button"]`
 *   - NOT `role="listitem"` or `role="option"`
 *   - AppBar action buttons also use `role="button"` (e.g., "Import YAML…")
 *   - Use CONFIG_ITEM_SELECTOR (filters by Java class-name content) to target list items
 *   - The list is virtualized: only ~10 items in DOM at any given scroll position
 */
import { Page } from '@playwright/test';

// ---------------------------------------------------------------------------
// Common selectors (canonical, shared across all spec files)
// ---------------------------------------------------------------------------

/**
 * Matches flt-semantics config list items (rendered as buttons with Java class
 * names) as well as plain-HTML list elements for forward compatibility.
 *
 * CanvasKit notes: Flutter ListView items appear as role="button" NOT "listitem".
 * This selector matches both CanvasKit buttons and any HTML fallback elements.
 */
export const LIST_ITEM_SELECTOR =
  'flt-semantics[role="button"],'
  + ' flt-semantics[role="listitem"],'
  + ' flt-semantics[role="option"],'
  + ' [role="listitem"],'
  + ' [role="option"]';

/**
 * Matches config list items specifically — flt-semantics buttons whose text
 * contains a Java-style type name ("Configuration" or "Config" suffix,
 * with a non-whitespace identifier character before it). The type may be
 * followed by merged subtitle/badge text with or without whitespace.
 * Excludes AppBar buttons ("Import YAML configurations", "System theme", etc.).
 * The prefix requirement also excludes the form action text "Build Config".
 */
export const CONFIG_ITEM_PATTERN = /\S(?:Configuration|Config)/;

export const BACK_BUTTON_SELECTOR =
  'flt-semantics[role="button"][aria-label*="back" i],'
  + ' button[aria-label*="back" i],'
  + ' [aria-label="Back"],'
  + ' flt-semantics[aria-label*="arrow_back" i]';

export const SEARCH_INPUT_SELECTOR =
  'input[aria-label*="Search" i], flt-semantics input, input[type="text"],'
  + ' flt-semantics[role="textbox"]';

export const SUBMIT_BUTTON_SELECTOR =
  'flt-semantics[role="button"][aria-label*="Build" i],'
  + ' button[aria-label*="Build" i],'
  + ' flt-semantics[role="button"]';  // last-resort: first visible button on form

export const FIELD_SELECTOR =
  'flt-semantics[role="textbox"], flt-semantics[role="switch"],'
  + ' flt-semantics[role="slider"], input[type="text"], input[type="number"]';

export const IMPORT_BUTTON_SELECTOR =
  'flt-semantics[role="button"][aria-label*="Import" i],'
  + ' button[aria-label*="Import" i],'
  + ' flt-semantics[aria-label*="Import YAML" i]';

export const SET_VALUES_SELECTOR =
  'flt-semantics[role="button"][aria-label*="Set Values" i],'
  + ' button[aria-label*="Set Values" i]';

export const APPLY_SELECTOR =
  'flt-semantics[role="button"][aria-label*="Apply" i],'
  + ' button[aria-label*="Apply" i]';

export const WRITE_BUTTON_SELECTOR =
  'flt-semantics[role="button"][aria-label*="Write" i],'
  + ' button[aria-label*="Write" i],'
  + ' flt-semantics[aria-label*="Write to File" i]';

export const RESET_BUTTON_SELECTOR =
  'flt-semantics[role="button"][aria-label*="Reset" i],'
  + ' button[aria-label*="Reset" i]';

// ---------------------------------------------------------------------------
// Core Flutter accessibility trigger
// ---------------------------------------------------------------------------

/**
 * Wait for the Flutter app to fully initialize and activate the accessibility
 * semantic tree.
 *
 * Steps:
 * 1. Wait for `flutter-view` — Flutter framework has mounted.
 * 2. Wait for the off-screen [aria-label="Enable accessibility"] placeholder
 *    to attach — the real signal that Flutter has rendered its first frame.
 * 3. JS-click that placeholder.
 *    This is what a screen reader would do; Flutter observes this and builds
 *    the flt-semantics elements.
 * 4. Wait up to 10 seconds for at least one flt-semantics element to appear.
 */
export async function waitForFlutter(page: Page): Promise<void> {
  // Wait for Flutter framework to mount
  await page.waitForSelector('flutter-view', { state: 'attached', timeout: 30_000 });

  // Real readiness wait instead of a fixed delay: the accessibility
  // placeholder only exists once Flutter has rendered its first frame, and a
  // fixed sleep either races ahead of a slow (CPU/GPU-contended,
  // software-rendered CanvasKit) first frame or wastes time on a fast one.
  // Falls through silently on timeout, same as before, so a page without
  // this placeholder is not a new failure mode.
  await page.locator('[aria-label="Enable accessibility"]')
    .waitFor({ state: 'attached', timeout: 15_000 })
    .catch(() => undefined);

  // Click the "Enable accessibility" placeholder — Flutter's off-screen
  // sentinel that triggers semantics tree construction when focused/clicked.
  await page.evaluate(() => {
    const el = document.querySelector('[aria-label="Enable accessibility"]');
    if (el) {
      (el as HTMLElement).focus();
      el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
      el.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }));
      el.dispatchEvent(new MouseEvent('pointerup', { bubbles: true }));
    }
  });

  // Wait for flt-semantics elements to appear
  await page.waitForSelector('flt-semantics', {
    state: 'attached',
    timeout: 10_000,
  }).catch(() => undefined);

  await page.waitForTimeout(500);
}

// ---------------------------------------------------------------------------
// Navigation helpers
// ---------------------------------------------------------------------------

/**
 * Navigate to home and wait for Flutter to render the config list.
 * Always call this instead of page.goto('/') + manual waits.
 */
export async function goHome(page: Page): Promise<void> {
  logPageDiagnostics(page);
  await page.goto('/');
  await waitForFlutter(page);
  // Wait for at least one flt-semantics button to appear (list items are buttons)
  await page.waitForSelector('flt-semantics[role="button"]', { timeout: 15_000 });
}

/**
 * Prints browser-side console messages, uncaught page errors, and failed network requests to the
 * test runner's own stdout as they happen — not an artifact, so it survives even on a run with no
 * upload step and shows up directly in a CI job log. Attach before navigating (`goHome` does this),
 * so nothing from the very first script evaluation is missed. Safe to call once per test/page.
 */
export function logPageDiagnostics(page: Page): void {
  page.on('console', (msg) => {
    if (msg.type() === 'error' || msg.type() === 'warning') {
      console.log(`[browser ${msg.type()}] ${msg.text()}`);
    }
  });
  page.on('pageerror', (err) => {
    console.log(`[browser pageerror] ${err.message}`);
  });
  page.on('requestfailed', (req) => {
    console.log(`[browser requestfailed] ${req.method()} ${req.url()} — ${req.failure()?.errorText}`);
  });
}

/**
 * Navigate home, click the first config in the list, wait for the form.
 * Returns the text of the item that was clicked.
 *
 * Config list items are flt-semantics[role="button"] with Java class names.
 */
export async function openFirstConfigForm(page: Page): Promise<string> {
  await goHome(page);

  // Find a button whose text matches a Java config class name
  const configButtons = page.locator('flt-semantics[role="button"]').filter({
    hasText: CONFIG_ITEM_PATTERN,
  });
  const count = await configButtons.count();
  if (count === 0) {
    // Fallback: take any button
    const anyBtn = page.locator('flt-semantics[role="button"]').first();
    const name = (await anyBtn.textContent()) ?? 'unknown';
    await anyBtn.click({ force: true });
    await page.waitForTimeout(2_000);
    return name.trim();
  }

  const firstItem = configButtons.first();
  const configName = (await firstItem.textContent()) ?? 'unknown';
  await firstItem.click({ force: true });
  await page.waitForTimeout(2_000);
  return configName.trim();
}

/**
 * Count config list items on the current page.
 * Returns the count of visible flt-semantics buttons matching Java class names.
 */
export async function countListItems(page: Page): Promise<number> {
  const buttons = page.locator('flt-semantics[role="button"]').filter({
    hasText: CONFIG_ITEM_PATTERN,
  });
  return buttons.count();
}

/**
 * Navigate back using the back button or browser history.
 * Always re-triggers the Flutter accessibility placeholder after navigation
 * because Flutter rebuilds its semantics tree on route changes.
 */
export async function goBack(page: Page): Promise<void> {
  const backBtn = page.locator(BACK_BUTTON_SELECTOR).first();
  if ((await backBtn.count()) > 0) {
    await backBtn.click({ force: true });
  } else {
    await page.goBack();
  }
  // Flutter rebuilds its semantics tree on route transitions; re-click the
  // accessibility trigger so flt-semantics elements reappear.
  await page.waitForTimeout(1_000);
  await page.evaluate(() => {
    const el = document.querySelector('[aria-label="Enable accessibility"]');
    if (el) {
      (el as HTMLElement).focus();
      el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
      el.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }));
      el.dispatchEvent(new MouseEvent('pointerup', { bubbles: true }));
    }
  });
  await page.waitForTimeout(800);
}
