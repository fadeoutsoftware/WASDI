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
| **Server** | Java | REST API — the main entry point for the web UI and the WASDI libraries. Handles authentication, workspace management, and registers operations as `ProcessWorkspace` records |
| **Scheduler** | Java | Continuously monitors the database and orchestrates operations across nodes. Picks up `CREATED` tasks, assigns them to an available node, and updates process states |
| **Launcher** | Java | Started by the Scheduler for each operation. Executes built-in operations directly (download, mosaic, subset, SNAP workflow execution). For user applications it acts as a wrapper: it sets up the runtime environment and then runs the user-provided code as a separate process |
| **Trigger** | Java | Lightweight component responsible for starting WASDI applications on a schedule (cron-style) |
| **Client** | Angular | Browser-based web UI — lets users manage workspaces, browse products, launch processors, visualise results on a map, and monitor process states |

