.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _SearchS1Images


Search Sentinel-1 Images
=========================================
The following code shows how to search S1 Images.


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A `valid Config file <https://wasdi.readthedocs.io/en/latest/PythonCookbook/createConfig.html>`_
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.


Recipe 
------------------------------------------

.. note::
	Assume you have at least one workspace and you have configured it in the config.json file.

These are different samples of Sentinel 1 Search. The mandatory fields to search are:
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
   sEndDate = wasdi.getParameter("END_DATE", "2023-01-31")

   # Start Search GRD Images
   aoProductsFoundArray = wasdi.searchEOImages("S1", sStartDate, sEndDate, sProductType="GRD", oBoundingBox=oBBox)

   # The result is an array of Objects. Each Object is a Dictionary. 
   
   # If we have results 
   if len(aoProductsFoundArray) > 0:

        # We just loop on the results and explore some properties
       for oFoundImage in aoProductsFoundArray:
           # This is where to read the relative Orbit
           iOrbit = oFoundImage["properties"]["relativeorbitnumber"]
           # THis is the name of the file
           sFileName = oFoundImage["fileName"]
           # There are many other properties, depending by the Provider and the Mission, that can be explored
   
   # Now lets search SLC Images
   aoSLCFoundArray = wasdi.searchEOImages("S1", sStartDate, sEndDate, sProductType="SLC", oBoundingBox=oBBox)
   wasdi.wasdiLog("Found " + str(len(aoSLCFoundArray)) + " SLC Images")

   # For Sentinel 1, we can also filter the Relative Orbit
   iRelativeOrbit = 43
   aoSLCPerOrbitFoundArray = wasdi.searchEOImages("S1", sStartDate, sEndDate, sProductType="SLC", oBoundingBox=oBBox, iOrbitNumber=iRelativeOrbit)
   wasdi.wasdiLog("Found " + str(len(aoSLCPerOrbitFoundArray)) + " SLC Images in orbit " + str(iRelativeOrbit))

   # If we have a String Bounding Box...
   sBBox = "20.1,43.2,19.3,44.4"
   # We can convert it in the object
   oBoundingBox = wasdi.bboxStringToObject(sBBox)
   # Or we can also use directly lat and lon in the search:
   aoSLCWithLatLonFound = wasdi.searchEOImages("S1", sStartDate, sEndDate,  fULLat=20.1, fULLon=43.2, fLRLat=19.3, fLRLon=44.4, sProductType="SLC")
   wasdi.wasdiLog("Found " + str(len(aoSLCWithLatLonFound)) + " SLC Images")



What it does:

 - Initializes the input variable needed.
 - Starts searching for S1 GRD Images
 - Loops over the results and accesses some properties
 - Searches for SLC Images
 - Searches for  SLC Images adding the relative orbit filter
 - Searches for GRD Images using the lat lon values and not the Bounding Box Object

.. note::
	The developer can decide what is needed in the params.json file. If you decide to use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
	With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsible to convert your data in your code.

.. note::
	The Bounding Box Format used here is the one used by the User Interface when renderAsStrings is missing or false. The Bounding Box format when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**.

.. note::
	The Date is formatted by the User Interface as "YYYY-MM-DD".
