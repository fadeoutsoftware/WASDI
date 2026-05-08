FileBufferResource
==================

Introduction
------------
File Buffer Resource.

Hosts the API for:

- Importing new files into WASDI from data providers (download).
- Publishing bands on GeoServer.
- Sharing files between workspaces.

All endpoints are under the base path ``/filebuffer``.

All endpoints require a valid session via the ``x-session-token`` header. The import (POST) endpoint additionally requires a valid active subscription.

Common Models
-------------
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue
- ImageImportViewModel: fileUrl, name, provider, workspace, bbox, parent, volumeName, volumePath, platform
- RabbitMessageViewModel: messageCode, messageResult, payload, workspaceId

APIs
----

GET /filebuffer/share
^^^^^^^^^^^^^^^^^^^^^^
- Description: Triggers an asynchronous copy of a product from one workspace to another. Validates that the calling user can read the origin workspace and write to the destination workspace, then enqueues a SHARE launcher operation. If the product is already present in the destination workspace the call returns immediately with ``PRODUCT_ALREADY_PRESENT``.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - originWorkspaceId (string, required) — ID of the workspace that owns the file
  - destinationWorkspaceId (string, required) — ID of the workspace to copy the file into
  - productName (string, required) — name of the product to share
  - parent (string, optional) — process workspace ID of the parent process
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=processObjId or ``"PRODUCT_ALREADY_PRESENT"``)
- Notes:
  - Path traversal characters (``/``, ``\``) in workspace IDs or product name are rejected with 400.
- Return codes:
  - 200 OK
  - 400 Bad Request (missing/invalid workspace IDs or product name, or product not found in origin)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot read origin or write destination workspace)
  - 500 Internal Server Error

GET /filebuffer/download
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deprecated convenience wrapper around ``POST /filebuffer/download``. Accepts the same parameters as query strings and delegates to the POST version. Kept for backward compatibility.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - fileUrl (string, optional) — URL of the file to import
  - name (string, optional) — target file name
  - provider (string, optional) — data provider code; defaults to the node default provider
  - workspace (string, required) — target workspace ID
  - bbox (string, optional) — WKT footprint / bounding box
  - parent (string, optional) — process workspace ID of the parent process
  - platform (string, optional) — satellite platform type (e.g. ``S1``, ``S2``)
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=processObjId or ``"DONE"`` if already present)
- Notes:
  - Delegates entirely to ``POST /filebuffer/download``; see that endpoint for full behaviour and return codes.
- Return codes:
  - Same as POST /filebuffer/download

POST /filebuffer/download
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Triggers an asynchronous import of a remote file into a WASDI workspace. Before starting the download, the server checks whether the file is already available on the target node and, if so, returns immediately with ``DONE`` without re-downloading. Requires the user to have write access to the workspace and a valid active subscription. Also validates that the user is allowed to access the satellite mission of the product.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: ImageImportViewModel (required): fileUrl, name, provider, workspace, bbox, parent, platform, volumeName, volumePath
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=processObjId when enqueued; stringValue=``"DONE"`` when already available)
- Notes:
  - If ``provider`` is empty, the node's default provider is used.
  - If ``platform`` is empty, it is inferred from the file name.
  - Returns boolValue=false with intValue set to an HTTP-like status code on failure rather than a proper HTTP error response.
- Return codes:
  - 200 OK (check boolValue; intValue carries the HTTP status on error)
  - 200 OK with intValue=401 (invalid session, no valid subscription, or no mission access)
  - 200 OK with intValue=403 (no workspace write access)
  - 200 OK with intValue=500 (server error)

GET /filebuffer/publishband
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Publishes a specific band of a product on GeoServer. If the band has already been published, returns the existing layer information immediately via a RabbitMessageViewModel without re-publishing. Otherwise enqueues an asynchronous PUBLISHBAND launcher operation. An optional default style can be supplied; if omitted, any default style stored in the product metadata is used.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - fileUrl (string, required) — file name of the product
  - workspace (string, required) — workspace ID containing the product
  - band (string, required) — name of the band to publish
  - style (string, optional) — GeoServer style to apply; falls back to the product's default style
  - parent (string, optional) — process workspace ID of the parent process
- Body: none
- Success:
  - 200 OK, body: RabbitMessageViewModel
    - When already published: messageCode=``"PUBLISHBAND"``, payload=PublishBandResultViewModel (boundingBox, bandName, layerId, productName, geoserverBoundingBox, geoserverUrl)
    - When enqueued: messageCode=``"WAITFORRABBIT"``, payload=processObjId
- Notes:
  - Returns ``null`` on invalid session, no workspace access, or product not found (does not return an HTTP error code).
- Return codes:
  - 200 OK (null body on session/access/product failures)
