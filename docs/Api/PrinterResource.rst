PrinterResource
===============

Introduction
------------
Printer Resource.

Hosts the API used to submit a map-print request to the external WASDI print service and then retrieve the rendered output.

The resource supports:

- Storing a map print job and obtaining a UUID for later retrieval.
- Fetching the generated print output as PDF or PNG.

All endpoints are under the base path ``/print``.

Behavior notes:

- This resource proxies requests to the external print server configured in ``WasdiConfig.Current.printServerAddress``.
- Unlike most other resources in this API set, these endpoints do not validate a WASDI session token.
- Upstream failures are surfaced as ``502 Bad Gateway`` responses.

Common Models
-------------
- PrinterViewModel: baseMap, title, description, zoomLevel, center, format, wmsLayers, wkts
- PrinterViewModel.Center: lat, lng
- PrinterViewModel.WmsLayer: name, layerId, wmsUrl
- PrinterViewModel.Wkt: name, geom

APIs
----

POST /print/storemap
^^^^^^^^^^^^^^^^^^^^
- Description: Stores a print job definition in the external print service and returns the UUID that identifies the stored map. The request body contains the map base layer, map center, output format, and optional overlay definitions such as WMS layers and WKT geometries.
- HTTP Verb: POST
- Headers: none
- Content-Type: application/json
- Query params: none
- Body: PrinterViewModel
	- baseMap (string, required) — selected base map identifier
	- title (string, optional) — map title
	- description (string, optional) — map description/subtitle
	- zoomLevel (integer, optional) — map zoom level
	- center (object, required) — map center
		- lat (number)
		- lng (number)
	- format (string, required) — output format, only ``pdf`` or ``png`` accepted
	- wmsLayers (array of WmsLayer, optional)
		- name (string)
		- layerId (string)
		- wmsUrl (string)
	- wkts (array of Wkt, optional)
		- name (string)
		- geom (string)
- Success:
	- 200 OK, body: UUID string returned by the external print service
- Notes:
	- The request is rejected if the body is missing, ``baseMap`` is blank, ``center`` is missing or has both coordinates equal to ``0.0``, or ``format`` is not ``pdf``/``png``.
	- If the external service responds with 200 but does not return a valid UUID field, this endpoint returns a server error.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid or incomplete request body, or JSON serialization/parsing issue)
	- 502 Bad Gateway (external print service failed)
	- 500 Internal Server Error

GET /print
^^^^^^^^^^
- Description: Fetches the generated print result from the external print service using the UUID obtained from ``POST /print/storemap``. The resource returns the upstream binary payload directly and preserves the upstream content type.
- HTTP Verb: GET
- Headers: none
- Query params:
	- uuid (string, required) — identifier of the stored print job
- Body: none
- Success:
	- 200 OK, body: binary PDF or PNG content
- Notes:
	- The response sets ``Content-Disposition: inline`` and chooses the file extension from the upstream ``Content-Type``.
	- Supported upstream content types are effectively ``application/pdf`` and ``image/png``.
- Return codes:
	- 200 OK
	- 400 Bad Request (missing UUID)
	- 502 Bad Gateway (external print service failed to return the file)
	- 500 Internal Server Error
