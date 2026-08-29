OGC API - Processes
===================

WASDI is compliant with the `OGC API - Processes standard <https://ogcapi.ogc.org/processes/>`_. It provides an OGC API - Processes interface to run WASDI applications and to deploy applications created according to the Application Package Best Practice.

Exposed Endpoints
-----------------

The ``ogcprocesses`` project exposes the following endpoint groups.

CoreResource
~~~~~~~~~~~~

* ``GET /``: landing page.
* ``GET /conformance``: implemented conformance classes.

ProcessesResource
~~~~~~~~~~~~~~~~~

* ``GET /processes``: list the WASDI applications available to the authenticated user.
* ``GET /processes/{processID}``: retrieve a process description.
* ``POST /processes/{processID}/execution``: execute a WASDI application.
* ``POST /processes/applications``: deploy an application package as a WASDI processor.

JobsResource
~~~~~~~~~~~~

* ``GET /jobs``: list jobs for the authenticated user.
* ``GET /jobs/{jobId}``: retrieve job status.
* ``DELETE /jobs/{jobId}``: dismiss a job.
* ``GET /jobs/{jobId}/results``: retrieve job results.