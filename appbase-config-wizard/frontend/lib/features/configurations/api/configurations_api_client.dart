import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../shared/security/browser_token.dart'
    if (dart.library.html) '../../../shared/security/browser_token_web.dart';
import '../models/models.dart';

/// Header the wizard UI sends carrying the per-run browser token (A10-01, internal work item 1449).
const String kBrowserTokenHeader = 'X-Appbase-Wizard-Token';

/// Typed HTTP client for the appbase-config-wizard REST API.
///
/// All endpoints are relative to [baseUrl]. Pass an empty string (the default)
/// when the frontend is served from the same origin as the API, so that
/// relative URLs are resolved automatically by the browser.
class ApiClient {
  final http.Client _client;

  /// Origin prepended to every configuration endpoint; empty means same-origin.
  final String baseUrl;

  /// Bounded deadline for every request (A10-05, internal work item 1449/internal work item 1492) — without one,
  /// a stalled connection (dead backend, proxy hang) leaves the caller's
  /// Future, and therefore any UI awaiting it, loading forever. Every call
  /// below wraps its request in `.timeout(timeout)`, which throws a
  /// [TimeoutException] the caller's existing error handling already
  /// surfaces deterministically — never a silent, indefinite hang.
  final Duration timeout;

  /// Creates a client with optional transport, origin, and request deadline overrides.
  ApiClient({http.Client? client, this.baseUrl = '', this.timeout = const Duration(seconds: 30)})
      : _client = client ?? http.Client();

  /// Builds the request headers, adding the per-run browser token from the cookie
  /// the server planted (A10-01, internal work item 1449) so same-origin `/api` calls are accepted.
  /// [extra] carries per-request headers such as `Content-Type`.
  Map<String, String> _headers([Map<String, String>? extra]) {
    final headers = <String, String>{...?extra};
    final token = readBrowserToken();
    if (token != null && token.isNotEmpty) {
      headers[kBrowserTokenHeader] = token;
    }
    return headers;
  }

  /// GET /api/configurations → list of config summaries.
  Future<List<ConfigSummary>> listConfigs() async {
    final response = await _client
        .get(Uri.parse('$baseUrl/api/configurations'), headers: _headers())
        .timeout(timeout);
    _assertOk(response);
    final list = jsonDecode(response.body) as List<dynamic>;
    return list
        .map((e) => ConfigSummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/configurations/{fqcn} → config detail with fields and defaults.
  Future<ConfigDetail> getConfig(String fqcn) async {
    final uri = Uri.parse('$baseUrl/api/configurations/${Uri.encodeComponent(fqcn)}');
    final response = await _client.get(uri, headers: _headers()).timeout(timeout);
    _assertOk(response);
    return ConfigDetail.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// POST /api/configurations/{fqcn}/build → YAML string or error message.
  Future<BuildResult> buildConfig(
      String fqcn, Map<String, dynamic> values) async {
    final uri = Uri.parse(
        '$baseUrl/api/configurations/${Uri.encodeComponent(fqcn)}/build');
    final response = await _client
        .post(
          uri,
          headers: _headers({'Content-Type': 'application/json'}),
          body: jsonEncode(values),
        )
        .timeout(timeout);
    _assertOk(response);
    return BuildResult.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// POST /api/configurations/{fqcn}/write → file path or error message.
  Future<WriteResult> writeConfig(String fqcn, Map<String, dynamic> values,
      {String? outputPath}) async {
    final uri = Uri.parse(
        '$baseUrl/api/configurations/${Uri.encodeComponent(fqcn)}/write');
    final body = <String, dynamic>{'values': values};
    if (outputPath != null) body['outputPath'] = outputPath;
    final response = await _client
        .post(
          uri,
          headers: _headers({'Content-Type': 'application/json'}),
          body: jsonEncode(body),
        )
        .timeout(timeout);
    _assertOk(response);
    return WriteResult.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// GET /api/configurations/{fqcn}/default → map of default field values.
  Future<Map<String, dynamic>> getDefaultValues(String fqcn) async {
    final uri = Uri.parse(
        '$baseUrl/api/configurations/${Uri.encodeComponent(fqcn)}/default');
    final response = await _client.get(uri, headers: _headers()).timeout(timeout);
    _assertOk(response);
    final body = jsonDecode(response.body) as Map<String, dynamic>;
    return (body['values'] as Map<String, dynamic>?) ?? {};
  }

  /// Closes the underlying [http.Client]. Call this when the [ApiClient] is no
  /// longer needed to release socket resources.
  void dispose() => _client.close();

  void _assertOk(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
          statusCode: response.statusCode, body: response.body);
    }
  }
}

/// Thrown when the API returns a non-2xx status code.
class ApiException implements Exception {
  /// HTTP status returned by the configuration service.
  final int statusCode;

  /// Unmodified response body supplied for diagnostics.
  final String body;

  /// Creates an exception for a non-successful API response.
  const ApiException({required this.statusCode, required this.body});

  @override
  String toString() => 'ApiException($statusCode): $body';
}
