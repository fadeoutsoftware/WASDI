ImagesResource
==============

Introduction
------------
Images Resource.

Hosts the API to upload, retrieve, check and delete images in WASDI. Images can be associated to different entities (User, Processor, Organization, etc.) and are organised into named **collections** (e.g. ``processors``, ``users``). Within a collection an optional **folder** further sub-divides images.

The API covers:

- Generic upload / get / exists / delete for any valid collection.
- Convenience wrappers for the processor logo and gallery (for backward compatibility).

All endpoints are under the base path ``/images``.

All endpoints require a valid session via the ``x-session-token`` header (or the ``token`` query parameter for browser-friendly GET calls).

Notes on collections and permissions:

- Only collections declared in ``ImagesCollections`` are accepted; any other value returns 400.
- Write access is checked per collection/folder/image name via ``PermissionsUtils.canUserWriteImage``.
- Read access is checked via ``PermissionsUtils.canUserAccessImage``.
- Path traversal characters (``/``, ``\``) in collection, folder or image name are rejected with 400.

Common Models
-------------
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue

APIs
----

POST /images/upload
^^^^^^^^^^^^^^^^^^^^
- Description: Uploads an image file to the specified collection and optional sub-folder. The image is saved under the given name. If an older image with the same name but a different extension exists it is removed first. Images larger than the configured maximum size are rejected and deleted after upload. Accepted file extensions are those declared in ``ImageResourceUtils``.
- HTTP Verb: POST
- Headers: x-session-token
- Content-Type: multipart/form-data
- Form fields:
  - image (file, required) — the image binary
- Query params:
  - collection (string, required) — target image collection identifier
  - folder (string, optional) — sub-folder within the collection
  - name (string, required) — desired file name (no path separators allowed)
  - resize (boolean, optional) — reserved for future use (currently disabled)
  - thumbnail (boolean, optional) — reserved for future use (currently disabled)
- Body: multipart/form-data
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid collection, missing name, invalid extension, path traversal attempt, or image too large)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot write to this collection/folder/name)
  - 500 Internal Server Error

GET /images/get
^^^^^^^^^^^^^^^^
- Description: Returns the raw bytes of the requested image. Session can be provided via the ``x-session-token`` header or the ``token`` query parameter (useful for ``<img>`` tags). For large image collections (e.g. Globathy) the response is streamed instead of buffered. Returns 204 No Content when the image file is not found on disk.
- HTTP Verb: GET
- Headers: x-session-token (or ``token`` query param)
- Query params:
  - collection (string, required) — image collection identifier
  - folder (string, optional) — sub-folder within the collection
  - name (string, required) — image file name
  - token (string, optional) — alternative way to pass the session token
- Body: none
- Success:
  - 200 OK, body: image byte stream (content type determined by the file)
- Return codes:
  - 200 OK
  - 204 No Content (image file not found on disk)
  - 400 Bad Request (invalid collection, missing name, or path traversal attempt)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot access this image)
  - 500 Internal Server Error

GET /images/exists
^^^^^^^^^^^^^^^^^^^
- Description: Checks whether an image exists in the specified collection and folder without returning its content.
- HTTP Verb: GET
- Headers: x-session-token (or ``token`` query param)
- Query params:
  - collection (string, required) — image collection identifier
  - folder (string, optional) — sub-folder within the collection
  - name (string, required) — image file name
  - token (string, optional) — alternative way to pass the session token
- Body: none
- Success:
  - 200 OK (image exists)
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid collection, missing name, or path traversal attempt)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot access this image)
  - 404 Not Found (image does not exist on disk)
  - 500 Internal Server Error

DELETE /images/delete
^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes an image from the specified collection and folder.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
  - collection (string, required) — image collection identifier
  - folder (string, optional) — sub-folder within the collection
  - name (string, required) — image file name to delete
- Body: none
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid collection, missing name, or path traversal attempt)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot write to this image)
  - 404 Not Found (image file does not exist)
  - 500 Internal Server Error

POST /images/processors/logo/upload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Convenience wrapper to upload or replace the logo of a processor. Delegates to ``POST /images/upload`` using the ``processors`` collection and the processor's name as the sub-folder. On success, the processor's ``logo`` field is updated in the database. Requires write access to the processor.
- HTTP Verb: POST
- Headers: x-session-token
- Content-Type: multipart/form-data
- Form fields:
  - image (file, required) — the logo image binary
- Query params:
  - processorId (string, required) — ID of the processor
- Body: multipart/form-data
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot write to the processor)
  - 404 / 500 (processor not found or server error)

POST /images/processors/gallery/upload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Convenience wrapper to add a new image to the gallery of a processor. The file is automatically assigned the next available gallery slot name. Returns the relative image link in the response body. Requires write access to the processor. Returns 400 when the maximum number of gallery images is already reached.
- HTTP Verb: POST
- Headers: x-session-token
- Content-Type: multipart/form-data
- Form fields:
  - image (file, required) — the gallery image binary
- Query params:
  - processorId (string, required) — ID of the processor
- Body: multipart/form-data
- Success:
  - 200 OK, body: PrimitiveResult (stringValue = relative image URL)
- Return codes:
  - 200 OK
  - 400 Bad Request (maximum gallery images reached)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (user cannot write to the processor)
  - 404 Not Found (processor not found)
