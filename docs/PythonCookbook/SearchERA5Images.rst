Search ERA5 products
=========================================
The following code shows how to search for ERA5 products in WASDI


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

To search for ERA5 products, the following fields are mandatory:
 - Collection: this is always going to be 'ERA5'
 - Start Date
 - End Date
 - Bounding Box

Two additional search parameters, specific for ERA5 products, are also mandatory:
 - dataset
 - aggregation: this is used to specify how you want your data to be stored into the downloaded products. "daily" will produce one file per day, while "monthly" will produce one file per each calendar month

The other search parameters that you need to specify will depend from the specific dataset you are interested in.
For ERA5 pressure levels, ERA5 single leels and ERA5 Land, you will need to speficy:
 - product type
 - variables
 - format

Additionally, for ERA5 pressure levels, you need to specify:
 - pressure levels.

.. note::
   The aforementioned search filters: dataset, aggregation, variables, format and pressure levels, should be added to a dictionary, 
   that you can then pass to the method `wasdi.searchEOImages` through the optional parameter `aoParams`


Let's see an example of code to search for ERA5 pressure levels products, which is the more comprehensive with respect to search parameters involved:

.. code-block:: python

    def get_pressure_levels_products():
        wasdi.wasdiLog("** get_pressure_levels_products **")

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

        aoParams = {'dataset': "reanalysis-era5-pressure-levels",
                    "pressureLevels": "1000",
                    'variables': "RH",
                    'format': "netcdf",
                    'aggregation': "daily"}

        aoProductsFoundArray = wasdi.searchEOImages(
            sPlatform='ERA5',
            oBoundingBox=oBBox,
            sDateFrom=sStartDate,
            sDateTo=sEndDate,
            sProvider='AUTO',
            sProductType='reanalysis',
            aoParams=aoParams)


        if len(aoProductsFoundArray) > 0:
            # let's see the file name corresponding to the product we found:
            wasdi.wasdiLog(aoProductsFoundArray[0]["title"])



What it does:

 - Initializes the input variables
 - Searches for the corresponding results
 - checks that at least one file is returned
 - accesses a field in the results

.. note::
   The developer can decide what is needed in the params.json file. If you decide to use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
   With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsible to convert your data in your code.

.. note::
   The Bounding Box Format used here is the one used by the User Interface when renderAsStrings is missing or false. The Bounding Box format when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**.

.. note::
   The Date is formatted by the User Interface as "YYYY-MM-DD".
