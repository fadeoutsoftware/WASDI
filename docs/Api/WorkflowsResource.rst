WorkflowsResource
=================

Introduction
------------
Workflows Resource.

Manages SNAP workflows represented as XML graph files.

Main capabilities:

- Upload and update workflow XML files
- Read workflow XML and workflow metadata
- Manage workflow sharing permissions
- Run workflows in a workspace
- Download workflow XML file

All endpoints are under base path /workflows.

Authentication:

- Most endpoints require x-session-token.
- Download supports token query parameter for browser-driven downloads.

Common Models
-------------
- PrimitiveResult:
	- IntValue, StringValue, DoubleValue, BoolValue
- SnapWorkflowViewModel:
	- workflowId, name, description, isPublic, userId, nodeUrl, sharedWithMe, readOnly, inputNodeNames, inputFileNames, outputNodeNames, outputFileNames, templateParams, lastUpdate
- WorkflowSharingViewModel:
	- userId, permissions

APIs
----

POST /workflows/uploadfile
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Uploads and saves a new SNAP workflow XML file and creates workflow DB metadata.
- Consumes: multipart/form-data
- Form field: file
- Query params: workspace (optional), name (required), description (optional), public (optional boolean)
- Success: 200 OK
- Return codes: 200, 400, 401, 500
- Notes: parses XML and auto-detects input/output node ids (Read/Write operators).

POST /workflows/updatefile
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Replaces workflow XML file for an existing workflow id.
- Consumes: multipart/form-data
- Form field: file
- Query params: workflowid (required)
- Success: 200 OK
- Return codes: 200, 401, 403, 304, 500
- Notes: updates detected input/output node ids in DB metadata.

GET /workflows/getxml
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns workflow XML content.
- Query params: workflowId (required)
- Produces: application/xml
- Success: 200 OK, body: XML string
- Return codes: 200, 401, 403, 400, 304, 500

POST /workflows/updatexml
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates workflow XML by passing XML text body (wrapper over updatefile).
- Query params: workflowId (required)
- Consumes: application/xml
- Produces: application/xml
- Body: workflow XML string
- Success: 200 OK
- Return codes: inherited from updatefile

POST /workflows/updateparams
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates workflow metadata parameters.
- Query params: workflowid (required), name (optional), description (optional), public (optional boolean)
- Consumes: multipart/form-data
- Success: 200 OK
- Return codes: 200, 401, 403, 400

GET /workflows/getbyuser
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns workflows visible to current user (own, public, shared).
- Success: 200 OK, body: array of SnapWorkflowViewModel
- Notes:
	- Sets sharedWithMe and readOnly according to ownership/sharing rights.
	- Includes lastUpdate from underlying XML file timestamp when available.

GET /workflows/delete
^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes workflow; if caller is not owner but has sharing, removes only their sharing entry.
- Query params: workflowId (required)
- Success: 200 OK
- Return codes: 200, 401, 403, 400, 500

PUT /workflows/share/add
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Shares a workflow with another user.
- Query params: workflowId (required), userId (required), rights (optional; defaults to READ if invalid)
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- Prevents auto-share for non-admin.
	- Sends notification email on success.

DELETE /workflows/share/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes one workflow sharing entry.
- Query params: workflowId (required), userId (required)
- Success: 200 OK, body: PrimitiveResult
- Notes: allowed for owner, shared user itself, or admin access flow.

GET /workflows/share/byworkflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Lists active sharings for a workflow.
- Query params: workflowId (required)
- Success: 200 OK, body: array of WorkflowSharingViewModel
- Notes: returns empty list on invalid session/no access/errors.

POST /workflows/run
^^^^^^^^^^^^^^^^^^^
- Description: Executes workflow as GRAPH launcher operation in target workspace.
- Query params: workspace (required), parent (optional parent process workspace id)
- Body: SnapWorkflowViewModel (input/output files/nodes and optional templateParams)
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- Requires valid user session and valid subscription.
	- Requires workflow access and workspace write permission.
	- If workflow XML is not present on current node, it attempts remote download from workflow node.

GET /workflows/download
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Downloads workflow XML file by workflow id.
- Query params: workflowId (required), token (optional if x-session-token header is provided)
- Produces: application/octet-stream
- Success: 200 OK, streaming attachment
- Return codes: 200, 204, 401, 403, 500

GET /workflows/byname
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns workflow metadata by workflow name.
- Query params: name (required)
- Success: 200 OK, body: SnapWorkflowViewModel
- Return codes: 200, 401, 403, 400, 304, 500
- Notes: includes lastUpdate from XML file timestamp.
