.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _SearchImport



Search and Import EO Images
=========================================
This tutorial has to goal to show the functionalities available in WASDI Libraries to search and import EO Images.


Introduction
------------------------------------------
WASDI is a multi-cloud multi-data-provider platform. 

**Multi-Cloud** means that WASDI run on different cloud environments; your workspace can be hosted on CreoDIAS, ONDA, AdwaisEO, EDOC or other clouds. Generic users are routed by WASDI in the shared nodes, Premium users can have dedicated computing nodes and they can also decide to have a specific cloud, depending the real world needs. This allow us to make always the more optimized choice, and also to be able to have the system up and running also when a specific cloud has a planned or unplanned mantainance or worst problem.

**Multi-Data-Provider** means that WASDI is able to fetch (or import, or download, it depends) different kind of Data from different Providers. At the moment we are writing this tutorial WASDI is able to search and fecth these data:

* Sentinel-1
* Sentinel-2
* Sentinel-3
* Sentinel-5P
* VIIRS Nasa Flood Composite
* PROBA-V
* ENVI
* LANDSAT8
* ERA5 Copernicus Data
* CMES Copernicus Marine Data
* PLANET Commercial Images

WASDI is connected with these data providers:

* LSA Data Center
* ESA Sentinel Hub
* CREODIAS
* ONDA
* SOBLOO
* EODC
* CDS (Copernicus Data Science)
* NOAA
* Terrascope
 

The Platforms supported and the Data Providers connected are continuously growing. 

In this tutorial we will see how to search and import images in WASDI.


Search EO Images
------------------------------------------
The function to search EO Images is searchEOImages.

It is a single function with different options. 

.. code-block:: python

	def searchEOImages(sPlatform, sDateFrom, sDateTo,
					   fULLat=None, fULLon=None, fLRLat=None, fLRLon=None,
					   sProductType=None, iOrbitNumber=None,
					   sSensorOperationalMode=None, sCloudCoverage=None,
					   sProvider=None, oBoundingBox=None):
		"""
		Search EO images

		:param sPlatform: satellite platform:(S1|S2|S3|S5P|VIIRS|L8|ENVI|ERA5)

		:param sDateFrom: inital date YYYY-MM-DD

		:param sDateTo: final date YYYY-MM-DD

		:param fULLat: Latitude of Upper-Left corner

		:param fULLon: Longitude of Upper-Left corner

		:param fLRLat: Latitude of Lower-Right corner

		:param fLRLon: Longitude of Lower-Right corner

		:param sProductType: type of EO product; Can be null. FOR "S1" -> "SLC","GRD", "OCN". FOR "S2" -> "S2MSI1C","S2MSI2Ap","S2MSI2A". FOR "VIIRS" -> "VIIRS_1d_composite","VIIRS_5d_composite". FOR "L8" -> "L1T","L1G","L1GT","L1GS","L1TP". For "ENVI" -> "ASA_IM__0P", "ASA_WS__0P"

		:param iOrbitNumber: orbit number

		:param sSensorOperationalMode: sensor operational mode

		:param sCloudCoverage: interval of allowed cloud coverage, e.g. "[0 TO 22.5]"

		:param sProvider: WASDI Data Provider to query (AUTO|LSA|ONDA|CREODIAS|SOBLOO|VIIRS|SENTINEL). None means default node provider = AUTO.
		
		:param oBoundingBox: alternative to the float lat-lon corners: an object expected to have these attributes: oBoundingBox["northEast"]["lat"], oBoundingBox["southWest"]["lng"], oBoundingBox["southWest"]["lat"], oBoundingBox["northEast"]["lng"]

		:return: a list of results represented as a Dictionary with many properties. The dictionary has the "fileName" and "relativeOrbit" properties among the others 
		"""
	
The only mandatory params are:

* sPlatform: a string with the code of the platform. Each search is done for a single platform. S1|S2|S3|S5P|VIIRS|L8|ENVI|ERA5 are the currently supported platforms
* sDateFrom: start date of the search. It is a string in the format YYYY-MM-DD (ie "2021-12-15")
* sDateTo: end date of the search. It is a string in the format YYYY-MM-DD (ie "2021-12-15")

The other highly recommanded parameter is the bounding box. WASDI accepts only rectangle bounding boxes. This method supports two ways to specify the rectangle:

* fULLat, fULLon, fLRLat, fLRLon: four float numbers indicating Upper Left Latitude (North), Upper Left Longitude (West), Lower Right Latitude (South), Lower Right Longitude (East)
* oBoundingBox: an alternative that is an object that has  these attributes: oBoundingBox["northEast"]["lat"], oBoundingBox["southWest"]["lng"], oBoundingBox["southWest"]["lat"], oBoundingBox["northEast"]["lng"]

sProductType is not mandatory. Can be specified as the "level of processing" of the Platform. Product Types supported are:

* S1
	#. SLC
	#. GRD
	#. OCN	
