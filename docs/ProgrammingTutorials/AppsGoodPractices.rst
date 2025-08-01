
WASDI Apps Good Practices
===========================

While developers have the freedom to design WASDI Applications as they see fit, following a set of good practices can significantly improve the quality, usability, and robustness of your work. This document outlines key recommendations that the WASDI team follows and suggests to all developers on the platform.

1.  Log the App Version
----------------------------

It is good practice to log a "user-oriented" version number as one of the first lines in your `run()` method. This helps users and support teams quickly identify which version of the code is executing, which is invaluable for debugging. A common three-number versioning scheme is: x.y.z (e.g., 1.0.0), representing Major, Minor, and Fix versions.

.. code-block:: python

    def run():
        wasdi.wasdiLog('My Awesome App v.1.0.0')
        # ... rest of the code


Remember to update this version number whenever you deploy a change to the application.

2.  Provide a Delete Flag for Intermediate Files
----------------------------

Applications often produce intermediate files before generating the final output. It is recommended to clean these files from the workspace at the end of a successful run. However, keeping these files can be useful for debugging or faster re-runs.

To handle this, include a boolean `DELETE` parameter that defaults to true.

.. code-block:: python

    # At the start of the run() method
    bDelete = wasdi.getParameter('DELETE', True)
    
    # ... at the end of the run() method
    if bDelete:
        wasdi.wasdiLog("Cleaning up intermediate files...")
        wasdi.deleteProduct('intermediate_file.tif')


3.  Check for Mandatory Parameters
----------------------------

Your application should always validate that mandatory parameters have been provided. If a required parameter is missing, the app should log a clear message and exit gracefully. Exiting with a `DONE` status is generally preferred in this case, as it clearly indicates to the user that it is not due to an issue with the application, but rather a problem with the inputs.

.. code-block:: python

    sBbox = wasdi.getParameter('BBOX')
    if sBbox is None:
        wasdi.wasdiLog("ERROR: BBOX parameter is mandatory.")
        wasdi.updateStatus("DONE", "BBOX parameter not provided.") 
        return

.. note::
   Make sure to give default values to the parameters whenever possible, so as to auto-recover.

    .. code-block:: python
    
        from datetime import datetime
    
        # Case 1
        iFloodValue = wasdi.getParameter('FLOOD_VALUE', 1) # Here the default value of FLOOD_VALUE is 1
    
        #Case 2
        sEventDate = wasdi.getParameter('EVENTDATE')
    
        if sEventDate is None or sEventDate == "":
            sEventDate = datetime.today() # Here sEventDate is assigned the current date



4.  Allow Optional Output Filenames
----------------------------

Giving users control over output filenames is a powerful feature. Your application should accept an optional parameter for the output name. If the parameter is not provided, the app should generate a sensible default name, often composed of other inputs like a base name, a date and a suffix.

.. code-block:: python

    sOutputName = wasdi.getParameter('OUTPUT_NAME')
    if sOutputName is None or sOutputName == "":
        # Create a default name
        sDate = wasdi.getParameter('DATE')
        sBaseName = wasdi.getParameter('BASENAME')
        sSuffix = wasdi.getParameter('SUFFIX')
        sOutputName = f"{sBaseName}_{sDate}_{sSuffix}.tif"


5.  Avoid Duplication and Add a 'Force Rerun' Flag
----------------------------

To save time and processing resources, an application should check if the files it is about to generate already exist in the workspace. If they do, it should skip that processing step.

To complement this, add a `FORCE_RERUN` flag (defaulting to false) that allows the user to delete existing files and force the generation of new ones.

.. code-block:: python

    bForceRerun = wasdi.getParameter('FORCE_RERUN', False)
    sOutputFile = "final_product.tif"
    asWorkspaceFiles = wasdi.getProductsByActiveWorkspace()
    
    if sOutputFile in asWorkspaceFiles and not bForceRerun:
        wasdi.wasdiLog(f"Output file {sOutputFile} already exists. Skipping.")
        return
    
    if sOutputFile in asWorkspaceFiles and bForceRerun:
        wasdi.wasdiLog("Forcing rerun, deleting existing file.")
        wasdi.deleteProduct(sOutputFile)
    
    # ... proceed with processing


6.  Clean Up Local Temporary Files
----------------------------

During processing, your app might create temporary files on the local disk of the processing node (e.g., by unzipping archives). It is a good practice to ensure these files are removed before the application finishes to conserve space.

.. code-block:: python

    import os
    
    try:
        # ... logic that creates a temporary local file ...
        sTempFilePath = wasdi.getPath(sTempFileName)
    finally:
        # Ensure cleanup happens even if errors occur
        if os.path.exists(sTempFilePath):
            wasdi.deleteProduct(sTempFilePath)
            os.remove(sTempFilePath)


7.  Structure the Processor Payload
----------------------------

The payload is a JSON object that stores the results of a processor run. It is extremely useful for traceability and for chaining applications together. It is good practice to structure the payload with distinct `inputs` and `outputs` sections.

  * The `inputs` section should contain a dictionary of the input parameters used for the run.
  * The `outputs` section should contain the names of the final files produced by the application.

.. code-block:: python

    # At the end of the run() method
    aoPayload = {}
    aoPayload["INPUTS"] = wasdi.getParametersDict()
    aoPayload["OUTPUTS"] = {
        "Flood_Map": "Final_Flood_Map.tif",
        "Water_Depth_Map": "Final_WDM.tif"
    }
    
    wasdi.setPayload(aoPayload)


8.  Write a Good Help File (readme.md)
----------------------------

The `readme.md` file is the user manual for your processor. It should clearly describe what the application does, what parameters it requires, and what outputs it produces. A well-structured help file makes your application accessible and easy to use.

We recommend the following standard format for your `readme.md` files:

**Overview** - 
A concise, one or two-sentence summary of the processor's main function.

**Key Features** - 
A bulleted list highlighting the most important capabilities of your application (e.g., "Automated Time-Series Generation", "Dual Flood Detection Algorithms", "Geospatial Cropping & Statistics").

**Output Maps** - 
This section describes the files created by the processor. Include the following for each output:

  * Filename Convention: Explain how output files are named.
  * Example: Provide a clear example filename.
  * Legend: Describe what the pixel values represent.
  * Data Type: Specify the raster data type (e.g., Float32, UInt8).
  * Payload: Mention any important information returned in the final job payload.

**Parameters** - 
List all user-configurable parameters, grouped into logical sections like "Basic", "Advanced", etc. For each parameter, provide:

  * `PARAMETER_NAME` (default is value): A brief description of what the parameter does.

**JSON Sample** - 
Include a complete JSON example showing a typical configuration for running the processor. 
For example: 


.. code-block:: json

   {
     "SUFFIX": "_flood.tiff",
     "PERMANENT_WATER_VALUE": 2,
     "DELETE_CONVERTED_FILE": true,
   }
