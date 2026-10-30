import {
  test as base,
  expect,
  request as playwrightRequest,
  APIRequestContext,
} from '@playwright/test';

const TOKEN_COOKIE = /(?:^|;\s*)appbase_wizard_token_[^=;]+=([^;]+)/;
const TOKEN_HEADER = 'X-Appbase-Wizard-Token';

type NewContextOptions = Parameters<typeof playwrightRequest.newContext>[0];

async function tokenFor(options?: NewContextOptions): Promise<string> {
  const bootstrap = await playwrightRequest.newContext(options);
  try {
    const response = await bootstrap.get('/');
    if (!response.ok()) {
      throw new Error(`browser-token bootstrap returned HTTP ${response.status()}`);
    }
    const setCookies = response.headersArray()
      .filter(header => header.name.toLowerCase() === 'set-cookie')
      .map(header => header.value);
    for (const cookie of setCookies) {
      const match = TOKEN_COOKIE.exec(cookie);
      if (match) {
        return decodeURIComponent(match[1]);
      }
    }
    throw new Error('app shell did not provide the per-run browser-token cookie');
  } finally {
    await bootstrap.dispose();
  }
}

async function newTokenedContext(options?: NewContextOptions): Promise<APIRequestContext> {
  const token = await tokenFor(options);
  return playwrightRequest.newContext({
    ...options,
    extraHTTPHeaders: {
      ...options?.extraHTTPHeaders,
      [TOKEN_HEADER]: token,
    },
  });
}

export const request = { newContext: newTokenedContext };

export const test = base.extend({
  request: async ({ baseURL }, use) => {
    const context = await newTokenedContext({ baseURL });
    await use(context);
    await context.dispose();
  },
});

export { expect };
export type { APIRequestContext, Page } from '@playwright/test';
