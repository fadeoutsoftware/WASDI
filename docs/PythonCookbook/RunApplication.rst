.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _RunApplication


Run Another WASDI Application
=========================================
This snippet shows how to run another WASDI Application from your code.


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
	We will use the hellowasdi app, but the same code can be used to call any WASDI Application that you have access to.

.. note::
	To call an App, you need to understand it. Look to the help section of the app or the Json Parameter sample in the store. Additionallyy, you can contact the developer (if they have provided a contact email address) to understand the inputs (and outputs) required.


.. code-block:: python

   # Read the name we want to pass to the hellowasdi
   sName = wasdi.getParameter("NAME", "my Test")

   # Create the dictionary with the parameters to pass to the application
   aoApplicationParameters = { "name": sName }

   # Run the application: Applications are ALWAYS executed in asynchronous way
   sProcessId = wasdi.executeProcessor("hellowasdiworld", aoApplicationParameters)

   # Here you are free to do what you want
   wasdi.wasdiLog("I started an app and I can do what I want")

   # Call this if you need to wait for it to finish
   wasdi.waitProcess(sProcessId)

   wasdi.wasdiLog("Here I know the application is finished")

What it does:

 - Reads Input Parameters
 - Creates the dictionary with the params to pass in input to our application
 - Runs the application
 - Waits for the application to finish
