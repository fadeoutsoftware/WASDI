CreditsPackageResource
======================

Introduction
------------
Credits Package Resource.

Hosts the API to manage WASDI credit packages:

- Retrieve the available credit package types.
- Query the total credits remaining for the authenticated user.
- List credit packages purchased by the authenticated user.
- Create a new credit package.
- Obtain a Stripe payment URL to purchase a credit package.
- Handle the Stripe payment confirmation callback to activate a credit package.

All endpoints are under the base path ``/credits``.

Most endpoints require a valid session via the ``x-session-token`` header. The Stripe confirmation endpoint is called by Stripe and does not require a session token.

Common Models
-------------
- CreditsPackageViewModel: creditPackageId, name, description, type, buyDate, userId, buySuccess, creditsRemaining, lastUpdate
- SubscriptionTypeViewModel: typeId, name, description
- SuccessResponse: message
- ErrorResponse: message

APIs
----

GET /credits/types
^^^^^^^^^^^^^^^^^^
- Description: Returns the list of available credit package types that a user can purchase. Does not require authentication.
- HTTP Verb: GET
- Headers: x-session-token (optional — not validated by this endpoint)
- Query params: none
- Body: none
- Success:
  - 200 OK, body: list of SubscriptionTypeViewModel (typeId, name, description)
- Return codes:
  - 200 OK
  - 500 Internal Server Error

GET /credits/totalbyuser
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the total amount of credits remaining across all credit packages owned by the authenticated user.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: Double (total credits remaining)
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

GET /credits/listbyuser
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns all credit packages that have been purchased by the authenticated user, optionally sorted by purchase date in ascending order.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - ascendingOrder (boolean, optional) — if true, results are sorted by buy date ascending; default is descending
- Body: none
- Success:
  - 200 OK, body: list of CreditsPackageViewModel
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

POST /credits/add
^^^^^^^^^^^^^^^^^^
- Description: Creates a new credit package for the authenticated user. The amount of credits is determined by the package type. Only admin users can create a package with ``buySuccess`` set to true; for non-admin users the flag is forced to false and the package must be activated via the Stripe payment flow.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: CreditsPackageViewModel (required): name, description, type, buySuccess
- Success:
  - 200 OK, body: SuccessResponse (message = new creditPackageId)
- Notes:
  - If a credit package with the same name already exists for the user, the name is automatically suffixed to make it unique.
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid package type)
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

GET /credits/stripe/paymentUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Generates a Stripe Checkout payment URL for the specified credit package. The URL includes the ``creditPackageId`` as a client reference so Stripe can link the payment back to the package on confirmation.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - creditPackageId (string, required) — ID of the credit package to pay for
- Body: none
- Success:
  - 200 OK, body: SuccessResponse (message = Stripe Checkout URL)
- Return codes:
  - 200 OK
  - 400 Bad Request (missing/invalid creditPackageId or unsupported package type)
  - 401 Unauthorized (invalid session)
  - 403 Forbidden (package belongs to a different user)
  - 500 Internal Server Error

GET /credits/stripe/confirmation/{CHECKOUT_SESSION_ID}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Stripe payment confirmation callback. Called by Stripe after a successful payment. Verifies the payment details with Stripe, then marks the corresponding credit package as successfully purchased (sets buySuccess=true, records the buy date and Stripe payment intent ID). This endpoint is not protected by a session token — it is intended to be called by Stripe's redirect mechanism.
- HTTP Verb: GET
- Headers: none
- Path params:
  - CHECKOUT_SESSION_ID (string, required) — the Stripe checkout session identifier embedded in the redirect URL
- Query params: none
- Body: none
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 401 Unauthorized (missing or empty CHECKOUT_SESSION_ID)
  - 500 Internal Server Error (Stripe verification failed or DB update error)
