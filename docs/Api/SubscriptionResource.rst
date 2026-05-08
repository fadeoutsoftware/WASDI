SubscriptionResource
====================

Introduction
------------
Subscription Resource.

Hosts APIs to create, manage, share, and purchase subscriptions.

Main capabilities:

- Read active/user/admin subscription lists and counts
- Create, update, and delete subscriptions
- Share subscriptions with users and manage sharing entries
- Retrieve available subscription types
- Generate Stripe checkout URL and confirm Stripe checkout callback

All endpoints are under base path /subscriptions.

Authentication:

- Most endpoints require x-session-token.
- Stripe confirmation callback endpoint is called by Stripe and uses path parameter only.

Common Models
-------------
- SubscriptionViewModel:
	- subscriptionId, name, description, typeId, typeName, buyDate, startDate, endDate, durationDays, userId, organizationId, organizationName, buySuccess, readOnly
- SubscriptionListViewModel:
	- subscriptionId, ownerUserId, name, typeId, typeName, startDate, endDate, organizationName, organizationId, reason, buySuccess, runningTime, readOnly
- SubscriptionSharingViewModel:
	- subscriptionId, userId, ownerId, permissions
- SubscriptionTypeViewModel:
	- typeId, name, description
- StripePaymentDetail:
	- clientReferenceId, customerName, customerEmail, paymentIntentId, paymentStatus, paymentCurrency, paymentAmountInCents, invoiceId, productDescription, paymentDateInSeconds, date, invoicePdfUrl
- PrimitiveResult:
	- IntValue, StringValue, DoubleValue, BoolValue
- SuccessResponse:
	- message
- ErrorResponse:
	- message

APIs
----

GET /subscriptions/active
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns active subscription for current user from valid subscriptions.
- Success: 200 OK, body: SubscriptionListViewModel
- Return codes: 200, 401, 404, 500

GET /subscriptions/byuser
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns subscriptions associated with current user (owned/shared/organization-based via permission utility).
- Query params: valid (optional boolean, default false)
- Success: 200 OK, body: array of SubscriptionListViewModel
- Return codes: 200, 401, 500

GET /subscriptions/count
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns total subscriptions count.
- Success: 200 OK, body: PrimitiveResult (IntValue)
- Return codes: 200, 401, 403, 500
- Notes: admin-only endpoint.

GET /subscriptions/byId
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns full subscription details by id.
- Query params: subscription (required)
- Success: 200 OK, body: SubscriptionViewModel
- Return codes: 200, 401, 400, 403, 500

POST /subscriptions/add
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Creates a subscription and automatically creates a default project with same name.
- Body: SubscriptionViewModel
- Success: 200 OK, body: SuccessResponse (message contains subscription id)
- Return codes: 200, 401, 500
- Notes:
	- For non-admin callers, target user is forced to current user.
	- Non-admin callers cannot force buySuccess=true.
	- Name collisions are resolved by cloning/renaming.

PUT /subscriptions/update
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates an existing subscription.
- Body: SubscriptionViewModel
- Success: 200 OK, body: SuccessResponse
- Return codes: 200, 401, 400, 403, 500
- Notes: non-admin callers cannot change buySuccess state.

DELETE /subscriptions/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes a subscription or removes caller sharing if caller is not owner/admin.
- Query params: subscription (required)
- Success: 200 OK, body: SuccessResponse
- Return codes: 200, 401, 400, 403, 500
- Notes:
	- Owner/admin path removes sharings, deletes related projects, then deletes subscription.
	- Non-owner shared user path removes own sharing only.

GET /subscriptions/types
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns available subscription types.
- Success: 200 OK, body: array of SubscriptionTypeViewModel
- Return codes: 200, 500

POST /subscriptions/share/add
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Shares a subscription with another user.
- Query params: subscription (required), userId (required), rights (optional; defaults to READ if invalid)
- Success: 200 OK, body: SuccessResponse
- Return codes: 200, 401, 400, 403, 500
- Notes:
	- Prevents sharing with self and owner.
	- Returns success with message Already Shared when permission already exists.

GET /subscriptions/share/bysubscription
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns users currently sharing a subscription.
- Query params: subscription (required)
- Success: 200 OK, body: array of SubscriptionSharingViewModel
- Return codes: 200, 401, 403, 500

DELETE /subscriptions/share/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes one user sharing entry from a subscription.
- Query params: subscription (required), userId (required)
- Success: 200 OK, body: SuccessResponse
- Return codes: 200, 401, 400, 403, 500
- Notes:
	- Cannot remove owner from its own subscription.
	- Allowed for owner/writer/admin, and in beneficiary-removal flow as implemented.

GET /subscriptions/stripe/paymentUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Generates Stripe checkout URL for a subscription type configured in server settings.
- Query params: subscription (required), workspace (optional)
- Success: 200 OK, body: SuccessResponse (message contains URL)
- Return codes: 200, 401, 400, 403, 500

GET /subscriptions/stripe/confirmation/{CHECKOUT_SESSION_ID}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Stripe callback endpoint to confirm payment and activate subscription.
- Path params: CHECKOUT_SESSION_ID (required)
- Success: 200 OK
- Return codes: 200, 401, 500
- Notes:
	- Retrieves payment details from Stripe using checkout session id.
	- Sets buySuccess=true and buyDate when confirmation succeeds.
	- Optionally emits RabbitMQ message if workspace id is present in client reference id.

GET /subscriptions/list
^^^^^^^^^^^^^^^^^^^^^^^
- Description: Admin dashboard listing endpoint with filters, sorting, and paging.
- Query params: userfilter, idfilter, namefilter, statusfilter, offset, limit, sortby, order
- Success: 200 OK, body: array of SubscriptionListViewModel
- Return codes: 200, 401, 403, 500
- Notes:
	- Admin-only endpoint.
	- Defaults: offset=0, limit=10, sortby=name, order=asc.
