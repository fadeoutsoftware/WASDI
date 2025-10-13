Landslide Detection S2
======================

.. contents:: Table of Contents
   :depth: 3

1 Description
---------------

The Landslide Detection S2 application is a two-tier system designed to automatically identify and map landslides over a specified Area of Interest (AoI) using Sentinel-2 L2A imagery. The system consists of two interconnected processors:

* **The Automatic Application (Orchestrator)**: This is the main user entry point (`landslide_detection_s2_automatic`). It automates the entire workflow, from finding the best satellite images to managing the analysis and producing a final, seamless mosaic.

* **The Manual Application (Analysis Engine)**: This is the core scientific processor (`landslide_detection_s2_manual`). It performs a detailed change detection analysis on a single pair of pre- and post-event images provided by the automatic app.

The core methodology is based on a robust change detection technique using the **Tasseled Cap Transformation (TCT)**. The algorithm identifies the unique signature of a landslide: a simultaneous, sharp **increase in soil brightness** and a **decrease in vegetation greenness**.

2 Process Workflow
--------------------

The end-to-end workflow is managed by the automatic processor and proceeds in the following steps:

1.  **Automated Image Search**: The automatic processor queries the Sentinel-2 L2A archive based on the user-defined `EVENT_DATE`, `BBOX`, and a time window (`DAYS_BACK`, `DAYS_FORWARD`). It filters the results by a maximum overall cloud percentage.

2.  **Intelligent Candidate Filtering**: The processor groups the found images by their satellite tile ID. For each tile, it identifies the most recent pre-event image and the earliest post-event image as the best candidates.

3.  **Precise Cloud Screening**: For the best candidate pair, the processor performs a detailed cloud check. It downloads and unzips the product to analyze the Scene Classification Layer (SCL), calculating the cloud/shadow percentage specifically within the user's BBOX. If the pair passes, the unzipped folder is preserved for the next step.

4.  **Sub-Process Execution**: For each validated, cloud-free pair, the automatic processor launches a `landslide_detection_s2_manual` sub-process. It passes the paths to the pre- and post-event `.zip` files and instructs the manual processor not to perform cleanup. This allows multiple tiles to be processed in parallel.

5.  **Core Analysis (in Manual App)**: Each manual app sub-process independently performs the following:

        * Co-registration: It loads the pre-event image to create a reference grid, then warps the post-event image to ensure perfect pixel-to-pixel alignment.
        * SCL Masking: It aligns the SCL band for both images and masks out all pixels corresponding to clouds and shadows.
        * TCT Change Detection: It calculates the **Delta TCT** (Brightness, Greenness, Wetness) to identify where the landscape has changed.
        * Prioritized Mask Creation: It builds a final 3-class mask based on the priority:   **1 (Landslide) > 255 (Cloud) > 0 (No Landslide)**.

6.  **Automated Mosaicking**: After all manual jobs are complete, the automatic processor collects the individual 3-class masks. It then uses a prioritized merging algorithm to stitch them into a single, seamless mosaic TIFF file that respects the `1 > 255 > 0` pixel priority across tile boundaries.

7.  **Final Cleanup**: If the `DELETE` parameter is `true`, the automatic processor's final cleanup routine deletes all intermediate files created during the entire run, including input `.zip` files, unzipped `.SAFE` folders, temporary `.vrt` files, and all outputs from the individual manual processor runs.

3 Application Parameters
------------------------

This section describes the parameters for the main **automatic processor**, which is the primary entry point for the workflow.

3.1 Basic Parameters
~~~~~~~~~~~~~~~~~~~~

-   **BASENAME**: A base name or prefix for the final mosaic output file.
-   **EVENT_DATE**: The date of the event (landslide trigger), in `YYYY-MM-DD` format. This is the reference date for finding pre- and post-event imagery.
-   **BBOX**: The Bounding Box that defines the overall Area of Interest for the search and analysis.

3.2 Advanced Parameters
~~~~~~~~~~~~~~~~~~~~~~~

