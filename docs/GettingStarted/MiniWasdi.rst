Mini-WASDI Docker Image Usage
==============================

This document explains how to run Mini-WASDI using the Docker Hub image:

``wasdi/wasdi:mwasdi``

1. Pull the image
------------------

.. code-block:: bash

   docker pull wasdi/wasdi:mwasdi

2. Prepare a host folder to mount in ``/data/wasdi``
------------------------------------------------------

Create a local folder that will be mounted to the container at ``/data/wasdi``.

Minimum required content:

- ``wasdiConfig.json``

You can use the sample config inside the container as a reference:

- ``/etc/wasdi/wasdiConfig.json``

Quick way to extract the sample config to your host folder (PowerShell):

.. code-block:: powershell

   docker run --rm `
     -v "c:\temp\wasdi\miniwasdi:/data/wasdi" `
     --entrypoint /bin/bash `
     wasdi/wasdi:mwasdi `
     -lc "cp /etc/wasdi/wasdiConfig.json /data/wasdi/wasdiConfig.json"

Quick way to extract the sample config to your host folder (Linux Bash):

.. code-block:: bash

   docker run --rm \
     -v "/tmp/wasdi/miniwasdi:/data/wasdi" \
     --entrypoint /bin/bash \
     wasdi/wasdi:mwasdi \
     -lc "cp /etc/wasdi/wasdiConfig.json /data/wasdi/wasdiConfig.json"

Suggested initial structure:

.. code-block:: text

   <your-folder>/
     wasdiConfig.json
     install/
       processors/
       workflows/

3. Optional startup content (``install`` folder)
-------------------------------------------------

The ``install`` folder is optional, but recommended.

3.1 Processors (``install/processors``)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Put WASDI application zip files in ``install/processors``.
- At startup, Mini-WASDI scans this folder.
- If an app is not installed yet, it is installed automatically.
- The app name is derived from the zip filename (without extension).
- Processor names must be lowercase.
- If already installed, Mini-WASDI compares update timestamps:

  - zip file timestamp (host file)
  - processor last update in WASDI

- If the zip is newer, the processor is updated automatically at startup.

3.2 Workflows (``install/workflows``)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Put SNAP workflow xml files in ``install/workflows``.
- At startup, Mini-WASDI scans this folder.
- New workflows are uploaded automatically.
- Existing workflows are updated if the xml file is newer.

4. Run the container
---------------------

PowerShell example:

.. code-block:: powershell

   docker run --rm `
     -v "c:\temp\wasdi\miniwasdi:/data/wasdi" `
     -e WASDI_RUN_APPLICATION="hello_test" `
     -e WASDI_PARAMS='{}' `
     -e WASDI_WORKSPACE="Test_ws" `
     -e WASDI_CONFIG_FILE="/data/wasdi/wasdiConfig.json" `
     wasdi/wasdi:mwasdi

Linux Bash example:

.. code-block:: bash

   docker run --rm \
     -v "/tmp/wasdi/miniwasdi:/data/wasdi" \
     -e WASDI_RUN_APPLICATION="hello_test" \
     -e WASDI_PARAMS='{}' \
     -e WASDI_WORKSPACE="Test_ws" \
     -e WASDI_CONFIG_FILE="/data/wasdi/wasdiConfig.json" \
     wasdi/wasdi:mwasdi

4.1 Enter the container (interactive shell)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Useful for inspection/debug and to check files such as ``/etc/wasdi/wasdiConfig.json`` from inside the image.

.. code-block:: powershell

   docker run -it --rm `
     -v "c:\temp\wasdi\miniwasdi:/data/wasdi" `
     --entrypoint /bin/bash `
     wasdi/wasdi:mwasdi

Linux Bash equivalent:

.. code-block:: bash

   docker run -it --rm \
     -v "/tmp/wasdi/miniwasdi:/data/wasdi" \
     --entrypoint /bin/bash \
     wasdi/wasdi:mwasdi

Once inside the container, for example, you can copy the sample config:

.. code-block:: bash

   cp /etc/wasdi/wasdiConfig.json /data/wasdi/wasdiConfig.json

5. Environment variables
-------------------------

- ``WASDI_RUN_APPLICATION``

  - Name of the application to run.
  - It must already be installed (or installed at startup from ``install/processors``).

- ``WASDI_PARAMS``

  - JSON string with input parameters for the application.
  - Example: ``'{}'``

- ``WASDI_WORKSPACE``

  - Workspace name.
  - If omitted, Mini-WASDI uses the default workspace.

- ``WASDI_WORKSPACE_ID``

  - Optional alternative to ``WASDI_WORKSPACE``.
  - Use this to target a workspace by id.

- ``WASDI_CONFIG_FILE``

  - Path to the config file inside the container.
  - Typical value: ``/data/wasdi/wasdiConfig.json``


- ``WASDI_CLEAR_HISTORY``

  - Optional flag to clear past process workspaces at startup. By default, the system keeps all past process workspaces, which can be useful for debugging and historical reference. Setting this flag to ``true`` will delete all past process workspaces and allow also to start processes marked as CREATED from a previous run. If this flag is set, the system will clean the CREATED queue and delete all the processes that have been already executed.
  - Typical value: ``1`` or ``true``. If it is not set or have any other value is considered false.


6. Configuration
-----------------

The ``wasdiConfig.json`` file controls all Mini-WASDI behavior. For detailed documentation, see the :doc:`Configuration guide </InsideWasdi/Configuration>` or visit the `online documentation <https://wasdi.readthedocs.io/en/latest/InsideWasdi/Configuration.html>`_.

