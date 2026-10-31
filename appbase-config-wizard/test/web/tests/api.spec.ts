/**
 * API integration tests for appbase-config-wizard REST endpoints.
 *
 * Covers:
 *   W8        GET /api/configurations        → JSON array with fqcn, simpleName, module.
 *   W9        GET /api/configurations/{fqcn} → object with fields array.
 *   W-nested  nestedTypeFqcn validation across all configs.
 */
import { test, expect, APIRequestContext } from './fixtures';

async function fetchConfigList(request: APIRequestContext): Promise<unknown[]> {
  const response = await request.get('/api/configurations');
  expect(response.status(), 'GET /api/configurations should return 200').toBe(200);

  const contentType = response.headers()['content-type'] ?? '';
  expect(contentType, 'Content-Type should be JSON').toContain('application/json');

  const body = await response.json() as unknown[];
  expect(Array.isArray(body), 'Response body should be a JSON array').toBe(true);
  return body;
}

// ---------------------------------------------------------------------------
// W8 — GET /api/configurations
// ---------------------------------------------------------------------------

test.describe('W8: GET /api/configurations', () => {
  test('returns a non-empty JSON array', async ({ request }) => {
    const configs = await fetchConfigList(request);
    expect(configs.length, 'Config list must not be empty').toBeGreaterThan(0);
  });

  test('each entry has fqcn, simpleName, and module fields', async ({ request }) => {
    const configs = await fetchConfigList(request);
    for (const entry of configs) {
      const item = entry as Record<string, unknown>;
      expect(typeof item['fqcn']).toBe('string');
      expect((item['fqcn'] as string).length).toBeGreaterThan(0);
      expect(typeof item['simpleName']).toBe('string');
      expect((item['simpleName'] as string).length).toBeGreaterThan(0);
      expect(typeof item['module']).toBe('string');
      expect((item['module'] as string).length).toBeGreaterThan(0);
    }
  });

  test('fqcn values look like Java FQCNs', async ({ request }) => {
    const configs = await fetchConfigList(request);
    for (const entry of configs) {
      const fqcn = (entry as Record<string, unknown>)['fqcn'] as string;
      expect(fqcn, 'fqcn must contain a dot').toContain('.');
    }
  });

  test('returns at least 10 configs (covers all registered modules)', async ({ request }) => {
    const configs = await fetchConfigList(request);
    expect(configs.length, 'Expected >=10 configs').toBeGreaterThanOrEqual(10);
  });
});

// ---------------------------------------------------------------------------
// W9 — GET /api/configurations/{fqcn}
// ---------------------------------------------------------------------------