-   **DAYS_BACK** (defaults to `200`): The number of days to search backward from the event date for a pre-event image.
-   **DAYS_FORWARD** (defaults to `200`): The number of days to search forward from the event date for a post-event image.
-   **MIN_DAYS_DISTANCE** (defaults to `0`): The minimum number of days required between the pre-event image and the event date.
-   **MAX_CLOUD** (defaults to `10.0`): The maximum overall cloud percentage for an S2 tile to be considered in the initial search.
-   **SPECIFIC_MAX_CLOUD_COVERAGE** (defaults to `1.0`): The maximum allowed cloud percentage within the BBOX during the precise SCL-based check.
-   **MIN_COVERAGE** (defaults to `10.0`): The minimum percentage of the BBOX that must be covered by a candidate image's footprint.
-   **FORCE_RERUN** (defaults to `false`): If `true`, the processor will delete any existing results for a tile pair and re-run the analysis.
-   **DELETE** (defaults to `true`): If `true`, all intermediate files will be deleted, leaving only the final mosaic.

4 Outputs
---------

The workflow generates a primary final output and several intermediate products (which are deleted if `DELETE` is `true`).

4.1 Primary Output
~~~~~~~~~~~~~~~~~~

-   **Landslide Mosaic Mask**: A single, mosaicked raster file where pixel values indicate the combined classification result from all processed tiles.

        *   `{BASENAME}_landslide-mask-mosaic.tif`
        *   Example: `Wayanad_Event_landslide-mask-mosaic.tif`


4.2 Understanding the Pixel Values
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The final mask uses a prioritized system to represent the analysis results:

* 1 (Landslide): A landslide was detected. This value has the highest priority.
* 255 (Cloud / No-Data): The area was obscured by clouds or shadows in either the pre- or post-event image, or has No-Data.
* 0 (No Landslide): The area was analyzed and found to be stable. This value is also used for areas at the edge of a satellite's imaging path (some S2 tiles may not fully cover the BBOX specified by the user). This is crucial for allowing valid data from adjacent tiles to correctly fill gaps during mosaicking.

4.3 Intermediate Outputs (from each Manual App sub-process)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

-   Aligned True-Color Images: `..._pre-event_...tif` and `..._post-event_...tif`
-   Individual Landslide Mask: `..._landslide-mask.tif`
-   Quick-Look PNGs: `..._landslide-mask.png` and `..._ndvi-distribution.png`

5 How to Use It
---------------

5.1 Running the Automatic Workflow
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The primary way to use the system is by running the automatic processor with a set of parameters.

.. code-block:: json

    {
      "BASENAME": "Wayanad",
      "EVENT_DATE": "2024-06-30",
      "BBOX": {
        "northEast": {
          "lat": 11.575830515901927,
          "lng": 76.20769500732423
        },
        "southWest": {
          "lat": 11.456741052534444,
          "lng": 76.09371185302736
        }
      },
      "PROVIDER": "AUTO",
      "DAYS_BACK": 200,
      "DAYS_FORWARD": 200,
      "MAX_CLOUD": 10,
      "SPECIFIC_MAX_CLOUD_COVERAGE": 1,
      "MIN_DAYS_DISTANCE": 0,
      "MIN_COVERAGE": 0,
      "DELETE_S2_FILES": true,
      "DELETE": false
    }


5.2 Running the Manual App Standalone
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The manual processor can also be run on its own. It requires the user to manually select the pre- and post-event `.zip` files from the workspace and provide a `BBOX`. It will perform the full analysis for that single pair.

6 References
------------

- Coluzzi, R., Perrone, A., Samela, C. et al. "Rapid landslide detection from free optical satellite imagery using a robust change detection technique." *Sci Rep* 15, 4697 (2025). [`Link <https://doi.org/10.1038/s41598-025-89542-8>`_]
- Shi, T., & Xu, H. (2019). "Tasseled Cap Transformation Coefficients for Sentinel-2 Surface Reflectance." *IEEE Journal of Selected Topics in Applied Earth Observations and Remote Sensing*, 12(9), 3174-3182. [`Link <https://doi.org/10.1109/JSTARS.2019.2938388>`_]