Key configuration settings for Mini-WASDI:

**Logging**

- ``logLevel``: Sets the general log level for all components (e.g., ``DEBUG``, ``INFO``, ``WARNING``, ``ERROR``).
- ``logLevelServer``, ``logLevelLauncher``, ``logLevelScheduler``: Component-specific log level overrides (optional).

**Database Engine**

- ``dbEngine``: Must be set to ``sqlitedb`` for Mini-WASDI. 
- MongoDB and NO2 databases are supported by WASDI in production deployments but are not included in the Docker container.

**Data Providers and Catalogues**

Data Providers are external services that supply Earth Observation data from missions/platforms such as Sentinel, Landsat, and others.

- Each **data provider** can supply data from one or more **missions/platforms**.
- Each **platform** can be searched through one or more **catalogues**.
- Data providers are configured with credentials to authenticate with the remote service.

The basic config declares all available data providers. You only need to provide credentials for the providers you intend to use.

Each data provider configuration includes:

.. code-block:: json

   {
     "user": "",
     "password": "",
     "apiKey": ""
   }

The specific fields required depend on the provider—typically ``user`` and ``password`` are most common.

**Recommended Data Providers**

For a minimum usage, we strongly recommend configuring at least these free services:

1. **Copernicus Data Space** – Free account, uses ``user`` and ``password``
   - URL: `https://dataspace.copernicus.eu/ <https://dataspace.copernicus.eu/>`_
   - Provides access to Sentinel-1, Sentinel-2, and other Copernicus missions
   
2. **CREODIAS** – Free account, uses ``user`` and ``password``
   - Often provides fast and reliable access to extensive Earth Observation catalogues
   - Supports multiple missions and platforms


7. Auto-created folders
------------------------

Mini-WASDI creates required folders automatically under the mounted base path.

Main folders:

- ``appwasdi.home``
- ``log``
- ``processors``
- ``sqlitedb``
- ``tmp``
- ``user``
- ``workflows``

Folder purpose:

- ``processors``: installed processors and their virtual environments.
- ``workflows``: installed SNAP graph xml files.
- ``sqlitedb``: sqlite repository.
- ``tmp``: temporary operations.
- ``user``: output/user data. It contains one subfolder per workspace id.

If applications use SNAP, ``appwasdi.home`` and ``log`` are also created/used.

8. Notes
---------

- If ``WASDI_RUN_APPLICATION`` is not provided, Mini-WASDI completes startup without running an app.
- Keep processor zip filenames lowercase to avoid naming inconsistencies.

9. ExtWebDataProviders
-----------------------

ExtWebDataProviders is a special WASDI extension point that allows users to inject their own external data provider.

The integration is based on a simple API contract.
This data provider allow each Mini WASDI User to connect directly their own data provider to WASDI, without the need to implement a full WASDI extension point, and without the need to modify the WASDI codebase.

Using Ext Web Data Providers, the user can decide also to use its own code also for the data already available in WASDI, for example Sentinel-1 or Sentinel-2. 
To connect Ext Web Data Provider just go in the configuration. If you are supporting an existing platform, you can change or add "EXT_WEB" in the relative Catalogue Entry. Then in the "dataProviders" section, search the configuration for the "EXT_WEB" provider and add the supported platforms.
Finally, you need to edit the extWebAdapterConfig.json file. 

In your config file, under "dataProviders" -> EXT_WEB you can change the adapterConfig and parserConfig. By default are  ``/etc/wasdi/extWebAdapterConfig.json`` and the file is empty.
You can put your version for example in /data/wasdi/extWebAdapterConfig.json.

In extWebAdapterConfig.json, for each of your servers, you need to specify the baseUrl, the mission/platform name and the apiKey for authentication. The baseUrl is the URL of your server that implements the required endpoints (see below). The mission/platform name must match the one declared in the "EXT_WEB" catalogue entry. The apiKey is optional and can be used for provider-side authentication if needed.

