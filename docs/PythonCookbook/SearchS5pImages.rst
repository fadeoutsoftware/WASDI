.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _SearchS3Images


Search Sentinel-5p products
=========================================
The following code shows how to search Sentinel-5p products in WASDI


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

These are different samples of Sentinel-5p Search. The mandatory fields to search are:
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

    # Set Start Date getting the parameter from parameters file, fallbacks to 2023-01-01 if value is not specified
    sStartDate = wasdi.getParameter("START_DATE", "2023-01-01")
    # Set End Date getting the parameter from parameters file, fallbacks to 2023-01-10 if value is not specified
    sEndDate = wasdi.getParameter("END_DATE", "2023-01-10")

    # product type: Nitrogen Dioxide (NO2)
    sProductType = 'L2__NO2___'

    # Start Search S3 Images using the automatic provider selection
    aoProductsFoundArray = wasdi.searchEOImages("S5P", sDateFrom=sStartDate, sDateTo=sEndDate,
                                                sProductType=sProductType,
                                                sProvider="AUTO", oBoundingBox=oBBox)

    # The result is an array of Objects. Each Object is a Dictionary.

    # Let's see how many products correspond to our query
    wasdi.wasdiLog(f'Your query identified {len(aoProductsFoundArray)} products')

    # If we have results
    if len(aoProductsFoundArray) > 0:
        # as an example, let's print the filename of the first product we found
        wasdi.wasdiLog(f'{aoProductsFoundArray[0]["fileName"]}')
           

What it does:

 - Initialize the input variable needed. 
 - Start Searching for Sentinel-5p products choosing automatically the provider
 - prints the number of results
 - as an example, prints the filename of the first product found

.. note::
   The developer can decide whatever is needed in the params.json file. If you will use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
   With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsible to convert your data in your code

.. note::
   The Bounding Box Format Here Used is the one used by the User Interface when renderAsStrings is missing or false. The Bounding Box format when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**

.. note::
   The Date is formatted by the User Interface as "YYYY-MM-DD"
