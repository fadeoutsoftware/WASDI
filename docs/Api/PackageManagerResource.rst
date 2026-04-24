PackageManagerResource
======================

Introduction
------------
Package Manager Resource.

Offers the API that lets users inspect and refresh the package environment of a WASDI processor application.

The resource supports:

- Listing the packages known for an application environment.
- Reading the package-manager version used by that application.
- Reading and resetting the history of environment actions executed for a processor.
- Triggering an environment refresh or a package-management action.

All endpoints are under the base path ``/packageManager``.

All endpoints require a valid session via the ``x-session-token`` header.

Behavior notes:

- Package and manager information is primarily read from the processor-local ``packagesInfo.json`` file.
- Environment actions are read from ``envActionsList.txt`` inside the processor folder.
- Some endpoints are available only on the main node.
- Access checks are enforced per processor or workspace using ``PermissionsUtils``.

Common Models
-------------
- PackageViewModel: managerName, packageName, currentVersion, currentBuild, latestVersion, type, channel
- PackageManagerViewModel: name, version, major, minor, patch
- PackageManagerFullInfoViewModel: packageManager, outdated, uptodate, all

APIs
----

GET /packageManager/listPackages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of packages known for an application environment. The endpoint reads ``packagesInfo.json`` from the processor folder, merges the ``outdated``, ``uptodate``, and ``all`` sections if present, sorts the resulting list by package name, and returns it.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- name (string, required) — processor/application name
- Body: none
- Success:
	- 200 OK, body: array of PackageViewModel
- Notes:
	- The caller must be allowed to access the processor by name.
	- If ``packagesInfo.json`` is missing, empty, or cannot be parsed, the endpoint returns a server error.
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access the processor)
	- 500 Internal Server Error

GET /packageManager/environmentActions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the history of package-manager actions executed for an application, as stored in the processor-local ``envActionsList.txt`` file. Empty lines are discarded.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- name (string, required) — processor/application name
- Body: none
- Success:
	- 200 OK, body: array of strings
- Notes:
	- This endpoint is available only on the main node.
	- If the actions file is missing or empty, the endpoint returns an empty list.
	- Access is checked against the processor by name.
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access the processor)
	- 400 Bad Request (called on a non-main node)
	- 500 Internal Server Error

GET /packageManager/managerVersion
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the version of the package manager used by an application. The endpoint first tries to read the manager info from ``packagesInfo.json``. If that is unavailable or invalid, it performs a live lookup using the processor's configured package-manager implementation.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- name (string, required) — processor/application name
- Body: none
- Success:
	- 200 OK, body: PackageManagerViewModel
- Notes:
	- If the processor exists but has no package manager configured, the endpoint returns ``200 OK`` with an empty body.
	- If the fallback live lookup cannot find the processor record, the endpoint returns a server error.
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 400 Bad Request (missing application name)
	- 403 Forbidden (user cannot access the processor)
	- 500 Internal Server Error

GET /packageManager/environmentupdate
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Triggers an environment refresh or a specific package-management action for a processor. The request schedules the operation through the normal WASDI process launcher using the special ``ENVIRONMENTUPDATE`` operation. On the main node, when a real ``updateCommand`` is provided, the action is also forwarded asynchronously to all compute nodes.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- processorId (string, required) — target processor ID
	- workspace (string, required) — workspace ID used as the execution/exchange context
	- updateCommand (string, optional) — package-manager command to execute; if present and shaped like ``action/package``, the package part is validated before scheduling
- Body: none
- Success:
	- 200 OK
- Notes:
	- The caller must have write permission on the processor.
	- The processor must have a package manager configured.
	- When ``updateCommand`` is empty, the endpoint behaves as a refresh and does not start the cross-node update worker.
	- The scheduled process runs in the local special workspace identified by ``Wasdi.s_sLocalWorkspaceName``.
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 400 Bad Request (processor not found, processor has no package manager, or invalid package in updateCommand)
	- 403 Forbidden (user cannot write the processor)
	- 500 Internal Server Error

GET /packageManager/reset
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Resets the stored environment action history for a processor by deleting its ``envActionsList.txt`` file, if present.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- processorId (string, required) — processor ID
	- workspace (string, required) — workspace ID used for permission validation
- Body: none
- Success:
	- 200 OK
- Notes:
	- The caller must be allowed to access both the workspace and the processor.
	- This endpoint is available only on the main node.
	- If the actions file does not exist, the endpoint still returns success.
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access the workspace or processor)
	- 400 Bad Request (called on a non-main node)
	- 500 Internal Server Error
