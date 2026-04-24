AdminDashboardResource
======================

Introduction
------------
Admin Dashboard Resource.

Host the API for the Admin backend:

- Find users, workspaces, and processors with partial names.
- Store and read metrics entries.

Metrics are pushed by each node to the main one to let WASDI decide the best node at runtime.

All endpoints in this resource are under base path /admin.

Unless differently noted, endpoints:

- Require header x-session-token.
- Produce application/json, application/xml, and text/xml.
- Return ErrorResponse on handled errors.

View models used by this resource are in:

- wasdishared/src/main/java/wasdi/shared/viewmodels

Common Models
-------------
- ErrorResponse: message
- SuccessResponse: message
- UserViewModel: userId, name, surname, type, role, publicNickName
- WorkspaceListInfoViewModel: workspaceId, workspaceName, ownerUserId, nodeCode, sharedUsers, creationDate
- GenericResourceViewModel: resourceType, resourceId, resourceName, userId
- DeployedProcessorViewModel: processorId, processorName, processorVersion, processorDescription, publisher, type, isPublic
- UserResourcePermissionViewModel: resourceId, resourceType, userId, ownerId, permissions, createdBy, createdDate
- MetricsEntry: node, timestamp, cpu, disks, memory, licenses
- UserListViewModel: userId, active, type, lastLogin, name, surname, publicNickName
- UsersSummaryViewModel: totalUsers, noneUsers, freeUsers, standardUsers, proUsers, organizations
- FullUserViewModel: userId, name, surname, link, type, role, active, defaultNode, registrationDate, confirmationDate, lastLogin, description, publicNickName

APIs
----

GET /admin/usersByPartialName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a list of users whose e-mail or name matches the given partial string. The search string must be at least 3 characters long. Only accessible to admin users.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - partialName (string, required, minimum length 3)
- Body: none
- Success:
  - 200 OK, body: list of UserViewModel
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid partialName)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

GET /admin/workspacesByPartialName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a list of workspaces whose name matches the given partial string. The search string must be at least 3 characters long. Only accessible to admin users.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - partialName (string, required, minimum length 3)
- Body: none
- Success:
  - 200 OK, body: list of WorkspaceListInfoViewModel
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid partialName)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)

GET /admin/resourceByPartialName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a paginated list of WASDI resources (WORKSPACE, PROCESSOR, SUBSCRIPTION, ORGANIZATION, WORKFLOW, STYLE) whose name matches the given partial string. The resource type must be specified. Results can be paginated using offset and limit. Only accessible to admin users.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - resourceType (string, required)
  - partialName (string, required, minimum length 3)
  - offset (integer, optional, default 0)
  - limit (integer, optional, default 10)
- Body: none
- Success:
  - 200 OK, body: paginated list of GenericResourceViewModel
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid partialName or missing resourceType)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

GET /admin/processorsByPartialName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a list of deployed processors whose name matches the given partial string. The search string must be at least 3 characters long. Only accessible to admin users.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - partialName (string, required, minimum length 3)
- Body: none
- Success:
  - 200 OK, body: list of DeployedProcessorViewModel
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid partialName)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)

GET /admin/resourcePermissions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of sharing permissions recorded for a resource, optionally filtered by resource type, resource ID, and/or user ID. At least one filter parameter must be provided. Non-admin users may query permissions for resources of type MISSION.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - resourceType (string, optional)
  - resourceId (string, optional)
  - userId (string, optional)
- Body: none
- Success:
  - 200 OK, body: list of UserResourcePermissionViewModel
- Notes:
  - At least one among resourceType, resourceId, userId must be provided.
  - Mission access is allowed also for non-admin users.
- Return codes:
  - 200 OK
  - 400 Bad Request (insufficient search criteria)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (no rights)

POST /admin/resourcePermissions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Grants a user access to a specific resource. The operation is delegated to the resource-type-specific share logic (e.g., workspace sharing, processor sharing). If no valid rights value is supplied, READ access is assigned by default.
- HTTP Verb: POST
- Headers: x-session-token
- Query params:
  - resourceType (string, required)
  - resourceId (string, required)
  - userId (string, required)
  - rights (string, optional, defaults to READ if invalid)
