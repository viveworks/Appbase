/**
 * Browser Back regression for Refs internal work item 2819.
 *
 * This stays separate from the strict UG-04 walkthrough so the single browser
 * history behavior has a focused failure and artifact.
 */
import { test, expect } from './fixtures';
import {
  BACK_BUTTON_SELECTOR,
  CONFIG_ITEM_PATTERN,
  SEARCH_INPUT_SELECTOR,
  countListItems,
  goHome,
  waitForFlutter,
} from './helpers';

test('in-app Back from a configuration form returns to the list', async ({ page }) => {
  await goHome(page);
  const semanticsButton = (text: string) => page
    .locator('flt-semantics[role="button"]')
    .filter({ hasText: new RegExp(`^\\s*${text}\\s*$`) });

  const configurations = page.locator('flt-semantics[role="button"]').filter({
    hasText: CONFIG_ITEM_PATTERN,
  });
  const listBefore = await configurations.allTextContents();
  expect(listBefore, 'configuration list must contain an item to open').not.toHaveLength(0);

  await configurations.first().click({ force: true });
  const resetButton = semanticsButton('Reset');
  const buildButton = semanticsButton('Build Config');
  await expect(resetButton).toHaveCount(1);
  await expect(buildButton).toHaveCount(1);
  const formPanel = page.locator('flt-semantics[role="tabpanel"]').filter({
    has: resetButton,
  });
  await expect(formPanel).toHaveCount(1);
  const semanticsTree = await page.locator('flt-semantics').evaluateAll((nodes) =>
    nodes.map((node) => {
      let depth = 0;
      let parent = node.parentElement?.closest('flt-semantics');
      while (parent) {
        depth += 1;
        parent = parent.parentElement?.closest('flt-semantics');
      }
      const element = node as HTMLElement;
      return `${'  '.repeat(depth)}<flt-semantics role=${JSON.stringify(element.getAttribute('role'))}`
        + ` aria-label=${JSON.stringify(element.getAttribute('aria-label'))}>`
        + ` text=${JSON.stringify((element.textContent ?? '').trim())}`;
    }).join('\n'));
  console.log(`[browser-back post-open semantics]\n${semanticsTree}`);
  expect(await countListItems(page), 'configuration form must replace the list').toBe(0);

  const inAppBackButton = page.locator(BACK_BUTTON_SELECTOR).first();
  await expect(inAppBackButton).toHaveCount(1);
  await inAppBackButton.click({ force: true });
  await waitForFlutter(page);

  await expect(page.locator(SEARCH_INPUT_SELECTOR).first()).toHaveValue('');
  expect(
    await page.locator('flt-semantics[role="button"]').filter({
      hasText: CONFIG_ITEM_PATTERN,
    }).allTextContents(),
    'browser Back must restore the exact visible configuration list',
  ).toEqual(listBefore);
  await expect(resetButton).toHaveCount(0);
  await expect(buildButton).toHaveCount(0);
  await expect(formPanel).toHaveCount(0);
});
