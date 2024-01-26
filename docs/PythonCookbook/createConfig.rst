.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _SearchS1Images


Create a config.json file
=========================================
For developing with WASDI in python, you need to create a config.json file.

.. note::
    The configuration file contains your credentials and some additional information to get WASDI started: never share it with others! It is required only for developing on your PC, so do not upload it to WASDI when deploying or updating an application

Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account

If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.


Recipe
------------------------------------------

The basic config.json file must contain:

 - your username, which usually corresponds to the email you used
 - your password (only you are supposed to know this)
 - the workspace ID (suggested) or the workspace name you intend to work in. The workspace ID can be obtained looking at the URL in the editor, it's going to look like this: https://www.wasdi.net/#!/71805896-654b-468c-8fc5-5d2ad6ba61f3/editor -> in this case, `71805896-654b-468c-8fc5-5d2ad6ba61f3` is the string you are looking for
 - path to the parameters file. Again, this is another JSON file used just for development purposes. Assuming it is called `params.json`, and that you saved it in the top folder with you your myProcessor.py and config.json files, the value would be `"./params.json"`


.. code-block:: json

   {
      "USER": "yourusername@goes.here",
      "PASSWORD": "your secret password goes here",
      "WORKSPACEID": "71805896-654b-468c-8fc5-5d2ad6ba61f3",
      "PARAMETERSFILEPATH": "./params.json"
   }

.. note::
   Quick, wasn't it? Try, and `reach out if you need help <https://discord.gg/JYuNhPaZbE>`_.
