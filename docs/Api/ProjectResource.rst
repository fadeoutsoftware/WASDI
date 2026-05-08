ProjectResource
===============

Introduction
------------
Project Resource.

Hosts APIs to manage projects and active project selection for users.

Main capabilities:

- List projects by user and by subscription
- Read project details
- Create and update projects
- Change active project for a user
- Delete projects

All endpoints are under base path /projects.

All endpoints require x-session-token.

Common Models
-------------
- ProjectEditorViewModel:
	- projectId, subscriptionId, name, description, targetUser, activeProject
- ProjectListViewModel:
	- projectId, subscriptionName, name, description, activeProject
- ProjectViewModel:
	- projectId, subscriptionId, name, description, activeProject
- SuccessResponse:
	- message
- ErrorResponse:
	- message

APIs
----

GET /projects/byuser
^^^^^^^^^^^^^^^^^^^^
- Description: Returns projects available to current user across subscriptions available to that user.
- Query params: valid (optional boolean, default false)
- Success: 200 OK, body: array of ProjectListViewModel
- Return codes: 200, 401, 500
- Notes: if no active project is currently set and at least one project exists, first project is auto-marked active and persisted on user profile.

GET /projects/bysubscription
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns projects for one subscription.
- Query params: subscription (required)
- Success: 200 OK, body: array of ProjectListViewModel
- Return codes: 200, 401, 400, 403, 500

GET /projects/byId
^^^^^^^^^^^^^^^^^^
- Description: Returns detailed view model for one project id.
- Query params: project (required)
- Success: 200 OK, body: ProjectViewModel
- Return codes: 200, 401, 400, 403, 500

POST /projects/add
^^^^^^^^^^^^^^^^^^
- Description: Creates a new project.
- Body: ProjectEditorViewModel
- Success: 200 OK, body: SuccessResponse (message contains created project id)
- Return codes: 200, 401, 403, 500
- Notes:
	- If another project with same name exists, the service clones/adjusts name until unique.
	- If activeProject is true, active project is updated for target user flow.

PUT /projects/update
^^^^^^^^^^^^^^^^^^^^
- Description: Updates an existing project.
- Body: ProjectEditorViewModel
- Success: 200 OK, body: SuccessResponse (message contains project id)
- Return codes: 200, 401, 400, 403, 500
- Notes:
	- Enforces unique project name (excluding current project).
	- If activeProject is true, active project is changed accordingly.
	- If activeProject is false and updated project is currently active for user, active project/subscription can be cleared.

PUT /projects/active
^^^^^^^^^^^^^^^^^^^^
- Description: Changes active project (and corresponding active subscription) for user.
- Query params: project (optional, nullable to clear), target (optional user id)
- Success: 200 OK, body: SuccessResponse
- Return codes: 200, 401, 400, 403, 500
- Notes:
	- target is optional; defaults to current user.
	- Changing active project for another user requires admin role.

DELETE /projects/delete
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes a project.
- Query params: project (required)
- Success: 200 OK, body: SuccessResponse
- Return codes: 200, 401, 400, 403, 500
