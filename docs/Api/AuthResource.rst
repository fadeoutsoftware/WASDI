AuthResource
============

Introduction
------------
Authorization Resource.

Hosts the API for:

- User login management (classic WASDI credentials and Keycloak).
- Session validation and logout.
- User registration and first-access validation.
- User profile editing and password management.
- SFTP upload account management.
- Client UI configuration (skin, private missions).

All endpoints in this resource are under the base path /auth.

Unless differently noted, endpoints:

- Require header ``x-session-token`` when the user must be authenticated.
- Produce ``application/json``, ``application/xml``, and ``text/xml``.

Common Models
-------------
- PrimitiveResult: intValue, stringValue, doubleValue, boolValue
- UserViewModel: userId, name, surname, type, role, publicNickName, sessionId, skin
- LoginInfo: userId, userPassword, googleIdToken
- RegistrationInfoViewModel: userId, name, surname, password, googleIdToken, optionalValidationToken
- ChangeUserPasswordViewModel: currentPassword, newPassword
- SkinViewModel: logoImage, logoText, helpLink, supportLink, brandMainColor, brandSecondaryColor, tabTitle, favIcon, defaultCategories, activateSubscriptions
- PrivateMissionViewModel: missionName, missionIndexValue, missionOwner, userId, permissionType, permissionCreationDate, permissionCreatedBy

APIs
----

POST /auth/login
^^^^^^^^^^^^^^^^
- Description: Authenticates a user with their credentials. The system tries Keycloak first and falls back to the legacy WASDI password store. On first login of a Keycloak-verified user, their account is automatically registered in WASDI. On success, a new session token is created and returned inside the UserViewModel.
- HTTP Verb: POST
- Headers: none
- Query params: none
- Body: LoginInfo (required): userId, userPassword
- Success:
  - 200 OK, body: UserViewModel (with sessionId populated)
- Notes:
  - On failure, returns an invalid UserViewModel (userId empty, boolValue false) rather than an HTTP error code.
  - If ``disableAuthentication`` is set in server config, a default admin user is returned without credential checks (development only).
- Return codes:
  - 200 OK (also used to convey invalid-login, check userId in response)

GET /auth/checksession
^^^^^^^^^^^^^^^^^^^^^^
- Description: Validates an existing session token and returns the user profile associated with it. Used by the client to verify that a stored session is still active.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: UserViewModel
- Notes:
  - Returns an invalid UserViewModel (userId empty) when the session is not valid rather than an HTTP error code.
- Return codes:
  - 200 OK (check userId in response to distinguish valid vs invalid session)

GET /auth/logout
^^^^^^^^^^^^^^^^
- Description: Invalidates the given session, deleting the session record from the database. Returns a PrimitiveResult indicating whether the operation succeeded.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=sessionId)
- Notes:
  - Returns an invalid PrimitiveResult when the session is not found.
- Return codes:
  - 200 OK

POST /auth/register
^^^^^^^^^^^^^^^^^^^
- Description: Registers a new WASDI user. The userId (e-mail) must exist and be verified in Keycloak; the endpoint creates the user record in the WASDI database and automatically assigns a 90-day FREE trial subscription.
- HTTP Verb: POST
- Headers: none
- Query params: none
- Body: RegistrationInfoViewModel (required): userId (e-mail)
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, intValue=200, stringValue="Welcome to space")
- Return codes:
  - 200 OK (boolValue=true)
  - 304 Not Modified (intValue=304, user already registered)
  - 400 Bad Request (intValue=400, missing userId or payload)
  - 404 Not Found (intValue=404, userId not found in Keycloak)
  - 500 Internal Server Error (intValue=500)

GET /auth/validateNewUser
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Completes the legacy e-mail-based account activation flow. The link embedded in the confirmation e-mail points to this endpoint. When the validation code matches the stored token the user account is activated and a FREE trial subscription is created.
- HTTP Verb: GET
- Headers: none
- Query params:
  - email (string, required) — the user's e-mail address
  - validationCode (string, required) — the UUID token from the confirmation e-mail
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, stringValue=userId)
- Notes:
  - Returns invalid PrimitiveResult on any validation failure.
- Return codes:
  - 200 OK

POST /auth/editUserDetails
^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Allows an authenticated user to update their own profile fields: name, surname, link, description, and public nick name. Returns the updated UserViewModel.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: UserViewModel (required): name, surname, link, description, publicNickName
- Success:
  - 200 OK, body: UserViewModel (updated)
- Notes:
  - Returns invalid UserViewModel on validation failure or invalid session.
- Return codes:
  - 200 OK

POST /auth/changePassword
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Changes the WASDI password of the authenticated user. Requires the current password for verification before accepting the new one.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: ChangeUserPasswordViewModel (required): currentPassword, newPassword
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true)
- Notes:
  - Returns invalid PrimitiveResult on invalid session, wrong current password, or policy violation.
- Return codes:
  - 200 OK

GET /auth/lostPassword
^^^^^^^^^^^^^^^^^^^^^^
- Description: Initiates the password recovery flow. For WASDI-native accounts a new random password is generated and sent by e-mail. For Keycloak accounts a password-reset e-mail is triggered via Keycloak.
- HTTP Verb: GET
- Headers: none
- Query params:
  - userId (string, required) — the user's e-mail address
- Body: none
- Success:
  - 200 OK, body: PrimitiveResult (boolValue=true, intValue=0)
- Return codes:
  - 200 OK (boolValue=true, intValue=0)
  - 400 Bad Request (intValue=400, missing/invalid userId or user not found)
  - 500 Internal Server Error (intValue=500)

GET /auth/config
^^^^^^^^^^^^^^^^
- Description: Returns the client UI configuration object for the authenticated user. The configuration is resolved from the missions repository and includes data-provider settings and feature flags relevant to the user's context.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: ClientConfig
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

GET /auth/privatemissions
^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of private missions accessible to the authenticated user, including missions they own and missions that have been shared with them (with their permission level).
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: list of PrivateMissionViewModel
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

GET /auth/skin
^^^^^^^^^^^^^^
- Description: Returns the branding and UI configuration for the specified skin name. Used by the client to apply the correct colours, logos, and feature flags on start-up.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
  - skin (string, optional) — skin identifier; defaults to the server-configured default skin
- Body: none
- Success:
  - 200 OK, body: SkinViewModel
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

POST /auth/upload/createaccount
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Creates an SFTP upload account for the authenticated user and sends the generated credentials (username and password) to the provided e-mail address.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: plain-text string — recipient e-mail address
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (missing e-mail)
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

GET /auth/upload/existsaccount
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Checks whether an SFTP account already exists for the authenticated user.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: boolean (true if account exists)
- Notes:
  - Returns false also on invalid session; does not return HTTP error codes.
- Return codes:
  - 200 OK

GET /auth/upload/list
^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of file names present in the authenticated user's SFTP account.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK, body: array of strings (file names)
- Notes:
  - Returns null on invalid session; does not return HTTP error codes.
- Return codes:
  - 200 OK

DELETE /auth/upload/removeaccount
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Removes the SFTP account of the authenticated user.
- HTTP Verb: DELETE
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error

POST /auth/upload/updatepassword
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Generates a new random SFTP password for the authenticated user and sends it to the provided e-mail address.
- HTTP Verb: POST
- Headers: x-session-token
- Query params: none
- Body: plain-text string — recipient e-mail address
- Success:
  - 200 OK
- Return codes:
  - 200 OK
  - 400 Bad Request (invalid e-mail)
  - 401 Unauthorized (invalid session)
  - 500 Internal Server Error