* S2
	#. S2MSI1C
	#. S2MSI2Ap
	#. S2MSI2A
* S3
	#. SR_1_SRA___
	#. SR_1_SRA_A_
	#. SR_1_SRA_BS
	#. SR_2_LAN___	
* S5P
	#. L1B_IR_SIR
	#. L1B_IR_UVN
	#. L1B_RA_BD1
	#. L1B_RA_BD2
	#. L1B_RA_BD3
	#. L1B_RA_BD4
	#. L1B_RA_BD5
	#. L1B_RA_BD6
	#. L1B_RA_BD7
	#. L1B_RA_BD8
	#. L2__AER_AI
	#. L2__AER_LH
	#. L2__CH4___
	#. L2__CLOUD_
	#. L2__CO____
	#. L2__HCHO__
	#. L2__NO2___
	#. L2__NP_BD3
	#. L2__NP_BD6
	#. L2__NP_BD7
	#. L2__O3_TCL
	#. L2__O3____
	#. L2__SO2___
	#. AUX_CTMFCT
	#. AUX_CTMANA
* VIIRS
	#. VIIRS_1d_composite
	#. VIIRS_5d_composite
* L8
	#. L1T
	#. L1G
	#. L1GT
	#. L1GS
	#. L1TP
* ENVI
	#. ASA_IM__0P
	#. ASA_WS__0P
* PROBAV	
	#. urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001
	#. urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001
* ERA5
	#. reanalysis
	#. ensemble_mean
	#. ensemble_members
	#. ensemble_spread
* PLANET	
	#. PSScene
	#. PSScene3Band
	#. PSScene4Band
	#. PSOrthoTile
	#. REOrthoTile
	#. REScene
	#. SkySatScene
	#. SkySatCollect
	#. SkySatVideo
	

iOrbitNumber is supported by Sentinel-1 files and is used to filter the relative Orbit.


sSensorOperationalMode is also supported by Sentinel-1 files and can be:


* SM
* EW
* IW
* WV

sCloudCover is supported by Sentinel-2, Landsat8 and PROBA-V. It is the accepted cloud cover percentage. It is a string in this format: [MIN TO MAX]. For example "[0 TO 30]" means a maximum of 30% of cloud cover. "[0 TO 50]" means a maximum of 50% of cloud cover in the image.


sProvider is the provider to use to search and import images: the user can select one of the providers supported by wasdi: AUTO|LSA|ONDA|CREODIAS|SOBLOO|VIIRS|SENTINEL. By default, if it is left to null, WASDI will use the automatic data provider.


Automatic Data Provider
-----------------------------------------
WASDI implements the Automatic Data Provider that is strongly suggested if you do not have a clear need to work with a specific Data Provider. As we have seen, WASDI supports many Platforms and many Data Provider. 

In general, data of one Platform, can be obtained by one or more Data Providers.

The goal of WASDI is always: move the processor near to the data. We say, more realistically, minimize the data transfer. 

Each Cloud has its own data policy: some has a full archive of some Platform, some have more platforms but with a Long Term Archive policy for file olders than some days, some allows only download, some the file access... it is a complex and varied scenario.

The Automatic Data Provider of WASDI knows the data you are searching and where your code is running: using this info, **WASDI makes a smart choice of the best Data Provider for you**. This functionality can be used both for search and import. Indeed, it is used by default if you do not specify a specific provider.

The Automatic Data Provider has also the advantage to try to get the image from another provider if, for any reason there is a problem to reach the best one. And this is done with all the providers that supports the platform you are searching, making WASDI very resilient to the external problems that may happen.

searchEOImages Output
-----------------------------------------
The output of searchEOImages, is a list of Dictionary Objects with many properties. 

The main important keys of the dictionary are **"fileName"** and **"link"**. 
This means you can access the file name with this code:

