ProcessorParametersTemplateResource
===================================

Introduction
------------
Processor Parameters Template Resource.

Hosts the API used to manage reusable processor parameter templates and share them with other users.

The resource supports:

- Creating, updating, reading, and deleting parameter templates.
- Listing templates visible for a processor (owned + shared).
- Sharing and unsharing templates with users.
- Listing users a template is shared with.

All endpoints are under the base path ``/processorParamTempl``.

All endpoints require a valid session via the ``x-session-token`` header.

Permission notes:

- Read access is validated with ``PermissionsUtils.canUserAccessProcessorParametersTemplate``.
- Write access is validated with ``PermissionsUtils.canUserWriteProcessorParametersTemplate``.
- For delete: owners delete the template itself; shared users only remove their own sharing row.

Common Models
-------------
- ProcessorParametersTemplateListViewModel: templateId, userId, processorId, name, updateDate, readOnly
- ProcessorParametersTemplateDetailViewModel: templateId, userId, processorId, name, description, jsonParameters, creationDate, updateDate, readOnly
- ProcessorParametersTemplateSharingViewModel: processorParametersTemplateId, userId, ownerId, permissions
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue

APIs
----

DELETE /processorParamTempl/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes a processor-parameters template. If the caller is the owner, the template is deleted and all sharings are cleaned up. If the caller is not the owner but has access, only the caller's sharing permission is removed.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
	- templateId (string, required) — template identifier (URL-decoded by the endpoint)
- Body: none
- Success:
	- 200 OK
- Return codes:
	- 200 OK
	- 400 Bad Request (template not found or no rows deleted)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access template)

POST /processorParamTempl/update
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates an existing template. Caller must have write permission on the template. The endpoint updates content fields and refreshes the update timestamp.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: ProcessorParametersTemplateDetailViewModel
	- templateId (string, required)
	- processorId (string, required)
	- name (string, required)
	- description (string, optional)
	- jsonParameters (string, optional)
	- creationDate (string, optional; preserved/converted when present)
- Success:
	- 200 OK
- Return codes:
	- 200 OK
	- 400 Bad Request (missing body or update not applied)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot write template)

POST /processorParamTempl/add
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Creates a new processor-parameters template for a processor the caller can access. A random template ID is generated server-side and creation/update timestamps are initialized.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: ProcessorParametersTemplateDetailViewModel
	- processorId (string, required)
	- name (string, required)
	- description (string, optional)
	- jsonParameters (string, optional)
- Success:
	- 200 OK
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid body, missing/invalid processorId, or processor not found)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access processor)
	- 500 Internal Server Error

GET /processorParamTempl/get
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns full details of a template by ID, including ``readOnly`` derived from write permissions.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- templateId (string, required)
- Body: none
- Success:
	- 200 OK, body: ProcessorParametersTemplateDetailViewModel
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access template)
	- 500 Internal Server Error

GET /processorParamTempl/getlist
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of templates visible to the caller for one processor. The result includes templates owned by the caller and templates shared with the caller. Each item includes a computed ``readOnly`` flag.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- processorId (string, required)
- Body: none
- Success:
	- 200 OK, body: array of ProcessorParametersTemplateListViewModel
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid processor)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access processor)
	- 500 Internal Server Error

PUT /processorParamTempl/share/add
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Shares a template with another user. Caller must have write permission on the template or be an admin. Rights default to ``read`` when missing/invalid. Sends a notification email after successful share.
- HTTP Verb: PUT
- Headers: x-session-token
- Query params:
	- processorParametersTemplate (string, required) — template ID
	- userId (string, required) — destination user ID
	- rights (string, optional) — sharing rights, defaults to ``read``
- Body: none
- Success:
	- 200 OK, body: PrimitiveResult with ``boolValue=true`` and ``stringValue=Done`` or ``Already Shared.``
- Return codes:
	- 200 OK (always HTTP 200; inspect PrimitiveResult)
	- PrimitiveResult intValue=401 (invalid session)
	- PrimitiveResult intValue=400 (invalid template, self-share, share-with-owner, or destination user not found)
	- PrimitiveResult intValue=403 (caller cannot share template)
	- PrimitiveResult intValue=500 (insert error)

GET /processorParamTempl/share/byprocessorParametersTemplate
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns users who currently have this template in sharing.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- processorParametersTemplate (string, required) — template ID
- Body: none
- Success:
	- 200 OK, body: array of ProcessorParametersTemplateSharingViewModel
- Notes:
	- On invalid session, forbidden access, or internal errors, this endpoint returns an empty list instead of an HTTP error status.
- Return codes:
	- 200 OK (possibly empty on errors)

DELETE /processorParamTempl/share/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes one user's sharing permission for a template.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
	- processorParametersTemplate (string, required) — template ID
	- userId (string, required) — user to remove from sharing
- Body: none
- Success:
	- 200 OK, body: PrimitiveResult with ``boolValue=true`` and ``stringValue=Done``
- Return codes:
	- 200 OK (always HTTP 200; inspect PrimitiveResult)
	- PrimitiveResult intValue=401 (invalid session)
	- PrimitiveResult intValue=400 (invalid destination user)
	- PrimitiveResult intValue=500 (delete error)
