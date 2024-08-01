.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _SavePayload


Save Payload
=========================================
This snippet demonstrates how to save a payload as additional output of your application. WASDI apps are mainly meant to create new products that will be added to the workspace. But often you may want to save also other information: the payload is the solution.
Payloads are just a string you can save. Usually, the payload is in JSON format. The user can view the payload in the WASDI Editor.


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
	We will save a JSON Payload. 
	**The payload is really saved only when the app is running in WASDI. When running locally the payload is not really saved.**

.. note::
	It is not mandatory, but is good practice in the payload to also save the inputs received.

.. code-block:: python

   # Read the input parameters
   aoInputParameters = wasdi.getParametersDict()

   # Declare the payload
   aoPayload = {}

   # Add the inputs as a member of the payload
   aoPayload["inputs"] = aoInputParameters

   # Do your own code here...

   # Here we add some sample values
   aoPayload["item_found"] = 3
   aoPayload["max_value"] = 1893
   aoPayload["selected_color"] = "red"

   # Save the payload
   wasdi.setPayload(aoPayload)

What it does:

 - Reads Input Parameters
 - Adds some elements to the payload
 - Saves the payload
