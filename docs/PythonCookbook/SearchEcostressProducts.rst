Search ECOSTRESS products
=========================================
The following code shows how to search for ECOSTRESS products in WASDI


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

To search for ECOSTRESS products, the following fields are mandatory:
 - Collection: this is always going to be 'ECOSTRESS'
 - Start Date
 - End Date
 - Bounding Box

Another additional search parameters, specific for ECOSTRESS products, is mandatory:
 - dataset

The other search parameters, that you can optionally specify, are:
 - relativeorbitnumber
 - parameterName
 - dayNightFlag

.. note::
   The  search filters: dataset, relativeorbitnumber and dayNightFlag should be added to a dictionary, 
   that you can then pass to the method `wasdi.searchEOImages` through the optional parameter `aoParams`


Let's see an example of code to search for ECOSTRESS products, by specifying all the available filters.

.. code-block:: python

    def get_ecostress_products():
        wasdi.wasdiLog("** get_ecostress_products **")

        # Create the Bounding Box Object: usually you will read it from the parameters
        oBBox = wasdi.getParameter("BBOX", None)

        # If it is null we show here how to initialize manually
        if oBBox is None:
            oBBox = {
                "northEast": {
                    "lat": 50,
                    "lng": 50
                },
                "southWest": {
                    "lat": 44,
                    "lng": 44
                }
            }

        sStartDate = wasdi.getParameter("START_DATE", "2018-01-10")
        sEndDate = wasdi.getParameter("END_DATE", "2018-12-19")

        oParameters = {'dataset': "L1B_RAD",
                    'relativeorbitnumber': "523",
                    'parameterName': "L1B_RAD",
                    'dayNightFlag': 'Day'}

        images = wasdi.searchEOImages(
            sPlatform='ECOSTRESS',
            oBoundingBox=oBBox,
            sDateFrom=sStartDate,
            sDateTo=sEndDate,
            sProvider='AUTO',
            aoParams=oParameters)

        wasdi.wasdiLog(f"get_ecostress_products: Found {len(images)} images")

        # let's print name of the first image found
        if len(images) > 0:
            print(images[0]['id'])



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
