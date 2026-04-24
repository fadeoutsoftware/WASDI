WorkspaceResource
=================

Introduction
------------
Workspace Resource.

Hosts APIs to manage workspaces lifecycle and sharing.

Main capabilities:

- List user and shared workspaces
- Read workspace editor/details model
- Create and update workspaces
- Delete workspace (or remove sharing for non-owner)
- Share/unshare workspaces with users
- Resolve workspace name by id

All endpoints are under base path /ws.

All endpoints require x-session-token.

Common Models
-------------
- PrimitiveResult:
	- IntValue, StringValue, DoubleValue, BoolValue
- WorkspaceListInfoViewModel:
	- workspaceId, workspaceName, ownerUserId, sharedUsers, activeNode, nodeCode, creationDate, storageSize, isPublic, readOnly
- WorkspaceEditorViewModel:
	- workspaceId, name, userId, apiUrl, creationDate, lastEditDate, sharedUsers, nodeCode, activeNode, processesCount, cloudProvider, slaLink, storageSize, isPublic, readOnly
- WorkspaceSharingViewModel:
	- workspaceId, userId, ownerId, permissions

APIs
----

GET /ws/byuser
^^^^^^^^^^^^^^
- Description: Returns workspaces visible to current user (owned and shared), including sharing and node-active metadata.
- Success: 200 OK, body: array of WorkspaceListInfoViewModel
- Notes: invalid session returns empty list.

GET /ws/getws
^^^^^^^^^^^^^
- Description: Returns workspace editor/details view model by workspace id.
- Query params: workspace (required)
- Success: 200 OK, body: WorkspaceEditorViewModel
- Notes:
	- Includes node routing/apiUrl, storage size, process count, cloud provider/SLA fields, and sharing list.
	- Invalid session returns null; access failures return empty/default view model.

GET /ws/create
^^^^^^^^^^^^^^
- Description: Creates a new workspace.
- Query params: name (optional), node (optional)
- Success: 200 OK, body: PrimitiveResult (StringValue contains workspaceId)
- Notes:
	- If name is missing, defaults to Untitled Workspace.
	- Node selection fallback: best node score, then user default node, then system default wasdi.
	- Invalid session returns null.

POST /ws/update
^^^^^^^^^^^^^^^
- Description: Updates workspace metadata (name/public/node settings).
- Body: WorkspaceEditorViewModel
- Success: 200 OK, body: updated WorkspaceEditorViewModel
- Notes:
	- Requires write access.
	- Handles duplicate-name resolution by cloning name.
	- Returns null on invalid session/invalid input/no access/failure.

DELETE /ws/delete
^^^^^^^^^^^^^^^^^
- Description: Deletes workspace and related data, or removes sharing when requester is not owner.
- Query params: workspace (required), deletelayer (optional bool), deletefile (optional bool)
- Success: 200 OK
- Return codes: 200, 400, 401, 403, 500
- Notes:
	- Owner path can terminate workspace notebook, kill running processes, delete DB references, sharings, process-workspaces, files and/or published layers based on flags.
	- If workspace is on another node, request is forwarded to target node.

PUT /ws/share/add
^^^^^^^^^^^^^^^^^
- Description: Shares workspace with another user.
- Query params: workspace (required), userId (required), rights (optional; defaults to READ if invalid)
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- Prevents sharing to self (non-admin) and owner.
	- Sends notification email on success.

GET /ws/share/byworkspace
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns sharing entries for a workspace.
- Query params: workspace (required)
- Success: 200 OK, body: array of WorkspaceSharingViewModel
- Notes: invalid session/no-access returns empty list.

DELETE /ws/share/delete
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes one user sharing from a workspace.
- Query params: workspace (required), userId (required)
- Success: 200 OK, body: PrimitiveResult
- Notes: allowed for shared user itself, workspace writer/owner, or admin.

GET /ws/wsnamebyid
^^^^^^^^^^^^^^^^^^
- Description: Returns workspace name by workspace id.
- Query params: workspace (required)
- Success: 200 OK, body: text/plain workspace name
- Return codes: 200, 400, 401, 403, 500
