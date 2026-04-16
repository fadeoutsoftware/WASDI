ProcessWorkspaceResource
========================

Introduction
------------
Process Workspace Resource.

Hosts the API used to monitor and control process workspaces (runs of processor and other launcher operations), including status updates, payload, runtime summaries, queue status, and project/subscription computing-time metrics.

All endpoints are under base path ``/process``.

All endpoints require ``x-session-token``.

Status model notes:

- Common process states: ``CREATED``, ``RUNNING``, ``WAITING``, ``READY``, ``DONE``, ``ERROR``, ``STOPPED``.
- Many read endpoints return empty lists/default view models on invalid session or permission failures instead of HTTP error responses.

Common Models
-------------
- ProcessWorkspaceViewModel: productName, operationType, operationSubType, operationDate, operationStartDate, operationEndDate, lastChangeDate, userId, fileSize, status, progressPerc, processObjId, pid, payload, workspaceId
- ProcessWorkspaceSummaryViewModel: userProcessWaiting, userProcessRunning, allProcessWaiting, allProcessRunning
- ProcessHistoryViewModel: processorName, operationDate, operationStartDate, operationEndDate, status, workspaceName, workspaceId
- AppStatsViewModel: applicationName, runs, error, done, stopped, mediumTime, uniqueUsers
- ComputingTimeViewModel: subscriptionId, projectId, userId, computingTime
- ProcessWorkspaceAggregatedViewModel: schedulerName, operationType, operationSubType, procCreated, procRunning, procWaiting, procReady
- NodeScoreByProcessWorkspaceViewModel: nodeCode, numberOfProcesses, disk/memory usage details, licenses, timestampAsString

APIs
----

GET /process/byws
^^^^^^^^^^^^^^^^^
- Description: Returns process workspaces for one workspace, with optional filters (status, operation type, name pattern, date range) and optional index pagination.
- Query params: workspace (required), status, operationType, namePattern, dateFrom, dateTo, startindex, endindex
- Success: 200 OK, body: array of ProcessWorkspaceViewModel
- Notes: invalid filters are ignored; invalid session/permissions return empty list.

GET /process/byusr
^^^^^^^^^^^^^^^^^^
- Description: Returns process workspaces for the authenticated user.
- Query params: ogc (optional boolean; when true, returns OGC processes only)
- Success: 200 OK, body: array of ProcessWorkspaceViewModel

GET /process/byapp
^^^^^^^^^^^^^^^^^^
- Description: Returns process history entries for one processor name and user, merged from main and active compute nodes when called on main node.
- Query params: processorName (required)
- Success: 200 OK, body: array of ProcessHistoryViewModel

GET /process/appstats
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns aggregated run statistics for one processor name; main node merges stats from compute nodes.
- Query params: processorName (required)
- Success: 200 OK, body: AppStatsViewModel

GET /process/lastbyws
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the latest process workspaces for one workspace (repository-side fixed limit).
- Query params: workspace (required)
- Success: 200 OK, body: array of ProcessWorkspaceViewModel

GET /process/lastbyusr
^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the latest process workspaces for current user (repository-side fixed limit).
- Success: 200 OK, body: array of ProcessWorkspaceViewModel

GET /process/summary
^^^^^^^^^^^^^^^^^^^^
- Description: Returns quick counters of waiting/running processes for current user and totals (optionally workspace-filtered).
- Query params: workspace (optional)
- Success: 200 OK, body: ProcessWorkspaceSummaryViewModel

GET /process/delete
^^^^^^^^^^^^^^^^^^^
- Description: Stops/kills a process workspace (optionally the entire tree) through ProcessWorkspaceService.
- Query params: procws (required), treeKill (optional boolean)
- Success: 200 OK
- Return codes: 200, 401, 403, 400, 500

GET /process/byid
^^^^^^^^^^^^^^^^^
- Description: Returns one process workspace by process object id.
- Query params: procws (required)
- Success: 200 OK, body: ProcessWorkspaceViewModel
- Notes: on invalid/forbidden/error returns default model with status preset to ``ERROR``.

POST /process/statusbyid
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Batch status lookup for multiple process ids.
- Body: array of process workspace ids
- Success: 200 OK, body: array of statuses (same index order as input)
- Notes: invalid/unreachable ids are replaced with ``ERROR`` in output.

GET /process/getstatusbyid
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns status string for one process id.
- Query params: procws (required)
- Success: 200 OK, body: status string
- Notes: returns ``ERROR`` for invalid session, forbidden access, invalid id, or internal errors.

GET /process/updatebyid
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates process status and optionally progress percentage; can optionally notify RabbitMQ.
- Query params: procws (required), status (required), perc (int), sendrabbit (optional: ``1`` or ``true``)
- Success: 200 OK, body: updated ProcessWorkspaceViewModel

GET /process/setpayload
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Sets payload for a process workspace (GET variant).
- Query params: procws (required), payload
- Success: 200 OK, body: updated ProcessWorkspaceViewModel

POST /process/setpayload
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Sets payload for a process workspace (POST variant).
- Query params: procws (required)
- Body: payload string
- Success: 200 OK, body: updated ProcessWorkspaceViewModel

GET /process/setsubpid
^^^^^^^^^^^^^^^^^^^^^^
- Description: Sets subprocess PID for a process workspace.
- Query params: procws (required), subpid (required int)
- Success: 200 OK, body: updated ProcessWorkspaceViewModel

GET /process/payload
^^^^^^^^^^^^^^^^^^^^
- Description: Gets payload string of a process workspace.
- Query params: procws (required)
- Success: 200 OK, body: payload string
- Notes: returns null when unauthorized/invalid/error.

GET /process/runningTime/UI
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns running time (ms) for a target user in a date interval; admin can query other users, non-admin only self.
- Query params: userId (required), dateFrom, dateTo
- Success: 200 OK, body: long
- Notes: main node merges running time from compute nodes.

GET /process/runningTime/SP
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns nested map of running time by subscription and project for subscriptions associated with current user.
- Success: 200 OK, body: map<subscriptionId, map<projectId, runningTimeMs>>
- Notes: main node merges per-node results.

GET /process/runningtimeproject
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns overall project computing time (all users) for projects in subscriptions associated with current user.
- Success: 200 OK, body: array of ComputingTimeViewModel
- Return codes: 200, 401, 500

GET /process/runningtimeproject/byuser
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns computing time for current user only, grouped by subscription/project.
- Success: 200 OK, body: array of ComputingTimeViewModel
- Return codes: 200, 401, 500

GET /process/paramsbyid
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns stored JSON parameters for a process workspace when underlying parameter record is a ProcessorParameter.
- Query params: procws (required)
- Success: 200 OK, body: JSON string
- Return codes: 200, 401, 403, 400, 500

GET /process/queuesStatus
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns scheduler queue aggregation by operation type/subtype and status counts, for local or remote node.
- Query params: nodeCode (optional), statuses (optional comma-separated statuses)
- Success: 200 OK, body: array of ProcessWorkspaceAggregatedViewModel

GET /process/nodesByScore
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns nodes sorted by process workspace score/resource pressure.
- Success: 200 OK, body: array of NodeScoreByProcessWorkspaceViewModel
- Notes: admin-only; non-admin gets empty list.
