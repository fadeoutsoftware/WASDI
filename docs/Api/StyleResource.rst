StyleResource
=============

Introduction
------------
Style Resource.

Hosts APIs to manage SLD styles in WASDI.

Main capabilities:

- Upload and update style files
- Read and update style XML and metadata
- List styles available to current user
- Share styles with users and manage sharings
- Download styles
- Delete styles (main-node and compute-node flows)

All endpoints are under base path /styles.

Authentication:

- Most endpoints require x-session-token.
- Download endpoints also support token query parameter for browser-driven downloads.

Common Models
-------------
- PrimitiveResult:
	- IntValue, StringValue, DoubleValue, BoolValue
- StyleViewModel:
	- styleId, name, description, isPublic, userId, readOnly, imgLink, sharedWithMe
- StyleSharingViewModel:
	- userId, permissions

APIs
----

POST /styles/uploadfile
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Uploads a new SLD style file, creates DB record, publishes to GeoServer, and generates preview image.
- Consumes: multipart/form-data
- Form field: file
- Query params: name (required), description (optional), public (optional boolean)
- Success: 200 OK, body: PrimitiveResult
- Return codes: 200, 400, 401, 500
- Notes: style name must be unique.

POST /styles/updatefile
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Replaces style SLD content for an existing style; can accept plain SLD or zipped payload.
- Consumes: multipart/form-data
- Form field: file
- Query params: styleId (required), zipped (optional boolean, default false)
- Success: 200 OK
- Return codes: 200, 400, 401, 403, 404, 304, 500
- Notes:
	- On main node, asynchronously propagates update to compute nodes.
	- Updates style in GeoServer if present, then refreshes preview image.

GET /styles/getxml
^^^^^^^^^^^^^^^^^^
- Description: Returns XML content of a style SLD file.
- Query params: styleId (required)
- Produces: application/xml
- Success: 200 OK, body: XML string
- Return codes: 200, 401, 403, 404, 304, 500

POST /styles/updatexml
^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates style by XML text content (wrapper around updatefile).
- Query params: styleId (required)
- Form field: styleXml
- Success: 200 OK
- Return codes: inherited from updatefile

POST /styles/updateparams
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates style metadata fields.
- Query params: styleId (required), description (optional), public (optional boolean)
- Success: 200 OK
- Return codes: 200, 400, 401, 403

GET /styles/getbyuser
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns styles visible to current user: own styles, public styles, and explicit sharings.
- Success: 200 OK, body: array of StyleViewModel
- Notes:
	- sharedWithMe and readOnly flags are computed from ownership and sharing permissions.
	- imgLink is set when preview image exists in styles image collection.

DELETE /styles/delete
^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes a style from main node.
- Query params: styleId (required)
- Success: 200 OK
- Return codes: 200, 400, 401, 403, 500
- Notes:
	- Main-node only endpoint.
	- If style is only shared to caller, API removes sharing for caller instead of deleting style globally.
	- Global delete removes sharings, DB style, propagates deletion to compute nodes, removes style in GeoServer, and deletes local SLD file.

DELETE /styles/nodedelete
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes style artifacts on a compute node.
- Query params: styleId (required), styleName (required)
- Success: 200 OK
- Return codes: 200, 400, 401, 403, 500
- Notes: compute-node only endpoint used by main-node propagation workflow.

PUT /styles/share/add
^^^^^^^^^^^^^^^^^^^^^
- Description: Shares a style with another user.
- Query params: styleId (required), userId (required), rights (optional; defaults to READ if invalid)
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- Prevents sharing with self (unless admin flow).
	- Validates target user and write/share permissions.
	- Sends notification email on success.

DELETE /styles/share/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes one style sharing for a user.
- Query params: styleId (required), userId (required)
- Success: 200 OK, body: PrimitiveResult
- Notes: deletion allowed for shared user, style owner, or admin.

GET /styles/share/bystyle
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Lists enabled style sharings for a style.
- Query params: styleId (required)
- Success: 200 OK, body: array of StyleSharingViewModel
- Notes: returns empty list on invalid session/style/errors.

GET /styles/download
^^^^^^^^^^^^^^^^^^^^
- Description: Downloads style SLD by style id.
- Query params: styleId (required), token (optional if x-session-token header is provided)
- Produces: application/octet-stream
- Success: 200 OK, streaming attachment
- Return codes: 200, 204, 401, 403, 500

GET /styles/downloadbyname
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Downloads style SLD by style name.
- Query params: style (required), token (optional if x-session-token header is provided)
- Produces: application/octet-stream
- Success: 200 OK, streaming attachment
- Return codes: 200, 204, 401, 403, 500
