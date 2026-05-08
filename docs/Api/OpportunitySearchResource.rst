OpportunitySearchResource
=========================

Introduction
------------
Opportunity Search Resource.

Hosts the planning and orbit-search API used to:

- Search new satellite acquisition opportunities over an area and time window.
- Retrieve the current orbit track of one satellite or a compact position update for multiple satellites.
- Generate a KML polygon for a selected opportunity footprint.
- List the satellites, sensors, and sensor modes available for planning.

All endpoints are under the base path ``/searchorbit``.

All endpoints require a valid session via the ``x-session-token`` header.

Common Models
-------------
- OpportunitiesSearchViewModel: satelliteFilters, polygon, acquisitionStartTime, acquisitionEndTime
- SatelliteFilterViewModel: enable, satelliteName, satelliteSensors
- SatelliteResourceViewModel: satelliteName, satelliteSensors
- SensorViewModel: description, enable, sensorModes
- SensorModeViewModel: name, enable
- CoverageSwathResultViewModel: IdCoverageSwathResultViewModel, SwathName, SatelliteName, SensorName, SensorLookDirection, SensorMode, Angle, SensorType, CoveredAreaName, Coverage, AcquisitionStartTime, AcquisitionEndTime, AcquisitionDuration, CoverageWidth, CoverageLength, SwathFootPrint, FrameFootPrint, CoveredArea, IdTriggerType, FrameFootPrintGeometry, IsAscending, aoChilds
- SatelliteOrbitResultViewModel: satelliteName, currentPosition, currentTime, code, lastPositions, nextPositions, lastPositionsTime, nextPositionsTime

APIs
----

POST /searchorbit/search
^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Searches for new acquisition opportunities using the planning engine. The request includes the area of interest polygon, acquisition time window, and one or more enabled satellite filters with their enabled sensors and modes. The response contains frame-level opportunities plus one aggregate swath-level entry for each swath result.
- HTTP Verb: POST
- Headers: x-session-token
- Content-Type: application/json
- Query params: none
- Body: OpportunitiesSearchViewModel
	- polygon (string) — area of interest, typically a polygon geometry
	- acquisitionStartTime (string) — start of the planning window
	- acquisitionEndTime (string) — end of the planning window
	- satelliteFilters (array of SatelliteFilterViewModel)
		- enable (boolean)
		- satelliteName (string)
		- satelliteSensors (array of SensorViewModel)
			- description (string)
			- enable (boolean)
			- sensorModes (array of SensorModeViewModel)
				- name (string)
				- enable (boolean)
- Success:
	- 200 OK, body: array of CoverageSwathResultViewModel
- Notes:
	- The endpoint initializes ``nfs.data.download`` to ``<user.home>/nfs/download`` if the JVM property is missing.
	- The returned array includes both child/frame entries and one parent swath entry per result; each item gets an incremental ``IdCoverageSwathResultViewModel`` assigned server-side.
	- Returns an empty array on invalid session or many runtime errors, but returns ``null`` if the underlying planning engine returns ``null``.
- Return codes:
	- 200 OK (array body, empty array on many failures, or ``null`` if no planning result is produced)

GET /searchorbit/track/{satellitename}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the current orbit track for one satellite, including current position, current time, historical ground-track points, and future ground-track points.
- HTTP Verb: GET
- Headers: x-session-token
- Path params:
	- satellitename (string, required) — satellite code used by the orbit engine
- Query params: none
- Body: none
- Success:
	- 200 OK, body: SatelliteOrbitResultViewModel
- Notes:
	- On invalid session or if the satellite cannot be resolved, the endpoint returns an empty/default SatelliteOrbitResultViewModel instead of an HTTP error.
	- The JVM property ``nfs.data.download`` is initialized if missing.
	- Positions are returned as ``lat;lon;altitude`` strings.
- Return codes:
	- 200 OK (possibly with an empty/default body on failure)

GET /searchorbit/getkmlsearchresults
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Builds and returns a KML document for a supplied footprint polygon and label text. The footprint is converted into a placemark polygon styled in blue and clamped to ground.
- HTTP Verb: GET
- Headers: x-session-token
- Query params:
	- text (string, required) — placemark name/title
	- footPrint (string, required) — polygon footprint to render
- Body: none
- Success:
	- 200 OK, body: KML document
- Notes:
	- Returns ``null`` on invalid session, empty inputs, or conversion errors instead of an HTTP error response.
- Return codes:
	- 200 OK (KML body, or ``null`` on failure)

GET /searchorbit/updatetrack/{satellitesname}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns a compact current-position update for multiple satellites in one request. Satellite codes are passed as a single path parameter separated by ``-``.
- HTTP Verb: GET
- Headers: x-session-token
- Path params:
	- satellitesname (string, required) — one or more satellite codes separated by ``-``; a trailing ``-`` is ignored
- Query params: none
- Body: none
- Success:
	- 200 OK, body: array of SatelliteOrbitResultViewModel
- Notes:
	- Each result contains the satellite code, descriptive name, and current position only.
	- Invalid individual satellites are skipped; valid ones are still returned.
	- Returns ``null`` on invalid session or invalid/empty satellite input.
- Return codes:
	- 200 OK (array body, or ``null`` on failure)

GET /searchorbit/getsatellitesresource
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Description: Returns the list of satellites enabled for planning, together with their onboard sensors and available sensor modes. The source list comes from ``WasdiConfig.Current.plan.listOfSatellites``.
- HTTP Verb: GET
- Headers: x-session-token
- Query params: none
- Body: none
- Success:
	- 200 OK, body: array of SatelliteResourceViewModel
- Notes:
	- Returns an empty array on invalid session, missing plan configuration, missing satellite list, or many runtime issues.
	- Only satellites that can be successfully built by the orbit library are included.
- Return codes:
	- 200 OK (array body, possibly empty)
