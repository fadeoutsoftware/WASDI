.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _BasicAppStructure


Read Parameters
=========================================
The following code is the snippet to read parameters in WASDI.


Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A valid Config file
 - A valid params.json file
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.


Recipe 
------------------------------------------

.. note::
	Assume we have a params.json file (configured in config.json)

This is our sample params.json file.

.. code-block:: json
    {
        "STRING_PARAM": "SAMPLE",
        "INT_PARAM": 2,
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
        "DATE": "2024-01-01"
    }


This is the code used to read Parameters.

.. code-block:: python

   # Read the String Parameter
   StringParameter = wasdi.getParameter("STRING_PARAM")
   # Read the String Parameter, with a Default value if the param is missing in the params.json file
   StringParameterWithDefault = wasdi.getParameter("STRING_PARAM", "My Default")
   # Read the Area of Interest
   oBbox = wasdi.getParameter("BBOX", None)
   # Read the integer value without any default
   IntegerValue = wasdi.getParameter("INT_PARAM")
   # Read the string-formatted Date
   DateString = wasdi.getParameter("DATE")

   #This method return a Key-Value Dictionary with all your parameters
   AllParametersDictionary = wasdi.getParametersDict()


What it does:

 - read different parameters 
 - read the full parameters dictionary at once

.. note::
	The developer can decide whatever is needed in the params.json file. If you will use the `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ your parameters will be generated automatically by WASDI.

.. note::
	With the  `WASDI User Interface <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html>`_ you can use the `renderAsStrings <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/UITutorial.html#render-as-string>`_ flag to ask WASDI to get all your parameters in String Format. In this case you will be responsable to convert your data in your code

.. note::
	The Boundig Box Format Here Used is the one used by the User Interface when renderAsStrings is missing or false. The Boundig Box fromat when renderAsStrings: true is **"NORTH,WEST,SOUTH,EAST"**

.. note::
	The Date is formatted by the User Interface as "YYYY-MM-DD"
