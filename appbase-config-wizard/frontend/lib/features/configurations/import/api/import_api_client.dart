import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../features/configurations/api/configurations_api_client.dart'
    show ApiClient, ApiException, kBrowserTokenHeader;
import '../../../../shared/security/browser_token.dart'
    if (dart.library.html) '../../../../shared/security/browser_token_web.dart';
import '../models/models.dart';

/// Typed HTTP client for the import-specific REST endpoints.
///
/// All endpoints are relative to [baseUrl]. Pass an empty string (the default)
/// when the frontend is served from the same origin as the API.
class ImportApiClient {
  final http.Client _client;

  /// Origin prepended to every import endpoint; empty means same-origin.
  final String baseUrl;

  /// Bounded deadline for every request (A10-05, internal work item 1449/internal work item 1492) — see
  /// [ApiClient.timeout]'s doc for why this exists.
  final Duration timeout;

  /// Creates a client with optional transport, origin, and request deadline overrides.
  ImportApiClient({http.Client? client, this.baseUrl = '', this.timeout = const Duration(seconds: 30)})
      : _client = client ?? http.Client();

  /// Builds request headers, adding the per-run browser token from the cookie the
  /// server planted (A10-01, internal work item 1449). [extra] carries per-request headers.
  Map<String, String> _headers([Map<String, String>? extra]) {
    final headers = <String, String>{...?extra};
    final token = readBrowserToken();
    if (token != null && token.isNotEmpty) {
      headers[kBrowserTokenHeader] = token;
    }
    return headers;
  }

  /// Sends `GET /api/configurations/import/scan?dir=<dir>`.
  ///
  /// Returns a list of YAML files found in [dir] with type candidates.
  Future<ImportScanResult> scan(String dir) async {
    final uri = Uri.parse('$baseUrl/api/configurations/import/scan')
        .replace(queryParameters: {'dir': dir});
    final response = await _client.get(uri, headers: _headers()).timeout(timeout);
    _assertOk(response);
    return ImportScanResult.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// POST /api/configurations/import/{fqcn}/write
  ///
  /// Writes the supplied [fieldValues] as a YAML file for the given [fqcn].
  /// Optionally specify [sourceFile] (original YAML path to overwrite) or
  /// [outputPath] (custom destination path).
  Future<ImportWriteResult> write(
    String fqcn,
    Map<String, dynamic> fieldValues, {
    String? sourceFile,
    String? outputPath,
  }) async {
    final uri = Uri.parse(
        '$baseUrl/api/configurations/import/${Uri.encodeComponent(fqcn)}/write');
    final body = <String, dynamic>{'fieldValues': fieldValues};
    if (sourceFile != null) body['sourceFile'] = sourceFile;
    if (outputPath != null) body['outputPath'] = outputPath;
    final response = await _client
        .post(
          uri,
          headers: _headers({'Content-Type': 'application/json'}),
          body: jsonEncode(body),
        )
        .timeout(timeout);
    _assertOk(response);
    return ImportWriteResult.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// Closes the underlying [http.Client] when the provider is disposed.
  void dispose() => _client.close();

  void _assertOk(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
          statusCode: response.statusCode, body: response.body);
    }
  }
}