test.describe('W9: GET /api/configurations/{fqcn}', () => {
  let firstFqcn: string;

  test.beforeAll(async ({ request }) => {
    const configs = await fetchConfigList(request);
    expect(configs.length).toBeGreaterThan(0);
    firstFqcn = (configs[0] as Record<string, unknown>)['fqcn'] as string;
  });

  test('returns 200 for a known fqcn', async ({ request }) => {
    const encoded = encodeURIComponent(firstFqcn);
    const response = await request.get(`/api/configurations/${encoded}`);
    expect(response.status()).toBe(200);
  });

  test('response has a fields array', async ({ request }) => {
    const encoded = encodeURIComponent(firstFqcn);
    const response = await request.get(`/api/configurations/${encoded}`);
    const body = await response.json() as Record<string, unknown>;
    expect(Array.isArray(body['fields']), 'fields must be an array').toBe(true);
  });

  test('fields array has items with name and typeName', async ({ request }) => {
    const encoded = encodeURIComponent(firstFqcn);
    const response = await request.get(`/api/configurations/${encoded}`);
    const body = await response.json() as Record<string, unknown>;
    const fields = body['fields'] as Record<string, unknown>[];

    expect(fields.length, 'fields array must not be empty').toBeGreaterThan(0);

    const first = fields[0];
    expect(typeof first['name'], 'field.name must be a string').toBe('string');
    expect((first['name'] as string).length, 'field.name must not be empty').toBeGreaterThan(0);

    // The API uses 'typeName' (not 'type' or 'fieldType')
    expect(typeof first['typeName'], 'field.typeName must be a string').toBe('string');
    expect((first['typeName'] as string).length, 'field.typeName must not be empty').toBeGreaterThan(0);
  });

  test('every field defaultValue is a string or null — never raw boolean/number', async ({ request }) => {
    // Regression guard: FieldSummaryDto.defaultValue was Object; booleans/ints
    // caused Dart's `as String?` cast to throw TypeError at runtime.
    const encoded = encodeURIComponent(firstFqcn);
    const response = await request.get(`/api/configurations/${encoded}`);
    const body = await response.json() as Record<string, unknown>;
    const fields = body['fields'] as Record<string, unknown>[];

    for (const field of fields) {
      const dv = field['defaultValue'];
      const ok = dv === null || dv === undefined || typeof dv === 'string';
      expect(
        ok,
        'field "' + field['name'] + '" has non-string defaultValue: ' + JSON.stringify(dv),
      ).toBe(true);
    }
  });

  test('returns 404 for an unknown fqcn', async ({ request }) => {
    const response = await request.get('/api/configurations/com.does.not.Exist');
    expect([404, 400], 'Unknown fqcn should return 404 or 400').toContain(response.status());
  });

  test('nestedTypeFqcn is null or a valid Java FQCN', async ({ request }) => {
    const encoded = encodeURIComponent(firstFqcn);
    const response = await request.get(`/api/configurations/${encoded}`);
    const body = await response.json() as Record<string, unknown>;
    const fields = body['fields'] as Record<string, unknown>[];

    for (const field of fields) {
      const nf = field['nestedTypeFqcn'];
      if (nf !== null && nf !== undefined) {
        expect(typeof nf, 'nestedTypeFqcn must be string for field "' + field['name'] + '"').toBe('string');
        expect((nf as string).includes('.'), 'nestedTypeFqcn must be a dotted FQCN').toBe(true);
      }
    }
  });

  test('response includes defaultValues map', async ({ request }) => {
    const encoded = encodeURIComponent(firstFqcn);
    const response = await request.get(`/api/configurations/${encoded}`);
    const body = await response.json() as Record<string, unknown>;
    expect(typeof body['defaultValues'], 'defaultValues must be an object').toBe('object');
  });
});

// ---------------------------------------------------------------------------
// W-nested — nestedTypeFqcn across ALL configs
// ---------------------------------------------------------------------------

test.describe('W-nested: nestedTypeFqcn across all configs', () => {
  test('every config field nestedTypeFqcn is null or a valid dotted FQCN', async ({ request }) => {
    const listRes = await request.get('/api/configurations');
    const configs = await listRes.json() as Array<Record<string, string>>;
    expect(configs.length).toBeGreaterThan(0);

    const failures: string[] = [];

    for (const config of configs) {
      const fqcn = config['fqcn'];
      const res = await request.get(`/api/configurations/${encodeURIComponent(fqcn)}`);
      if (!res.ok()) {
        failures.push(fqcn + ': GET returned ' + res.status());
        continue;
      }
      const body = await res.json() as Record<string, unknown>;
      const fields = (body['fields'] ?? []) as Record<string, unknown>[];
      for (const field of fields) {
        const nf = field['nestedTypeFqcn'];
        if (nf != null) {
          if (typeof nf !== 'string' || !nf.includes('.')) {
            failures.push(fqcn + '.' + field['name'] + ': invalid nestedTypeFqcn: ' + JSON.stringify(nf));
          }
        }
        if (nf == null) {
          const dv = field['defaultValue'];
          if (dv !== null && dv !== undefined && typeof dv !== 'string') {
            failures.push(fqcn + '.' + field['name'] + ': non-string defaultValue: ' + typeof dv);
          }
        }
      }
    }

    expect(failures, 'Field type violations:\n' + failures.join('\n')).toHaveLength(0);
  });
});
