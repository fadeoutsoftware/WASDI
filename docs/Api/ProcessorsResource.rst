ProcessorsResource
==================

Introduction
------------
Processors Resource.

Hosts the API for processor lifecycle, marketplace metadata, execution, payments, logs, sharing, and UI descriptors.

All endpoints are under base path ``/processors``.

Most endpoints require ``x-session-token``. A few download endpoints also accept token via query parameters for browser usage.

High-level capability groups:

- Upload, update, redeploy, library update, delete processor assets.
- Execute processors and estimate credits for area-priced apps.
- Marketplace list/detail retrieval and app payment integration with Stripe.
- Runtime and build logs retrieval.
- Processor sharing and UI descriptor management.

Common Models
-------------
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue
- DeployedProcessorViewModel: processorId, processorName, processorVersion, processorDescription, imgLink, logo, publisher, publisherNickName, paramsSample, isPublic, minuteTimeout, type, sharedWithMe, readOnly, deploymentOngoing, lastUpdate
- AppFilterViewModel: categories, publishers, name, score, minPrice, maxPrice, itemsPerPage, page, orderBy, orderDirection
- AppListViewModel: processorId, processorName, processorDescription, imgLink, publisher, publisherNickName, score, votes, friendlyName, price, squareKilometerPrice, isMine, buyed, logo, readOnly
- AppDetailViewModel: marketplace app detail payload with description, pricing, categories, images, score, store visibility and metadata
- RunningProcessorViewModel: processorId, name, processingIdentifier, status, jsonEncodedResult, message
- ProcessorLogViewModel: processWorkspaceId, logDate, logRow, rowNumber
- ProcessorSharingViewModel: userId, permissions
- AppPaymentViewModel: appPaymentId, paymentName, userId, processorId, buySuccess, buyDate, runDate
- SuccessResponse / ErrorResponse: standard success/error wrappers used by several endpoints

APIs
----

Processor Lifecycle
^^^^^^^^^^^^^^^^^^^

POST /processors/uploadprocessor
- Description: Uploads a new processor package (multipart), creates the processor entity, and schedules deploy operation.
- Headers: x-session-token
- Query params: workspace, name, version, description, type, paramsSample, public, timeout, force
- Body: multipart/form-data with file field ``file``
- Notes: name uniqueness and filename/path checks enforced; paramsSample must be valid JSON when provided.
- Return: PrimitiveResult (HTTP-like status in intValue)

POST /processors/update
- Description: Updates basic processor metadata (description, visibility, timeout, parameter sample).
- Headers: x-session-token
- Query params: processorId
- Body: DeployedProcessorViewModel
- Return codes: 200, 401, 403, 400/500 on validation/storage errors

POST /processors/updatefiles
- Description: Uploads and applies processor file update (zip or single file), optionally unzips, propagates to nodes, and triggers library update.
- Headers: x-session-token
- Query params: processorId, workspace, file
- Body: multipart/form-data with file field ``file``
- Return codes: 200, 401, 403, 400, 500

GET /processors/redeploy
- Description: Schedules processor redeploy on local node and propagates to compute nodes when called on main node.
- Headers: x-session-token
- Query params: processorId, workspace
- Return codes: 200, 401, 403, 500

GET /processors/libupdate
- Description: Forces library update for processor runtime environment; propagates to compute nodes from main node.
- Headers: x-session-token
- Query params: processorId, workspace
- Return codes: 200, 401, 403, 500

GET /processors/cleadbuildflag
- Description: Clears processor deployment ongoing flag.
- Headers: x-session-token
- Query params: processorId
- Return codes: 200, 401, 403, 500

GET /processors/delete
- Description: Deletes processor from main node workflow; if caller is not owner but has sharing, removes sharing only.
- Headers: x-session-token
- Query params: processorId, workspace
- Notes: on-demand priced processors trigger Stripe deactivation checks.
- Return codes: 200, 401, 400, 500

GET /processors/nodedelete
- Description: Node-side delete endpoint for distributed cleanup on compute nodes.
- Headers: x-session-token
- Query params: processorId, workspace, processorName, processorType, version
- Notes: valid only on compute nodes (not main node).
- Return codes: 200, 401, 400, 500

GET /processors/help
- Description: Returns help/readme markdown text found in processor folder.
- Headers: x-session-token
- Query params: name
- Return: PrimitiveResult (boolValue true + stringValue help text on success)

Execution
^^^^^^^^^

GET /processors/run
- Description: Runs processor using query ``encodedJson``.
- Headers: x-session-token
- Query params: name, encodedJson, workspace, parent, notify
- Return: RunningProcessorViewModel

POST /processors/run
- Description: Runs processor using request body as encoded JSON payload (supports longer parameters).
- Headers: x-session-token
- Query params: name, workspace, parent, notify
- Body: encoded parameter JSON string
- Return: RunningProcessorViewModel

POST /processors/getcredits
- Description: Computes credits required for area-priced processor run based on AOI/bounding box parameter.
- Headers: x-session-token
- Query params: processorId
- Body: encoded JSON parameters
- Return codes: 200 (number), 400, 401, 403, 500

