How to Import GFS Products
=========================================
The following code shows how to import GFS products to a workspace.


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

To search for GFS products, the following fields are mandatory:
 - Platform (this is always going to be 'GFS')
 - Start Date
 - End Date
 - Bounding Box


Four additional search parameters, specific for GFS products, are also mandatory:
 - Product Type: Specifies the meteorological variable or product (e.g., temperature, wind, precipitation)
 - Product Level: Defines the vertical atmospheric level (e.g., surface, 500 hPa, 2m above ground)
 - Model Run: Indicates the model cycle runtime (00, 06, 12, 18 UTC)
 - Forecast Time: Sets the forecast hour of product (from 000 to 384 hours)


.. note::
   The aforementioned search filters: producttype, productlevel, modelRun, and forecastTime, should be added to a dictionary,
   that you can then pass to the method `wasdi.searchEOImages` through the optional parameter `aoParams`


Let's see an example of code to search for GFS precipitation forecast products:

.. code-block:: python

    def get_precipitation_forecast_products():
        wasdi.wasdiLog("** GFS Precipitation Forecast Products **")

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

        # Set Search Date
        sSearchDate = wasdi.getParameter("SEARCH_DATE", "2025-01-01")

        # Define parameters for the search
        aoParams = {
            "producttype": "PRATE",
            "productlevel": "surface",
            "modelRun": "06",
            "forecastTime": "f000"
        }

        # Search for products with the specified parameters
        aoProductsFoundArray = wasdi.searchEOImages(
            sPlatform="GFS",
            oBoundingBox=oBBox,
            sDateFrom=sSearchDate,
            sDateTo=sSearchDate,
            sProvider='AUTO',
            aoParams=aoParams)

        # if we found a product:
        if len(aoProductsFoundArray) > 0:
            wasdi.wasdiLog(f"Found precipitation product for date - {oSearchDate.strftime('%Y-%m-%d')}, run - 06, forecast time - 00 hr UTC.")

            # Import the images
            wasdi.importProductList(aoProductsFoundArray)

            for oProduct in aoProductsFoundArray:
                sProductName = oProduct.get("fileName", "")

                # Ensure the file name ends with .grb2
                if not sProductName.endswith(".grb2"):
                    sProductName += ".grb2"


What it does:

 - Initializes the input variables
 - Searches for the corresponding GFS products for the given date
 - Checks that at least one product is found
 - Imports the product to the workspace
 - Ensures that the filename of the product ends with its format ".grb2"

.. note::
   The developer can decide what is needed in the search parameters, by specifying it in the `params.json` file.

.. note::
   The Bounding Box Format used here is the one used by the User Interface when renderAsStrings is missing or false. The Bounding Box format when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**.

.. note::
   The Date is formatted by the User Interface as "YYYY-MM-DD". A single search date is given for both the start and end date, as we are looking for the product from this specific date.

.. note::
   Ensure that the Product name always ends with ".grb2", so that the product can be accessed for further processing.
