Use CDS library to download ERA-5 products in WASDI
====================================================
The following code shows how to use the CDS (Climate Data Store) Python library to download ERA-5 products in WASDI.
This is an alternave procedure with respect to the one explained in `this recipe <https://wasdi.readthedocs.io/en/latest/PythonCookbook/SearchERA5Images.html>`_.
The code below, indeed, will not rely on the standard methods to search for products in WASDI and import them in the workspace (i.e., ``wasdi.searchEOImages``, ``wasdi.importProductList``,  ``wasdi.importProduct`` and  ``wasdi.asynchImportProduct``). By contrast, it will rely on the CDS Python library to directly import the desired ERA-5 product in a workspace in WASDI.


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A valid Config file
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.

Additonally, you will need a valid account and Personal Access Token to download ERA-5 products via the CDS library. You can create an account and find your Personal Access Token from the `CDS website <https://cds.climate.copernicus.eu/>`_. 


Recipe 
------------------------------------------

First of all, you will need to install the Python CDS library using pip, as explained in `the CDS user guide <https://cds.climate.copernicus.eu/how-to-api#install-the-cds-api-client>`_, running the command


.. code-block:: python

	pip install 'cdsapi>=0.7.2'


Secondly, you can import the CDS library in your code and use the CDS client to download the product you need. Lastly, you can add it to your WASDI workspace. 
The code below, for instance, shows how to download a grib file representing a ERA-5 Land product containing four variables, with the data referring to multiple days in a month, for all the 24 hours in a day, in a specific boundig box. 


.. code-block:: python

    def getEraLandData():
        oBoundingBox = wasdi.getParameter("BBOX")
        sYear = wasdi.getParameter("YEAR")
        sMonth = wasdi.getParameter("MONTH")
        asDays = wasdi.getParameter("DAYS")

        if oBoundingBox is None or sYear is None or sMonth is None or asDays is None:
            wasdi.wasdiLog("Some input parameters are missing")
            return

        if not isinstance(asDays, list):
            wasdi.wasdiLog("Please, specify the input days as a list")
            return

        dNorth = oBoundingBox["northEast"]["lat"]
        dEast = oBoundingBox["northEast"]["lng"]
        dSouth = oBoundingBox["southWest"]["lat"]
        dWest = oBoundingBox["southWest"]["lng"]

        wasdi.wasdiLog(f"Bounding box: {dNorth}, {dWest}, {dSouth}, {dEast}")
        
        asVariables = ["2m_dewpoint_temperature",
               "2m_temperature",
               "surface_solar_radiation_downwards",
               "surface_thermal_radiation_downwards"]

        # preparing the input for CDS
        asHours = [f"{hour:02}:00" for hour in range(24)]
        sDataset = "reanalysis-era5-land"
        oRequest = {
            "variable": asVariables,
            "year": sYear,
            "month": sMonth,
            "day": asDays,
            "time": asHours,
            "data_format": "grib",              # could also be read from the param file
            "download_format": "unarchived",    # could also be read from the param file
            "area": [dNorth, dWest, dSouth, dEast]
        }

        # file name can be customized as needed
        sFileName = f"ERA5-Land_{sYear}-{sMonth}-{min(asDays)}_{sYear}-{sMonth}-{max(asDays)}.grib"
        sSavePath = wasdi.getSavePath()
        if not sSavePath.endswith(os.path.sep):
            sSavePath += os.path.sep
        sDownloadPath = os.path.join(sSavePath, sFileName)
        wasdi.wasdiLog(f"output file path {sDownloadPath}")

        wasdi.wasdiLog("calling the CDS client")
        sCDSUrl = "https://cds-beta.climate.copernicus.eu/api"
		sCDSAPIKey = "1111-1111-1111-111" # TODO: add here the CDS Personal Access Token
        oCDSClient = cdsapi.Client(url=sCDSUrl, key=sCDSAPIKey)
        oCDSClient.retrieve(sDataset, oRequest).download(target=sDownloadPath)

        wasdi.wasdiLog("adding file to WASDI")
        wasdi.addFileToWASDI(sFileName)



What it does:

- Read a bouding box and a temporal reference from the parameters file
- Prepares the input for the CDS client
- Initializes the CDS client, by specifying its URL and the user Personal Access Token
- downloads the ERA-5 Land file corresponding to the input parameters sent in the CDS request
- add the downloaded file to the WASDI workspace


.. note::
   The developer can decide what is needed in the params.json file and in the specific input parameters for the CDS request. Each dataset on the CDS website, provides an useful interface to select the needed parameters and visualize the corresponding Python request to integrate in the code. For ERA-5 Land datastet, that interface is available `at the following URL <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_. Do not forget to accept, on the same user interface, the Terms of Use of the data, otherwise your download will fail.

 
Below we provide an example of prams.json file

    .. code-block:: json

             {
             "BBOX": {
                "northEast": {
                  "lat": 20.1,
                  "lng": -71.4
                },
                "southWest": {
                  "lat": 17.9,
                  "lng": -74.9
                }
             },
            "YEAR": "2023",
            "MONTH": "10",
            "DAYS": ["10", "11"]
          }
