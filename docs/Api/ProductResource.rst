ProductResource
===============

Introduction
------------
Product Resource.

Hosts APIs to manage products in workspaces:

- Add/remove products to/from a workspace
- Retrieve product lists, names, metadata, and details
- Update product presentation fields (friendly name, style, description)
- Upload products (with or without launcher ingestion)

All endpoints are under base path ``/product``.

All endpoints require ``x-session-token``.

Common Models
-------------
- PrimitiveResult:
	- IntValue, StringValue, DoubleValue, BoolValue
- ProductViewModel:
	- name, fileName, productFriendlyName, metadata, bandsGroups, metadataFileReference, metadataFileCreated, style, description
- GeorefProductViewModel (extends ProductViewModel):
	- bbox
- MetadataViewModel:
	- name, elements (array of MetadataViewModel), attributes (array of AttributeViewModel)

APIs
----

GET /product/addtows
^^^^^^^^^^^^^^^^^^^^
- Description: Adds a product file already present on node filesystem to a workspace.
- Query params: name (required), workspace (required)
- Success: 200 OK, body: PrimitiveResult
- Notes: if product is already linked to workspace, returns ``BoolValue=true``; returns null on invalid session/permission failures.

GET /product/byname
^^^^^^^^^^^^^^^^^^^
- Description: Returns one product by file name in a workspace.
- Query params: name (required), workspace (required)
- Success: 200 OK, body: GeorefProductViewModel
- Notes: returns null on invalid session, no access, or not found.

GET /product/metadatabyname
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns metadata for a product.
- Query params: name (required), workspace (required)
- Success: 200 OK, body: MetadataViewModel
- Notes:
	- If metadata file is not available and was never generated, the API triggers a ``READMETADATA`` launcher process and returns a minimal metadata model with ``name = Generating Metadata, try later``.
	- Returns null on invalid session/access or errors.

GET /product/byws
^^^^^^^^^^^^^^^^^
- Description: Returns full product list for a workspace.
- Query params: workspace (required)
- Success: 200 OK, body: array of GeorefProductViewModel
- Notes: enriches output with bbox, style, description; metadata is cleared in returned list entries for lighter payload.

GET /product/bywslight
^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns light product list for a workspace (used for initial editor listing).
- Query params: workspace (required)
- Success: 200 OK, body: array of GeorefProductViewModel
- Notes: includes bbox and name/friendly name only.

GET /product/namesbyws
^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns only product file names for a workspace.
- Query params: workspace (required)
- Success: 200 OK, body: array of string

POST /product/update
^^^^^^^^^^^^^^^^^^^^
- Description: Updates editable product fields in a workspace.
- Query params: workspace (required)
- Body: ProductViewModel
- Success: 200 OK
- Return codes: 200, 401, 403, 500
- Notes:
	- Update scope is effectively friendly name, style, and description.
	- If style changes, the service attempts to update associated published layers in GeoServer.

POST /product/uploadfile
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Uploads a file and triggers ingestion (launcher ``INGEST`` operation).
- Consumes: ``multipart/form-data``
- Form field: file
- Query params: workspace (required), name (optional), style (optional), platform (optional)
- Success: 200 OK
- Return codes: 200, 400, 401, 403, 500
- Notes:
	- Includes path-injection guard on ``name`` and ``workspace``.
	- If ``name`` is empty, a random default name is generated.

POST /product/uploadfilebylib
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Uploads a file without triggering ingestion (library-oriented endpoint).
- Consumes: ``multipart/form-data``
- Form field: file
- Query params: workspace (required), name (optional)
- Success: 200 OK
- Return codes: 200, 400, 401, 403, 500
- Notes: includes path-injection guard on ``name`` and ``workspace``.

GET /product/delete
^^^^^^^^^^^^^^^^^^^
- Description: Deletes a product from a workspace; can also delete disk files and published layers.
- Query params: name (required), workspace (required), deletefile (optional bool, default true), deletelayer (optional bool, default true)
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- Handles associated sidecar data for known formats (for example ``.dim/.data``, ``.grib`` indexes, shapefile sidecars).
	- Updates workspace storage size after file deletion.
	- Sends asynchronous delete notification on RabbitMQ.

POST /product/deletelist
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Starts asynchronous deletion of multiple products.
- Consumes: ``application/json``
- Query params: workspace (required), deletefile (optional), deletelayer (optional)
- Body: array of product names
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- Uses background worker (``DeleteProductWorker``) and returns immediately.
	- Returns ``BoolValue=true, IntValue=200`` when worker is started; returns ``BoolValue=false, IntValue=500`` on validation/failure.
