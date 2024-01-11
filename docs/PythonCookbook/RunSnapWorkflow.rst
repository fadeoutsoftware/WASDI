.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _RunSnapWorkflow


Run Snap Workflow
=========================================
This snippet show how to run SNAP worklows by code


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A valid Config file
 - A valid params.json file
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.


Recipe 
------------------------------------------

.. note::
	We will use Sentinel-1 GRD and the public LISTSinglePreproc2 workflow for this snippet, but can be used with any mission and any compatible workflow.

.. note::
	Remember that you can upload in WASDI your own SNAP Workflows. Thake a look `here <https://wasdi.readthedocs.io/en/latest/UserManual/UsingYourWorkspace.html#workflows>`_.


This is our sample params.json file.

.. code-block:: json

   {
       "START_DATE": "2023-01-01",
       "END_DATE": "2023-01-01",
       "BBOX": {
       "northEast": {
           "lat": 20.1,
           "lng": 44.4
           },
       "southWest": {
           "lat": 19.3,
           "lng": 43.2
           }
       },
       "MISSION": "S1",
       "PRODUCT_TYPE": "GRD",
       "WORKFLOW": "LISTSinglePreproc2"
   }




.. code-block:: python

   # Read Bounding Box, Start and End Date
   oBBox = wasdi.getParameter("BBOX", None)
   sStartDate = wasdi.getParameter("START_DATE", "2023-01-01")
   sEndDate = wasdi.getParameter("END_DATE", "2023-01-31")

   # Read Mission and Product Type
   sMission = wasdi.getParameter("MISSION", "S1")
   sProductType = wasdi.getParameter("PRODUCT_TYPE", "GRD")
   sWorkflow = wasdi.getParameter("WORKFLOW", "LISTSinglePreproc2")

   # Search Images
   aoProductsFound = wasdi.searchEOImages(sMission, sStartDate, sEndDate, sProductType=sProductType, oBoundingBox=oBBox)

    if len(aoImagesToProcess)>0:
        # Import the first image
        wasdi.importProduct(aoProductsFound[0])

        # Get the name of the image
        sImageName = aoProductsFound[0]["fileName"]

        # In general, workflows can have multiple inputs
        asInputImages = [sImageName]

        # We need to decide the name of the output file: here you may add a more smart code (add a suffix to the original name for example)
        sOutputImage = "preprocessed.tif"

        # In general, workflows can have also multiple outputs
        asOutputImages = [sOutputImage]

        #OPTION 1: run the workflow and wait for the result
        wasdi.executeWorkflow(asInputImages, asOutputImages, sWorkflow)

        #ALTENRATIVE OPTION 2: run asynch
        sProcessId = wasdi.asynchExecuteWorkflow(asInputImages, asOutputImages, sWorkflow)
        # Here you are free to do what you want
        wasdi.wasdiLog("I started a workflow")
        # Call this if you need to wait for it to finish
        wasdi.waitProcess(sProcessId)
    else:
        wasdi.wasdiLog("No file to pre-process found")

What it does:

 - Read Input Parameters
 - Start Search S1 GRD Images
 - Run the workflow waiting for it
 - Run the workflow without waiting for it
