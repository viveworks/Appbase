# Task 3 Report: core-service.md

## Status: DONE

## Commit

- Initial commit: `5c21367c` — feat(P-002/Task-3): add core-service.md recipe for Core layer
- Trim commit: `539001cc` — feat(P-002/Task-3): trim core-service.md to 296 lines (minor whitespace cleanup)
- Final commit hash: `539001cc`

## Line Count

296 lines (target ≤ 300).

## File Created

`appbase-java/dev-tools/skills/add-appbase-service/core-service.md`

## Content Summary

Covers all 9 new files and 8 existing-file modifications:
- Files 1–4: XxxService interface, DefaultXxxService impl, DefaultXxxServiceConfiguration (Jackson Builder + @Valid), DefaultXxxServiceConfigurationValidator
- Files 5–6: XxxServiceMXBean (@MXBean interface), JmxXxxService (AbstractAppbaseServiceMXBean impl)
- Files 7–8: XxxServlet (HTTP management servlet), XxxJaxrsResource (JAX-RS + OpenAPI)
- File 9: XxxServiceFactory (AppbaseCoreServiceFactory impl)
- M1–M8: AppbaseCoreServices, AppbaseCoreServicesConfiguration, AppbaseCoreServicesAdapter, JettyAppbaseServer, AppbaseServiceConstants, AppbaseServletRegistrar, AppbaseCore, DefaultAppbaseCore
- Tests 9–12: DefaultXxxServiceTest, ConfigurationValidatorTest, JmxXxxServiceTest, XxxServiceGuiceModuleTest

## Constraints Applied

- No `T`/`V` substitution tokens (Core layer only uses `Xxx`/`xxx`)
- Constructor parameter named `builder`
- All imports on separate lines
- One statement per line in constructor/method bodies
- Factory constructors: one statement per line
- Stubs throw UnsupportedOperationException with TODO message
- Ends with "Stub quality contract" section

## Concerns

None. File is self-contained, under 300 lines, and all required sections present.
