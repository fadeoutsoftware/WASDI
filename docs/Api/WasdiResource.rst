WasdiResource
=============

Introduction
------------
Wasdi Resource.

Hosts lightweight utility APIs:

- Service keep-alive/health hello endpoint
- User feedback submission endpoint

All endpoints are under base path /wasdi.

Common Models
-------------
- PrimitiveResult:
	- IntValue, StringValue, DoubleValue, BoolValue
- FeedbackViewModel:
	- title, message

APIs
----

GET /wasdi/hello
^^^^^^^^^^^^^^^^
- Description: Keep-alive endpoint used to verify service availability.
- Success: 200 OK, body: PrimitiveResult
- Notes: returns StringValue = Hello Wasdi!!

POST /wasdi/feedback
^^^^^^^^^^^^^^^^^^^^
- Description: Sends a feedback message by email for authenticated user.
- Headers: x-session-token (required)
- Body: FeedbackViewModel
- Success: 200 OK, body: PrimitiveResult
- Notes:
	- On success returns IntValue = 201 and BoolValue = true.
	- Returns IntValue = 401 for missing/invalid session.
	- Returns IntValue = 404 for invalid payload (missing title/message).