.. code-block:: python

	aoFound = wasdi.searchEOImages("S1", sDateFrom="2021-02-01", sDateTo="2021-02-02", sProductType="GRD", fULLat=44.5, fULLon=8.5, fLRLat=44.0, fLRLon=9.0)
	if len(aoFound) > 0:
		wasdi.wasdiLog("Image 0 name: " + aoFound[0]["fileName"]

Usually, you can use directly that object for what you need (ie to import the image/images), but as we have seen you can directly access the properties you need.
In the returnet object, you can find also many other properties: these properties depends by the Platform and the selected Data Provider, and are easy to explore with a print or in a debug session.


Search Sample Code
-----------------------------------------
In this code we just make different search, of different data types. 

.. note::
	This tutorial goes streight to the point: if you need help how to setup a project, add a parameter files and run your WASDI Application please refer to :doc:`Python Tutorial </PythonTutorial>`.


First of all fill your params.json file:

.. code-block::

	{
	  "bbox": {
		"northEast": {
		  "lat": 30.0,
		  "lng": -96.0
		},
		"southWest": {
		  "lat": 29.5,
		  "lng": -96.5
		}
	  },
	  "date": "2020-10-09",
	  "searchdays": 10,
	  "provider": "AUTO",
	  "maxCloud": 30,
	  "s1Type": "GRD"
	}

We are defining a bounding box, a reference date, the number of days to search back and the Data Provider.
We also add a Max Cloud for S2 data and the product type for S1.

The full code is here:

.. code-block:: python

	import wasdi
	import sys
	from datetime import datetime
	from datetime import timedelta

	def run():

		try:
			# Read the bbox
			oBbox = wasdi.getParameter("bbox", None)
			# Read the reference Date
			sDate = wasdi.getParameter("date")
			# Read the provider
			sProvider = wasdi.getParameter("provider")
			# Read the number of days we want to search back from reference date
			iDays = wasdi.getParameter("searchdays", 10)
			# Cloud Cover
			iMaxCloud = wasdi.getParameter("maxCloud", 30)
			# S1 Product Type
			sS1ProductType = wasdi.getParameter("s1Type", "GRD")

			# A boundig box is really needed
			if oBbox is None:
				wasdi.wasdiLog("Boundig Box is null. The world is still too big.")
				wasdi.updateStatus("ERROR")
				sys.exit(1)

			# Initialize a safe date
			oEventDay = datetime.today()

			# Convert date from YYYY-MM-DD to a valid python date
			try:
				oEventDay = datetime.strptime(sDate, '%Y-%m-%d')
			except:
				wasdi.wasdiLog('Date not valid, assuming today')

			# Now we want to go back of iDays day
			oTimeDelta = timedelta(days=iDays)
			# Ok this is the start date
			oStartDay = oEventDay - oTimeDelta
			# And this is the end date
			oEndDay = oEventDay

			# Get back the date in string format
			sStartDate = oStartDay.strftime("%Y-%m-%d")
			sEndDate = oEndDay.strftime("%Y-%m-%d")

			# We start searching Sentinel 1 Data: here we use also product type
			aoFound = wasdi.searchEOImages("S1", sDateFrom=sStartDate, sDateTo=sEndDate, sProductType=sS1ProductType, sProvider=sProvider, oBoundingBox=oBbox)

			# Log how many images we found
			wasdi.wasdiLog("S1 found " + str(len(aoFound)))

			# This will be used to log but not too much
			iCount = 0

			# For each image
			for oImage in aoFound:
				# Log the file name
				wasdi.wasdiLog(" " + oImage["fileName"] + " Orbit " + str(oImage["relativeOrbit"]))
				# Increment the counter
				iCount = iCount +1
				if iCount>5:
					# Ok, understood the concept, now lets go on
					wasdi.wasdiLog("Break")
					break

			# Search S2 Data
			sCloudCoverage = "[0 TO " + str(iMaxCloud) + "]"
			aoFound = wasdi.searchEOImages("S2", sDateFrom=sStartDate, sDateTo=sEndDate, sCloudCoverage=sCloudCoverage, sProvider=sProvider, oBoundingBox=oBbox)

			# Log results, as before
			wasdi.wasdiLog("S2 found " + str(len(aoFound)))

			iCount = 0

			for oImage in aoFound:
				wasdi.wasdiLog(" " + oImage["fileName"])
				iCount = iCount +1
				if iCount>5:
					wasdi.wasdiLog("Break")
					break

			# Search S3 Data
			aoFound = wasdi.searchEOImages("S3", sDateFrom=sStartDate, sDateTo=sEndDate, sProvider=sProvider, oBoundingBox=oBbox)

			# Log results, as before
			wasdi.wasdiLog("S3 found " + str(len(aoFound)))

			iCount = 0

			for oImage in aoFound:
				wasdi.wasdiLog(" " + oImage["fileName"])
				iCount = iCount +1
				if iCount>5:
					wasdi.wasdiLog("Break")
					break

			# Search S5P Data
			aoFound = wasdi.searchEOImages("S5P", sDateFrom=sStartDate, sDateTo=sEndDate, sProvider=sProvider, oBoundingBox=oBbox)

			# Log results, as before
			wasdi.wasdiLog("S5P found " + str(len(aoFound)))

			iCount = 0

			for oImage in aoFound:
				wasdi.wasdiLog(" " + oImage["fileName"])
				iCount = iCount +1
				if iCount>5:
					wasdi.wasdiLog("Break")
					break

			# Search L8 Data
			aoFound = wasdi.searchEOImages("L8", sDateFrom=sStartDate, sDateTo=sEndDate, sProvider=sProvider, oBoundingBox=oBbox)

			wasdi.wasdiLog("L8 found " + str(len(aoFound)))

			iCount = 0

			# For each image
			for oImage in aoFound:
				wasdi.wasdiLog(" " + oImage["fileName"])
				iCount = iCount +1
				if iCount>5:
					wasdi.wasdiLog("Break")
					break

			#Search ENVI Data
			aoFound = wasdi.searchEOImages("ENVI", sDateFrom=sStartDate, sDateTo=sEndDate, sProvider=sProvider, oBoundingBox=oBbox)

			wasdi.wasdiLog("ENVI found " + str(len(aoFound)))

			iCount = 0

			# For each image
			for oImage in aoFound:
				wasdi.wasdiLog(" " + oImage["fileName"])
				iCount = iCount +1
				if iCount>5:
					wasdi.wasdiLog("Break")
					break

			# Search VIIRS Data
			aoFound = wasdi.searchEOImages("VIIRS", sDateFrom=sStartDate,sDateTo=sEndDate,sProvider=sProvider, oBoundingBox=oBbox)

			wasdi.wasdiLog("VIIRS found " + str(len(aoFound)))

			iCount = 0

			# For each image
			for oImage in aoFound:
				wasdi.wasdiLog(" " + oImage["fileName"])
				iCount = iCount +1
				if iCount>5:
					wasdi.wasdiLog("Break")
					break

		except Exception as oE:
			wasdi.wasdiLog("Error " + str(oE))
			wasdi.updateStatus('ERROR')
			sys.exit(1)

		wasdi.wasdiLog('Done bye bye')
		wasdi.updateStatus('DONE', 100)

	if __name__ == '__main__':
		wasdi.init('./config.json')
		run()

The output of this code depends by the params you are using: for example, to find ENVI images, you have to go in the past. L8 images at the moment are found only in Europe. The cloud coverage can influence the S2 results. We suggest to play a little bit with the params to see different results.

Import functionalities
------------------------------------------
Once you found your images, usually you need to import one or more of the results in your workspace to continue your work. To do this, the lib gives you different options

* importProductByFileUrl: import a single product using directly file name and url. Almost a legacy method, but left for advanced use.
* importProduct: import a single product. Takes in input one of the objects returned by searchEOImages.
* importProductList: import a list of products. Takes in input an array of objects as returned by searchEOImages.

These are the synch version. Synch means that the function will not exit until the import is done.  All methods returns the status of the operation, a string that can be:

* DONE: operation done with success
* ERROR: operation not done with an error
* STOPPED: operation stopped, by the user or by a timeout

A more advanced use of WASDI can bring you to use the asynch version of these methods. Asynch means that the method will return not the status but the processId of the import operation. This id can be used to query or wait the status of the import with waitProcess, waitProcesses, or getProcessStatus.

* asynchImportProductByFileUrl: import a single product using directly file name and url. Almost a legacy method, but left for advanced use.
* asynchImportProduct: import a single product. Takes in input one of the objects returned by searchEOImages.
* asynchImportProductList: import a list of products. Takes in input an array of objects as returned by searchEOImages.

The last option, is an optimized way to import a list of products and apply to them a specific SNAP Workflow. It can be a S1 search that, after the import, run a workflow to calibrate and geo-reference the image or a S2 that after the import run a workflow to run an Index like NDVI o many others.

* importAndPreprocess: Imports in WASDI and apply a SNAP Workflow to an array of EO Images as returned by searchEOImages. Takes in input the array of images, the name of the workflow to run, and the suffix to add to input files to create workflow output files.



Import Sample Code
------------------------------------------

The following python app make a search of S1 images and import the results in synch mode. It uses the same params used for the search sample.

.. code-block:: python

	import wasdi
	import sys
	from datetime import datetime
	from datetime import timedelta

	def run():

		try:
			# Read the bbox
			oBbox = wasdi.getParameter("bbox", None)
			# Read the reference Date
			sDate = wasdi.getParameter("date")
			# Read the provider
			sProvider = wasdi.getParameter("provider")
			# Read the number of days we want to search back from reference date
			iDays = wasdi.getParameter("searchdays", 10)
			# Cloud Cover
			iMaxCloud = wasdi.getParameter("maxCloud", 30)
			# S1 Product Type
			sS1ProductType = wasdi.getParameter("s1Type", "GRD")

			# A boundig box is really needed
			if oBbox is None:
				wasdi.wasdiLog("Boundig Box is null. The world is still too big.")
				wasdi.updateStatus("ERROR")
				sys.exit(1)

			# Initialize a safe date
			oEventDay = datetime.today()

			# Convert date from YYYY-MM-DD to a valid python date
			try:
				oEventDay = datetime.strptime(sDate, '%Y-%m-%d')
			except:
				wasdi.wasdiLog('Date not valid, assuming today')

			# Now we want to go back of iDays day
			oTimeDelta = timedelta(days=iDays)
			# Ok this is the start date
			oStartDay = oEventDay - oTimeDelta
			# And this is the end date
			oEndDay = oEventDay

			# Get back the date in string format
			sStartDate = oStartDay.strftime("%Y-%m-%d")
			sEndDate = oEndDay.strftime("%Y-%m-%d")

			# We start searching Sentinel 1 Data: here we use also product type
			aoFound = wasdi.searchEOImages("S1", sDateFrom=sStartDate, sDateTo=sEndDate, sProductType=sS1ProductType, sProvider=sProvider, oBoundingBox=oBbox)

			# Log how many images we found
			wasdi.wasdiLog("S1 found " + str(len(aoFound)))

			# Take the current product list
			asCurrentFiles = wasdi.getProductsByActiveWorkspace()

			if asCurrentFiles is not None:
				wasdi.wasdiLog("Products in the workspace before the import:  " + str(len(asCurrentFiles)))

			# Import products, it may take time...
			wasdi.importProductList(aoFound)

			# Refresh the list
			asCurrentFiles = wasdi.getProductsByActiveWorkspace()

			if asCurrentFiles is not None:
				wasdi.wasdiLog("Products in the workspace after the import:  " + str(len(asCurrentFiles)))

		except Exception as oE:
			wasdi.wasdiLog("Error " + str(oE))
			wasdi.updateStatus('ERROR')
			sys.exit(1)

		wasdi.wasdiLog('Done bye bye')
		wasdi.updateStatus('DONE', 100)

	if __name__ == '__main__':
		wasdi.init('./config.json')
		run()


The same work can be done in an asynch way:

.. code-block:: python

	import wasdi
	import sys
	from datetime import datetime
	from datetime import timedelta

	def run():

		try:
			# Read the bbox
			oBbox = wasdi.getParameter("bbox", None)
			# Read the reference Date
			sDate = wasdi.getParameter("date")
			# Read the provider
			sProvider = wasdi.getParameter("provider")
			# Read the number of days we want to search back from reference date
			iDays = wasdi.getParameter("searchdays", 10)
			# Cloud Cover
			iMaxCloud = wasdi.getParameter("maxCloud", 30)
			# S1 Product Type
			sS1ProductType = wasdi.getParameter("s1Type", "GRD")

			# A boundig box is really needed
			if oBbox is None:
				wasdi.wasdiLog("Boundig Box is null. The world is still too big.")
				wasdi.updateStatus("ERROR")
				sys.exit(1)

			# Initialize a safe date
			oEventDay = datetime.today()

			# Convert date from YYYY-MM-DD to a valid python date
			try:
				oEventDay = datetime.strptime(sDate, '%Y-%m-%d')
			except:
				wasdi.wasdiLog('Date not valid, assuming today')

			# Now we want to go back of iDays day
			oTimeDelta = timedelta(days=iDays)
			# Ok this is the start date
			oStartDay = oEventDay - oTimeDelta
			# And this is the end date
			oEndDay = oEventDay

			# Get back the date in string format
			sStartDate = oStartDay.strftime("%Y-%m-%d")
			sEndDate = oEndDay.strftime("%Y-%m-%d")

			# We start searching Sentinel 1 Data: here we use also product type
			aoFound = wasdi.searchEOImages("S1", sDateFrom=sStartDate, sDateTo=sEndDate, sProductType=sS1ProductType, sProvider=sProvider, oBoundingBox=oBbox)

			# Log how many images we found
			wasdi.wasdiLog("S1 found " + str(len(aoFound)))

			# Take the current product list
			asCurrentFiles = wasdi.getProductsByActiveWorkspace()

			if asCurrentFiles is not None:
				wasdi.wasdiLog("Products in the workspace before the import:  " + str(len(asCurrentFiles)))

			# Import products, in an async mode
			asProcIds = wasdi.asynchImportProductList(aoFound)

			# Do something else in the meanwhile: maybe smarter than this
			wasdi.wasdiLog("Here we are, while is working")
			iSampleNumber = 1
			iSampleNumber = iSampleNumber * 100
			wasdi.wasdiLog("We made something useless, in the meantime: " + str(iSampleNumber))

			# Ok now wait for WASDI to finish
			wasdi.waitProcesses(asProcIds)

			wasdi.wasdiLog("Imports done")

			# Refresh the list
			asCurrentFiles = wasdi.getProductsByActiveWorkspace()

			if asCurrentFiles is not None:
				wasdi.wasdiLog("Products in the workspace after the import:  " + str(len(asCurrentFiles)))

		except Exception as oE:
			wasdi.wasdiLog("Error " + str(oE))
			wasdi.updateStatus('ERROR')
			sys.exit(1)

		wasdi.wasdiLog('Done bye bye')
		wasdi.updateStatus('DONE', 100)

	if __name__ == '__main__':
		wasdi.init('./config.json')
		run()

Note that, while you are importing images, if you open the workspace on WASDI, you will see your operations on going.

Welcome to space.

Search Parameter Documentation
-----------------------------------------

Sentinel-1 Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.filename:** This is the Satellite Platform. It can be of type “S1A” or “S1B”. 

**.producttype:** This is the Product Type. It can be of type “SLC”, “GRD” or “OCN”. 

**.polarisationmode:** This is the Polarisation. It can be of type “HH”, “VV”, “HV”, “VH”, “HH+HV”, “VV+HH”. 

**.sensoroperationalmode:** This is the Sensor Mode. It can be of type “SM”, “IW”, “EW”, or “WV”. 

**.relativeorbitnumber:** This is the Relative Orbit Number. It can be an integer from 1 to 175. 

**.swathidentifier: ** This is the Swath.

Sentinel-2 Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.filename:** this is the Satellite Platform. It can be of type “S2A” or “S2B”. 

**.producttype:** this is the product type. It can be of type “S2MSI1C”, “S2MSI2Ap” or “S2MSI2A”. 

**.cloudcoverpercentage:** this is the Cloud Coverage Percentage. It can be an integer range from 0 to 100 - including float numbers (e.g., 0 to  9.4).

Sentinel-3 Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
**.productlevel:** this is the product level. It can be of type “L1” or “L2”. 

**.insturment:** This is the Insturment. It can only be of type “SRAL”. 

**.producttype:** This is the Product Type. Its value is determined by the following conditions: 

* If .productlevel is set to “L1” it can be of type "SR_1_SRA___”, "SR_1_SRA_A_” or “SR_1_SRA_BS”.

* If .productlevel is set to “L2” it can be of type “SR_2_LAN___”

**.timeliness:** This is the Timeliness. It can be of value “Near Real Time”, “Short Time Critical”, or “Non Time Critical”. 

**.realtiveorbitstart:** this is the Relative Orbit Start. The value can be an integer from 1 to 385.

Sentinel-5P Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.productlevel:** This is the product level. It can be of value “LEVEL1B” or “LEVEL2”. 

**.producttype:** This is the Product Type. Its value is determined by the following conditions: 

* If .productlevel is set to “LEVEL1B, the value can be “L1B_IR_SIR”, “L1B_IR_UVN”, “L1B_RA_BD1”, “L1B_RA_BD2”, “L1B_RA_BD3”, “L1B_RA_BD4”, “L1B_RA_BD5”, “L1B_RA_BD6”, “L1B_RA_BD7”, “L1B_RA_BD8”, or “AUX_CTMFCT”, “AUX_CTMANA”

* If .productlevel is set to “LEVEL2”, then the value of .producttype can be: “L2__AER_AI”, “L2__AER_LH”, “L2__CH4___”, “L2__CLOUD_”, “L2__CO____”, “L2__HCHO__”, “L2__NO2___”, “L2__NP_BD3”, “L2__NP_BD6”, “L2__NP_BD7”, “L2__O3_TCL”, “L2__O3____”, “L2__SO2___”, “AUX_CTMFCT”, or “AUX_CTMANA”

**.timeliness:** This is the Timeliness. It can be of value “Offline”, “Near real time”, or “Reprocessing”. 

**.absoluteorbit:** This is the Absolute Orbit Number.

PROBA-V Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.collection:** This is the Collection. The value can be "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001", or "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001"

**.cloudcoverpercentage:** This is the Cloud Coverage Percentage. It can be set to a range of 0 to 100 (e.g., 0 to 9.4). 

**.snowcoverpercentage:** This is the snowcoverpercentage. It can be set to a range of 0 to 100 (e.g., 0 to 9.4). 

**.productref:** This is the product ref. 

**.cameraId:** This is the Camera Id. 

**.productID:** This is the Product ID. 

**.year:** This is the year. 

**.Insturment:** This is the Insturment. It can be of value “VG1” or “VG2”.

Envisat Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.name:** This is the Type. It can be of value “ASA_IM__0P”, or “ASA_WS__0P”. 

**.orbitDirection:** This is the Orbit Direction. It can be of value “ASCENDING” or “DESCENDING”

Landsat8 Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.name:** This is the Type. It can be of value “L1T”, “L1G”, “L1GT”, “L1GS”, “L1TP”. 

**.cloudcoverpercentage:** This is the Cloud Cover Percentage. It can be set to a range of 0 to 100 (e.g., 0 to 94).

VIIRS Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.producttype:** This is the Product. It can be of value “VIIRS_1d_composite” or “VIIRS_5d_composite”. 

ERA5 Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.dataset:** This is the Dataset. It can be of value “reanalysis-era5-pressure-levels” or “reanalysis-era5-single-levels”. 

**.productType:** This is the Product Type. 

* If .dataset is set to “reanalysis-era5-pressure-levels” then the .producttype can be of value “reanalysis”, “ensemble_mean”, “ensemble_members”, or “ensemble_spread”.

* If .dataset is set to “reanalysis-era5-single-levels” then the .producttype can be of value “reanalysis”, “ensemble_mean”, “ensemble_members”, or “ensemble_spread”.

**.pressureLevels:** This is the Pressure Levels in hPa. If .dataset is set to “reanalysis-era5-pressure-levels” then the value of .pressureLevels can be “1”, “2”, “1+2”, or “1000”. 

**.variables:** This is the Variables. 

* If .dataset is set to “reanalysis-era5-pressure-levels” then the value of .variables can be “RH”, “U”, “V”, or “RH+U+V”.

* If .dataset is set to “reanalysis-era5-single-levels” then the value of .variables can be “SST+SP+ST” or “10U+10V+2DT+2T+SP”.

**.format:** This is the Format. It can be of value “grib” or “netcdf”.

CAMS Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.dataset:** This is the Dataset and can be of value “cams-global-atmospheric-composition-forecasts”.

**.type:** This is the Type. If the .dataset is set to “cams-global-atmospheric-composition-forecasts” then the value can be “forecast”. 

**.variables:** This is the Variables. If the .dataset is set to “cams-global-atmospheric-composition-forecasts” then the value will be set to “dust_aerosol_optical_depth_550nm”.

**.format:** This is the Format. It can be of value “grib” or “netcdf_zip”.

PLANET Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.producttype:** This is the Planet Item Type. It can be of value “PSScene”, “PSScene3Band”, “PSScene4Band”, “PSOrthoTile”, “REOrthoTile”, “REScene”, “SkySatScene”, “SkySatCollect”, or “SkySatVideo”. 

DEM Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.dataset:** This is the Dataset. It can be of value “DEM_30M” or “DEM_90M”. 

WorldCover Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.dataset:** This is the Dataset. It can be of value “10m_2020_V1”.

StaticFiles Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.producttype:** This is the Product Type. It can be of either value “ESA_CCI_LAND_COVER_2015” or “ESACCI-Ocean-Land-Map-150m-P13Y-2000”.

IMERG Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.latency:** This is the Latency. It can be of value “Early” or “Late”. 

**.duration:** This is the Duration. Its value is based on the following conditions:

* If .latency is set to “Early” .duration can have a of value “HHR”.

* If .latency is set to “Late” .duration can have a value of “HHR”, “DAY”, or “MO”

**.accumulation:** This is the Accumulation. Its value is based on the following conditions: 

* If .latency is set to “Early” and .duration is set to “HHR” then .accumulation can have a value of “30min”, “1day”, “3hr”, or “All”.

* If .latency is set to “Late” and .duration is set to “HHR” then .accumulation can have a value of “30min”, “1day”, “3day”, “7day”, “3hr” or “All”.

CM Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.producttype:** This is the Product Type. It can have a value of 

* “OCEANCOLOUR_MED_CHL_L4_NRT_OBSERVATIONS_009_041-TDS”, 

* “OCEANCOLOUR_GLO_BGC_L4_MY_009_108-TDS”,

* “SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012-TDS”,

* “SST_MED_SST_L4_NRT_OBSERVATIONS_010_004-TDS”,

* “GLOBAL_ANALYSIS_FORECAST_WAV_001_027-TDS”,

* “INSITU_MED_NRT_OBSERVATIONS_013_035”, or

* “OCEANCOLOUR_GLO_CHL_L3_NRT_OBSERVATIONS_009_032-TDS”

**.protocol:** This is the Protocol. If .producttype has a value matching any from the list below, then .protocol can have a value of either "SUBS" or "FTP".

* OCEANCOLOUR_MED_CHL_L4_NRT_OBSERVATIONS_009_041-TDS

* OCEANCOLOUR_GLO_BGC_L4_MY_009_108-TDS

* SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012-TDS

* SST_MED_SST_L4_NRT_OBSERVATIONS_010_004-TDS

* GLOBAL_ANALYSIS_FORECAST_WAV_001_027-TDS

* OCEANCOLOUR_GLO_CHL_L3_NRT_OBSERVATIONS_009_032-TDS

If .product type is set to the value “INSITU_MED_NRT_OBSERVATIONS_013_035” then .protocol can have a value of “FTP”. 

**.dataset:** This is the Dataset. Its possible value is determined by these following conditions: 

* If the .producttype has a value of “OCEANCOLOUR_MED_CHL_L4_NRT_OBSERVATIONS_009_041-TDS” then .dataset can have a value of one of the following:
	
	* “dataset-oc-med-chl-multi-l4-chl_1km_monthly-rt-v02”
	 
	* “dataset-oc-med-chl-multi-l4-interp_1km_daily-rt-v02”

	* "dataset-oc-med-chl-olci_a-l4-chl_1km_monthly-rt-v02” or

	* “dataset-oc-med-chl-olci-l4-chl_300m_monthly-rt"

* If the .producttype has a value of “SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012-TDS” then .dataset can have a value of either:

    * “SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012_a” or

    * “SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012_b”

* If .producttype is set to “SST_MED_SST_L4_NRT_OBSERVATIONS_010_004-TDS” then .dataset can have a value of:

    * "SST_MED_SST_L4_NRT_OBSERVATIONS_010_004_a_V2"

    * "SST_MED_SST_L4_NRT_OBSERVATIONS_010_004_c_V2"

    * "SST_MED_SSTA_L4_NRT_OBSERVATIONS_010_004_b" or

    * "SST_MED_SSTA_L4_NRT_OBSERVATIONS_010_004_d"

* If the .producttype has a value of “GLOBAL_ANALYSIS_FORECAST_WAV_001_027-TDS” and .protocol is set to “SUBS” then .dataset can havea value of “global-analysis-forecast-wav-001-027”.

* If the .producttype is set to “GLOBAL_ANALYSIS_FORECAST_WAV_001_027-TDS” and .protocol is set to “FTP” then .dataset can have a value of:

    * “global-analysis-forecast-wav-001-027” or

    * “global-analysis-forecast-wav-001-027-statics”

* If the .producttype is “INSITU_MED_NRT_OBSERVATIONS_013_035” and the .protocol is “FTP” then the value of .dataset can be “med_multiparameter_nrt”.

* If .producttype is set to “OCEANCOLOUR_GLO_CHL_L3_NRT_OBSERVATIONS_009_032-TD” then the value of .datset can be “dataset-oc-glo-bio-multi-l3-chl_300m_daily-rt”.

**.variables:** This is the Variables. The possible values of .variabls is determined by the following conditions: 

* If .protocol is set to “SUBS” AND .dataset is set to one of the following, then the value of variables can be “CHL+CHL_count+CHL_error”, “CHL”, “CHL_count” or “CHL_error”.

* If .dataset is set to “dataset-oc-med-chl-multi-l4-interp_1km_daily-rt-v02” and .protocol is set to “SUBS” then the value of .variables can be “CHL”.

* If .dataset is set to “c3s_obs-oc_glo_bgc-plankton_my_l4-multi-4km_P1M” and .protocol is set to “SUBS” then the value of .variables can be “CHL+CHL_count+CHL_error+grid_mapping”, “CHL”, “CHL_count”, “CHL_error”, or “grid_mapping”.

* If .protocol is set to “SUBS” and .dataset is set either "SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012_a" or "SST_MED_SST_L3S_NRT_OBSERVATIONS_010_012_b" then variables can have a value of one of the following: 

	* “adjusted_sea_surface_temperature+quality_level+sea_surface_temperature+source_of_sst”, 
	
	* “adjusted_sea_surface_temperature|quality_level”
	
	* “sea_surface_temperature”, or 
	
	* “source_of_salt”

* If .protocol is set to "SUBS" and .dataset is set to either "SST_MED_SST_L4_NRT_OBSERVATIONS_010_004_a_V2" or "SST_MED_SST_L4_NRT_OBSERVATIONS_010_004_c_V2" then .variables can have a value of one of the following: 

	* “analysed_sst+analysis_error”
	
	* “analysed_sst”, or 
	
	* “analysis_error”:

* If .protocol is set to "SUBS" and .dataset is set to either "SST_MED_SSTA_L4_NRT_OBSERVATIONS_010_004_b" or "SST_MED_SSTA_L4_NRT_OBSERVATIONS_010_004_d" then .variables can have a value of "sst_anomaly". 

* If .dataset is set to “global-analysis-forecast-wav-001-027” and .protocol is set to “SUBS” then .varaibles can have a value of one of the following: 
	
	* VHM0+VHM0_SW1+VHM0_SW2+VHM0_WW+VMDR+VMDR_SW1+VMDR_SW2+VMDR_WW+
		VPED+VSDX+VSDY+VTM01_SW1+VTM01_SW2+VTM01_WW+VTM02+VTM10+VTPK
	* VHM0

	* VHM0_SW1

	* VHM0_SW2

	* VHM0_WW

	* VHDR

	* VMDR_SW1

	* VMDR_SW2

	* VMDR_WW

	* VPED

	* VSDX

	* VSDY

	* VTM01_SW1

	* VTM01_SW2

	* VTM01_WW

	* VTM02

	* VTM10

	* VTPK

* If .dataset has a value of “dataset-oc-glo-bio-multi-l3-chl_300m_daily-rt” and .protocol is set to “SUBS” then .variables can have a value of one of the following: 

	* HL+CHL_error+CHL_flags
	* CHL
	* CHL_error
	* CHL_flags

ECOSTRESS Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**.dataset:** This is the Dataset. It can have a value of one of the following:

* EEHCM

* EEHSEBS 

* EEHSTIC

* EEHSW

* EEHTES

* EEHTSEB

**.relativeorbitnumber:** This is the Orbit Range. It can have a value of an integer from 1 to 19999. 

**.parameteName:** This is the Parameter. It can have a value of either “Day” or “Night”. 

**.dayNightFlag:** This is the DayNightFlag. It can have a value of either “L1B_RAD” or “Night”.