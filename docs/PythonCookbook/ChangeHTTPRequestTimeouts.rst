Change HTTP request timeouts
=========================================
The following code shows how to set a custom HTTP request timeout to the wasdi lib.
Specifically, the new value will affect the HTTP connection timeout and the response timeout. 
The default value is set to 120 seconds.

Prerequisites
------------------------------------------

To run this code you need:
 - A running Python 3.x Environment
 - A valid WASDI Account
 - A `valid Config file <https://wasdi.readthedocs.io/en/latest/PythonCookbook/createConfig.html>`_
 
If this is not clear, you probably need to take a look to the `Python Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/PythonTutorial.html>`_ before.

Recipe 
------------------------------------------
You can change the default value of the HTTP request timeout by calling the method `setRequestsTimeout`. That method takes
an integer as parameter to represent the timeout value, expressed in seconds. 

The following code snippet provides an example of how to set the HTTP request timeout.

.. code-block:: python

    wasdi.wasdiLog(f"Default timeout value: {wasdi.geRequestsTimeout()}")

    iNewTimeOut = wasdi.getParameter("REQUEST_TIMEOUT", 240)

    if iNewTimeOut is not None:
        wasdi.setRequestsTimeout(iNewTimeOut)

    wasdi.wasdiLog(f"New timeout value: {wasdi.geRequestsTimeout()}")
           

**What it does:**

The code first prints the current HTTP request timeout value, got using the method `getRequestsTimeout`.

Then, it tries to read the new timeout value from the parameters file, looking for a field "REQUEST_TIMEOUT". 
If such a parameter is not present, then it takes 240 as fallback value (for an overview on how to use the parameters file, have a look at `this recipe <https://wasdi.readthedocs.io/en/latest/PythonCookbook/ReadParameters.html>`_).

The new value is passed as a parameter to the `setRequestsTimeoutValue` and the value of the request timeout is printed again, to verify that it changed accordingly. 