.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _PythonTutorial:

Javascript Web Tutorial
===========================

In this tutorial we will show you how you can start using to use the Javascript library
for WASDI. In this tutorial we will create a web page that show data gathered through
the library just by using one <script> tags.

This guide will not require to know any information about WASDI because
a brief explanation of the core concepts involved will be reported at each step.

To keep the requirements of this tutorial as small and easy as possible all examples will be using 
browser based DOM manipulation: no Javascript frameworks will be used and the produced code will (should) 
be compatible with any browser. (If it's not it's indeed time for an update :-) )

If you are an Angular developer, please refer to the Angular dedicated tutorial.

Setup & tools
---------------------------

For this tutorial there are no specific tools required.

In general, to write an Html document you can use:

    * Microsoft Visual Studio Code (or any HTML compatible IDE)
    * -OR-
    * A text editor (Notepad, for instance)

The images in the following will show the Visual Studio Code option.

Create index.html file with the following content:

.. code-block:: html

    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Js Wasdi Tutorial</title>

    </head>
    <body></body>
    </html>


.. image:: img/1.png

Include the library
---------------------
The library is served through npm so it is also automatically available through
its related CDN at cnd.jsdelivr.net.
The current version for Wasdi Javascript library is 0.0.16.

Please check package page on npmjs.org for the latest updates.

Link -> `Wasdi - JavaScript library <https://www.npmjs.com/package/wasdi>`_

Please edit index.html and add the import of the library a the end of the head section

.. code-block:: html

    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Js Wasdi Tutorial</title>

    <script src="https://cdn.jsdelivr.net/npm/wasdi@0.0.16/build/wasdi-javascript.js"></script>

    </head>
    <body></body>
    </html>


Now,to start using the functionalities exposed by the library, create a new file next to index.html
and name it **main.js**.

Include the file index.html :

.. code-block:: html

    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Js Wasdi Tutorial</title>
    <!-- This script loads the library -->
    <script src="https://cdn.jsdelivr.net/npm/wasdi@0.0.16/build/wasdi-javascript.js"></script>
    <!-- This script contains your custom code -->
    <script src="main.js"></script>

    </head>
    <body></body>
    </html>


Login
---------------------------

WASDI is a web application that allows users to download, process and obtains result from satellite imagery.

To continue with this tutorial you will need a valid account on the platform: 
please, proceed to register to WASDI services and keep note of your credential.

The first step to start interacting with `WASDI <https://www.wasdi.net>`_ services is to login by using the library facilities.

To achieve this you must add 2 files next the index.html file :
- config.json
- parameters.json

The second will be introduced later on the tutorial, when we will start using processors.
Create file config.json next to index.html file.

Add the following content, changing **[YOUR_USERNAME]** and **[YOUR_PASSWORD]** with your WASDI credentials

.. code-block:: json

    {
      "USER": "[YOUR_USERNAME]",
      "PASSWORD": "[YOUR_PASSWORD]",
      "WORKSPACE": "",
      "PARAMETERSFILEPATH": "./parameters.json",
      "WORKSPACEID": "test",
      "BASEPATH":"test",
      "DOWNLOADACTIVE":"test",
      "UPLOADACTIVE": "test",
      "VERBOSE":"test",
      "BASEURL" : "https://www.wasdi.net/wasdiwebserver/rest",
      "REQUESTTIMEOUT":120
    }

Notte that this file name is a **conventional one**. Please check library documentation for more details about the 
**loadconfig()** function.

Please open main.js and start editing the file.
Wasdi librariy is exposed as a global singleton, a common practise for Javascript library. 

The variable to be used to access library methods is "**wasdi**"
Add the following lines:

.. code-block:: javascript
    // load the configuration from config.json file  
    wasdi.loadConfig();
    // login to Wasdi
    wasdi.login();


After the successful login call, the wasdi global object will keep its state, 
allowing to make further request to the system.

Create and list user's Workspaces
-----------------------

A **Workspaces** is a basic concept of WASDI: one of the main objective of the platform is to connect 
to various satellite imagery portals and download files from such services. 
The workspace is composed by a collection of images downloaded, called **products**. 

The download doesn't require local storage because it "happens" in dedicated cluod instances.
Also, a workspace, holds the informations about the elaborations on such data, done by the **processors**.
Each users can create his own workspace, but he can also share them with other users.

In the following steps we will add some controls to HTML and some code to our main.js
file to create a Workspace on WASDI.

First edit the index.html file by adding there lines, inside the body tags :

.. code-block:: html
    // load the configuration from config.json file
    wasdi.loadConfig();
    // login to Wasdi
    wasdi.login();




