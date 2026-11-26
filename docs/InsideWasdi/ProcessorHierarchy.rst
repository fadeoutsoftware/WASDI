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
   +-- DockerProcessorEngine (abstract; shared Docker lifecycle)
   |   +-- PipProcessorEngine (shared Python/Pip behavior)
   |   |   +-- UbuntuPython37ProcessorEngine [ubuntu_python37_snap]
   |   |   +-- DockerBuildOnceEngine (shared build-once behavior)
   |   |       +-- OneShotProcessorEngine (abstract; transient app containers)
   |   |       |   +-- PipOneShotProcessorEngine [pip_oneshot]
  |   |       |   |   +-- Python312Ubuntu24ProcessorEngine [python312]
  |   |       |   +-- CondaProcessorEngine [conda]
   |   |       +-- CSharpProcessorEngine [csharp]
   |   |       +-- IDL2ProcessorEngine [ubuntu_idl372]
   |   |       +-- Java17Ubuntu22ProcessorEngine [java_17_Ubuntu_22]
   |   |       +-- OctaveProcessorEngine [octave]
   |   |       +-- PersonalizedDockerProcessor [personalized_docker]
   |   |       +-- PythonPipProcessorEngine2 [python_pip_2]
   |   |       +-- Ubuntu20Pip2ProcessorEngine [python_pip_2_ubuntu_20]
   |   +-- EoepcaProcessorEngine [eoepca]
   |   +-- JupyterNotebookProcessorEngine [jupyter-notebook]
   |   +-- OgcAppPackageProcessorEngine [ogc_app_package]
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
* ``MeluxinaRuntimeDriver`` is an inactive draft for one Slurm job per run.
  It builds and pushes locally, submits a configured batch script through
  Slurm REST, stores the job ID, polls its status, and requests cancellation.
  Existing saved MeluXina jobs can be routed back to the driver, but new jobs
  are not automatically selected for it.

MeluXina draft configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Runtime configuration provides generic ``runtimeUrl``, ``user``, ``password``,
``workingDirectory`` and ``startScript`` fields, plus an ``options`` map of
provider-specific string values. Existing Runpod configuration fields remain
unchanged.

For a driver named ``meluxina``, set ``workingDirectory`` and ``startScript``,
and provide ``account`` and ``qos`` in ``options``. The account identifies the
project allocation, not the login username. ``user`` and ``password`` are not
used by the current authentication stub. Optional ``partition`` selects a
resource pool; optional ``tresPerNode`` requests trackable resources per node,
such as GPUs. QoS controls scheduling policy and limits.

If omitted, the driver defaults ``runtimeUrl`` to
``https://slurm-api.lxp.lu`` and ``options.apiVersion`` to ``v0.0.43``. From the
cloud partition, configure the site-provided cloud URL instead. The processor
timeout is sent as the Slurm time limit, rounded up to minutes.

Illustrative configuration (project, policy, resources and paths must be
confirmed with the site; replace the example script with the real launcher):

.. code-block:: json

   {
     "name": "meluxina",
     "runtimeUrl": "https://slurm-api.lxp.lu",
     "workingDirectory": "/project/home/pXXXXXX",
     "startScript": "#!/bin/bash -l\nexec /project/home/pXXXXXX/wasdi-launch.sh\n",
     "options": {
       "apiVersion": "v0.0.43",
       "account": "pXXXXXX",
       "qos": "short",
       "partition": "gpu",
       "tresPerNode": "gres/gpu=1"
     }
   }

The earlier draft's ``slurmUrl``, ``slurmWorkingDirectory`` and
``slurmBatchScript`` fields are replaced by ``runtimeUrl``, ``workingDirectory``
and ``startScript``. Move ``slurmApiVersion``, ``slurmAccount``, ``slurmQos``,
``slurmPartition`` and ``slurmTresPerNode`` to their corresponding ``options``
keys shown above. The old fields are no longer supported.

The batch script receives ``WASDI_IMAGE`` (a Docker registry image reference),
``WASDI_PROCESS_WORKSPACE_ID``, ``WASDI_USER``, ``WASDI_SESSION_ID``,
``WASDI_WORKSPACE_ID``, ``WASDI_WEBSERVER_URL``,
``WASDI_ONESHOT_ENCODED_PARAMS`` and ``WASDI_ONESHOT_ON_SERVER=false`` in the
Slurm job environment. It must load Apptainer, arrange registry access and
storage bindings, set a writable ``WASDI_BASE_PATH``, pass the WASDI variables
into the container, and run the processor executor. GPU jobs also need an
appropriate allocation and Apptainer's ``--nv`` option. The current Python
one-shot templates require the executor's ``--runpod`` flag for a nonzero exit
on initialization or application failure; its use outside Runpod is provisional.

The token acquisition method is intentionally a stub, and no new MeluXina
jobs can run until authentication and driver selection are implemented. The
batch script, image access, site resource settings, networking and job-state
handling have not been verified on MeluXina. Slurm may retain the submitted
environment, including the WASDI session ID; confirm site security and retention
policies before enabling this integration. A failed submit can be ambiguous, so
check Slurm before retrying. Cancellation means the request was accepted, not
that the job has already stopped. WASDI may report a terminal application status
before Slurm finishes releasing its resources.

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