WASDI Configuration Documentation
=================================

This document describes the structure and configuration options for the
``wasdiConfig.json`` file, which contains all WASDI system configuration
settings.

Overview
--------

The main WASDI configuration is stored in a single JSON file that
represents the ``WasdiConfig`` Java object and all its child
configuration objects. This configuration is used by all WASDI
components including:

-  Launcher
-  Trigger
-  Scheduler
-  Web Server
-  Database Utils

Main }
------

}

::



   ### Root Level Properties

   ```json
   {
     "nodeCode": "wasdi",
     "printServerAddress": "",
     "mainNodeCloud": "CREODIAS",
     "usersDefaultNode": "wasdi",
     "defaultSkin": "wasdi",
     "activateSubscriptionChecks": true,
     "systemUserName": "appwasdi",
     "systemUserId": 2042,
     "systemGroupName": "appwasdi", 
     "systemGroupId": 2042,
     "baseUrl": "https://www.wasdi.net/wasdiwebserver/rest/",
     "connectionTimeout": 10000,
     "readTimeout": 10000,
     "msWaitAfterChmod": 1000,
     "shellExecLocally": true,
     "useLog4J": true,
     "logHttpCalls": true,
     "logLevel": "INFO",
     "logLevelServer": "",
     "logLevelLauncher": "",
     "logLevelScheduler": "",
     "logLevelTrigger": "",
     "addDateTimeToLogs": false,
     "filterInternalHttpCalls": true,
     "useNotebooksDockerAddress": false,
     "nvidiaGPUAvailable": false
   }

Root Properties Description
^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``nodeCode``          | String      | Unique identifier for this    |
|                       |             | WASDI node (default: “wasdi”) |
+-----------------------+-------------+-------------------------------+
| `                     | String      | URL of the WASDI print server |
| `printServerAddress`` |             |                               |
+-----------------------+-------------+-------------------------------+
| ``mainNodeCloud``     | String      | Cloud provider of the main    |
|                       |             | node (e.g., “CREODIAS”).      |
|                       |             | Cloud Providers are just      |
|                       |             | strings. They are used here   |
|                       |             | in the main section, and in   |
|                       |             | the dataProviders Config.     |
|                       |             | This cloud name is used to    |
|                       |             | understand if the node is in  |
|                       |             | the same cloud of the         |
|                       |             | dataProvider or not, and this |
|                       |             | information is useful to      |
|                       |             | select the best data fetching |
|                       |             | option.                       |
+-----------------------+-------------+-------------------------------+
| ``usersDefaultNode``  | String      | Default node assigned to new  |
|                       |             | users                         |
+-----------------------+-------------+-------------------------------+
| ``defaultSkin``       | String      | Default UI skin to apply      |
|                       |             | (default: “wasdi”)            |
+-----------------------+-------------+-------------------------------+
| ``activat             | String      | Flag to activate or not the   |
| eSubscriptionChecks`` |             | check and managament of       |
|                       |             | subscriptions                 |
+-----------------------+-------------+-------------------------------+
| ``systemUserName``    | String      | System user name for WASDI    |
|                       |             | processes (default:           |
|                       |             | “appwasdi”)                   |
+-----------------------+-------------+-------------------------------+
| ``systemUserId``      | Integer     | System user ID (default:      |
|                       |             | 2042)                         |
+-----------------------+-------------+-------------------------------+
| ``systemGroupName``   | String      | System group name (default:   |
|                       |             | “appwasdi”)                   |
+-----------------------+-------------+-------------------------------+
| ``systemGroupId``     | Integer     | System group ID (default:     |
|                       |             | 2042)                         |
+-----------------------+-------------+-------------------------------+
| ``baseUrl``           | String      | Base URL of WASDI API         |
+-----------------------+-------------+-------------------------------+
| ``connectionTimeout`` | Integer     | Connection timeout for        |
|                       |             | third-party API calls (ms)    |
+-----------------------+-------------+-------------------------------+
| ``readTimeout``       | Integer     | Read timeout for third-party  |
|                       |             | API calls (ms)                |
+-----------------------+-------------+-------------------------------+
| ``msWaitAfterChmod``  | Integer     | Wait time after chmod         |
|                       |             | commands (ms)                 |
+-----------------------+-------------+-------------------------------+
| ``shellExecLocally``  | Boolean     | Execute external components   |
|                       |             | locally vs using Docker       |
|                       |             | commands. WASDI uses          |
|                       |             | different command line tools  |
|                       |             | (e.g., gdalinfo, gdal_warp)   |
|                       |             | and Python scripts for        |
|                       |             | utilities and data providers. |
|                       |             | When ``true``, commands are   |
|                       |             | executed as system-based      |
|                       |             | shell exec (fully tested on   |
|                       |             | Ubuntu, mostly compatible     |
|                       |             | with Windows). When           |
|                       |             | ``false``, WASDI uses fully   |
|                       |             | dockerized mode and checks    |
|                       |             | each shell exec command       |
|                       |             | against the                   |
|                       |             | ``shellExecCommands``         |
|                       |             | configuration to determine    |
|                       |             | whether to use system shell   |
|                       |             | exec or docker run command.   |
|                       |             | See Docker Configuration      |
|                       |             | section for more details.     |
+-----------------------+-------------+-------------------------------+
| ``useLog4J``          | Boolean     | Use Log4J configuration vs    |
|                       |             | standard output               |
+-----------------------+-------------+-------------------------------+
| ``logHttpCalls``      | Boolean     | Enable HTTP call logging      |
+-----------------------+-------------+-------------------------------+
| ``logLevel``          | String      | General log level             |
+-----------------------+-------------+-------------------------------+
| ``logLevelServer``    | String      | Web server specific log level |
+-----------------------+-------------+-------------------------------+
| ``logLevelLauncher``  | String      | Launcher specific log level   |
+-----------------------+-------------+-------------------------------+
| ``logLevelScheduler`` | String      | Scheduler specific log level  |
+-----------------------+-------------+-------------------------------+
| ``logLevelTrigger``   | String      | Trigger specific log level    |
+-----------------------+-------------+-------------------------------+
| ``addDateTimeToLogs`` | Boolean     | Add timestamp to log lines    |
+-----------------------+-------------+-------------------------------+
| ``filt                | Boolean     | Filter internal HTTP calls    |
| erInternalHttpCalls`` |             | from HTTP logs                |
+-----------------------+-------------+-------------------------------+
| ``useNot              | Boolean     | Use Docker internal names for |
| ebooksDockerAddress`` |             | Jupyter notebooks             |
+-----------------------+-------------+-------------------------------+
| `                     | Boolean     | Enable NVIDIA GPU support for |
| `nvidiaGPUAvailable`` |             | containers: set it true if    |
|                       |             | this node have a GPU          |
+-----------------------+-------------+-------------------------------+

Database Configuration
----------------------

MongoDB Configurations
~~~~~~~~~~~~~~~~~~~~~~

WASDI supports multiple MongoDB instances for different purposes:

.. code:: json

   {
     "mongoMain": {
       "address": "localhost",
       "port": 27017,
       "dbName": "wasdi",
       "user": "username",
       "password": "password",
       "replicaName": ""
     },
     "mongoLocal": {
       "address": "localhost",
       "port": 27017,
       "dbName": "wasdilocal",
       "user": "username",
       "password": "password",
       "replicaName": ""
     }
   }

The most important is the mongoMain section: this must target the main
Mongo Db of the main node. All the computational nodes MUST be able to
reach this database: usually this is done using an SSH tunnel.

The main node MUST have only mongoMain.

Any computational node MUST have mongoLocal: this is a small subset of
the main database with only the collections that are used locally by the
computational node.

MongoDB Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

=============== ======= ================================
Property        Type    Description
=============== ======= ================================
``address``     String  MongoDB server address
``port``        Integer MongoDB server port
``dbName``      String  Database name
``user``        String  Database username
``password``    String  Database password
``replicaName`` String  Replica set name (if applicable)
=============== ======= ================================

Authentication Configuration
----------------------------

Keycloak Configuration
~~~~~~~~~~~~~~~~~~~~~~

.. code:: json

   {
     "keycloack": {
       "address": "https://auth.wasdi.net",
       "cliSecret": "cli-secret",
       "authTokenAddress": "https://auth.wasdi.net/auth/realms/wasdi/protocol/openid-connect/token",
       "introspectAddress": "https://auth.wasdi.net/auth/realms/wasdi/protocol/openid-connect/token/introspect",
       "confidentialClient": "wasdi-confidential",
       "client": "wasdi",
       "clientSecret": "client-secret",
       "realm": "wasdi",
       "sessionExpireHours": 24
     }
   }

Keycloak Properties
^^^^^^^^^^^^^^^^^^^

====================== ======= ================================
Property               Type    Description
====================== ======= ================================
``address``            String  Keycloak server address
``cliSecret``          String  CLI client secret
``authTokenAddress``   String  Token endpoint URL
``introspectAddress``  String  Token introspection endpoint URL
``confidentialClient`` String  Confidential client name
``client``             String  Public client name
``clientSecret``       String  Client secret
``realm``              String  Keycloak realm name
``sessionExpireHours`` Integer Session expiration time in hours
====================== ======= ================================

File System Paths Configuration
-------------------------------

.. code:: json

   {
     "paths": {
       "downloadRootPath": "/data/wasdi/",
       "serializationPath": "/data/wasdi/params/",
       "metadataPath": "/data/wasdi/metadata/",
       "dockerTemplatePath": "/data/wasdi/dockertemplate/",
       "sftpRootPath": "/data/wasdi/sftp/",
       "geoserverDataDir": "/data/wasdi/geoserver/",
       "sen2CorePath": "/usr/local/sen2cor/",
       "userHomePath": "/home/appwasdi/",
       "missionsConfigFilePath": "/opt/wasdi/config/missions.json",
       "gdalPath": "/usr/bin/",
       "wasdiTempFolder": "/tmp/wasdi/",
       "pythonExecPath": "/usr/bin/python3",
       "traefikMountedVolume": "/data/wasdi/traefik/",
       "s3VolumesBasePath": "/mnt/wasdi/users-volumes/",
       "wasdiConfigFilePath": "/opt/wasdi/config/wasdiConfig.json"
     }
   }

Paths Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``downloadRootPath``  | String      | Base root path containing     |
|                       |             | subfolders (workspaces,       |
|                       |             | metadata, styles, workflows,  |
|                       |             | processors, images)           |
+-----------------------+-------------+-------------------------------+
| ``serializationPath`` | String      | Path where parameters are     |
|                       |             | serialized                    |
+-----------------------+-------------+-------------------------------+
| ``metadataPath``      | String      | Metadata files path           |
+-----------------------+-------------+-------------------------------+
| `                     | String      | Docker templates path         |
| `dockerTemplatePath`` |             |                               |
+-----------------------+-------------+-------------------------------+
| ``sftpRootPath``      | String      | Root of the local SFTP server |
+-----------------------+-------------+-------------------------------+
| ``geoserverDataDir``  | String      | Geoserver data directory      |
+-----------------------+-------------+-------------------------------+
| ``sen2CorePath``      | String      | Sen2Core binary path          |
+-----------------------+-------------+-------------------------------+
| ``userHomePath``      | String      | User home path                |
+-----------------------+-------------+-------------------------------+
| ``mis                 | String      | Missions configuration file   |
| sionsConfigFilePath`` |             | path                          |
+-----------------------+-------------+-------------------------------+
| ``gdalPath``          | String      | GDAL binary path              |
+-----------------------+-------------+-------------------------------+
| ``wasdiTempFolder``   | String      | Temporary files folder        |
+-----------------------+-------------+-------------------------------+
| ``pythonExecPath``    | String      | Full path to Python           |
|                       |             | executable                    |
+-----------------------+-------------+-------------------------------+
| ``t                   | String      | Traefik mounted volume path   |
| raefikMountedVolume`` |             |                               |
+-----------------------+-------------+-------------------------------+
| ``s3VolumesBasePath`` | String      | S3 volumes mount folder path  |
+-----------------------+-------------+-------------------------------+
| ``                    | String      | Path to this configuration    |
| wasdiConfigFilePath`` |             | file                          |
+-----------------------+-------------+-------------------------------+

Docker Configuration
--------------------

.. code:: json

   {
     "dockers": {
       "extraHosts": [],
       "pipInstallWasdiAddress": "https://pypi.org/simple/",
       "internalDockersBaseAddress": "localhost",
       "numberOfAttemptsToPingTheServer": 4,
       "millisBetweenAttmpts": 5000,
       "millisBetweenStatusPolling": 1000,
       "numberOfPollStatusPollingCycleForLog": 30,
       "millisWaitAfterDelete": 15000,
       "millisWaitAfterDeployScriptCreated": 2000,
       "millisWaitForLogin": 4000,
       "dockerComposeCommand": "docker-compose",
       "internalDockerAPIAddress": "http://127.0.0.1:2375/",
       "dockersDeployLogFilePath": "/var/log/wasdi/dockers.log",
       "logDockerAPICallsPayload": false,
       "removeDockersAfterShellExec": true,
       "removeParameterFilesForPythonsShellExec": true,
       "dockerNetworkMode": "net-wasdi",
       "processorsInternalPort": 5000,
       "groupAdd": [],
       "registers": [],
       "shellExecCommands": {},
       "processorTypes": [],
       "eoepca": {}
     }
   }

Docker Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``extraHosts``        | Ar          | Extra hosts to add to         |
|                       | ray[String] | containers (may be needed in  |
|                       |             | some clouds for network       |
|                       |             | reasons)                      |
+-----------------------+-------------+-------------------------------+
| ``pip                 | String      | Address to use to access PyPI |
| InstallWasdiAddress`` |             | to install waspy              |
+-----------------------+-------------+-------------------------------+
| ``interna             | String      | Address to use to reach       |
| lDockersBaseAddress`` |             | internal dockers (default:    |
|                       |             | “localhost”)                  |
+-----------------------+-------------+-------------------------------+
| ``numberOfAtte        | Integer     | Number of attempts to ping    |
| mptsToPingTheServer`` |             | server before deciding it’s   |
|                       |             | down (default: 4)             |
+-----------------------+-------------+-------------------------------+
| ``m                   | Integer     | Time (ms) to wait between     |
| illisBetweenAttmpts`` |             | attempts to check if docker   |
|                       |             | is started (default: 5000)    |
+-----------------------+-------------+-------------------------------+
| ``millisB             | Integer     | Time (ms) to wait between     |
| etweenStatusPolling`` |             | calls to docker engine API    |
|                       |             | for status (default: 1000)    |
+-----------------------+-------------+-------------------------------+
| ``numberOfPollStatu   | Integer     | Number of cycles before       |
| sPollingCycleForLog`` |             | waitContainer function logs   |
|                       |             | activity (default: 30)        |
+-----------------------+-------------+-------------------------------+
| ``mi                  | Integer     | Time (ms) to wait for docker  |
| llisWaitAfterDelete`` |             | to complete delete operation  |
|                       |             | (default: 15000)              |
+-----------------------+-------------+-------------------------------+
| ``millisWaitAfter     | Integer     | Time (ms) to wait after       |
| DeployScriptCreated`` |             | deploy.sh file creation       |
|                       |             | (default: 2000)               |
+-----------------------+-------------+-------------------------------+
| `                     | Integer     | Time (ms) to wait for docker  |
| `millisWaitForLogin`` |             | login operation (default:     |
|                       |             | 4000)                         |
+-----------------------+-------------+-------------------------------+
| ``d                   | String      | Command to use to start       |
| ockerComposeCommand`` |             | docker compose (default:      |
|                       |             | “docker-compose”)             |
+-----------------------+-------------+-------------------------------+
| ``inter               | String      | Address of the local Docker   |
| nalDockerAPIAddress`` |             | instance API (default:        |
|                       |             | “http://127.0.0.1:2375/”)     |
+-----------------------+-------------+-------------------------------+
| ``docke               | String      | Path of file with docker      |
| rsDeployLogFilePath`` |             | build logs (default:          |
|                       |             | “/var/log/wasdi/dockers.log”) |
+-----------------------+-------------+-------------------------------+
| ``logDo               | Boolean     | Enable logging of payload for |
| ckerAPICallsPayload`` |             | Docker Engine API calls       |
|                       |             | (default: false)              |
+-----------------------+-------------+-------------------------------+
| ``removeDo            | Boolean     | Remove containers after shell |
| ckersAfterShellExec`` |             | execute (true for production, |
|                       |             | false for debug)              |
+-----------------------+-------------+-------------------------------+
| `                     | Boolean     | Remove input/output files     |
| `removeParameterFiles |             | after Python shell exec (true |
| ForPythonsShellExec`` |             | for production)               |
+-----------------------+-------------+-------------------------------+
| ``dockerNetworkMode`` | String      | Docker network mode (default: |
|                       |             | “net-wasdi”, can be           |
|                       |             | overridden by shell exec      |
|                       |             | items)                        |
+-----------------------+-------------+-------------------------------+
| ``pro                 | Integer     | Standard processors internal  |
| cessorsInternalPort`` |             | port (default: 5000)          |
+-----------------------+-------------+-------------------------------+
| ``groupAdd``          | Ar          | List of group IDs to add to   |
|                       | ray[String] | docker create command         |
+-----------------------+-------------+-------------------------------+
| ``registers``         | Array       | List of supported docker      |
|                       | [DockerRegi | registries                    |
|                       | stryConfig] |                               |
+-----------------------+-------------+-------------------------------+
| ``shellExecCommands`` | Map[String, | Map local shell exec commands |
|                       | ShellExec   | to equivalent docker          |
|                       | ItemConfig] | commands. When                |
|                       |             | ``shellExecLocally`` is       |
|                       |             | false, WASDI checks each      |
|                       |             | shell exec command against    |
|                       |             | this configuration. If a      |
|                       |             | command (e.g., “gdalinfo”,    |
|                       |             | “wasdi-launcher”) is found in |
|                       |             | this map, WASDI executes a    |
|                       |             | docker run command instead of |
|                       |             | the classic shell exec. This  |
|                       |             | enables fully dockerized      |
|                       |             | WASDI deployment where all    |
|                       |             | external tools run in         |
|                       |             | containers.                   |
+-----------------------+-------------+-------------------------------+
| ``processorTypes``    | Arra        | Configuration of processor    |
|                       | y[Processor | types                         |
|                       | TypeConfig] |                               |
+-----------------------+-------------+-------------------------------+
| ``eoepca``            | E           | Configuration of EoEpca       |
|                       | OEPCAConfig | related docker parameters     |
+-----------------------+-------------+-------------------------------+

ShellExecItemConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Each ``ShellExecItemConfig`` object in the ``shellExecCommands`` map
contains the following properties:

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``dockerImage``       | String      | Name of the docker image to   |
|                       |             | use instead of the command    |
+-----------------------+-------------+-------------------------------+
| ``containerVersion``  | String      | Version of the docker image   |
|                       |             | to use                        |
+-----------------------+-------------+-------------------------------+
| ``                    | Boolean     | If true (default), all        |
| includeFirstCommand`` |             | command line parts are passed |
|                       |             | as docker args. If false, the |
|                       |             | first element is not passed   |
|                       |             | to docker command line        |
+-----------------------+-------------+-------------------------------+
| ``forceLocal``        | Boolean     | If true, the command is       |
|                       |             | executed locally even if      |
|                       |             | WASDI is configured to be     |
|                       |             | dockerized (default: false)   |
+-----------------------+-------------+-------------------------------+
| ``rem                 | Boolean     | If true, WASDI removes the    |
| ovePathFromFirstArg`` |             | path from the first command   |
|                       |             | in the arg list               |
+-----------------------+-------------+-------------------------------+
| `                     | String      | Prefix that will be added as  |
| `addPrefixToCommand`` |             | arg[0] of the shell execute   |
+-----------------------+-------------+-------------------------------+
| ``ad                  | Ar          | List of additional mount      |
| ditionalMountPoints`` | ray[String] | points for this specific      |
|                       |             | docker                        |
+-----------------------+-------------+-------------------------------+
| ``o                   | Boolean     | Enable override of            |
| verrideDockerConfig`` |             | system/docker config (user,   |
|                       |             | group, network) with specific |
|                       |             | values for this docker        |
+-----------------------+-------------+-------------------------------+
| ``systemUserName``    | String      | System user name for this     |
|                       |             | docker (default: “appwasdi”)  |
+-----------------------+-------------+-------------------------------+
| ``systemUserId``      | Integer     | System user ID for this       |
|                       |             | docker (default: 2042)        |
+-----------------------+-------------+-------------------------------+
| ``systemGroupName``   | String      | System group name for this    |
|                       |             | docker (default: “appwasdi”)  |
+-----------------------+-------------+-------------------------------+
| ``systemGroupId``     | Integer     | System group ID for this      |
|                       |             | docker (default: 2042)        |
+-----------------------+-------------+-------------------------------+
| ``dockerNetworkMode`` | String      | Docker network mode for this  |
|                       |             | container (default:           |
|                       |             | “net-wasdi”)                  |
+-----------------------+-------------+-------------------------------+

ProcessorTypeConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Each ``ProcessorTypeConfig`` object in the ``processorTypes`` array
contains the following properties:

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``processorType``     | String      | Name of the processor type    |
+-----------------------+-------------+-------------------------------+
| ``ad                  | Ar          | List of additional mount      |
| ditionalMountPoints`` | ray[String] | points for this specific      |
|                       |             | docker                        |
+-----------------------+-------------+-------------------------------+
| ``commands``          | Ar          | List of additional commands   |
|                       | ray[String] | for this specific docker      |
+-----------------------+-------------+-------------------------------+
| ``e                   | Array[Envi  | List of environment variables |
| nvironmentVariables`` | ronmentVari | to pass when creating the     |
|                       | ableConfig] | container                     |
+-----------------------+-------------+-------------------------------+
| ``image``             | String      | Name of the base image to use |
|                       |             | (if needed)                   |
+-----------------------+-------------+-------------------------------+
| ``version``           | String      | Version of the base image to  |
|                       |             | use (if needed)               |
+-----------------------+-------------+-------------------------------+
| ``extraHosts``        | Ar          | Personalized extra hosts for  |
|                       | ray[String] | this processor type           |
+-----------------------+-------------+-------------------------------+
| ``mount               | Boolean     | If true, mount only           |
| OnlyWorkspaceFolder`` |             | ``/data/wasdi/[usr]/[wsid]/`` |
|                       |             | folder instead of entire      |
|                       |             | ``/data/wasdi`` folder        |
+-----------------------+-------------+-------------------------------+
| ``templateFilesTo     | Ar          | List of file names that must  |
| ExcludeFromDownload`` | ray[String] | not be downloaded when        |
|                       |             | zipping the processor         |
+-----------------------+-------------+-------------------------------+

Docker Registry Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

} }

::




   ### Docker Registry Configuration

   ```json
   {
     "registers": [
       {
         "address": "nexus.wasdi.net",
         "user": "username",
         "password": "password",
         "priority": 1,
         "isDefault": true
       }
     ]
   }

