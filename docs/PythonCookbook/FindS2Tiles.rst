.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _FindS2Tiles


Get list of S2 tiles in an area of interest
=========================================
The following code shows how to list the different S2 tiles that intersects the area of interest


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A valid Config file
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.


Recipe 
------------------------------------------

.. note::
	Assume you have at least one workspace and you configured it in the config.json file

This snippet is meant to get a list of unique S2 Tiles as a result of a search. For more info about the search of S2 please see `Search Sentinel-2 Images <https://https://wasdi.readthedocs.io/en/latest/PythonCookbook/SearchS2Images.html>`_ .


.. code-block:: python

   # Read the Bounding Box Object: usually you will take if from the parameters
   oBBox = wasdi.getParameter("BBOX", None)
   # Set Start Date
   sStartDate = wasdi.getParameter("START_DATE", "2023-01-01")
   # Set End Date
   sEndDate = wasdi.getParameter("END_DATE", "2023-01-10")

   # Start Search S2 MSI1C Images (Level 1)
   aoProductsFoundArray = wasdi.searchEOImages("S2", sStartDate, sEndDate, sProductType="S2MSI1C", oBoundingBox=oBBox)

   # The result is an array of Objects. Each Object is a Dictionary. 

   # Here we will put our list of tiles
   asTiles = []
   try:
      for oS2L2Image in aoProductsFoundArray:
         sImage = oS2L2Image["title"]
         sTile = sImage.split('_')[5]
         if sTile not in asTiles:
               asTiles.append(sTile)

   except Exception as oE:
       wasdi.wasdiLog(f'Error listing the tiles: {type(oE)}: {oE}')
       wasdi.updateStatus('ERROR', 0)
       return
   
   wasdi.wasdiLog("Involved Tiles " + str(asTiles))



What it does:

 - Read Bounding Box and Dates from parameters
 - Starts searching S2 L1 Images
 - Loop the results and extract unique Tiles

.. note::
	The same snippet can be used also for Level 2 Data.
