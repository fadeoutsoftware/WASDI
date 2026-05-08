NodeResource
============

Introduction
------------
Node Resource.

Hosts the API to manage WASDI compute nodes:

- List available nodes visible to the authenticated user.
- Get, create, and update node details (admin only).
- Share a node with another user and manage sharing permissions.

All endpoints are under the base path ``/node``.

All endpoints require a valid session via the ``x-session-token`` header. The node detail, create, and update endpoints are restricted to admin users.

Common Models
-------------
- NodeViewModel: nodeCode, cloudProvider, apiUrl
- NodeFullViewModel: nodeCode, cloudProvider, apiUrl, nodeDescription, nodeGeoserverAddress, active, shared
- NodeSharingViewModel: nodeCode, userId
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue
- ErrorResponse: message

APIs
----

GET /node/allnodes
^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of WASDI nodes visible to the authenticated user. A node is included if it is active and at least one of the following is true: the user is an admin, the node is flagged as publicly shared, the node has been explicitly shared with the user, or it is the user's default node. Non-active nodes are excluded unless the ``all=true`` flag is passed (admin only relevant in practice).
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - all (boolean, optional) — if true, also includes inactive nodes; default false
- Body: none
- Success:
  - 200 OK, body: list of NodeViewModel (nodeCode, cloudProvider, apiUrl)
- Notes:
  - Returns null on invalid session rather than an HTTP error code.
- Return codes:
  - 200 OK (null body on invalid session)

GET /node
^^^^^^^^^^
- Description: Returns the full details of a specific node. Restricted to admin users.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - node (string, required) — node code
- Body: none
- Success:
  - 200 OK, body: NodeFullViewModel
- Return codes:
  - 200 OK
  - 400 Bad Request (node not found)
  - 401 Unauthorized (invalid session or not admin)
  - 500 Internal Server Error

POST /node
^^^^^^^^^^^
- Description: Creates a new WASDI node. Restricted to admin users. The node code must be unique — if a node with the same code already exists the request is rejected.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: NodeFullViewModel (required): nodeCode, cloudProvider, apiUrl, nodeDescription, nodeGeoserverAddress, active, shared
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (missing nodeCode, or a node with that code already exists)
  - 401 Unauthorized (invalid session or not admin)
  - 500 Internal Server Error

PUT /node
^^^^^^^^^^
- Description: Updates an existing WASDI node. Restricted to admin users. The node identified by ``nodeCode`` must already exist.
- HTTP Verb: PUT
- Headers: x-session-token
- Query params: none
- Body: NodeFullViewModel (required): nodeCode, cloudProvider, apiUrl, nodeDescription, nodeGeoserverAddress, active, shared
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (missing nodeCode, or node not found)
  - 401 Unauthorized (invalid session or not admin)
  - 500 Internal Server Error

PUT /node/share/add
^^^^^^^^^^^^^^^^^^^^
- Description: Shares a node with another user. Only admin users can share nodes. If the node is already shared with the destination user the call returns successfully without creating a duplicate entry. A notification e-mail is sent to the destination user. The ``rights`` parameter defaults to ``read`` if not supplied or invalid.
- HTTP Verb: PUT
- Headers: x-session-token
- Query params:
  - node (string, required) — node code to share
  - userId (string, required) — user ID to share the node with
  - rights (string, optional) — access rights (e.g. ``read``, ``write``); defaults to ``read``
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=``"Done"`` or ``"Already Shared."``)
- Return codes:
  - 200 OK (check boolValue; intValue carries the HTTP status on error)
  - 200 OK with intValue=401 (invalid session)
  - 200 OK with intValue=400 (invalid node or destination user not found)
  - 200 OK with intValue=403 (caller is not admin)
  - 200 OK with intValue=500 (server error)

GET /node/share/bynode
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of users that have the specified node shared with them.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - node (string, required) — node code
- Body: none
- Success:
  - 200 OK, body: list of NodeSharingViewModel (nodeCode, userId)
- Notes:
  - Returns an empty list on invalid session or error rather than an HTTP error code.
- Return codes:
  - 200 OK

DELETE /node/share/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes the sharing permission of a specific user for the given node.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
  - node (string, required) — node code
  - userId (string, required) — user ID whose sharing permission should be removed
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=``"Done"``)
- Return codes:
  - 200 OK (check boolValue; intValue carries the HTTP status on error)
  - 200 OK with intValue=401 (invalid session)
  - 200 OK with intValue=400 (destination user not found)
  - 200 OK with intValue=500 (server error)
