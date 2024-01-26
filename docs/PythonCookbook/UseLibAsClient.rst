.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _UseLibAsClient


Use Library as client
=========================================
This snippet shows how to use the WASDI Lib as client, to run applications and get results.


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A valid Config file
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.


Recipe 
------------------------------------------
We are going to init the WASDI Lib, call an application and get back the result

.. note::
	Often WASDI is used to deploy application, but in the same lib can be used also as a Client of WASDI.


.. note::
	In this sample we use hellowasdiworld application that DOES NOT produce any file, so this snippet as it is will not dowload locally any file. In the real life you will probably use an app that will add some file in the workspace.


.. code-block:: python

   # Initialize the lib: you must set the right path to your config file
   wasdi.init('myconfig.json')

   # Create a workspace where we will run our application
   wasdi.createWorkspace('NAME')

   # Create the dictionary with the params to pass to the application
   aoParams = {}
   aoParams['NAME'] = “Test”

   # Run the application: Applications are ALWAYS executed in asynchronous way
   sProcessId = wasdi.executeProcessor("hellowasdiworld", aoParams)

   # Here you are free to do what you want
   wasdi.wasdiLog("I started an app and I can do what I want")

   # Call this when you need to wait for it to finish
   wasdi.waitProcess(sProcessId)

   # Get the List of Files in Workspace
   asFilesProduced = wasdi.getFileByActiveWorkspace()

   # For all the files produced
   for sFile in asFilesProduced
      # Since you are running out of wasdi, this will take the produced files locally for you
      wasdi.getPath(file)

What it does:

 - Initializes the lib
 - Creates a workspace
 - Creates the parameters for the app
 - Starts the app
 - Waits for it
 - Get the files in the workspace
 - Get a local copy of all the produced files
