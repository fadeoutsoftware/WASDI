.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _UseLibAsClient


Install GDAL in Windows
=========================================
This recipe shows how to install gdal in your Windows Environment


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - Know if your computer is 32 bit or 64 bit


Recipe 
------------------------------------------
Often GDAL is a very important library to manipulate satellite data. 

.. note::
	You can find the architecture of your PC going in: Settings -> System -> About

To install GDAL:

 - Open the web page https://github.com/cgohlke/geospatial-wheels/releases
 - If needed, click on the small "Show all XXX assets" link to expand the list
 - Identify your GDAL wheel

.. note::
	| A sample is: GDAL-3.9.2-cp312-cp312-win_amd64.whl
	| GDAL-3.9.2: means gdal version 3.9.2
	| cp312-cp312: means build for python 3.12
	| win_amd64: means a 64 bit windows
	
	| You can find the version of gdal you need for your python version.
	| Both Intel and AMD Processors are in the amd64 architecture
	

 - Download GDAL wheel, in this tutorial we will assume GDAL-3.9.2-cp312-cp312-win_amd64.whl
 - Open your python environment 
 - Install the wheel:

.. code-block:: python

   pip install "C:\Users\YOUR_USER\Downloads\GDAL-3.9.2-cp312-cp312-win_amd64.whl"

Now, you should be able to import gdal in your code.

.. code-block:: python

   from osgeo import gdal

.. note::
	| You may have still a problem when running a python script with gdal.
	| If you see this error:
	| ImportError: DLL load failed while importing _gdal: The specified module could not be found.
	| This probably means that you are missing the VC - redist libraries.
	| These can be found here:
	| https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170#visual-studio-2015-2017-2019-and-2022
	|
	| This is the direct link to the version for x64 Machines:
	| https://aka.ms/vs/17/release/vc_redist.x64.exe