Docker Registry Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``id``                | String      | Unique identifier of the      |
|                       |             | registry                      |
+-----------------------+-------------+-------------------------------+
| ``user``              | String      | Username for registry         |
|                       |             | authentication                |
+-----------------------+-------------+-------------------------------+
| ``password``          | String      | Password for the above user   |
+-----------------------+-------------+-------------------------------+
| ``address``           | String      | HTTP address of the registry  |
+-----------------------+-------------+-------------------------------+
| ``priority``          | Integer     | Priority of the registry      |
|                       |             | (lower numbers = higher       |
|                       |             | priority)                     |
+-----------------------+-------------+-------------------------------+
| ``apiAddress``        | String      | HTTP address of the registry  |
|                       |             | API                           |
+-----------------------+-------------+-------------------------------+
| ``repositoryName``    | String      | Name of the repository inside |
|                       |             | the registry (default:        |
|                       |             | “wasdi-docker”)               |
+-----------------------+-------------+-------------------------------+

Messaging Configuration
-----------------------

RabbitMQ Configuration
~~~~~~~~~~~~~~~~~~~~~~

.. code:: json

   {
     "rabbit": {
       "host": "localhost",
       "user": "guest",
       "password": "guest",
       "port": 5672,
       "queueName": "wasdi"
     }
   }

