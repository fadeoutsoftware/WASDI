.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _SearchS1Images


Search Copernicus Marine products
=========================================
The following code shows how to search for Copernicus Marine products in WASDI


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
	Assume you have at least one workspace and you have configured it in the config.json file.

To search for Copernicus Marine products, the following fields are mandatory:
 - Collection: this is always going to be 'CM'
 - Start Date
 - End Date
 - Bounding Box
 - product type

Then, it's also mandatory to provide search parameters specific for Copernicus Marine. CM products are usually organized with the following hierarchy:
 - dataset
 - variables

Additional parameters may apply, in particular, the protocol: SUBS usually tend to work in most cases, but there may be exceptions, so check case by case and don't hesitate to `reach our for support <https://discord.gg/JYuNhPaZbE>`_.

Let's see an example

.. code-block:: python

   # Create the Bounding Box Object: usually you will read it from the parameters
    oBBox = wasdi.getParameter("BBOX", None)

    # If it is null we show here how to initialize manually
    if oBBox is None:
        oBBox = {
            "northEast": {
                "lat": 44.2879447888337,
                "lng": 9.5
            },
            "southWest": {
                "lat": 43.5,
                "lng": 8.4
            }
        }
    # Set Start Date
    sStartDate = wasdi.getParameter("START_DATE", "2023-07-01")
    # Set End Date
    sEndDate = wasdi.getParameter("END_DATE", "2023-07-31")

    sProductType = 'OCEANCOLOUR_MED_BGC_HR_L3_NRT_009_205 - TDS'
    aoParams = {
        "dataset": "cmems_obs_oc_med_bgc_tur-spm-chl_nrt_l3-hr-mosaic_P1D-m",
        "variables": "CHL",
        "protocol": "SUBS"
    }

    aoProductsFoundArray = wasdi.searchEOImages(
        sPlatform='CM',
        oBoundingBox=oBBox, sDateFrom=sStartDate, sDateTo=sEndDate,
        sProvider='AUTO',
        sProductType=sProductType, aoParams=aoParams
    )

    # Usually, the result is a list of Dictionaries
    # In the case of Copernicus Marine, however, the list contain only one element encompassing all the data we required:
    wasdi.wasdiLog(f'Your query identified {len(aoProductsFoundArray)} products')

    # If we have results
    if len(aoProductsFoundArray) > 0:
        # let's see the filename corresponding to the product we found:
        wasdi.wasdiLog(f'{aoProductsFoundArray[0]["fileName"]}')



What it does:

 - Initializes the input variables
 - Searches for the corresponding results
 - count the results (should always be 1 for CM)
 - access a field in the results

.. note::
	The developer can decide what is needed in the params.json file. If you decide to use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
	With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsible to convert your data in your code.

.. note::
	The Bounding Box Format used here is the one used by the User Interface when renderAsStrings is missing or false. The Bounding Box format when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**.

.. note::
	The Date is formatted by the User Interface as "YYYY-MM-DD".
