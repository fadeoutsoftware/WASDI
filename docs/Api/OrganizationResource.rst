OrganizationResource
====================

Introduction
------------
Organization Resource.

Hosts the API that lets users create and manage organizations in WASDI.

The resource supports:

- Listing the organizations owned by or shared with the current user.
- Reading the full details of one organization.
- Creating, updating, and deleting organizations.
- Sharing an organization with other users and managing those sharings.

All endpoints are under the base path ``/organizations``.

All endpoints require a valid session via the ``x-session-token`` header.

Permission notes:

- Read access is enforced with ``PermissionsUtils.canUserAccessOrganization``.
- Write access is enforced with ``PermissionsUtils.canUserWriteOrganization``.
- Admin users can also share organizations even when they are not the normal writer.
- Deleting an organization behaves differently for owners and non-owners: owners delete the organization itself, while non-owners only remove their own sharing permission.

Common Models
-------------
- OrganizationListViewModel: organizationId, ownerUserId, name, readOnly
- OrganizationEditorViewModel: organizationId, name, description, address, email, url
- OrganizationViewModel: organizationId, userId, name, description, address, email, url, readOnly, sharedUsers
- OrganizationSharingViewModel: organizationId, userId, ownerId, permissions
- SuccessResponse: message/value payload used by the REST layer to report success
- ErrorResponse: error message payload used by the REST layer to report failures

APIs
----

GET /organizations/byuser
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of organizations associated with the authenticated user. The response includes organizations owned by the user and organizations shared with the user. The ``readOnly`` flag is ``false`` for owned organizations and is computed from the user's write permission for shared ones.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
	- 200 OK, body: array of OrganizationListViewModel
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 500 Internal Server Error

GET /organizations/byId
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the full details of a specific organization, including the list of users it is shared with. The response also contains a ``readOnly`` flag based on whether the current user can write to the organization.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- organization (string, required) — organization ID
- Body: none
- Success:
	- 200 OK, body: OrganizationViewModel
- Return codes:
	- 200 OK
	- 400 Bad Request (missing or invalid organization ID)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access the organization)
	- 500 Internal Server Error

POST /organizations/add
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Creates a new organization owned by the authenticated user. The organization name must be unique. The server generates a random organization ID and returns it in the success payload.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: OrganizationEditorViewModel
	- organizationId (string, optional/ignored on create)
	- name (string, required)
	- description (string, optional)
	- address (string, optional)
	- email (string, optional)
	- url (string, optional)
- Success:
	- 200 OK, body: SuccessResponse containing the new organization ID
- Return codes:
	- 200 OK
	- 400 Bad Request (null body or another organization with the same name already exists)
	- 401 Unauthorized (invalid session)
	- 500 Internal Server Error

PUT /organizations/update
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates an existing organization. The caller must have write permission on the organization. The organization must already exist, and its new name must not collide with another organization.
- HTTP Verb: PUT
- Headers: x-session-token
- Query params: none
- Body: OrganizationEditorViewModel
	- organizationId (string, required)
	- name (string, required)
	- description (string, optional)
	- address (string, optional)
	- email (string, optional)
	- url (string, optional)
- Success:
	- 200 OK, body: SuccessResponse containing the organization ID
- Return codes:
	- 200 OK
	- 400 Bad Request (missing organization body, organization not found, or duplicate name conflict)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot write the organization)
	- 500 Internal Server Error

DELETE /organizations/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes an organization or, if the caller is not the owner, removes only the caller's sharing on that organization. When the owner deletes the organization, all sharing permissions are removed first and any linked subscriptions are detached by clearing their ``organizationId``.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
	- organization (string, required) — organization ID
- Body: none
- Success:
	- 200 OK, body: SuccessResponse containing the organization ID
- Notes:
	- Non-owners do not delete the organization itself; they only remove their own permission row.
- Return codes:
	- 200 OK
	- 400 Bad Request (missing organization ID, organization not found, or deletion failed)
	- 401 Unauthorized (invalid session)
	- 500 Internal Server Error

POST /organizations/share/add
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Shares an organization with another user. The caller must have write permission on the organization or be an admin. If the ``rights`` value is missing or invalid, it defaults to ``read``. An e-mail notification is attempted after a successful share.
- HTTP Verb: POST
- Headers: x-session-token
- Query params:
	- organization (string, required) — organization ID
	- userId (string, required) — destination user ID
	- rights (string, optional) — access rights; defaults to ``read`` when invalid
- Body: none
- Success:
	- 200 OK, body: SuccessResponse with ``Done`` or ``Already Shared.``
- Notes:
	- Non-admin users cannot share the organization with themselves.
	- The organization cannot be shared with its owner.
	- Sharing with a non-existent user is rejected.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid organization, self-share, share with owner, or destination user not found)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (caller cannot share the organization)
	- 500 Internal Server Error

GET /organizations/share/byorganization
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of users the organization is currently shared with, including the stored permissions for each share.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- organization (string, required) — organization ID
- Body: none
- Success:
	- 200 OK, body: array of OrganizationSharingViewModel
- Return codes:
	- 200 OK
	- 400 Bad Request (missing organization ID)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access the organization)
	- 500 Internal Server Error

DELETE /organizations/share/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes the sharing permission of a specific user for the given organization. The caller must have write permission on the organization.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
	- organization (string, required) — organization ID
	- userId (string, required) — destination user whose share must be removed
- Body: none
- Success:
	- 200 OK, body: SuccessResponse with ``Done``
- Return codes:
	- 200 OK
	- 400 Bad Request (missing organization ID or invalid destination user)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot write the organization)
	- 500 Internal Server Error
