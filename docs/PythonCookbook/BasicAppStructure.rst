.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _BasicAppStructure


Python Application Skeleton 
=========================================
The following code is the basic structure of a Python Application.


Requirement
------------------------------------------
To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A valid Config file
 
If this is not clear, probably you need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>` before.


Recipe 
------------------------------------------

.. code-block:: python

   import wasdi   
   
   def run():
       wasdi.wasdiLog("Here I can start to code")
   
   
   if __name__ == '__main__':
       wasdi.init("./config.json")
       run()


This is the basic structure of a WASDI Application. 

We just need to:

 - import the library
 - handle **__main__** in the file
 - define a **run()** method
 - initialize the lib
 - call the **run()** method

.. note::
	The main file **MUST** be called myProcessor.py. You can add then all the libraries, files and module you may want to code or include
	

.. note::
	This structure is mandatory if you plan to deploy your application in WASDI. To use the library only as a client, this is not necessary.
