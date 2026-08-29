# OGC Application Package template folder

This folder is intentionally (almost) empty.

An OGC Best Practice Application Package (https://docs.ogc.org/bp/20-089r1.html) uploaded by
the user is expected to already contain everything it needs:
- a `*.cwl` file (mandatory, describes the Workflow + CommandLineTool),
- either its own `Dockerfile` and source code (self-contained package, WASDI will build it), or
  no `Dockerfile` at all, in which case the CWL `DockerRequirement.dockerPull` must reference an
  already published image that WASDI will pull.

`OgcAppPackageProcessorEngine` still copies the content of this folder into the processor folder
(same lifecycle as every other Docker-based processor engine), so it is kept around for
consistency / future shared assets, but currently has nothing to add to the user's package.
