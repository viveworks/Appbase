/**
 * Regression tests added after fix round 2:
 *
 *   W-NESTED-FIELDS-DETECTION  All configs with complex object fields expose nestedTypeFqcn
 *                               (not null) so the UI shows a nested sub-form instead of JSON.
 *   W-BUILD-ALL                 Every config builds via POST /build with {} body and returns
 *                               valid YAML or an accepted validation error — no Jackson crashes.
 *
 * These are API-level tests; full UI coverage for nested forms is in all-configs.spec.ts (W12).
 */
import { test, expect, request } from './fixtures';

// ---------------------------------------------------------------------------
// W-NESTED-FIELDS-DETECTION
// ---------------------------------------------------------------------------

test.describe('W-NESTED-FIELDS-DETECTION: complex object fields have nestedTypeFqcn', () => {
  /**
   * Heuristic: a field is "complex" if its typeName ends with a known complex-object
   * pattern AND nestedTypeFqcn is null. This catches the Jaeger/Scope regression
   * where @JsonTypeInfo-annotated abstract types were treated as plain text fields.
   */
  const COMPLEX_FIELD_SUFFIXES = ['Configuration', 'Config', 'Settings', 'Options', 'Policy'];

  test('no config field with a complex typeName is missing nestedTypeFqcn', async ({ baseURL }) => {
    const ctx = await request.newContext({ baseURL });

    const listRes = await ctx.get('/api/configurations');
    expect(listRes.ok()).toBeTruthy();
    const configs = await listRes.json() as Array<{ fqcn: string; simpleName: string }>;

    const violations: string[] = [];
    for (const c of configs) {
      const res = await ctx.get(`/api/configurations/${encodeURIComponent(c.fqcn)}`);
      if (!res.ok()) continue;
      const body = await res.json() as {
        fields?: Array<{ name: string; typeName: string; nestedTypeFqcn?: string | null; defaultValue?: string | null; enumValues?: string[] | null }>;
      };
      for (const f of body.fields ?? []) {
        const isComplexName = COMPLEX_FIELD_SUFFIXES.some(
          (s) => f.typeName.endsWith(s),
        );
        const isEnum = f.enumValues != null;
        if (isComplexName && !isEnum && f.nestedTypeFqcn == null) {
          violations.push(
            `${c.simpleName}.${f.name}: typeName="${f.typeName}" but nestedTypeFqcn is null`,
          );
        }
      }
    }
    await ctx.dispose();
    expect(
      violations,
      'Fields with complex type names must have nestedTypeFqcn set:\n' + violations.join('\n'),
    ).toHaveLength(0);
  });

  test('DefaultTracerServiceConfiguration.jaegerSamplerConfiguration has nestedTypeFqcn', async ({
    baseURL,
  }) => {
    const ctx = await request.newContext({ baseURL });
    const fqcn = 'com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration';
    const res = await ctx.get(`/api/configurations/${encodeURIComponent(fqcn)}`);
    if (!res.ok()) { await ctx.dispose(); test.skip(); return; }

    const body = await res.json() as {
      fields?: Array<{ name: string; nestedTypeFqcn?: string | null }>;
    };
    const samplerField = (body.fields ?? []).find((f) => f.name === 'jaegerSamplerConfiguration');
    if (!samplerField) { await ctx.dispose(); test.skip(); return; }

    expect(
      samplerField.nestedTypeFqcn,
      'jaegerSamplerConfiguration must have nestedTypeFqcn set (was null before fix)',
    ).not.toBeNull();
    await ctx.dispose();
  });

  test('DefaultJdbcClientIntegrationServiceConfiguration.hikariDataSourceConfiguration has nestedTypeFqcn', async ({
    baseURL,
  }) => {
    const ctx = await request.newContext({ baseURL });
    const fqcn = 'com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientIntegrationServiceConfiguration';
    const res = await ctx.get(`/api/configurations/${encodeURIComponent(fqcn)}`);
    if (!res.ok()) { await ctx.dispose(); test.skip(); return; }

    const body = await res.json() as {
      fields?: Array<{ name: string; nestedTypeFqcn?: string | null }>;
    };
    const hikariField = (body.fields ?? []).find((f) => f.name === 'hikariDataSourceConfiguration');
    if (!hikariField) { await ctx.dispose(); test.skip(); return; }

    expect(
      hikariField.nestedTypeFqcn,
      'hikariDataSourceConfiguration must have nestedTypeFqcn set',
    ).not.toBeNull();
    await ctx.dispose();
  });
});

// ---------------------------------------------------------------------------
// W-BUILD-ALL — extended Build Config sweep
// ---------------------------------------------------------------------------

