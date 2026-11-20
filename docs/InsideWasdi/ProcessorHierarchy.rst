.. _ProcessorHierarchy:

WASDI Processor Engine Hierarchy
================================

This document describes the processor engine inheritance tree and the
processor types currently registered by WASDI. It is intended as a quick
reference when checking which engine is a parent or child of another engine.

There are three related, but separate, concepts:

* A **processor type** is the string stored on a processor (for example,
  ``pip_oneshot``).
* A **processor engine** implements the lifecycle for a processor type. Engines
  inherit from ``WasdiProcessorEngine``.
* A **container runtime driver** implements container build/run operations for
  selected Docker-based engines. It is not a processor engine and is not part
  of the inheritance tree below.

Class hierarchy
----------------

The following tree shows the current Java inheritance relationships. Classes
marked ``abstract`` or ``shared base`` are implementation layers; they are not
processor types returned directly by the engine factory.

.. code-block:: text

   WasdiProcessorEngine (abstract)
   |
   +-- DockerProcessorEngine (abstract; shared Docker lifecycle)
   |   |
   |   +-- PipProcessorEngine (shared Python/Pip behavior)
   |   |   |
   |   |   +-- UbuntuPython37ProcessorEngine [ubuntu_python37_snap]
   |   |   |
   |   |   +-- DockerBuildOnceEngine (shared build-once behavior)
   |   |       |
   |   |       +-- OneShotProcessorEngine (abstract; transient app containers)
   |   |       |   |
   |   |       |   +-- PipOneShotProcessorEngine [pip_oneshot]
   |   |       |   |   |
   |   |       |   |   +-- Python312Ubuntu24ProcessorEngine [python312]
   |   |       |   |   
   |   |       |   +-- CondaProcessorEngine [conda]
   |   |       |
   |   |       +-- CSharpProcessorEngine [csharp]
   |   |       |
   |   |       +-- IDL2ProcessorEngine [ubuntu_idl372]
   |   |       |   
   |   |       +-- Java17Ubuntu22ProcessorEngine [java_17_Ubuntu_22]
   |   |       |   
   |   |       +-- OctaveProcessorEngine [octave]
   |   |       |   
   |   |       +-- PersonalizedDockerProcessor [personalized_docker]
   |   |       |   
   |   |       +-- PythonPipProcessorEngine2 [python_pip_2]
   |   |       |   
   |   |       +-- Ubuntu20Pip2ProcessorEngine [python_pip_2_ubuntu_20]
   |   |   
   |   +-- EoepcaProcessorEngine [eoepca]
   |   |
   |   +-- JupyterNotebookProcessorEngine [jupyter-notebook]
   |   |
   |   +-- OgcAppPackageProcessorEngine [ogc_app_package]
   |
   +-- LocalProcessorEngine [local_python312]

Registered processor types
--------------------------

The factory is ``WasdiProcessorEngine.getProcessorEngine`` in
``launcher/src/main/java/wasdi/processors/WasdiProcessorEngine.java``. The
string constants and template-folder mapping are in
``wasdishared/src/main/java/wasdi/shared/business/processors/ProcessorTypes.java``.

