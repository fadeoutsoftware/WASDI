ProcessorsMediaResource
=======================

Introduction
------------
Processors Media Resource.

Hosts the API for app-store related processor media and interactions:

- Listing app-store categories and visible publishers.
- Managing processor reviews (add, update, delete, list).
- Managing review comments (add, update, delete, list).

All endpoints are under the base path ``/processormedia``.

All endpoints require a valid session via the ``x-session-token`` header.

Permission and ownership notes:

- Access to reviews/comments is guarded through access to the underlying processor.
- Delete operations for reviews/comments require ownership checks on the target entity.
- Some IDs received as query parameters are URL-decoded server-side before use.

Common Models
-------------
- AppCategoryViewModel: id, category, count
- ReviewViewModel: id, processorId, userId, vote, date, title, comment
- ListReviewsViewModel: reviews, avgVote, numberOfOneStarVotes, numberOfTwoStarVotes, numberOfThreeStarVotes, numberOfFourStarVotes, numberOfFiveStarVotes, alreadyVoted
- CommentDetailViewModel: commentId, reviewId, userId, date, text
- CommentListViewModel: commentId, reviewId, userId, date, text
- PublisherFilterViewModel: publisher, nickName, appCount

APIs
----

GET /processormedia/categories/get
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of all application categories available in the app store.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
	- 200 OK, body: array of AppCategoryViewModel
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)

POST /processormedia/reviews/add
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Adds a new review for a processor.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: ReviewViewModel
	- processorId (string, required)
	- vote (float, required, valid range 0.0 to 5.0)
	- title (string, optional)
	- comment (string, optional)
- Success:
	- 200 OK
- Notes:
	- One review per processor per user is enforced (already voted -> rejected).
	- Caller must be allowed to access the processor.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid payload, vote out of range, missing/invalid processor, or already voted)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access processor)

POST /processormedia/reviews/update
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates an existing review.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: ReviewViewModel
	- id (string, required) — review ID to update
	- processorId (string, required)
	- vote (float, required, valid range 0.0 to 5.0)
	- title (string, optional)
	- comment (string, optional)
- Success:
	- 200 OK
- Notes:
	- The endpoint validates vote range, then updates the repository entry.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid payload, invalid vote, or update failed)
	- 401 Unauthorized (invalid session)

DELETE /processormedia/reviews/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes a review by processor/review ID and removes all comments associated with that review.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
	- processorId (string, required) — processor ID (URL-decoded server-side)
	- reviewId (string, required) — review ID (URL-decoded server-side)
- Body: none
- Success:
	- 200 OK
- Notes:
	- Caller must be the owner of the review.
	- After successful review deletion, child comments are deleted.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid processor/review or delete count is zero)
	- 401 Unauthorized (invalid session or user is not review owner)

GET /processormedia/reviews/getlist
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns paginated reviews for a processor plus aggregate rating statistics.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- processorName (string, required) — processor name
	- page (integer, optional) — page index, default 0
	- itemsperpage (integer, optional) — items per page, default 4
- Body: none
- Success:
	- 200 OK, body: ListReviewsViewModel
- Notes:
	- ``alreadyVoted`` is set for the current user.
	- If no reviews exist, returns an empty ``ListReviewsViewModel``.
	- Pagination is applied after computing full statistics.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid processor)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access processor)

POST /processormedia/comments/add
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Adds a comment to an existing review.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: CommentDetailViewModel
	- reviewId (string, required)
	- text (string, required)
- Success:
	- 200 OK
- Notes:
	- The endpoint resolves review -> processor and enforces processor access permissions.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid payload, missing/invalid review, or missing processor)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access processor)

POST /processormedia/comments/update
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Updates an existing comment.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: CommentDetailViewModel
	- commentId (string, required)
	- reviewId (string, required)
	- text (string, required)
- Success:
	- 200 OK
- Notes:
	- The updated comment entity always uses the caller as userId and the current timestamp.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid payload or update failed)
	- 401 Unauthorized (invalid session)

DELETE /processormedia/comments/delete
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Deletes one comment from a review.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params:
	- reviewId (string, required) — parent review ID (URL-decoded server-side)
	- commentId (string, required) — comment ID (URL-decoded server-side)
- Body: none
- Success:
	- 200 OK
- Notes:
	- Caller must be the owner of the comment.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid review/comment or delete count is zero)
	- 401 Unauthorized (invalid session or user is not comment owner)

GET /processormedia/comments/getlist
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns all comments associated with a review.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- reviewId (string, required)
- Body: none
- Success:
	- 200 OK, body: array of CommentListViewModel
- Notes:
	- If there are no comments, returns an empty ``CommentListViewModel`` object.
	- Access to comments is validated through the review's processor.
- Return codes:
	- 200 OK
	- 400 Bad Request (invalid review or missing processor)
	- 401 Unauthorized (invalid session)
	- 403 Forbidden (user cannot access processor)

GET /processormedia/publisher/getlist
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns visible publishers in the marketplace with the number of visible apps per publisher.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
	- 200 OK, body: array of PublisherFilterViewModel
- Notes:
	- Includes only deployed processors that are shown in store.
	- For non-public processors, entries are included only if the caller is owner or has sharing permissions.
	- ``nickName`` is taken from the publisher's public nickname, then falls back to name, then to user ID.
- Return codes:
	- 200 OK
	- 401 Unauthorized (invalid session)
	- 500 Internal Server Error (unable to load deployed processors)
