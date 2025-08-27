
WASDI : Web Advanced Space Developer Interface
==================================================

.. 
   Available Videos, to be embedded
   .. youtube:: 6LHIwdyh45U <-> Wasdi | Democratizing EO
   .. youtube:: NJpZyRh5Hgw <-> Hazard Demo
   .. youtube:: nA674SwSpxo <-> HASARD Teaser
   .. youtube:: Ot92qAhJkXs <-> EoExpert Demo
   .. youtube:: 0xNs9O_x9kE <-> EndUser Demo
  

   
WASDI implements a unique, simple and intuitive interface to foster the exploitation of EO data and satellite products, for satisfying requirements of users’ communities and, in particular:

 - experts/researchers in the field of Earth Sciences
 - managers of services and public administrations (i.e. civil protection decision makers)
 - private companies (i.e. insurance, agriculture)

WASDI allows researchers to gather satellite data, in particular the Sentinel ones, display them on-line, run algorithms, displaying and evaluating the results, and allows to share these projects among different users.

The results of the calculations will then be available for download, allowing local further processing, or published directly through the Web.

Getting Started with WASDI
---------------------------
WASDI web platform is the best starting point for your journey on Earth Observations (EO) resources !

This :doc:`basic tutorial </WasdiTutorial>`  will help to acquire the main concepts and use WASDI for your research.

If you're acquired the basic concepts of WASDI and you're interested in how processors can be launched,  take a look at
:doc:`this tutorial </SynchAsynch>`. This will highlights Synchronous and Asynchronous WASDI programming.

.. youtube:: 6LHIwdyh45U


.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Getting started

   GettingStarted/WasdiTutorial.rst
   GettingStarted/LibsConcepts.rst



WASDI User Manual
-------------------------------
WASDI has created a comprehensive user manual to explain and simplify all operations in WASDI. If you require explanation for any concepts in WASDI, please see the corresponding section in the manual. 
A good starting point to search for and executing applications is the tutorial on the Space Marketplace 

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Wasdi User Manual

   UserManual/SigningUpAndSigningIn.rst
   UserManual/UsingYourWorkspace.rst
   UserManual/SearchingForProducts.rst
   UserManual/SubscriptionsAndOrganizations.rst
   UserManual/Other.rst


  
  

WASDI Marketplace
---------------------------
All the WASDI Applications are available for end users' with a simple and intuitive Interface. Choose your App, set your input data with a few clicks and enjoy the result.

A good starting point to applications is the :doc:`App store overview </AppStoreTutorial>`

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Wasdi Applications

   WasdiApplications/AppStoreTutorial.rst
   WasdiApplications/FloodMapping.rst
   WasdiApplications/SARArchiveGenerator.rst
   WasdiApplications/FloodFrequencyMapGenerator.rst
   WasdiApplications/WheatLocator.rst



Add your App to WASDI
---------------------------
Unleash the real power of WASDI, developing and uploading your own downstream application to run it on EO images on the fly!
Wasdi supports several programming languages:

* **Python 3.x**
* **IDL 3.7.2**
* **Octave 6.x**
* **C#**
* **Javascript**


If you already know WASDI features and you are a Python developer check out the :doc:`python tutorial </PythonTutorial>`

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Programming tutorials

   ProgrammingTutorials/PythonTutorial.rst
   ProgrammingTutorials/JupyterNotebookTutorial.rst
   ProgrammingTutorials/PythonLandsatTutorial.rst
   ProgrammingTutorials/SearchImport.rst
   ProgrammingTutorials/OctaveTutorial.rst
   ProgrammingTutorials/ConfigTutorial.rst
   ProgrammingTutorials/LibWorkspaces.rst
   ProgrammingTutorials/SynchAsynch.rst
   ProgrammingTutorials/C#Tutorial.rst
   ProgrammingTutorials/SiteMap.rst
   ProgrammingTutorials/UITutorial.rst
   ProgrammingTutorials/JavascriptTutorialHtml.rst
   ProgrammingTutorials/JavascriptTutorial.rst
   ProgrammingTutorials/AppsGoodPractices.rst


Reference center
---------------------------

WASDI allows users and developer to interact though **libraries** and **APIs**. Find the reference of your language library on the left menu.


.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Libraries references

   Libraries/c#/WasdiLib.rst
   Libraries/java/WasdiLib.rst
   Libraries/matlab/matlab.rst
   Libraries/octave/octave.rst
   Libraries/python/waspy.rst
   Libraries/typescript/wasdi.rst
   
.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Python Cookbook

   PythonCookbook/createConfig.rst
   PythonCookbook/BasicAppStructure.rst
   PythonCookbook/ReadParameters.rst
   PythonCookbook/ImportSearchedImages.rst
   PythonCookbook/ImportAndPreprocess.rst
   PythonCookbook/SearchS1Images.rst
   PythonCookbook/SearchS2Images.rst
   PythonCookbook/SearchS3Images.rst
   PythonCookbook/SearchS5pImages.rst
   PythonCookbook/SearchCMImages.rst
   PythonCookbook/SearchEcostressProducts.rst
   PythonCookbook/SearchERA5Images.rst
   PythonCookbook/ImportGFSProducts.rst
   PythonCookbook/CallCDSAPIsFromWASDI.rst
   PythonCookbook/RunSnapWorkflow.rst
   PythonCookbook/RunApplication.rst
   PythonCookbook/SavePayload.rst
   PythonCookbook/FindS2Tiles.rst
   PythonCookbook/UseLibAsClient.rst
   PythonCookbook/ChangeHTTPRequestTimeouts.rst
   PythonCookbook/InstallCustomPackagesInNotebook.rst
   PythonCookbook/plottingPackages.rst
   PythonCookbook/importingShapefile.rst
   PythonCookbook/InstallGDAL.rst


.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Inside wasdi

   InsideWasdi/AddDataProvider.rst
   InsideWasdi/AddAppUIControl.rst
   InsideWasdi/AddProcessor.rst


Terms and Conditions
---------------------------

Please, before start using WASDI, check our terms and condition.

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Legal

   Legal/EULA.rst
   Legal/PrivacyPolicy.rst