.. list-table:: Processor type to engine mapping
   :header-rows: 1
   :widths: 23 37 25 55

   * - Stored processor type
     - Engine class
     - Template folder
     - Quick description
   * - ``ubuntu_python37_snap``
     - ``UbuntuPython37ProcessorEngine``
     - ``python37``
     - Python 3.7 Docker processor using the local-build path.
   * - ``ubuntu_idl372``
     - ``IDL2ProcessorEngine``
     - ``idl``
     - IDL processor, built once and published for execution on compute nodes.
   * - ``octave``
     - ``OctaveProcessorEngine``
     - ``octave``
     - GNU Octave processor; the engine generates the Octave entry-point wrapper.
   * - ``conda``
     - ``CondaProcessorEngine``
     - ``conda``
     - Conda one-shot processor. The engine applies ``env.yml`` and runs the
       app in a transient container.
   * - ``jupyter-notebook``
     - ``JupyterNotebookProcessorEngine``
     - ``jupyter-notebook``
     - Creates and manages a JupyterLab notebook container for a workspace.
   * - ``csharp``
     - ``CSharpProcessorEngine``
     - ``csharp``
     - C# application using the build-once Docker flow.
   * - ``eoepca``
     - ``EoepcaProcessorEngine``
     - ``eoepca``
     - Builds and publishes an image, then deploys the application to an
       EOEPCA ADES.
   * - ``ogc_app_package``
     - ``OgcAppPackageProcessorEngine``
     - ``ogcAppPackage``
     - Deploys and runs an OGC Application Package described with CWL.
   * - ``python_pip_2``
     - ``PythonPipProcessorEngine2``
     - ``python_pip_2``
     - Legacy Python/Pip Docker processor using a persistent application
       container and build-once image publication.
   * - ``pip_oneshot``
     - ``PipOneShotProcessorEngine``
     - ``pip_oneshot``
     - Python/Pip one-shot processor. The app runs to completion in a transient
       container rather than serving an internal HTTP process.
   * - ``python_pip_2_ubuntu_20``
     - ``Ubuntu20Pip2ProcessorEngine``
     - ``wasdiUbuntuFocalPython``
     - Legacy Python/Pip persistent-container processor based on the Ubuntu
       20.04 template.
   * - ``java_17_Ubuntu_22``
     - ``Java17Ubuntu22ProcessorEngine``
     - ``java17``
     - Java 17 application using the build-once Docker flow.
   * - ``personalized_docker``
     - ``PersonalizedDockerProcessor``
     - ``personalized_docker``
     - User-provided/custom Docker environment using build-once publication.
   * - ``python312``
     - ``Python312Ubuntu24ProcessorEngine``
     - ``python312``
     - Python 3.12 on Ubuntu 24.04, based on the Pip one-shot engine.
   * - ``local_python312``
     - ``LocalProcessorEngine``
     - ``local_python312``
     - Runs Python 3.12 directly in a per-processor ``uv`` virtual environment;
       no Docker image is built.

Shared base classes
-------------------

``WasdiProcessorEngine``
   Root lifecycle abstraction. Defines operations such as deploy, run, delete,
   redeploy, environment update, and package refresh.

``DockerProcessorEngine``
   Shared Docker lifecycle for image assembly, deployment, and Docker-specific
   engine behavior. It also provides the runtime-driver factory.

``PipProcessorEngine``
   Shared Pip package-manifest handling and Python/Pip package-manager support.
   It is a parent class, not a separately registered processor type.

``DockerBuildOnceEngine``
   Shared centralized build-and-publish flow for engines whose compute nodes
   consume an image from a registry. Despite the historical name, specialized
   subclasses can override parts of this lifecycle.

``OneShotProcessorEngine``
   Shared transient-container model for applications that execute once and
   exit. Package-specific behavior is supplied by its descendants.

Runtime drivers
---------------

Runtime drivers are selected by ``DockerProcessorEngine.getContainerRuntimeDriver``
and are separate from processor-engine inheritance:

* ``LocalDockerDriver`` implements local Docker image and execution operations.
* ``RunPodRuntimeDriver`` delegates image build/push to local Docker and uses a
  Runpod Serverless endpoint for configured Pip one-shot GPU inference runs.

The saved ``ProcessWorkspace.runtimeDriver`` and ``runtimeReference`` identify
the driver and app execution for a specific run. ``ProcessWorkspace.pid`` and
``containerId`` identify the launcher process/container, not the app runtime.

Factory behavior
---------------

``WasdiProcessorEngine.getProcessorEngine`` maps the stored processor type to a
concrete engine. A missing type defaults to ``ubuntu_python37_snap``. An
unrecognized non-empty type also falls back to ``UbuntuPython37ProcessorEngine``;
the factory does not currently reject unknown values.

The class schema and registry table describe different relationships: the
schema is Java inheritance, while the table is the factory mapping exposed to
stored processor records.