import { test, expect } from './fixtures';

test.describe('W-SWAGGER', () => {
  test('swagger-ui loads and lists /api/configurations paths', async ({ page }) => {
    const openApiResponse = page.waitForResponse(
      response => response.url().endsWith('/api/openapi.json') && response.status() === 200,
    );
    await page.goto('/api/swagger-ui/index.html');
    await openApiResponse;
    await page.waitForSelector('.opblock', { timeout: 20_000 });
    const paths = await page.locator('.opblock-summary-path').allTextContents();
    expect(paths.some(p => p.includes('configurations'))).toBe(true);
  });
});
