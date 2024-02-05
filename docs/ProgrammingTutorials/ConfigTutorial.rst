Wasdi config file
====================================================
The present Tutorial showcase all the feature available in the *WASDI configuration file*.
The config file is required to connect the running instance of your code with WASDI services.
This tutorial showcase all the available features with working example, explaining all the available features.
As a reference library waspy python library is used but the same concepts applies for all wasdi libraries.
Please note : 

.. note::
    The configuration file contains your credentials and some additional information to get WASDI started: never share it with others! It is required only for developing on your PC, so do not upload it to WASDI when deploying or updating an application


Basic parameters 
====================================================
The first step required is to login in the WASDI web services. 
To do this we can create `config.json` file.
Please remember that the config file follows the *JSON* syntax specification. 
.. code-block:: json

   {
      "USER": "[]",
      "PASSWORD": "[]",
   }

Upon initialization the waspy library will 