Example:

.. code-block:: json

   {
     "providers": [
       {
         "baseUrl": "https://your_url/your_path/",
         "mission": "YourPlatformName",
         "apiKey": "YourAPIKey"
       }
     ]
   }


Required endpoints
~~~~~~~~~~~~~~~~~~

- ``/hello`` (GET)
  - Health endpoint used to verify that the provider service is alive.

- ``/query/count`` (POST)
  - Receives a ``QueryViewModel`` payload.
  - Must return the number of matching items.

- ``/query/list`` (POST)
  - Receives a ``QueryViewModel`` payload.
  - Must return an array of ``QueryResultViewModel`` objects.

- ``/download`` (GET)
  - Streams the requested file to WASDI.
  - Uses query parameter ``fileName`` to identify the file to download.
  - Can use request header ``x-api-key`` for provider-side authorization.

QueryViewModel
~~~~~~~~~~~~~~

Fields expected by the provider service:

.. code-block:: text

   offset: int = -1
   limit: int = -1
   north: Double = null
   south: Double = null
   east: Double = null
   west: Double = null

   startFromDate: String
   startToDate: String
   endFromDate: String
   endToDate: String

   platformName: String
   productType: String
   productLevel: String
   relativeOrbit: int = -1
   absoluteOrbit: int = -1
   cloudCoverageFrom: Double = null
   cloudCoverageTo: Double = null
   sensorMode: String

   productName: String = null
   timeliness: String = ""
   polarisation: String
   platformSerialIdentifier: String
   instrument: String

   filters: Map<String, String> = {}

Notes:

- ``filters`` is a generic map for additional custom filters not explicitly modeled.
- Date and string formats should be consistent with your provider implementation and the consuming WASDI workflow.

QueryResultViewModel
~~~~~~~~~~~~~~~~~~~~

Fields expected in each search result item:

.. code-block:: text

   preview: String = ""
   title: String = ""
   summary: String = ""
   id: String = ""
   link: String = ""
   footprint: String = ""
   provider: String = ""
   properties: Map<String, String> = {}
   volumeName: String = ""
   volumePath: String = ""
   platform: String = ""

Notes:

- ``summary`` can include readable metadata such as date, instrument, mode, satellite, and size.
- ``footprint`` should contain the item geometry in WKT format.
- ``link`` should identify how the provider resolves the file during ``/download``.

10. Tuning your MiniWasdi
--------------------------

WASDI applications are optimized for scientists. An app can be standalone, or it can call linked apps, run SNAP workflows, import images, and chain multiple operations.

MiniWasdi can run processes one by one, but it is designed to leverage available hardware through scheduler queues. Queue behavior can be configured in the scheduler section of ``wasdiConfig.json``.

Example of a dedicated queue:

.. code-block:: json

   "schedulers": [
     {
       "name": "DOWNLOAD.LSA",
       "maxQueue": "5",
       "timeoutMs": "3600000",
       "opTypes": "DOWNLOAD",
       "opSubType": "LSA",
       "enabled": "1"
     }
   ]

This configuration means that ``DOWNLOAD`` operations for the ``LSA`` provider run in an independent queue with size ``5``.

The ``name`` field is only a label. A common convention is ``operation.suboperation`` (for example ``DOWNLOAD.LSA``), but routing is driven by ``opTypes`` and ``opSubType``.

Users can add or remove queues as needed, and tune ``maxQueue`` values according to available CPU, RAM, disk, and I/O throughput.

Two important queues to tune are:

.. code-block:: json

   {
     "name": "GRAPH",
     "maxQueue": "2",
     "timeoutMs": "10800000",
     "opTypes": "GRAPH",
     "opSubType": "",
     "enabled": "1"
   },
   {
     "name": "RUNPROCESSOR",
     "maxQueue": "5",
     "timeoutMs": "10800000",
     "opTypes": "RUNPROCESSOR",
     "opSubType": "",
     "enabled": "1"
   }

- ``GRAPH`` controls concurrency for SNAP graph executions.
- ``RUNPROCESSOR`` controls concurrency for WASDI processor executions.

On more powerful hardware, these queue sizes can be increased.

Operations without a dedicated queue definition are handled by the default scheduler.

Valid operations are:
- ``RUNPROCESSOR``: WASDI processor execution.
- ``GRAPH``: SNAP graph execution.
- ``DOWNLOAD``: Data download from external providers. Can have a subType for provider-specific queues.
- ``INGEST``: 
- ``MOSAIC``: 
- ``KILL``: 
- ``MULTISUBSET``: 
- ``DELETEPROCESSOR``: 