RabbitMQ Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

============ ======= ==========================================
Property     Type    Description
============ ======= ==========================================
``user``     String  RabbitMQ username for authentication
``password`` String  RabbitMQ password for the above user
``host``     String  RabbitMQ server hostname or IP address
``port``     Integer RabbitMQ server port (default: 5672)
``exchange`` String  RabbitMQ exchange name for message routing
============ ======= ==========================================

Notifications Configuration
---------------------------

.. code:: json

   {
     "notifications": {
       "mercuriusAPIAddress": "https://mercurius.cimafoundation.org/",
       "pwRecoveryMailTitle": "WASDI Password Recovery",
       "pwRecoveryMailSender": "noreply@wasdi.net",
       "pwRecoveryMailText": "Password recovery email text",
       "sftpMailTitle": "WASDI SFTP Account",
       "sftpManagementMailSender": "noreply@wasdi.net", 
       "sftpMailText": "SFTP account notification text",
       "wasdiAdminMail": "admin@wasdi.net"
     }
   }

Notifications Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``                    | String      | Address of the Mercurius      |
| mercuriusAPIAddress`` |             | service (CIMA service API to  |
|                       |             | send e-mails)                 |
+-----------------------+-------------+-------------------------------+
| ``                    | String      | Title of the password         |
| pwRecoveryMailTitle`` |             | recovery email                |
+-----------------------+-------------+-------------------------------+
| ``p                   | String      | Sender address for password   |
| wRecoveryMailSender`` |             | recovery emails               |
+-----------------------+-------------+-------------------------------+
| `                     | String      | Text content of the password  |
| `pwRecoveryMailText`` |             | recovery email                |
+-----------------------+-------------+-------------------------------+
| ``sftpMailTitle``     | String      | Title of the SFTP account     |
|                       |             | notification email            |
+-----------------------+-------------+-------------------------------+
| ``sftpM               | String      | Sender address for SFTP       |
| anagementMailSender`` |             | account emails                |
+-----------------------+-------------+-------------------------------+
| ``sftpMailText``      | String      | Text content of the SFTP      |
|                       |             | account notification email    |
+-----------------------+-------------+-------------------------------+
| ``wasdiAdminMail``    | String      | Declared WASDI administrator  |
|                       |             | email address                 |
+-----------------------+-------------+-------------------------------+

