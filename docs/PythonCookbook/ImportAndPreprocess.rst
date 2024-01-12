.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _ImportAndPreprocess


Import And Pre-Process
=========================================
Often you may need to pre-process your images before beeing able to work. This snippets shows a convenient method to automatically pre-process all the imported images


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
	The LISTSinglePreproc2 is designed to geo reference a Sentinel-1 GRD Image (apply orbit, radiometric calibration, terrain correction...)


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
        # Import and pre-process all the images: '_preproc.tif' is the suffix added to the original file name that will be used as output name of the workflow
        wasdi.importAndPreprocess(aoProductsFound, sWorkflow, '_preproc.tif')

What it does:

 - Read Input Parameters
 - Start Search S1 GRD Images
 - Import and run the workflow on all the images
