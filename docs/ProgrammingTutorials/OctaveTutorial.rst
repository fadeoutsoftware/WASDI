.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _OctaveTutorial:

Octave Tutorial
===========================

This tutorial showcase the capabilities of WASDI Octave library.
For more information about Octave and Matlab compatibility, please check the following  `URL <https://octave.org/#:~:text=The%20Octave%20syntax%20is%20largely,operations%20on%20vectors%20and%20matrices.>`_

In general WASDI, by leveraging libraries, allows developers to upload and run applications directly in the cloud, using 
powerful cloud resources and data providers enabling intensive model execution.

The tutorial is divided upon two main parts : 

- Local setup 
- Octave package management
- WASDI deployment and execution 

During the first part of this tutorial the execution of the code will be done on a local environment. 
For the sake of simplicity few operations will be included keeping the actual operational code as small as possible.
Next a brief introduction to the Octave package management in WASDI is presented.

The last phase will be to adapt the code to be deployed in a new WASDI application, to be executed in the cloud.

Local Setup
---------------------------
This first chapter covers the preparation of the folder in order to execute the code as it will be executed by WASDI as an application.

Remember the WASDI motto "Develop at home, deploy to the cloud" ? 
That's pretty much the spirit, but little setup is required.
The setup of this environment is required to align with the effective execution setup when the application will be deployed in WASDI.

Download the following `zip archive <https://raw.githubusercontent.com/fadeoutsoftware/WASDI/master/libraries/matlabwasdilib/matlabwasdilib.zip>`_. 

The archive contains the following assets: 
- Matlab/Octave library functions (a set of various .m files)
- Java library to interact with WASDI itself

Create a new folder with the application name, in this case we will use **OctaveTutorialExample**: extract the content of the archive just downloaded inside the folder.

.. image:: ../_static/octave_tutorial_images/octave_tutorial_1.png

Add configuration 
^^^^^^^^^^^^^^^^^
Wasdi application requires an initialization in order to connect to the service.
This initialization is done by leveraging a JSON file named **config.json**.

Please proceed to create a file with the following content, replacing fields value with your data: 

.. code-block:: JSON

 {
    "USER": "The e-mail used to register in WASDI",
    "PASSWORD": "Your password",
    "PARAMETERSFILEPATH": "./parameters.json",
    "WORKSPACE" :"The name of the workspace you're working"
 }

Create application entry point 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
We can now create the main entry point for the application: by convention **the entrypoint must have the same name as the application !**.
So, if we call our application **octavetutorialexample**, we need to add a file called **octavetutorialexample.m**.
NOTE: In order to avoid problems related to encoding all Octave applications names are converted to lowercase: please follow this convention also in the naming of the main 
entrypoint file.

Create a new file name it "octavetutorialexample.m".
This will represent the main entry point of the application: there is still the possibility to manage separated files defining separated functions.
Add to the file the following content: 

.. code-block:: Matlab

   function octavetutorialexample(Wasdi)
      
      wLog(Wasdi,"Octave app");

   end

For the moment nothing too complicated, just a single log leveraging the Wasdi library: the first objective of this tutorial is to 
showcase how applications are managed on WASDI. 

Zip the "octavetutorialexample.m" file, open wasdi and add a new application. 
Fill the form fields and upload the zip file. 

.. image:: ../_static/octave_tutorial_images/octave_tutorial_2.png

Proceed with the execution of the application: open the app, and click on "Run".
Looking at the logs we'll see our log from the application.

.. image:: ../_static/octave_tutorial_images/octave_tutorial_3.png