Inventory And Marketplace
^^^^^^^^^^^^^^^^^^^^^^^^^

GET /processors/getdeployed
- Description: Lists all deployed processors accessible to the user (owned, shared, public), with readOnly/shared flags.
- Headers: x-session-token
- Return: array of DeployedProcessorViewModel
- Notes: returns empty list on invalid session/errors.

GET /processors/getprocessor
- Description: Returns one deployed processor by id or name.
- Headers: x-session-token
- Query params: processorId, name
- Return: DeployedProcessorViewModel
- Notes: returns empty/default view model when not found/forbidden.

POST /processors/getmarketlist
- Description: Returns marketplace list with filters (name, categories, publishers, score, price), sorting, and pagination.
- Headers: x-session-token
- Body: AppFilterViewModel
- Return: array of AppListViewModel

GET /processors/getmarketdetail
- Description: Returns detailed marketplace info for one processor name.
- Headers: x-session-token
- Query params: processorname
- Return: AppDetailViewModel
- Return codes: 200, 401, 403, 400, 500

Payments
^^^^^^^^

POST /processors/addAppPayment
- Description: Registers on-demand payment intent metadata before Stripe checkout.
- Headers: x-session-token
- Body: AppPaymentViewModel (paymentName, processorId, buyDate required)
- Return: SuccessResponse(appPaymentId)
- Return codes: 200, 400, 401, 403, 404, 500

GET /processors/stripe/onDemandPaymentUrl
- Description: Retrieves Stripe payment URL for one app payment entry.
- Headers: x-session-token
- Query params: processor, appPayment
- Return: SuccessResponse(url)
- Return codes: 200, 400, 401, 403, 500

GET /processors/stripe/confirmation/{CHECKOUT_SESSION_ID}
- Description: Stripe callback-like confirmation endpoint; validates checkout session and marks app payment as successful.
- Path params: CHECKOUT_SESSION_ID
- Return: HTML/JS snippet that closes browser window

GET /processors/isAppPurchased
- Description: Checks if current user can run a priced app now (free/owner/shared/already paid and not currently consumed).
- Headers: x-session-token
- Query params: processor
- Return: boolean
- Return codes: 200, 400, 401, 404, 500

GET /processors/byAppPaymentId
- Description: Returns payment metadata by app payment id.
- Headers: x-session-token
- Query params: appPayment
- Return: AppPaymentViewModel
- Return codes: 200, 400, 401, 404, 500

Logs
^^^^

POST /processors/logs/add
- Description: Adds one runtime log row for a processworkspace (or writes to main log if DB app logging disabled).
- Headers: x-session-token
- Query params: processworkspace
- Body: plain log line text
- Return codes: 200, 401, 403, 500

GET /processors/logs/count
- Description: Returns total number of log rows for processworkspace.
- Headers: x-session-token
- Query params: processworkspace
- Return: integer (``-1`` on errors/unauthorized)

GET /processors/logs/list
- Description: Returns process logs; optional row range pagination via startrow/endrow.
- Headers: x-session-token
- Query params: processworkspace, startrow, endrow
- Return: array of ProcessorLogViewModel

GET /processors/logs/build
- Description: Returns stored build logs associated with processor deployment.
- Headers: x-session-token
- Query params: processorId
- Return: array of strings
- Return codes: 200, 401, 403, 400, 500

Artifacts
^^^^^^^^^

GET /processors/downloadprocessor
- Description: Downloads processor files as zip stream.
- Headers: x-session-token (or token query param)
- Query params: token, processorId
- Return: application/octet-stream zip
- Return codes: 200, 401, 403, 204, 500

GET /processors/getcwl
- Description: Downloads CWL descriptor file for processor.
- Query params: processorName
- Return: application/octet-stream attachment
- Return codes: 200, 204, 404, 500

Sharing
^^^^^^^

PUT /processors/share/add
- Description: Shares processor with another user with optional access rights.
- Headers: x-session-token
- Query params: processorId, userId, rights
- Return: PrimitiveResult (boolValue + message)
- Notes: autoshare blocked for non-admins; rights default to READ.

GET /processors/share/byprocessor
- Description: Lists users and permissions the processor is shared with.
- Headers: x-session-token
- Query params: processorId
- Return: array of ProcessorSharingViewModel (or null on invalid/forbidden)

DELETE /processors/share/delete
- Description: Removes one user sharing from a processor.
- Headers: x-session-token
- Query params: processorId, userId
- Return: PrimitiveResult

Processor UI
^^^^^^^^^^^^

GET /processors/ui
- Description: Gets saved processor UI JSON descriptor (defaults to ``{"tabs":[]}``).
- Headers: x-session-token
- Query params: name
- Return: JSON string
- Return codes: 200, 401, 403, 400, 500

POST /processors/saveui
- Description: Saves/updates processor UI JSON descriptor.
- Headers: x-session-token
- Query params: name
- Body: UI JSON string
- Return codes: 200, 401, 403, 400, 500
