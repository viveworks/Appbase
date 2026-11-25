/**
 * Filter persistence regression for Refs internal work item 2818.
 *
 * This stays separate from the strict UG-04 walkthrough so storage, reload,
 * and CanvasKit's DOM editing proxy each have a focused assertion.
 */
import { test, expect } from './fixtures';
import {
  CONFIG_ITEM_PATTERN,
  SEARCH_INPUT_SELECTOR,
  goHome,
  waitForFlutter,
} from './helpers';

const FILTER_STORAGE_KEY = 'appbase_config_wizard_configuration_filter';
const CANVASKIT_EDITING_INPUT = 'flt-text-editing-host input.flt-text-editing';

test('configuration filter persists through Escape and reload, then clears', async ({ page }) => {
  await goHome(page);
  await page.evaluate((key) => sessionStorage.removeItem(key), FILTER_STORAGE_KEY);
  await page.reload();
  await waitForFlutter(page);

  const searchInput = page.locator(SEARCH_INPUT_SELECTOR).first();
  await expect(searchInput).toHaveValue('');

  const configurationRows = page.locator('flt-semantics[role="button"]').filter({
    hasText: CONFIG_ITEM_PATTERN,
  });
  const unfilteredNames = await configurationRows.allTextContents();
  expect(unfilteredNames, 'unfiltered configuration list must not be empty').not.toHaveLength(0);

  // Retried as one unit, not via Playwright test retries (retries stays 0 per the
  // owner directive in playwright.config.ts): a dropped/late TextField.onChanged
  // event under host load is re-sent by re-filling, which a bare poll cannot do.
  await expect(async () => {
    await searchInput.fill('redis');
    expect(await page.evaluate((key) => sessionStorage.getItem(key), FILTER_STORAGE_KEY))
      .toBe('redis');
  }).toPass({ timeout: 30_000 });
  await searchInput.press('Escape');
  await expect(searchInput).toHaveValue('redis');
  expect(await page.evaluate((key) => sessionStorage.getItem(key), FILTER_STORAGE_KEY))
    .toBe('redis');

  await page.reload();
  await waitForFlutter(page);

  // CanvasKit's focused text-editing input is the authoritative editable
  // surface. internal work item 2833 tracks the duplicate semantics mirror lagging this value.
  const restoredInput = page.locator(CANVASKIT_EDITING_INPUT).first();
  await expect(restoredInput).toBeFocused();
  await expect(restoredInput).toHaveValue('redis');
  const filteredRows = page.locator('flt-semantics[role="button"]').filter({
    hasText: CONFIG_ITEM_PATTERN,
  });
  const filteredNames = await filteredRows.allTextContents();
  expect(filteredNames, 'redis filter must retain at least one visible row').not.toHaveLength(0);
  expect(
    filteredNames.every((name) => name.toLowerCase().includes('redis')),
    'every visible configuration row must match the restored redis filter',
  ).toBe(true);

  // Same retry-as-one-unit treatment as the fill above, for the clear.
  await expect(async () => {
    await restoredInput.fill('');
    expect(await page.evaluate((key) => sessionStorage.getItem(key), FILTER_STORAGE_KEY))
      .toBeNull();
  }).toPass({ timeout: 15_000 });
  await expect(restoredInput).toHaveValue('');
  await page.reload();
  await waitForFlutter(page);

  await expect(page.locator(SEARCH_INPUT_SELECTOR).first()).toHaveValue('');
  expect(
    await page.locator('flt-semantics[role="button"]').filter({
      hasText: CONFIG_ITEM_PATTERN,
    }).allTextContents(),
    'clearing the filter must restore the original visible list',
  ).toEqual(unfilteredNames);
});
