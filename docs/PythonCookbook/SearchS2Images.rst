.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _BasicAppStructure


Search Sentinel-2 Images
=========================================
The following code show how to search S2 Images


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

These are different samples of Sentinel 2 Search. The mandatory fields to search are:
 - Mission Type
 - Start Date
 - End Date
 - Product Type
 - Bounding Box


.. code-block:: python

   # Create the Bounding Box Object: usually you will take if from the parameters
   oBBox = wasdi.getParameter("BBOX", None)

   # If it is null we show here how to initialize manually
   if oBBox is None:
        oBBox = {"northEast": {}, "southWest": {}}
        oBBox["northEast"]["lat"] = 20.1
        oBBox["northEast"]["lng"] = 44.4
        oBBox["southWest"]["lat"] = 19.3
        oBBox["southWest"]["lng"] = 43.2

   # Set Start Date
   sStartDate = wasdi.getParameter("START_DATE", "2023-01-01")
   # Set End Date
   sEndDate = wasdi.getParameter("END_DATE", "2023-01-10")

   # Start Search S2 MSI1C Images (Level 1)
   aoProductsFoundArray = wasdi.searchEOImages("S2", sStartDate, sEndDate, sProductType="S2MSI1C", oBoundingBox=oBBox)

   # The result is an array of Objects. Each Object is a Dictionary. 
   
   # If we have results 
   if len(aoProductsFoundArray) > 0:

        # We just loop on the results and log file names
       for oFoundImage in aoProductsFoundArray:
           # THis is the name of the file
           sFileName = oFoundImage["fileName"]
           wasdi.wasdiLog("Found " + sFileName)
           # There are many other proprties, depending by the Provider and the Mission, that can be explored
   
   # Now lets search L2 Images
   aoL2FoundArray = wasdi.searchEOImages("S2", sStartDate, sEndDate, sProductType="S2MSI2A", oBoundingBox=oBBox)
   wasdi.wasdiLog("Found " + str(len(aoL2FoundArray)) + " S2MSI2A Images")

   # For Sentinel 1, we can also filter on the Cloud Coverage
   sCloudCover = "[0 TO 50]"
   aoL2CloudCoverFound = wasdi.searchEOImages("S2", sStartDate, sEndDate, sProductType="S2MSI2A", oBoundingBox=oBBox, sCloudCoverage=sCloudCover)
   wasdi.wasdiLog("Found " + str(len(aoL2CloudCoverFound)) + " S2MSI2A Images with CloudCover = " + sCloudCover)

   # If we have a String Boundig Box...
   sBBox = "20.1,43.2,19.3,44.4"
   # We can convert it in the object
   oBoundingBox = wasdi.bboxStringToObject(sBBox)
   # Or we can also use directly lat and lon in the search:
   aoL2LatLonFound = wasdi.searchEOImages("S2", sStartDate, sEndDate,  fULLat=20.1, fULLon=43.2, fLRLat=19.3, fLRLon=44.4, sProductType="S2MSI2A")
   wasdi.wasdiLog("Found " + str(len(aoL2LatLonFound)) + " S2MSI2A Images")



What it does:

 - Initialize the input varialbe needed. 
 - Start Search S2 L1 Images
 - Loop the results and print file names
 - Search L2 Images
 - Search L2 Images adding the cloud coverage
 - Search L2 Images using the lat lon values and not the Bounding Box Object

.. note::
	The developer can decide whatever is needed in the params.json file. If you will use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
	With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsible to convert your data in your code

.. note::
	The Boundig Box Format Here Used is the one used by the User Interface when renderAsStrings is missing or false. The Boundig Box format when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**

.. note::
	The Date is formatted by the User Interface as "YYYY-MM-DD"
