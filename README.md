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

| Language | Library |
|----------|---------|
| Python   | `wasdi` | 
| JavaScript | `wasdi-js` |
| TypeScript | `wasdi-ts` |
| Java | `wasdi-java` |
| C# | `wasdi-csharp` |
| Octave | `wasdi-octave` |

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

