OpenSearchResource
==================

Introduction
------------
Open Search Resource.

Hosts the API used to query external data providers and retrieve catalogue search results.

The resource supports:

- Counting results for a single query or a list of queries.
- Executing paginated searches against one query.
- Executing full multi-page searches for a list of queries.
- Listing configured data providers.

All endpoints are under the base path ``/search``.

All endpoints require a valid session via the ``x-session-token`` header.

Provider selection notes:

- If ``providers`` is omitted or set to ``AUTO``, WASDI selects the provider from the catalogue configuration for the mission extracted from the query.
- When a provider fails or returns an invalid count, the API may automatically try the next configured provider for the same platform.
- Mission access is checked from the parsed query platform; if the user cannot access that mission, search endpoints fail.

Common Models
-------------
- DataProviderViewModel: code, description, link
- QueryViewModel: parsed search structure including offset, limit, bounding box, dates, platformName, productType, productLevel, orbit filters, cloud coverage, sensorMode, productName, timeliness, polarisation, platformSerialIdentifier, instrument, filters
- QueryResultViewModel: preview, title, summary, id, link, footprint, provider, properties, volumeName, volumePath, platform

APIs
----

GET /search/query/count
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the total number of products matching a single query. The query string is parsed to detect the mission/platform, then the configured provider is selected. If ``providers=AUTO`` or not provided, the configured catalogue providers for that mission are tried in order until a valid count is returned.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- query (string, required) — WASDI search query string
	- providers (string, optional) — provider code, or ``AUTO`` to let the server choose; defaults to ``AUTO``
- Body: none
- Success:
	- 200 OK, body: integer total count
- Notes:
	- Returns ``-1`` on invalid session, permission failure, malformed input, or provider-side errors instead of using HTTP error codes.
- Return codes:
	- 200 OK (integer result, with ``-1`` meaning failure)

GET /search/query
^^^^^^^^^^^^^^^^^
- Description: Executes a paginated query against a provider and returns normalized search results. The provider is selected explicitly or via ``AUTO``. On success, each returned item is enriched with the detected platform; when the original request used ``AUTO``, the response ``provider`` field is set back to ``AUTO``.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- providers (string, optional) — provider code, or ``AUTO``; defaults to ``AUTO``
	- query (string, required) — WASDI search query string
	- offset (string, optional) — zero-based result offset
	- limit (string, optional) — maximum number of elements to return
	- sortedby (string, optional) — sort field
	- order (string, optional) — sort direction
- Body: none
- Success:
	- 200 OK, body: array of QueryResultViewModel
- Notes:
	- Returns ``null`` on invalid session, access denial, provider resolution failure, or execution errors rather than emitting HTTP error codes.
	- If a provider fails, the resource may try the next configured provider for the same mission.
- Return codes:
	- 200 OK (array body, or ``null`` on failure)

GET /search/providers
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of configured data providers from the WASDI configuration.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
	- 200 OK, body: array of DataProviderViewModel
- Notes:
	- ``description`` falls back to the provider ``code`` when not configured.
	- Returns ``null`` on invalid session or server-side errors.
- Return codes:
	- 200 OK (array body, or ``null`` on failure)

POST /search/query/countlist
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the total number of products found across multiple queries. Each query is processed independently, its mission is extracted, permissions are checked, and the server selects the best provider for that mission. The counts from all queries are summed.
- HTTP Verb: POST
- Headers: x-session-token
- Query params:
	- providers (string, optional) — provider code, or ``AUTO``; if omitted the server tries to resolve one from configuration
- Body: array of strings — each element is a WASDI search query string
- Success:
	- 200 OK, body: integer total count across all queries
- Notes:
	- Returns ``-1`` on invalid session, empty query list, mission access denial, malformed requests, or provider-side errors.
	- For each query, if the selected provider fails, the server may try the next provider configured for that mission.
- Return codes:
	- 200 OK (integer result, with ``-1`` meaning failure)

POST /search/querylist
^^^^^^^^^^^^^^^^^^^^^^
- Description: Executes a non-paginated logical search for multiple queries by internally paging through provider results until all available products have been retrieved. Results from all queries are merged into one array, with duplicate items removed based on provider, link, and title. The resource uses a provider-specific page size when configured; otherwise the default page size is 100.
- HTTP Verb: POST
- Headers: x-session-token
- Query params:
	- providers (string, optional) — provider code, or ``AUTO``; defaults to ``AUTO``
- Body: array of strings — each element is a WASDI search query string
- Success:
	- 200 OK, body: array of QueryResultViewModel
- Notes:
	- Returns ``null`` on invalid session, empty query list, access denial, or unrecoverable execution errors.
	- The API first counts the results for each provider, then fetches them page by page.
	- A safety cap is applied to the internal page loop to avoid runaway pagination.
	- Before returning, the ``provider`` field of every result is set to the original request value, including ``AUTO``.
- Return codes:
	- 200 OK (array body, or ``null`` on failure)
