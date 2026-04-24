ConsoleResource
===============

Introduction
------------
Console Resource.

Hosts the API to create and manage Jupyter Notebook instances inside a WASDI workspace:

- Create a Jupyter Notebook for a workspace (starts the Docker container if needed).
- Check whether the notebook container is active (running and reachable).
- Check whether the notebook is ready (active and up-to-date).

All endpoints are under the base path ``/console``.

All endpoints require a valid session via the ``x-session-token`` header and a workspace the user can access. The user must also have a valid active subscription.

Common Models
-------------
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue

APIs
----

POST /console/create
^^^^^^^^^^^^^^^^^^^^^
- Description: Creates or resumes a Jupyter Notebook instance for the given workspace. If a notebook already exists and is reachable, its URL is returned immediately. If the IP of the calling client is new, the Traefik firewall rule is updated to allow it. If the notebook is absent or stale, a new Docker container is launched asynchronously via the WASDI launcher. Requires a valid subscription.
- HTTP Verb: POST
- Headers: x-session-token
- Query params:
  - workspaceId (string, required) — ID of the target workspace
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=notebook URL or ``"PATIENCE IS THE VIRTUE OF THE STRONG"`` while the container is starting)
- Notes:
  - When the container is still starting, stringValue contains a placeholder message and the client should poll ``/console/active`` or ``/console/ready``.
  - Returns PrimitiveResult with boolValue=false and an intValue set to an HTTP status code on failure (not an HTTP error response).
- Return codes:
  - 200 OK (check boolValue; intValue carries the HTTP status on error)
  - 200 OK with intValue=401 (invalid session)
  - 200 OK with intValue=403 (no workspace access or no valid subscription)
  - 200 OK with intValue=400 (workspace not found)

GET /console/active
^^^^^^^^^^^^^^^^^^^^
- Description: Checks whether the Jupyter Notebook container for the specified workspace is currently up and reachable. Returns the notebook URL when active.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - workspaceId (string, required) — ID of the target workspace
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=notebook URL when active; boolValue=false, stringValue=error message when inactive)
- Notes:
  - Returns PrimitiveResult with boolValue=false and intValue set to an HTTP status code on validation errors.
- Return codes:
  - 200 OK (check boolValue; intValue carries the HTTP status on error)
  - 200 OK with intValue=401 (invalid session)
  - 200 OK with intValue=403 (no workspace access)
  - 200 OK with intValue=400 (missing or unknown workspace)

GET /console/ready
^^^^^^^^^^^^^^^^^^^
- Description: Checks whether the Jupyter Notebook container is both active (reachable) and up-to-date. Returns the notebook URL when both conditions are met. Returns a standard HTTP error code (not just a PrimitiveResult) on access or session failures.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - workspaceId (string, required) — ID of the target workspace
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=notebook URL when ready; boolValue=false when not ready)
- Return codes:
  - 200 OK
  - 400 Bad Request (missing workspaceId or workspace not found)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (no workspace access)
  - 500 Internal Server Error
