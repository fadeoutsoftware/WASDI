.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _ImportSearchedImages


Import Images after a Search
=========================================
The following code show how to import the Workspace the results of a search.


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
	We will use Sentinel-1 for this sample but the same code can be used for all the missions and image types

.. note::
	In the code you will see different options. Probably in your code you will want to choose and use the one that best fits your needs


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
       "PRODUCT_TYPE": "GRD"
   }




.. code-block:: python

   # Read Bounding Box, Start and End Date
   oBBox = wasdi.getParameter("BBOX", None)
   sStartDate = wasdi.getParameter("START_DATE", "2023-01-01")
   sEndDate = wasdi.getParameter("END_DATE", "2023-01-31")

   # Read Mission and Product Type
   sMission = wasdi.getParameter("MISSION", "S1")
   sProductType = wasdi.getParameter("PRODUCT_TYPE", "GRD")

   # Search Images
   aoProductsFound = wasdi.searchEOImages(sMission, sStartDate, sEndDate, sProductType=sProductType, oBoundingBox=oBBox)

   # OPTION 1: Import a single image and wait for the image to be available
   if len(aoProductsFoundArray) > 0:
      wasdi.importProduct(aoProductsFoundArray[0])
   
   # OPTION 2: Import a single image WITHOUT waiting for it:
   if len(aoProductsFoundArray) > 0:
      sProcessId = wasdi.asynchImportProduct(aoProductsFoundArray[0])
      # Here you can do what you want
      wasdi.wasdiLog("Started Import of one image, the associated process id is " + sProcessId)
      # Call this if you need to wait
      wasdi.waitProcess(sProcessId)

   # OPTION 3: Import All Products and wait for the images to be available
   wasdi.importProductList(aoProductsFoundArray)

   # OPTION 4: Import All Products without waiting
   sProcessId = wasdi.asynchImportProductList(aoProductsFoundArray)
   # Here you can do what you want
   wasdi.wasdiLog("Started Import of all images, the associated process id is " + sProcessId)
   # Call this if you need to wait
   wasdi.waitProcess(sProcessId)


What it does:

 - Read Input Parameters
 - Start Search S1 GRD Images
 - Import 1 Product
 - Asynch Import of 1 Product
 - Import All Products
 - Asynch Import of All Products
