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

  await searchInput.fill('redis');
  await expect
    .poll(() => page.evaluate((key) => sessionStorage.getItem(key), FILTER_STORAGE_KEY))
    .toBe('redis');
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

  await restoredInput.fill('');
  await expect(restoredInput).toHaveValue('');
  await expect
    .poll(() => page.evaluate((key) => sessionStorage.getItem(key), FILTER_STORAGE_KEY))
    .toBeNull();
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