- Body: none
- Success:
  - 200 OK
- Notes:
  - Delegates to resource-specific share operations according to resourceType.
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid resource type, invalid resource id, insert error)
  - 401 Unauthorized (invalid session)
  - Other status codes can be propagated from delegated resource-specific operations.

DELETE /admin/resourcePermissions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes a sharing permission, revoking a user's access to a specific resource. The operation is delegated to the resource-type-specific unshare logic.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
  - resourceType (string, required)
  - resourceId (string, required)
  - userId (string, required)
- Body: none
- Success:
  - 200 OK
- Notes:
  - Delegates to resource-specific unshare operations according to resourceType.
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid resource type)
  - 401 Unauthorized (invalid session)
  - Other status codes can be propagated from delegated resource-specific operations.

GET /admin/resourcePermissions/types
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of available WASDI resource type names (e.g., WORKSPACE, PROCESSOR, SUBSCRIPTION, ORGANIZATION, WORKFLOW, STYLE, MISSION). Useful to populate resource-type drop-downs in admin UIs.
- HTTP Verb: GET
- Headers: x-session-token (declared but not validated in current implementation)
- Query params: none
- Body: none
- Success:
  - 200 OK, body: list of resource type names as strings
- Return codes:
  - 200 OK
  - 500 Internal Server Error

PUT /admin/metrics
^^^^^^^^^^^^^^^^^^
- Description: Stores or updates a metrics entry for a WASDI node. Nodes periodically push their hardware status (CPU usage, memory, disk, software licenses) to the main node via this endpoint so that WASDI can select the best node at runtime.
- HTTP Verb: PUT
- Headers: x-session-token
- Query params: none
- Body:
  - MetricsEntry (required)
- Success:
  - 200 OK, body: SuccessResponse
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid payload)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error (insert error)

GET /admin/metrics/latest
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the most recent metrics entry recorded for the specified node. If no nodeCode is provided the repository may return the globally latest entry.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - nodeCode (string, optional)
- Body: none
- Success:
  - 200 OK, body: MetricsEntry
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

GET /admin/users/list
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a paginated and sortable list of registered WASDI users. Results can be filtered by a partial name match and sorted by name, surname, or userId in ascending or descending order.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - partialName (string, optional, default empty)
  - offset (integer, optional, default 0)
  - limit (integer, optional, default 10)
  - sortedby (string, optional, allowed values: name, surname, userId)
  - order (string, optional, default asc; desc or equivalents map to descending)
- Body: none
- Success:
  - 200 OK, body: list of UserListViewModel
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

GET /admin/users/summary
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a summary overview of all registered users, grouped by subscription type (NONE, FREE, STANDARD, PROFESSIONAL), plus the total number of organizations. Intended for dashboard KPIs.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: UsersSummaryViewModel
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

GET /admin/users
^^^^^^^^^^^^^^^^
- Description: Returns the full profile details of a specific user identified by their userId, including account status, subscription type, role, registration and confirmation dates, and preferred node.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - userId (string, required)
- Body: none
- Success:
  - 200 OK, body: FullUserViewModel
- Return codes:
  - 200 OK
  - 400 Bad Request (missing userId or user not found)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

PUT /admin/users
^^^^^^^^^^^^^^^^
- Description: Updates the editable profile fields of an existing user (name, surname, role, description, link, preferred node, public nick name, registration date). The target user is identified by the userId contained in the request body.
- HTTP Verb: PUT
- Headers: x-session-token
- Query params: none
- Body:
  - FullUserViewModel (required)
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (missing payload or target user not found)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

DELETE /admin/users
^^^^^^^^^^^^^^^^^^^
- Description: Permanently deletes a user account and all resources associated with it. This operation is irreversible.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
  - userId (string, required)
- Body: none
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (missing userId or user not found)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

DELETE /admin/cleanProcessesQueue
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Marks all processes that are stuck in CREATED state as ERROR, effectively draining the process queue. Useful when a node crash leaves processes in an unfinished state.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error

DELETE /admin/cleanOldProcesses
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Marks stale past process workspace entries as ERROR. Intended to clean up old or orphaned process records that were never properly closed.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (requester is not admin)
  - 500 Internal Server Error