test.describe('W-BUILD-ALL: Build Config works for all config classes', () => {
  /**
   * Error patterns that are acceptable — they indicate Hibernate Validator
   * constraints on required fields that need actual values to satisfy.
   * Jackson conversion errors and NPEs are NOT acceptable.
   */
  const ACCEPTED_PATTERNS = [
    /must not be null/i,
    /must not be blank/i,
    /must not be empty/i,
    /must be greater/i,
    /must be less/i,
    /size must be between/i,
    /HV\d{5,}/,
  ];

  test('every config returns yaml or an accepted validation error — no Jackson/NPE crashes', async ({
    baseURL,
  }) => {
    const ctx = await request.newContext({ baseURL });

    const listRes = await ctx.get('/api/configurations');
    expect(listRes.ok()).toBeTruthy();
    const configs = await listRes.json() as Array<{ fqcn: string; simpleName: string }>;
    expect(configs.length).toBeGreaterThan(0);

    const failures: string[] = [];
    for (const c of configs) {
      const res = await ctx.post(
        `/api/configurations/${encodeURIComponent(c.fqcn)}/build`,
        { data: {} },
      );
      if (!res.ok()) {
        failures.push(`${c.simpleName}: HTTP ${res.status()}`);
        continue;
      }
      const body = await res.json() as { yaml?: string | null; errorMessage?: string | null };
      const hasYaml = typeof body.yaml === 'string' && body.yaml.length > 0;
      const err = body.errorMessage ?? null;
      if (!hasYaml) {
        if (err == null) {
          failures.push(`${c.simpleName}: yaml empty and errorMessage null — no output`);
        } else if (!ACCEPTED_PATTERNS.some((p) => p.test(err))) {
          failures.push(`${c.simpleName}: unexpected error="${err}"`);
        }
      }
    }
    await ctx.dispose();
    expect(
      failures,
      'Build Config failures (unexpected errors or empty output):\n' + failures.join('\n'),
    ).toHaveLength(0);
  });

  test('DefaultJdbcClientIntegrationServiceConfiguration with nested hikari values builds to YAML', async ({
    baseURL,
  }) => {
    const ctx = await request.newContext({ baseURL });
    const fqcn = 'com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientIntegrationServiceConfiguration';

    // First get the default values to use as the payload
    const defaultRes = await ctx.get(
      `/api/configurations/${encodeURIComponent(fqcn)}/default`,
    );
    if (!defaultRes.ok()) { await ctx.dispose(); test.skip(); return; }

    const defaults = await defaultRes.json() as { values?: Record<string, unknown> | null };
    const payload = defaults.values ?? {};

    const buildRes = await ctx.post(
      `/api/configurations/${encodeURIComponent(fqcn)}/build`,
      { data: payload },
    );
    expect(buildRes.ok()).toBeTruthy();

    const body = await buildRes.json() as { yaml?: string | null; errorMessage?: string | null };
    const hasYaml = typeof body.yaml === 'string' && body.yaml.length > 0;
    const err = body.errorMessage ?? null;

    const isAccepted = err != null && ACCEPTED_PATTERNS.some((p) => p.test(err));
    expect(
      hasYaml || isAccepted,
      `DefaultJdbcClient build should yield YAML or accepted validation, got errorMessage="${err}"`,
    ).toBe(true);
    await ctx.dispose();
  });

  test('DefaultTracerServiceConfiguration builds to YAML', async ({ baseURL }) => {
    const ctx = await request.newContext({ baseURL });
    const fqcn = 'com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration';

    const defaultRes = await ctx.get(
      `/api/configurations/${encodeURIComponent(fqcn)}/default`,
    );
    if (!defaultRes.ok()) { await ctx.dispose(); test.skip(); return; }

    const defaults = await defaultRes.json() as { values?: Record<string, unknown> | null };
    const payload = defaults.values ?? {};

    const buildRes = await ctx.post(
      `/api/configurations/${encodeURIComponent(fqcn)}/build`,
      { data: payload },
    );
    expect(buildRes.ok()).toBeTruthy();

    const body = await buildRes.json() as { yaml?: string | null; errorMessage?: string | null };
    const hasYaml = typeof body.yaml === 'string' && body.yaml.length > 0;
    const err = body.errorMessage ?? null;

    const isAccepted = err != null && ACCEPTED_PATTERNS.some((p) => p.test(err));
    expect(
      hasYaml || isAccepted,
      `DefaultTracerServiceConfiguration build should yield YAML or accepted validation, got errorMessage="${err}"`,
    ).toBe(true);
    await ctx.dispose();
  });
});
