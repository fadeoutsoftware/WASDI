.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _BasicAppStructure


Search S1 Images
=========================================
The following code show how to search S1 Images


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

These are different samples of Sentinel 1 Search. The mandatory fields to search are:
 - Mission Type
 - Start Date
 - End Date
 - Product Type
 - Bounding Box


.. code-block:: python

   # Create the Boundig Box Object: usually you will take if from the parameters
   oBbox = wasdi.getParameter("BBOX", None)

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
   ProductsFoundArray = wasdi.searchEOImages("S1", sStartDate, sEventDate, sProductType="GRD", oBoundingBox=oBBox)

   # The result is an array of Objects. Each Object is a Dictionary. 
   
   # If we have results 
   if len(ProductsFoundArray) > 0:

        # We just loop on the results and explore some properties
       for oFoundImage in aoReturnList:
           # This is where to read the relative Orbit
           iOrbit = oFoundImage["properties"]["relativeorbitnumber"]
           # THis is the name of the file
           sFileName = oFoundImage["fileName"]
           # There are many other proprties, depending by the Provider and the Mission, that can be explored
   
   # Now lets search SLC Images
   SLCFoundArray = wasdi.searchEOImages("S1", sStartDate, sEventDate, sProductType="SLC", oBoundingBox=oBBox)
   wasdi.wasdiLog("Found " + str(len(SLCFoundArray)) + " SLC Images")

   # For Sentinel 1, we can also filter the Relative Orbit
   RelativeOrbit = 43
   SLCPerOrbitFoundArray = wasdi.searchEOImages("S1", sStartDate, sEventDate, sProductType="SLC", oBoundingBox=oBBox, iOrbitNumber=RelativeOrbit)
   wasdi.wasdiLog("Found " + str(len(SLCPerOrbitFoundArray)) + " SLC Images in orbit " + str(RelativeOrbit))

   # If we have a String Boundig Box...
   StringBBox = "20.1,43.2,19.3,44.4"
   # We can convert it in the object
   oBoundingBox = wasdi.bboxStringToObject(StringBBox)
   # Or we can also use directly lat and lon in the search:
   SLCWithLatLonFound = wasdi.searchEOImages("S1", sStartDate, sEventDate,  fULLat=20.1, fULLon=43.2, fLRLat=19.3, fLRLon=44.4, sProductType="SLC")
   wasdi.wasdiLog("Found " + str(len(SLCWithLatLonFound)) + " SLC Images")



What it does:

 - Initialize the input varialbe needed. 
 - Start Search S1 GRD Images
 - Loop the results and access some properties
 - Serach SLC Images
 - Search SLC Images adding the relative orbit filter
 - Sarch GRD Images using the string-based Bounding Box

.. note::
	The developer can decide whatever is needed in the params.json file. If you will use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
	With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsable to convert your data in your code

.. note::
	The Boundig Box Format Here Used is the one used by the User Interface when renderAsStrings is missing or false. The Boundig Box fromat when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**

.. note::
	The Date is formatted by the User Interface as "YYYY-MM-DD"
