Install specific packages in Jupyter Notebooks
=========================================
You may need to work with specific packages in your Jupyter notebooks. Or maybe, you need specific versions of commonly used packages. No worries, we've got you covered!

Prerequisites
------------------------------------------

To run this code you need:
 - A valid WASDI Account
 - A workspace
 - a Jupyter Notebook running in that workspace
 
If this is not clear, you probably need to take a look to the `Jupyter Notebook Tutorial <https://wasdi.readthedocs.io/en/latest/ProgrammingTutorials/JupyterNotebookTutorial.html>`_ before.

Recipe 
------------------------------------------
To install packages, you can use the usual :code:`pip install` string (it is no longer required to precede that with an exclamation mark). Here's an example for you:

.. code-block::

    pip install matplotlib -U

If you need to install specific versions, just write them after the :code:`==`, as in this example:

.. code-block::

    pip install matplotlib==3.9.2 numpy==1.23.5 -U

**What it does:**

pip is called to install the packages you specify. You may need to restart the kernel to use the updated versions. You (and your collaborators whom you shared the workspace with) won't need to reinstall the packages every time you reopen the notebook.