Data Provider Configuration
---------------------------

Each Data Provider is a component able to query and fecth data from a
generic external service that can offer one or more data collection.

The main business entities involved in this operation are:

-  Platform: this is the type of data. Usually identified as a Satellite
   Mission. Platorms are for example Sentinel1, Sentinel2, ENVISat etc.
   Each Platform, in general, can be found in one or more data
   providers.
-  Query Executors / catalogue: the Query executor is the WASDI
   hierarchy used to query the Data Provider Catalogue
-  Provider Adapters: Objects used by the launcher to download/import
   files from an external service.

Each Data Provider can support one or more platforms (and each platform
can be supported by one or more Data Providers.)

All Data Providers have: .name .description .link .user .password

Different data providers uses also different parameters. This config has
been re-written after that many data providers were already avaiable and
is done to support also legacy objects.

Is due to control for Each Data Provider witch params are really needed.

At the moment of fetching some data, WASDI will try to use the “best”
Data Provider: this is an algorithm based on the availability of the
data providers and on the Cloud Environment declared by the Node and the
data provider itself.

For example, a node in Creodias, will try to select the Creodias Data
Provider since it assumes it will be faster beeing in the same cloud.

A node in LSA Data Center, will probably choose LSA Data Center.

In any case, in case of problems, WASDI will try to back-up using all
the Data Providers available for that specific Platform.

