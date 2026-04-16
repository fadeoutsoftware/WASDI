# WASDI

**Web Advanced Space Developer Interface**

[![Documentation Status](https://readthedocs.org/projects/wasdi/badge/?version=latest)](https://wasdi.readthedocs.io/en/latest/?badge=latest)

> Full documentation: https://wasdi.readthedocs.io/en/latest/

---

## What is WASDI?

WASDI is an **Earth Observation (EO) Platform** designed to let scientists concentrate on science — not on IT infrastructure. It provides a cloud environment where EO processing algorithms can be developed locally and then deployed as scalable cloud services with minimal effort.

- Getting started tutorial: [WASDI Tutorial](https://wasdi.readthedocs.io/en/latest/GettingStarted/WasdiTutorial.html)
- User manual: [Signing Up and Signing In](https://wasdi.readthedocs.io/en/latest/UserManual/SigningUpAndSigningIn.html)

---

## WASDI Libraries

WASDI provides client libraries for the most common scientific and development languages:

| Language | Install | Documentation | Tutorial |
|----------|---------|---------------|----------|
| Python | `pip install wasdi` | [waspy](https://wasdi.readthedocs.io/en/latest/Libraries/python/waspy.html) | [Python Tutorial](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html) |
| JavaScript | `npm install wasdi` | [wasdi-js](https://wasdi.readthedocs.io/en/latest/Libraries/typescript/wasdi.html) | [JavaScript Tutorial](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/JavascriptTutorialHtml.html) |
| TypeScript | `npm i wasdi` | [wasdi-ts](https://wasdi.readthedocs.io/en/latest/Libraries/typescript/wasdi.html) | [TypeScript Tutorial](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/JavascriptTutorial.html) |
| Java | [Download ZIP](https://www.wasdi.net/javawasdilib.zip) | [WasdiLib](https://wasdi.readthedocs.io/en/latest/Libraries/java/WasdiLib.html) | [Getting Started](https://wasdi.readthedocs.io/en/latest/GettingStarted/LibsConcepts.html) |
| C# | `Install-Package WasdiLib -Version 0.0.3.5` | [C# Docs](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/C%23Tutorial.html) | [C# Tutorial](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/C%23Tutorial.html) |
| Octave | [Download ZIP](https://github.com/fadeoutsoftware/WASDI/blob/master/libraries/matlabwasdilib/matlabwasdilib.zip) | [Octave Docs](https://wasdi.readthedocs.io/en/latest/Libraries/octave/octave.html) | [Octave Tutorial](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/OctaveTutorial.html) |

### The key concept: `getPath()`

The only adaptation needed to make existing code run on WASDI is to **never use hard-coded file paths**. Instead, always resolve paths through the library:

```python
# Python example
import wasdi
file_path = wasdi.getPath("myfile.tif")
```

This single change is all that is required. The code will continue to work on your local machine exactly as before — and once dragged and dropped into WASDI, it automatically becomes a **scalable cloud service**.

### Advanced library features

Beyond path resolution, the WASDI libraries offer a rich set of features:

- **Search EO and non-EO images** — query satellite data catalogues
- **Ingest images into the workspace** — download and stage data for processing
- **Run another application** — chain WASDI apps together via `wasdi.executeProcessor()`
- **Run a SNAP workflow** — execute ESA SNAP graph processing tool chains
- **Mosaic / subset images** — spatial operations on raster data

---

## WASDI Applications

Every WASDI application, regardless of the language it is written in:

- **Takes a JSON file as input** — parameters are passed as a structured JSON object
- **Produces output files** in the workspace — typically geospatial files such as GeoTIFF, GeoPackage, or Shapefile
- **Can optionally store a JSON payload** in the database — a structured result object accessible after execution

All apps, in all supported languages, can interoperate with each other using:

```python
wasdi.executeProcessor("another-app-id", oParameters)
```

This makes it straightforward to build complex multi-step pipelines by composing independent applications.

---

## Software Architecture

### Database

WASDI uses a configurable persistence layer. The default in production is **MongoDB**, but the system can also be configured to run with **H2** (embedded, suited for development/testing) or **SQLite**.

The main entities stored in the database are:

| Entity | Description |
|--------|-------------|
| **Users** | Registered WASDI accounts |
| **Workspaces** | Isolated environments where users store data and run processing |
| **Processors** | WASDI applications (user-provided code packaged as a service) |
| **Workflows** | ESA SNAP graph processing workflows; the XML files exported by SNAP can be uploaded directly into WASDI |
| **Styles** | SLD files uploaded by users to control the visual rendering of images on the map |
| **ProductWorkspace** | The list of products (files) present in each workspace |
| **ProcessWorkspace** | A task — any operation executed in a workspace (app execution, workflow run, download, mosaic, etc.) |

#### ProcessWorkspace — the execution model

`ProcessWorkspace` is the central concept of the WASDI execution model. When a user triggers any operation, the **Web Server** creates a `ProcessWorkspace` record in the database with state `CREATED`. The **Scheduler** picks it up as soon as capacity is available and drives it through its lifecycle.

Possible states:

| State | Meaning |
|-------|---------|
| `CREATED` | The task has been registered and is waiting to be scheduled |
| `WAITING` | The task is queued, waiting for a dependency or a free slot |
| `READY` | The task has been assigned to a node and is about to start |
| `RUNNING` | The task is actively executing |
| `DONE` | The task completed successfully |
| `ERROR` | The task terminated with an error |
| `STOPPED` | The task was explicitly stopped by the user |

> See the [Synchronous and Asynchronous operations](https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/SynchAsynch.html) documentation for details on how to work with process states from your application code.

---

### Main Components

```
┌─────────────┐        ┌───────────┐        ┌────────────┐
│   Client    │──────▶│  Server   │──────▶ │ Database   │
│  (Angular)  │        │  (Java)   │        │ (MongoDB)  │
└─────────────┘        └───────────┘        └────────────┘
                                                   ▲
                              ┌────────────────────┘
                              │
                        ┌─────────────┐
                        │  Scheduler  │
                        │   (Java)    │
                        └──────┬──────┘
                               │ spawns
                        ┌──────▼──────┐
                        │  Launcher   │
                        │   (Java)    │
                        └──────┬──────┘
                               │ wraps
                  ┌────────────┴────────────┐
                  │                         │
          Built-in operations        User application
          (download, mosaic,         (Python / Java /
           run workflow…)             C# / JS / TS…)

                        ┌─────────────┐
                        │   Trigger   │
                        │   (Java)    │
                        └─────────────┘
                     (scheduled app execution)
```

| Component | Tech | Role |
|-----------|------|------|
| **WASDI Shared** | Java | Shared library included by all other Java components. Declares all entities, the data layer, and the view models. Also provides cross-cutting utility functions (logging, HTTP helpers, Docker utilities, etc.) |
| **Server** | Java | REST API — the main entry point for the web UI and the WASDI libraries. Handles authentication, workspace management, and registers operations as `ProcessWorkspace` records |
| **Scheduler** | Java | Continuously monitors the database and orchestrates operations across nodes. Picks up `CREATED` tasks, assigns them to an available node, and updates process states |
| **Launcher** | Java | Started by the Scheduler for each operation. Executes built-in operations directly (download, mosaic, subset, SNAP workflow execution). For user applications it acts as a wrapper: it sets up the runtime environment and then runs the user-provided code as a separate process |
| **Trigger** | Java | Lightweight component responsible for starting WASDI applications on a schedule (cron-style) |
| **Client** | Angular | Browser-based web UI — lets users manage workspaces, browse products, launch processors, visualise results on a map, and monitor process states |

---

### Optional Components

| Component | Tech | Role |
|-----------|------|------|
| **dbUtils** | Java | Command-line maintenance tool packaged as a JAR (`dbUtils.jar`). Built as a secondary Maven artifact associated with the Launcher. Used to perform administrative and maintenance tasks on the database directly from the command line |
| **ogcprocesses** | Java | Additional server that exposes all WASDI applications through the [OGC API — Processes 2.0](https://ogcapi.ogc.org/processes/) standard, enabling interoperability with any OGC-compliant client |
| **openeo-java-server** | Java | Additional server that implements the [openEO](https://openeo.org/) API, allowing openEO workflows and applications to be executed inside WASDI |

---

### Third-Party Components

| Component | Version (from codebase) | Role | Links |
|-----------|------------------------|------|-------|
| **MongoDB** | driver `5.6.1` | Default production database. Stores all WASDI entities (users, workspaces, processors, process tasks, etc.) | [mongodb.com](https://www.mongodb.com/) |
| **Nitrite DB (no2)** | `4.3.2` | Embedded NoSQL alternative to MongoDB, useful for lightweight or standalone deployments | [nitrite-java](https://github.com/nitrite/nitrite-java) |
| **SQLite** | JDBC driver `3.47.1.0` | Embedded SQL alternative for development or single-node deployments | [sqlite.org](https://www.sqlite.org/) |
| **Keycloak** | `21.0.2` | Optional identity and access management server. Can be used to store users and passwords and to handle authentication and single sign-on | [keycloak.org](https://www.keycloak.org/) |
| **RabbitMQ** | AMQP client `5.29.0` | Message broker used for asynchronous communication between the Server, the Launcher, and the Client | [rabbitmq.com](https://www.rabbitmq.com/) |
| **GeoServer** | manager lib `1.7.0` | Used to publish geospatial images and expose them via OGC web services (WMS, WFS, WCS) | [geoserver.org](https://geoserver.org/) |
| **Nexus Repository** | — | Artifact repository used to store and distribute WASDI application packages | [sonatype.com/nexus](https://www.sonatype.com/products/sonatype-nexus-repository) |
| **Docker** | java client `3.4.0` | Container runtime used to isolate and execute WASDI applications. Different execution modes are available (covered in a dedicated section) | [docker.com](https://www.docker.com/) |

---

## Processor Engines

The execution of third-party applications is the real core of WASDI. The basic idea is that a user uploads a ZIP file containing their code, and WASDI automatically turns it into a scalable cloud service — typically by building a Docker container from the uploaded code.

The abstraction that models this is the `WasdiProcessorEngine` interface, defined in the Launcher.

### Interface operations

| Method | Description |
|--------|-------------|
| `deploy` | Install a new processor for the first time (build image, register, etc.) |
| `run` | Execute the processor for a given set of parameters |
| `redeploy` | Update an existing processor (rebuild/re-register) |
| `delete` | Remove the processor and all associated resources |
| `libraryUpdate` | Update only the WASDI client library inside the processor environment |
| `stopApplication` | Stop a currently running processor instance |
| `environmentUpdate` | *(Optional)* Trigger a full environment update (OS packages, runtime, etc.) |
| `refreshPackagesInfo` | *(Optional)* Refresh the `packagesInfo.json` file that describes installed packages — relevant for engines that expose a remote package manager |

### Execution targets

WASDI processors are not tied to a single runtime. The architecture supports multiple execution targets:

- **Docker** — the standard production target; each app runs in an isolated container on the WASDI node
- **Local** — runs directly on the host (or MiniWasdi container) without building a Docker image
- **Kubernetes (k8s)** — supported in other branches; follows the same `WasdiProcessorEngine` interface
- **EOEPCA** — delegates execution to an external [EOEPCA](https://eoepca.org/) platform instance

### Class hierarchy

```
WasdiProcessorEngine  (abstract — launcher)
│
├── LocalProcessorEngine
│     Runs Python apps directly on the host using a per-processor
│     virtual environment managed by uv. No Docker image required.
│     Type: local_python312
│
└── DockerProcessorEngine  (abstract)
      Base for all Docker-based engines. Handles container lifecycle
      (start, stop, inspect) via the Docker Java client.
      │
      ├── JupyterNotebookProcessorEngine
      │     Runs Jupyter notebooks inside a Docker container.
      │     Type: jupyter-notebook
      │
      ├── EoepcaProcessorEngine
      │     Delegates processor execution to an external EOEPCA node.
      │     Type: eoepca
      │
      └── PipProcessorEngine
            Docker engine with pip package manager support. Builds a
            container at deploy time and manages pip packages remotely.
            │
            ├── UbuntuPython37ProcessorEngine  (legacy)
            │     Ubuntu + Python 3.7 + ESA SNAP. The original WASDI
            │     processor type, still supported.
            │     Type: ubuntu_python37_snap
            │
            └── DockerBuildOnceEngine
                  Variant that builds the Docker image once and stores
                  it in the Nexus registry. Nodes pull the pre-built
                  image instead of building it locally.
                  │
                  ├── PythonPipProcessorEngine2
                  │     Updated pip-based Python engine (v2).
                  │     Type: python_pip_2
                  │
                  ├── PipOneShotProcessorEngine
                  │     Pip-based engine where the container is started
                  │     fresh for each execution (one-shot model).
                  │     Type: pip_oneshot
                  │     │
                  │     └── Python312Ubuntu24ProcessorEngine
                  │           Python 3.12 on Ubuntu 24.
                  │           Type: python312
                  │
                  ├── Ubuntu20Pip2ProcessorEngine
                  │     Pip v2 engine on Ubuntu 20 (Focal).
                  │     Type: python_pip_2_ubuntu_20
                  │
                  ├── Java17Ubuntu22ProcessorEngine
                  │     Java 17 applications on Ubuntu 22.
                  │     Type: java_17_Ubuntu_22
                  │
                  ├── CSharpProcessorEngine
                  │     C# / .NET applications.
                  │     Type: csharp
                  │
                  ├── IDL2ProcessorEngine
                  │     IDL (Interactive Data Language) applications.
                  │     Type: ubuntu_idl372
                  │
                  ├── OctaveProcessorEngine
                  │     GNU Octave applications.
                  │     Type: octave
                  │
                  ├── CondaProcessorEngine
                  │     Python applications using a Conda environment.
                  │     Type: conda
                  │
                  └── PersonalizedDockerProcessor
                        User-supplied Dockerfile. The user provides their
                        own full Docker image definition.
                        Type: personalized_docker
```

### Deploy flow

When a user uploads a ZIP file, the deploy procedure follows these steps:

1. **Check existence** — verify that no processor with the same name exists yet for this user
2. **Create processor folder** — create `<basePath>/processors/<appName>/` on the node filesystem
3. **Unzip user code** — extract the uploaded ZIP into the processor folder
4. **Copy template files** — copy the contents of `<basePath>/dockertemplate/<processorType>/` into the processor folder (Dockerfile, executor scripts, bundled libraries, etc.)
5. **Build Docker image** — trigger `docker build` in the processor folder, producing a tagged image
6. **Push to registry** — push the resulting image to one or more configured registries (typically the WASDI Nexus instance)
7. **Register** — record the processor metadata and image reference in the database

On subsequent nodes (for `DockerBuildOnceEngine`-based engines), the build step is skipped: the node pulls the pre-built image directly from the registry.

### Processor type templates (`processorTypes/`)

Each processor type has a dedicated template folder under `processorTypes/`. Its contents are copied into the new processor folder at deploy time.

| Template folder | Processor type | Key template files |
|-----------------|----------------|--------------------|
| `python312/` | `python312` | `Dockerfile`, `wasdiProcessorExecutor.py` |
| `pipOneShotDocker/` | `pip_oneshot` | `Dockerfile`, `wasdiProcessorExecutor.py` |
| `wasdiPythonPip2/` | `python_pip_2` | `Dockerfile`, `wasdiProcessorExecutor.py`, `wasdiProcessorServer.py` |
| `wasdiUbuntuFocalPython/` | `python_pip_2_ubuntu_20` | `Dockerfile`, `wasdiProcessorExecutor.py` |
| `wasdiPython37Docker/` | `ubuntu_python37_snap` | `Dockerfile`, executor scripts |
| `wasdiCondaDocker/` | `conda` | `Dockerfile`, `wasdiProcessorExecutor.py`, `wasdiProcessorServer.py`, `myProcessor.py` |
| `wasdiJava17Docker/` | `java_17_Ubuntu_22` | `Dockerfile`, `jwasdilib-0.7.4.jar`, `wasdiProcessorServer.py` |
| `wasdiCSharpDocker/` | `csharp` | `Dockerfile`, `WasdiLib.dll`, `WasdiRunner.dll`, `wasdiProcessorServer.py` |
| `wasdiIDLDocker/` | `ubuntu_idl372` | `Dockerfile`, `wasdiProcessorServer.py`, `runProcessor.sh`, `docker-entrypoint.sh` |
| `wasdiOctaveDocker/` | `octave` | `Dockerfile`, `jwasdilib-0.7.4.jar`, `wasdiProcessorServer.py` |
| `wasdiJupyterNotebookDocker/` | `jupyter-notebook` | `Dockerfile`, `traefik_notebook.yml.j2` |
| `eoepcaDocker/` | `eoepca` | `Dockerfile`, `appDeployBody.json.j2`, `wasdi-processor.cwl.j2`, `eoepcaProcessorExecutor.py` |
| `local_python312/` | `local_python312` | `wasdiProcessorExecutor.py` (no Docker build) |

The two recurring scripts are:

- **`wasdiProcessorServer.py`** — a small HTTP server (Flask/Gunicorn) that wraps the user code and exposes a `/run` endpoint; used by container-based types where the container stays alive across calls
- **`wasdiProcessorExecutor.py`** — a one-shot runner that launches the user code directly and exits; used by one-shot types where a fresh container is started per execution

---

## Launcher Operations

### Startup arguments

The Launcher is a Java process started by the Scheduler. It accepts three arguments:

| Argument | Description |
|----------|-------------|
| `-o <OPERATION>` | Uppercase operation code (e.g. `RUNPROCESSOR`, `DOWNLOAD`) — must match a value in `wasdi.shared.LauncherOperations` |
| `-p <guid>` | GUID of the `ProcessWorkspace` record created by the Server. The parameters for the operation are read from the database by this GUID |
| `-c <path>` | Path to the WASDI JSON configuration file (e.g. `/etc/wasdi/config.json`) |

### Startup sequence

1. The **Server** receives a user request, creates a `ProcessWorkspace` record in the database with state `CREATED`, and stores the operation parameters there
2. The **Scheduler** detects the `CREATED` record, picks a node, and spawns the Launcher process passing `-o`, `-p`, and `-c`
3. The Launcher reads its arguments, loads the config, and fetches the `ProcessWorkspace` from the database
4. It sets the `ProcessWorkspace` state to `RUNNING`
5. It instantiates the correct `Operation` subclass using **reflection** — the class name is derived from the operation code (first letter uppercased, rest lowercase) under the package `wasdi.operations`
6. It calls `operation.executeOperation(parameter, processWorkspace)`
7. On success the state transitions to `DONE`; on failure to `ERROR`

### Operations

All operation classes live in `wasdi.operations`, extend `Operation`, and their names match the `LauncherOperations` enum value with only the first letter capitalised.

| Operation code | Class | Description |
|----------------|-------|-------------|
| `DOWNLOAD` | `Download` | Downloads an EO product from a data provider. Checks for an existing local copy first, then delegates to the appropriate `DataProvider` implementation. Registers the file in the database and adds it to the workspace |
| `INGEST` | `Ingest` | Ingests a file already present on the node into a WASDI workspace |
| `SHARE` | `Share` | Copies a product from one workspace to another on the same node. Adds the file to the target workspace in the database |
| `PUBLISHBAND` | `Publishband` | Publishes a file (or a specific band) to GeoServer so it can be visualised on the map via WMS/WCS |
| `GRAPH` | `Graph` | Executes an ESA SNAP graph processing workflow (XML) on one or more input products |
| `MOSAIC` | `Mosaic` | Creates a mosaic from multiple input raster files |
| `SUBSET` | `Subset` | Extracts a spatial subset from a raster product |
| `MULTISUBSET` | `Multisubset` | Applies a subset operation to multiple input products in one task |
| `REGRID` | `Regrid` | Reprojects or resamples a raster product to a target grid |
| `DEPLOYPROCESSOR` | `Deployprocessor` | Deploys a user-uploaded application on the local node using the appropriate `WasdiProcessorEngine` |
| `RUNPROCESSOR` | `Runprocessor` | Executes a deployed processor (user application) on the node |
| `RUNIDL` | `Runidl` | Runs an IDL application; extends `Runprocessor` with IDL-specific handling |
| `REDEPLOYPROCESSOR` | `Redeployprocessor` | Re-deploys a processor (update) on the local node |
| `DELETEPROCESSOR` | `Deleteprocessor` | Removes a processor and its associated files/image from the local node |
| `LIBRARYUPDATE` | `Libraryupdate` | Updates only the WASDI client library inside a running processor container |
| `ENVIRONMENTUPDATE` | `Environmentupdate` | Triggers a full environment update inside a processor container (OS packages, runtime dependencies, etc.) |
| `FTPUPLOAD` | `Ftpupload` | Uploads a workspace file to a remote FTP server |
| `COPYTOSFTP` | `Copytosftp` | Copies a workspace file to the user's SFTP folder on the local node. Supports a `user;path` syntax to target another user's account |
| `LAUNCHJUPYTERNOTEBOOK` | `Launchjupyternotebook` | Starts a Jupyter Notebook container for interactive use |
| `TERMINATEJUPYTERNOTEBOOK` | `Terminatejupyternotebook` | Stops and removes a running Jupyter Notebook container |
| `KILLPROCESSTREE` | `Killprocesstree` | Forcefully kills a running process and its entire child process tree |
| `READMETADATA` | `Readmetadata` | Reads and stores metadata from a product file |
| `SEN2COR` | `Sen2cor` | Runs the ESA Sen2Cor atmospheric correction processor on a Sentinel-2 product |

---

## High-Level Code Architecture

WASDI follows a **Model → ViewModel → View / Controller** pattern across its Java codebase. The shared library (`wasdishared`) provides the model, data layer, and view models; the web server (`wasdiwebserver`) provides the controllers (REST resources); the Angular client is the view.

```
┌──────────────────────────────────────────────────────────┐
│                      wasdishared                         │
│                                                          │
│  business/       ← Entities (Model)                      │
│  data/           ← Repositories (Data Layer)             │
│  viewmodels/     ← View Models                           │
│  config/         ← Configuration classes                 │
│  parameters/     ← Launcher operation parameters         │
│  payloads/       ← Launcher operation payloads           │
│  queryexecutors/ ← EO catalogue query adapters           │
│  packagemanagers/← Pip / Conda package manager impls     │
│  geoserver/      ← GeoServer utilities                   │
│  rabbit/         ← RabbitMQ utilities                    │
│  utils/          ← General-purpose utilities             │
└──────────────────────────────────────────────────────────┘
         ▲                        ▲
         │                        │
┌────────┴──────────┐   ┌─────────┴──────────┐
│   wasdiwebserver  │   │      launcher       │
│  it.fadeout.rest  │   │   wasdi.operations  │
│    .resources/    │   │                     │
│  (Controllers)    │   │  (Operation classes)│
└───────────────────┘   └─────────────────────┘
```

### Model — `wasdi.shared.business`

Entities are plain Java classes serialised to and from the database. Key entities:

| Entity class | Description |
|--------------|-------------|
| `users/` | User accounts and authentication data |
| `Workspace` | An isolated working environment per user |
| `processors/` | Processor (application) metadata |
| `ProcessWorkspace` | A task instance — any operation executed in a workspace |
| `ProductWorkspace` | A product (file) registered in a workspace |
| `SnapWorkflow` | An ESA SNAP graph processing workflow |
| `Style` | An SLD style file for map rendering |
| `Schedule` | A cron-based scheduled app execution |
| `Node` | A WASDI computing node |
| `Organization`, `Project`, `Subscription` | Multi-tenancy and billing entities |

### Data Layer — `wasdi.shared.data`

Each entity has a corresponding `*Repository` class. The repository hierarchy is:

```
*Repository          ← public API (one per entity)
    │
    ├── interfaces/I*RepositoryBackend   ← interface defining backend operations
    │
    ├── mongo/       ← MongoDB implementation
    ├── no2/         ← Nitrite (no2) implementation
    └── sqlite/      ← SQLite implementation
```

The active backend is selected via the `dbProvider` field in `WasdiConfig`. All higher-level code depends only on the public `*Repository` class, never on a specific backend.

### View Models — `wasdi.shared.viewmodels`

View models are the data structures exchanged between the Server and its clients (web UI, libraries). Each view model is designed to match exactly what a particular client screen or library method needs. A single entity may have multiple view models (e.g. a full detail VM and a compact list VM), and a single VM may aggregate data from several entities.

### Configuration — `wasdi.shared.config`

All configuration is read from a single JSON file (path passed as `-c` to the Launcher, or configured in Tomcat for the Server). The JSON is deserialised directly into strongly-typed configuration classes. The resulting object is available globally as `WasdiConfig.Current`.

### Parameters and Payloads — `wasdi.shared.parameters` / `wasdi.shared.payloads`

- **`parameters/`** — one parameter class per Launcher operation. The web server serialises the parameters to the database when creating a `ProcessWorkspace`; the Launcher reads them back before executing the operation
- **`payloads/`** — payload classes for the result of each operation. After execution, the Launcher can write a structured JSON payload back to the `ProcessWorkspace` record, queryable by the client

### Query Executors — `wasdi.shared.queryexecutors`

Each supported EO data catalogue has its own `QueryExecutor` sub-package. The factory (`QueryExecutorFactory`) instantiates the correct executor based on the requested data provider. Currently supported catalogues include:

`Copernicus Dataspace` · `CREODIAS 2` · `CDS (Copernicus Climate)` · `ADS` · `DLR` · `EODC` · `ESA` · `Globathy` · `GPM` · `JRC` · `LP DAAC` · `LSA Data Center` · `ONDA` · `Planet` · `ProbaV` · `SINA` · `SkyWatch` · `Sobloo` · `Terrascope` · `VIIRS` · `web plugin`

### Utils — `wasdi.shared.utils`

| Sub-package / class | Purpose |
|---------------------|---------|
| `runtime/RunTimeUtils` | Abstracts execution of external processes. Depending on configuration can issue host shell calls or Docker API calls — the rest of the codebase calls this without caring about the actual runner |
| `docker/` | Docker Java client wrappers (build, push, run, stop, inspect) |
| `log/` | Logging utilities (wraps Log4j) |
| `gis/` | GIS / raster helper functions |
| `jinja/` | Jinja2-style template rendering (used for Dockerfiles and config templates) |
| `HttpUtils` | HTTP client helpers |
| `MailUtils` | Email notification helpers |
| `JsonUtils` | JSON serialisation/deserialisation helpers |
| `WasdiFileUtils` / `ZipFileUtils` / `TarUtils` | File and archive utilities |

### REST Controllers — `it.fadeout.rest.resources`

The web server exposes a JAX-RS REST API. Each resource class handles one area of the API:

| Resource class | Base path | Responsibility |
|----------------|-----------|----------------|
| `AuthResource` | `/auth` | Login, logout, session management |
| `WorkspaceResource` | `/ws` | Workspace CRUD and sharing |
| `ProductResource` | `/product` | Product (file) management within a workspace |
| `ProcessorsResource` | `/processors` | Processor (app) registration, deploy, run, search |
| `ProcessWorkspaceResource` | `/process` | Query and manage `ProcessWorkspace` task records |
| `ProcessingResources` | `/processing` | Trigger built-in processing operations (mosaic, subset, etc.) |
| `CatalogResources` | `/catalog` | EO catalogue search (proxies to `QueryExecutors`) |
| `OpenSearchResource` | `/search` | OpenSearch catalogue interface |
| `OpportunitySearchResource` | `/searchorbit` | Orbit-based opportunity search |
| `WorkflowsResource` | `/workflows` | SNAP workflow upload and execution |
| `StyleResource` | `/styles` | SLD style upload and management |
| `ImagesResource` | `/images` | Published band / image layer management |
| `NodeResource` | `/node` | Computing node management |
| `PackageManagerResource` | `/packageManager` | Remote package manager operations on processors |
| `ProcessorsMediaResource` | `/processormedia` | Processor logo / media management |
| `ProcessorParametersTemplateResource` | `/processorParamTempl` | Saved parameter templates for processors |
| `OrganizationResource` | `/organizations` | Organisation management |
| `ProjectResource` | `/projects` | Project management |
| `SubscriptionResource` | `/subscriptions` | Subscription and billing |
| `CreditsPackageResource` | `/credits` | Credits package management |
| `ConsoleResource` | `/console` | Real-time process log streaming |
| `FileBufferResource` | `/filebuffer` | Chunked file upload/download buffer |
| `PrinterResource` | `/print` | Map printing utilities |
| `AdminDashboardResource` | `/admin` | Admin-only operations (user management, metrics, queue management) |
| `WasdiResource` | `/wasdi` | General WASDI info and health check endpoints |

---

## Developer Instructions

### Prerequisites

- **Java 21** (JDK)
- **Maven 3.x**
- A running database — MongoDB locally, a cloud-hosted instance, a Docker container (`docker run -p 27017:27017 mongo`), or SQLite for the simplest setup
- *(Optional)* Eclipse IDE for Java EE Developers or VS Code with the Extension Pack for Java

---

### Building

All server-side projects are Maven modules declared in the root `pom.xml`. Each module's `pom.xml` uses the `${revision}` property inherited from the parent. You must set this variable when running Maven — either on the command line or in your IDE run configuration.

**Build the full project from the repo root:**

```bash
mvn clean install -Drevision=1.0
```

**Build a single module (e.g. `wasdiwebserver`):**

```bash
cd wasdiwebserver
mvn clean install -Drevision=1.0
```

The modules must be built in dependency order. The parent pom declares them in the correct order, so a full build from the root always works. If you need to build only a subset, the dependency order is:

```
wasdishared → launcher / wasdiwebserver / scheduler / wasditrigger / ogcprocesses / openeo-java-server
```

---

### Configuration

The server reads a single JSON configuration file at startup. A template is provided at `configuration/wasdiConfig.json.j2`.

Create your own `wasdiConfig.json` by filling in:

```json
{
  "dbEngine": "mongo",
  "mongoMain": {
    "address": "localhost",
    "port": 27017,
    "dbName": "wasdi",
    "user": "",
    "password": ""
  },
  "mongoLocal": {
    "address": "localhost",
    "port": 27017,
    "dbName": "wasdi",
    "user": "",
    "password": ""
  }
}
```

To use SQLite instead of MongoDB, set `"dbEngine": "sqlite"` — no external process is required and the database file is created automatically on first run.

The full list of configuration fields is documented in `wasdishared/src/main/java/wasdi/shared/config/WasdiConfig.java` and in the [Configuration reference](https://wasdi.readthedocs.io/en/latest/InsideWasdi/Configuration.html).

---

### IDE Setup

#### Eclipse

1. Install **Eclipse IDE for Java EE Developers** and the **M2Eclipse** Maven plugin (usually bundled)
2. Clone the repository
3. **File → Import → Maven → Existing Maven Projects** — select the repo root; Eclipse will discover all modules
4. In the build/run configuration set the environment variable or system property `-Drevision=1.0`
5. Make sure Eclipse uses the JRE inside the JDK: **Window → Preferences → Java → Installed JREs** — add the JDK and set it as default

#### VS Code

1. Install the **Extension Pack for Java** (Microsoft)
2. Open the repo root folder
3. VS Code will detect the Maven projects automatically via the Java extension
4. Add `"revision": "1.0"` to your Maven runner settings, or pass `-Drevision=1.0` in the terminal build command
5. Use the **Maven** side panel to run `clean install` on individual modules

---

### Running the Server

**Option 1 — Standalone (recommended for development):**

Run `it.fadeout.MiniWasdiServer` as a plain Java application. The embedded Jetty server starts on port `8080` and exposes the API at:

```
http://localhost:8080/wasdiwebserver/rest/
```

Pass the config file path as a VM argument:

```
-DwasdiConfigFilePath=/path/to/wasdiConfig.json
```

**Option 2 — Tomcat:**

Deploy the `wasdiwebserver` WAR to a local Tomcat instance and configure the config file path in Tomcat's `context.xml` or as a JNDI resource.

---

### Connecting a Client

Once the server is running, connect one of:

- **WASDI Web Client** — clone [wasdi-cloud/wasdiClient2](https://github.com/wasdi-cloud/wasdiClient2) and point it to your local server URL
- **WASDI Python Library** — configure `wasdi.init("/path/to/wasdi_config.json")` pointing to `http://localhost:8080/wasdiwebserver/rest/`
- **Any WASDI library** — set the base URL to the local server in the library configuration

---

### Debugging the Launcher

When you trigger an operation from the client or library, the server stores a `ProcessWorkspace` record in the database and logs its GUID. You can then run or debug the Launcher directly in your IDE:

1. Find the GUID in the server logs (look for `ProcessWorkspace guid:` or similar)
2. Create a run/debug configuration for `wasdi.LauncherMain` with arguments:

```
-o <OPERATION_CODE> -p <process_workspace_guid> -c /path/to/wasdiConfig.json
```

For example, to debug a processor execution:

```
-o RUNPROCESSOR -p fab5028a-341b-4bd3-ba7e-a321d6eb54ca -c /etc/wasdi/wasdiConfig.json
```

3. Set breakpoints in the relevant operation class under `wasdi.operations` or in the processor engine under `wasdi.processors`
4. Start the debug session — the Launcher will pick up the `ProcessWorkspace`, set its state to `RUNNING`, and execute the operation


