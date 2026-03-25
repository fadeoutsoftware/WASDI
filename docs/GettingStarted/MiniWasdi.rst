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

4.1 Enter the container (interactive shell)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Useful for inspection/debug and to check files such as ``/etc/wasdi/wasdiConfig.json`` from inside the image.

.. code-block:: powershell

   docker run -it --rm `
     -v "c:\temp\wasdi\miniwasdi:/data/wasdi" `
     --entrypoint /bin/bash `
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

6. Auto-created folders
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

7. Notes
---------

- If ``WASDI_RUN_APPLICATION`` is not provided, Mini-WASDI completes startup without running an app.
- Keep processor zip filenames lowercase to avoid naming inconsistencies.