Each Data Provider can also have two additional config files: these are
not mandatory. Are meant to be generic JSON files, that may be needed by
any specific Data Provider for its own purpose.

The two files are: - parserConfig: config file dedicated to the Query
Executor - adapterConfig: config file dedicated to the Data Adapter

.. code:: json

   {
     "dataProviders": [
       {
         "name": "COPERNICUS",
         "queryExecutorClasspath": "wasdi.dataproviders.CopernicusQueryExecutor",
         "providerAdapterClasspath": "wasdi.dataproviders.CopernicusProviderAdapter",
         "pythonScript": "",
         "description": "Copernicus Data Provider",
         "link": "https://scihub.copernicus.eu/",
         "searchListPageSize": "25",
         "defaultProtocol": "https://",
         "user": "username",
         "password": "password",
         "apiKey": "",
         "localFilesBasePath": "",
         "urlDomain": "https://scihub.copernicus.eu/",
         "connectionTimeout": "10000",
         "readTimeout": "30000",
         "adapterConfig": "",
         "parserConfig": "",
         "cloudProvider": "COPERNICUS",
         "supportedPlatforms": ["S1", "S2", "S3"]
       }
     ]
   }

Data Provider Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``name``              | String      | Name/Code of the data         |
|                       |             | provider (required to get     |
|                       |             | QueryExecutors and            |
|                       |             | ProviderAdapters)             |
+-----------------------+-------------+-------------------------------+
| ``que                 | String      | Full class name and path of   |
| ryExecutorClasspath`` |             | the QueryExecutor             |
|                       |             | implementation (used to       |
|                       |             | create the Java class with    |
|                       |             | reflection)                   |
+-----------------------+-------------+-------------------------------+
| ``provi               | String      | Full class name and path of   |
| derAdapterClasspath`` |             | the ProviderAdapter           |
|                       |             | implementation (used to       |
|                       |             | create the Java class with    |
|                       |             | reflection)                   |
+-----------------------+-------------+-------------------------------+
| ``pythonScript``      | String      | Python script path for        |
|                       |             | External Python Provider      |
+-----------------------+-------------+-------------------------------+
| ``description``       | String      | Description of the data       |
|                       |             | provider                      |
+-----------------------+-------------+-------------------------------+
| ``link``              | String      | Link to the data provider’s   |
|                       |             | website                       |
+-----------------------+-------------+-------------------------------+
| `                     | String      | Size for paginated requests   |
| `searchListPageSize`` |             | when making WASDI searchList  |
|                       |             | operations                    |
+-----------------------+-------------+-------------------------------+
| ``defaultProtocol``   | String      | Default protocol for data     |
|                       |             | fetch (“https://” or          |
|                       |             | “file://”)                    |
+-----------------------+-------------+-------------------------------+
| ``parserConfig``      | String      | Path to parser config JSON    |
|                       |             | file for query conversion     |
+-----------------------+-------------+-------------------------------+
| ``user``              | String      | Username for the data         |
|                       |             | provider                      |
+-----------------------+-------------+-------------------------------+
| ``password``          | String      | Password for the data         |
|                       |             | provider                      |
+-----------------------+-------------+-------------------------------+
| ``apiKey``            | String      | API key for the data provider |
+-----------------------+-------------+-------------------------------+
| `                     | String      | Local base folder for direct  |
| `localFilesBasePath`` |             | file access (when             |
|                       |             | defaultProtocol is “file://”) |
+-----------------------+-------------+-------------------------------+
| ``urlDomain``         | String      | API address of the data       |
|                       |             | provider                      |
+-----------------------+-------------+-------------------------------+
| ``connectionTimeout`` | String      | Specific connection timeout   |
|                       |             | for this data provider        |
+-----------------------+-------------+-------------------------------+
| ``readTimeout``       | String      | Specific read timeout for     |
|                       |             | this data provider            |
+-----------------------+-------------+-------------------------------+
| ``adapterConfig``     | String      | Path to file with specific    |
|                       |             | ProviderAdapter               |
|                       |             | configurations                |
+-----------------------+-------------+-------------------------------+
| ``cloudProvider``     | String      | Code of the cloud provider    |
|                       |             | where this data provider is   |
|                       |             | hosted                        |
+-----------------------+-------------+-------------------------------+
| `                     | Ar          | List of platforms supported   |
| `supportedPlatforms`` | ray[String] | by this data provider         |
+-----------------------+-------------+-------------------------------+

Catalogue Configuration
-----------------------

Catalogues are strongly connected with Data Providers. A Catalogue is a
service we can use to query a data collection. As each data provider can
support more platforms (data collections), each platform can in general
be queried in different catalogues.

In this catalogues section, we are intructing WASDI, for each platform,
what are the avaiable catalogues. WASDI will try to query the first one
first: if there is any problem, WASDI can switch on the other ones in
the list.

Each name of a catalogue, must match a Data Provider.

There is a kind of double link: in the Data Provider, we list all the
platforms supported. In this catalogues section, we list for each
platform the list of Catalogues (Data Providers) that we can query.

The two list in general should match, but is possible for the
administrator, for example, to drop from the catalogues list a Data
Provider that we do not want to query but, eventually, only to fecth
data.

.. code:: json

   {
     "catalogues": [
       {
         "platform": "S1",
         "catalogues": ["COPERNICUS", "ONDA", "CREODIAS"]
       },
       {
         "platform": "S2", 
         "catalogues": ["COPERNICUS", "ONDA", "CREODIAS"]
       }
     ]
   }

Catalogue Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``platform``          | String      | Code of the Platform Type as  |
|                       |             | defined by the Platform class |
|                       |             | (e.g., “S1”, “S2”, “S3”)      |
+-----------------------+-------------+-------------------------------+
| ``catalogues``        | Ar          | List of catalogues supporting |
|                       | ray[String] | the Platform Type. Each       |
|                       |             | string is the code of a       |
|                       |             | Q                             |
|                       |             | ueryExecutor/ProviderAdapter. |
|                       |             | The first element has the     |
|                       |             | highest priority.             |
+-----------------------+-------------+-------------------------------+

Additional Service Configurations
---------------------------------

GeoServer Configuration
~~~~~~~~~~~~~~~~~~~~~~~

.. code:: json

   {
     "geoserver": {
       "address": "http://localhost:8080/geoserver/",
       "user": "admin",
       "password": "geoserver",
       "workspace": "wasdi"
     }
   }

GeoServer Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``address``           | String      | GeoServer server address      |
|                       |             | (e.g.,                        |
|                       |             | “http:                        |
|                       |             | //localhost:8080/geoserver/”) |
+-----------------------+-------------+-------------------------------+
| ``user``              | String      | GeoServer username for        |
|                       |             | authentication                |
+-----------------------+-------------+-------------------------------+
| ``password``          | String      | GeoServer password for the    |
|                       |             | above user                    |
+-----------------------+-------------+-------------------------------+
| ``maxGeot             | String      | Maximum dimension in MB to    |
| iffDimensionPyramid`` |             | publish single images. Over   |
|                       |             | this limit, WASDI will create |
|                       |             | a pyramid of the image        |
+-----------------------+-------------+-------------------------------+
| ``gdalRetileCommand`` | String      | GDAL retile command for       |
|                       |             | pyramid creation (default:    |
|                       |             | “gdal_retile.py -r bilinear   |
|                       |             | -levels 4 -ps 2048 2048 -co   |
|                       |             | TILED=YES”)                   |
+-----------------------+-------------+-------------------------------+
| ``l                   | Boolean     | Special debug mode for        |
| ocalDebugPublisBand`` |             | PublishBand operation. Forces |
|                       |             | input DownloadedFile to be    |
|                       |             | gathered from database using  |
|                       |             | /data/wasdi/ instead of real  |
|                       |             | local folder (default: false) |
+-----------------------+-------------+-------------------------------+
| ``defaultLa           | String      | Default layer used to get     |
| yerToGetStyleImages`` |             | style images (default:        |
|                       |             | “wa                           |
|                       |             | sdi:ESA_CCI_LAND_COVER_2015”) |
+-----------------------+-------------+-------------------------------+

SFTP Configurationiguration Structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

SFTP Configuration
~~~~~~~~~~~~~~~~~~

.. code:: json

   {
     "sftp": {
       "address": "sftp.wasdi.net",
       "user": "sftpuser",
       "password": "sftppassword",
       "port": 22,
       "uploadRootPath": "/upload/"
     }
   }

SFTP Configuration Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``sftpManagem         | String      | Address of the local Web      |
| entWSServiceAddress`` |             | Socket server to create and   |
|                       |             | delete SFTP users             |
+-----------------------+-------------+-------------------------------+

*Note: The JSON example above shows additional properties (``address``,
``user``, ``password``, ``port``, ``uploadRootPath``) that may be used
in specific configurations but are not currently defined in the
SftpConfig.java class.*

S3 Bucket Configuration
~~~~~~~~~~~~~~~~~~~~~~~

.. code:: json

   {
     "s3Bucket": {
       "accessKey": "access-key",
       "secretAccessKey": "secret-key",
       "region": "eu-west-1",
       "bucketName": "wasdi-bucket"
     }
   }

S3BucketConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^

This configuration is dedicated to the specific access to Ecostress
Data, may be not of interest in a personal installation.

============== ====== =============================================
Property       Type   Description
============== ====== =============================================
``accessKey``  String Access Key for S3 bucket authentication
``secretKey``  String Secret Key for S3 bucket authentication
``endpoint``   String S3 service endpoint URL
``bucketName`` String Name of the S3 bucket
``folders``    String Folder structure or path within the S3 bucket
============== ====== =============================================

Stripe Configuration
~~~~~~~~~~~~~~~~~~~~

.. code:: json

   {
     "stripe": {
       "secretKey": "sk_test_...",
       "publicKey": "pk_test_...",
       "webhookSecret": "whsec_..."
     }
   }

StripeConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``apiKey``            | String      | Stripe API key for            |
|                       |             | authentication and payment    |
|                       |             | processing                    |
+-----------------------+-------------+-------------------------------+
| ``products``          | Lis         | List of product-related       |
|                       | t<StripePro | configuration entries         |
|                       | ductConfig> |                               |
+-----------------------+-------------+-------------------------------+

StripeProductConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

======== ====== =========================================
Property Type   Description
======== ====== =========================================
``id``   String Product ID in Stripe
``url``  String Product URL for accessing product details
======== ====== =========================================

Storage Usage Control
---------------------

.. code:: json

   {
     "storageUsageControl": {
       "enabled": true,
       "maxDaysBeforeWarning": 7,
       "maxDaysBeforeDeletion": 30,
       "adminEmail": "admin@wasdi.net"
     }
   }

StorageUsageControl Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``storageS            | Long        | Maximum storage space (in     |
| izeFreeSubscription`` |             | bytes) for FREE subscription  |
|                       |             | plans (default: 20GB)         |
+-----------------------+-------------+-------------------------------+
| ``storageSizeS        | Long        | Maximum storage space (in     |
| tandardSubscription`` |             | bytes) for STANDARD           |
|                       |             | subscription plans (default:  |
|                       |             | 100GB)                        |
+-----------------------+-------------+-------------------------------+
| ``delet               | int         | Number of days to wait before |
| ionDelayFromWarning`` |             | proceeding to workspace       |
|                       |             | deletion after warning email  |
|                       |             | is sent (default: 10)         |
+-----------------------+-------------+-------------------------------+
| ``i                   | boolean     | If true, workspaces with      |
| sDeletionInTestMode`` |             | invalid subscriptions will    |
|                       |             | not be actually deleted, but  |
|                       |             | admin notification emails     |
|                       |             | will be sent instead          |
+-----------------------+-------------+-------------------------------+
| `                     | Warning     | Configuration for warning     |
| `warningEmailConfig`` | EmailConfig | emails sent to users before   |
|                       |             | workspace deletion            |
+-----------------------+-------------+-------------------------------+

WarningEmailConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``title``             | String      | Subject line of the warning   |
|                       |             | email (default: “Storage      |
|                       |             | exceeded in WASDI”)           |
+-----------------------+-------------+-------------------------------+
| ``message``           | String      | Body content of the warning   |
|                       |             | email with placeholders for   |
|                       |             | user info, storage size,      |
|                       |             | limit, and warning delay      |
+-----------------------+-------------+-------------------------------+

Skin Configuration
------------------

.. code:: json

   {
     "skins": [
       {
         "name": "wasdi",
         "description": "Default WASDI Skin",
         "logoPath": "/assets/logos/wasdi.png",
         "primaryColor": "#1976d2"
       }
     ]
   }

SkinConfig Properties
^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``name``              | String      | Name identifier for the skin  |
|                       |             | (default: “wasdi”)            |
+-----------------------+-------------+-------------------------------+
| ``logoImage``         | String      | Path to the logo image file   |
|                       |             | (default:                     |
|                       |             | “                             |
|                       |             | /assets/icons/logo-only.svg”) |
+-----------------------+-------------+-------------------------------+
| ``logoText``          | String      | Path to the logo text/name    |
|                       |             | image file (default:          |
|                       |             | “                             |
|                       |             | /assets/icons/logo-name.svg”) |
+-----------------------+-------------+-------------------------------+
| ``helpLink``          | String      | URL link to help              |
|                       |             | documentation (default:       |
|                       |             | “https://wasd                 |
|                       |             | i.readthedocs.io/en/latest/”) |
+-----------------------+-------------+-------------------------------+
| ``supportLink``       | String      | URL link to support channels  |
|                       |             | (default:                     |
|                       |             | “ht                           |
|                       |             | tps://discord.gg/FkRu2GypSg”) |
+-----------------------+-------------+-------------------------------+
| ``brandMainColor``    | String      | Primary brand color in hex    |
|                       |             | format (default: “#43526B”)   |
+-----------------------+-------------+-------------------------------+
| ``                    | String      | Secondary brand color in hex  |
| brandSecondaryColor`` |             | format (default: “#009036”)   |
+-----------------------+-------------+-------------------------------+
| ``defaultCategories`` | ArrayL      | List of default categories    |
|                       | ist<String> | for the skin                  |
+-----------------------+-------------+-------------------------------+

Node Score Configuration
------------------------

.. code:: json

   {
     "nodeScore": {
       "minTotalMemoryGBytes": 30
     }
   }

NodeScoreConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``m                   | int         | Minimum total memory in GB    |
| inTotalMemoryGBytes`` |             | required for node scoring     |
|                       |             | (default: 30)                 |
+-----------------------+-------------+-------------------------------+

SNAP Configuration
------------------

.. code:: json

   {
     "snap": {
       "auxPropertiesFile": "/usr/lib/wasdi/launcher/snap.auxdata.properties",
       "launcherLogActive": true,
       "webLogActive": true,
       "launcherLogFile": "/var/log/wasdi/snap-launcher.log",
       "webLogFile": "/var/log/wasdi/snap-web.log",
       "launcherLogLevel": "SEVERE",
       "webLogLevel": "SEVERE"
     }
   }

SnapConfig Properties
^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``auxPropertiesFile`` | String      | Full path to SNAP auxiliary   |
|                       |             | properties file (default:     |
|                       |             | “/usr/lib/wasdi/launc         |
|                       |             | her/snap.auxdata.properties”) |
+-----------------------+-------------+-------------------------------+
| ``launcherLogActive`` | boolean     | Flag to activate SNAP logs in |
|                       |             | the launcher (default: true)  |
+-----------------------+-------------+-------------------------------+
| ``webLogActive``      | boolean     | Flag to activate SNAP logs in |
|                       |             | the web server (default:      |
|                       |             | true)                         |
+-----------------------+-------------+-------------------------------+
| ``launcherLogFile``   | String      | Full path to SNAP launcher    |
|                       |             | log file                      |
+-----------------------+-------------+-------------------------------+
| ``webLogFile``        | String      | Full path to SNAP web server  |
|                       |             | log file                      |
+-----------------------+-------------+-------------------------------+
| ``launcherLogLevel``  | String      | SNAP launcher log level       |
|                       |             | (default: “SEVERE”)           |
+-----------------------+-------------+-------------------------------+
| ``webLogLevel``       | String      | SNAP web server log level     |
|                       |             | (default: “SEVERE”)           |
+-----------------------+-------------+-------------------------------+

Scheduler Configuration
-----------------------

This section configures the WASDI Scheduler. For each operation type, it
can add a queue. Is up to the admin to refine the configuration given
the capabilities of the node where it is installed.

All the operations that are not explicitly assigned, will go in the
default queue.

The admin can then add all the needed queues.

.. code:: json

   {
     "scheduler": {
       "processingThreadWaitStartMS": "2000",
       "processingThreadSleepingTimeMS": "2000",
       "launcherPath": "/opt/wasdi/launchers/launcher.jar",
       "javaExe": "java",
       "killCommand": "kill -9",
       "maxQueue": "50",
       "timeoutMs": "300000",
       "lastStateChangeDateOrderBy": -1,
       "sometimesCheckCounter": 30,
       "watchDogCounter": 30,
       "activateWatchDog": true,
       "schedulers": [
         {
           "name": "default",
           "maxQueue": "50",
           "timeoutMs": "300000",
           "opTypes": "RUNPROCESSOR,RUNIDL,RUNMATLAB",
           "opSubType": "",
           "enabled": "true",
           "specialWaitCondition": false,
           "maxWaitingQueue": 100
         },
         {
           "name": "download",
           "maxQueue": "10",
           "timeoutMs": "600000",
           "opTypes": "DOWNLOAD",
           "opSubType": "",
           "enabled": "true",
           "specialWaitCondition": true,
           "maxWaitingQueue": 50
         }
       ]
     }
   }

SchedulerConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^

These are the configurations of the default scheduler

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``processi            | String      | Number of milliseconds to     |
| ngThreadWaitStartMS`` |             | wait after a process is       |
|                       |             | started                       |
+-----------------------+-------------+-------------------------------+
| ``processingT         | String      | Number of milliseconds to     |
| hreadSleepingTimeMS`` |             | sleep between scheduler       |
|                       |             | cycles                        |
+-----------------------+-------------+-------------------------------+
| ``launcherPath``      | String      | Full path of Launcher jar     |
|                       |             | file                          |
+-----------------------+-------------+-------------------------------+
| ``javaExe``           | String      | Local Java command line       |
|                       |             | executable                    |
+-----------------------+-------------+-------------------------------+
| ``killCommand``       | String      | OS kill command for           |
|                       |             | terminating processes         |
+-----------------------+-------------+-------------------------------+
| ``maxQueue``          | String      | Default maximum queue size    |
+-----------------------+-------------+-------------------------------+
| ``timeoutMs``         | String      | Default queue timeout in      |
|                       |             | milliseconds                  |
+-----------------------+-------------+-------------------------------+
| ``lastSta             | int         | Direction to order process    |
| teChangeDateOrderBy`` |             | workspaces in scheduler queue |
|                       |             | (default: -1)                 |
+-----------------------+-------------+-------------------------------+
| ``so                  | int         | Number of cycles before       |
| metimesCheckCounter`` |             | starting periodic checks      |
|                       |             | (default: 30)                 |
+-----------------------+-------------+-------------------------------+
| ``watchDogCounter``   | int         | Counter for deadlock          |
|                       |             | detection when only waiting   |
|                       |             | processes exist (default: 30) |
+-----------------------+-------------+-------------------------------+
| ``activateWatchDog``  | boolean     | Flag to activate or           |
|                       |             | deactivate the watch dog      |
|                       |             | functionality (default: true) |
+-----------------------+-------------+-------------------------------+
| ``schedulers``        | ArrayList   | List of configured scheduler  |
|                       | <SchedulerQ | queues                        |
|                       | ueueConfig> |                               |
+-----------------------+-------------+-------------------------------+

SchedulerQueueConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Each of these, is a specific queue.

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``name``              | String      | Name identifier of the queue  |
+-----------------------+-------------+-------------------------------+
| ``maxQueue``          | String      | Maximum number of elements    |
|                       |             | allowed in this queue         |
+-----------------------+-------------+-------------------------------+
| ``timeoutMs``         | String      | Queue timeout in milliseconds |
|                       |             | for this specific queue       |
+-----------------------+-------------+-------------------------------+
| ``opTypes``           | String      | Comma-separated operation     |
|                       |             | types supported by this queue |
+-----------------------+-------------+-------------------------------+
| ``opSubType``         | String      | Operation subtype (requires   |
|                       |             | opTypes to contain only one   |
|                       |             | operation)                    |
+-----------------------+-------------+-------------------------------+
| ``enabled``           | String      | Flag to enable or disable     |
|                       |             | this queue                    |
+-----------------------+-------------+-------------------------------+
| ``s                   | boolean     | Flag to apply special wait    |
| pecialWaitCondition`` |             | condition considering waiting |
|                       |             | queue (default: false)        |
+-----------------------+-------------+-------------------------------+
| ``maxWaitingQueue``   | int         | Maximum waiting processes     |
|                       |             | before breaking FIFO rules    |
|                       |             | (default: 100)                |
+-----------------------+-------------+-------------------------------+

Load Balancer Configuration
---------------------------

.. code:: json

   {
     "loadBalancer": {
       "includeMainClusterAsNode": false,
       "diskOccupiedSpaceMaxPercentage": 90,
       "metricsMaxAgeSeconds": 600,
       "minTotalMemoryGBytes": 30,
       "activateMetrics": true
     }
   }

LoadBalancerConfig Properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

+-----------------------+-------------+-------------------------------+
| Property              | Type        | Description                   |
+=======================+=============+===============================+
| ``inclu               | boolean     | Flag to include main node in  |
| deMainClusterAsNode`` |             | computational node evaluation |
|                       |             | (default: false)              |
+-----------------------+-------------+-------------------------------+
| ``diskOccupie         | int         | Maximum percentage of disk    |
| dSpaceMaxPercentage`` |             | space occupied to consider a  |
|                       |             | node available (default: 90)  |
+-----------------------+-------------+-------------------------------+
| ``m                   | int         | Maximum age in seconds for    |
| etricsMaxAgeSeconds`` |             | node metrics before           |
|                       |             | considering node down         |
|                       |             | (default: 600)                |
+-----------------------+-------------+-------------------------------+
| ``m                   | int         | Minimum RAM in GB required    |
| inTotalMemoryGBytes`` |             | for a node to avoid low       |
|                       |             | performance penalty (default: |
|                       |             | 30)                           |
+-----------------------+-------------+-------------------------------+
| ``activateMetrics``   | boolean     | Flag to enable/disable        |
|                       |             | metrics-based node selection  |
|                       |             | (default: true)               |
+-----------------------+-------------+-------------------------------+

Configuration File Location
---------------------------

The configuration file is typically located at: -
``/opt/wasdi/config/wasdiConfig.json`` (production) -
``/data/wasdi/config/wasdiConfig.json`` (development)

Loading the Configuration
-------------------------

The configuration is loaded automatically when WASDI components start up
using the ``WasdiConfig.readConfig(String configFilePath)`` method. The
loaded configuration is available statically through
``WasdiConfig.Current``.

Environment-Specific Configurations
-----------------------------------

Different environments (development, staging, production) should have
separate configuration files with appropriate values for:

-  Database connections
-  External service URLs
-  Authentication settings
-  File system paths
-  Docker registry settings
-  API keys and secrets

Security Considerations
-----------------------

-  Store sensitive information (passwords, API keys) securely
-  Use environment variables or secure vaults for production secrets
-  Regularly rotate authentication credentials
-  Restrict file system permissions on the configuration file
-  Use HTTPS for all external service communications

Validation
----------

The configuration is validated when loaded. Missing required properties
or invalid values will cause WASDI components to fail during startup
with appropriate error messages.
