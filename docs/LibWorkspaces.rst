.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _LibWorkspaces



Working with Workspaces and Products
=========================================
This tutorial has to goal to intruduce the main functionality of the WASDI Libraries to work with workspaces (see :doc:`Wasdi Libraries Concepts </LibsConcepts>` for the main concepts).


Introduction
------------------------------------------
Workspaces are the space where you can import files, run applications, run workflows, run your code, create and add your own files.
Each workspace has a Name, an Id (guid) and a owner.
Each workspace is hosted on a WASDI Computing node: this can be a shared node for generic users or a dedicated Computing node for premium users.
Each workspace can be shared with other WASDI Users.
Developers can access their own workspaces or the workspaces other users shared with them.

Workspace functionalities
------------------------------------------
Using the libraries, a developer can:
*Get the id of the active workspace*
*Get the name of a workpace from the Id or viceversa*
*Open another workspace*
*Get the list of user workspaces*

Workspaces Sample Code
------------------------------------------

The following python app make some sample of what you can do with workspaces:

.. code-block:: python

	import wasdi

	def run():

		# Get The Active Workspace Id
		sWorkpaceId = wasdi.getActiveWorkspaceId()
		wasdi.wasdiLog("WorkspaceId is: " + sWorkpaceId)

		# Get the name of a workspace from the id
		sWorkspaceName = wasdi.getWorkspaceNameById(sWorkpaceId)
		wasdi.wasdiLog("WorkspaceName is: " + sWorkspaceName)

		# Get all the user workspaces
		aoWorkspaces = wasdi.getWorkspaces()
		wasdi.wasdiLog("User has " + str(len(aoWorkspaces)) + " Workspaces")

		# get the last workspace (we have at least one, this one!)
		oLastWorkspace = aoWorkspaces[len(aoWorkspaces)-1]
		wasdi.wasdiLog("Last Workspace is: " + oLastWorkspace["workspaceName"])

		# Open the last workspace
		sNewWorkspaceId = wasdi.openWorkspace(oLastWorkspace["workspaceName"])
		if sNewWorkspaceId == "":
			wasdi.wasdiLog("Error opening the last workspace")
		else:
			wasdi.wasdiLog("Now active workspace is " + wasdi.getActiveWorkspaceId())

		# Re open the original workspace
		wasdi.openWorkspaceById(sWorkpaceId)
		wasdi.wasdiLog("Re Opened the original workspace " + wasdi.getWorkspaceNameById(wasdi.getActiveWorkspaceId()))


	if __name__ == '__main__':
		wasdi.init('./config.json')
		run()

At the beginning we read the Id of the active workspace (getActiveWorkspaceId). This is defined in the config file and is the workspace where your code is running. 
Then we get the name of this workspace (getWorkspaceNameById).
We want next to get the list of the users' workspaces (getWorkspaces): this method returns a list of dictionaries: each object has these properties

.. code-block:: java

	"ownerUserId":STRING,
	"sharedUsers":[STRING],
	"workspaceId":STRING,
	"workspaceName":STRING

Next step is to open another workspace (openWorkspace): this method returns the workspaceId if ok, an empty string in case of error.
Finally, we come back to our original workspace using the id we collected before (openWorkspaceById) and verify using its name (getWorkspaceNameById).

The output will be something similar to this:
.. code-block::

	WorkspaceId is: a5dc8f79-3e89-46b5-8d39-169e9ecb0a98
	WorkspaceName is: TutorialWorkspace
	User has 108 Workspaces
	Last Workspace is: S3_Day_ActiveFire
	Now active workspace is ab34e55b-d233-466b-983e-223b42915869
	Re Opened the original workspace TutorialWorkspace

Products functionalities
------------------------------------------
The functionalities to work with products are:

*get the list of products in a workspace*
*check if a product is in the workspace or not*
*get the local path of the product*
*add a new product to the workspace*


Products Sample Code
------------------------------------------

The following python app make some sample of what you can do with products.
To make it run, you should create a workspace and put there at least one file using the WASDI Search web user interface or the upload.
Please note that this code can take some time to be executed the first time you run it beacuse it shows how to access file locally (so download) and to upload results in WASDI.

The goal of this tutorial is not to manipulate files so, the "new" file, is created just making a copy of an existing one with a different name.

.. code-block:: python

	import wasdi
	import os
	from shutil import copyfile

	def run():

		# Get the list of file names
		aoProducts = wasdi.getProductsByActiveWorkspace()
		wasdi.wasdiLog("In the workspace we have " + str(len(aoProducts)))

		# Make sure we have at least one
		if len(aoProducts)>0:
			# Double check
			bCheck = wasdi.fileExistsOnWasdi(aoProducts[0])
			wasdi.wasdiLog("Product " + aoProducts[0] + " is on workspace? " + str(bCheck))
			
			# This line will return the local path: it assume you need it to open the image, so the first time will automatically download the image
			sLocalPath = wasdi.getPath(aoProducts[0])

			# Generate the name of a new file, not existing yet: start taking the original file without extension
			sCopyLocalPath = os.path.splitext(sLocalPath)[0]
			# add _copy and re-put extension
			sCopyLocalPath = sCopyLocalPath + "_copy" + os.path.splitext(sLocalPath)[1]
			# Make a local copy, as it was another file
			copyfile(sLocalPath, sCopyLocalPath)

			# Get only the file name
			sCopiedFileName = os.path.basename(sCopyLocalPath)
			wasdi.wasdiLog("We 'created' a second new file: " + sCopiedFileName)
			# Add the file to wasdi: this will upload the new file to the cloud
			wasdi.addFileToWASDI(sCopiedFileName)

		wasdi.wasdiLog("Tutorial Done!")


	if __name__ == '__main__':
		wasdi.init('./config.json')
		run()

The code starts taking a list of the products in the workspace (getProductsByActiveWorkspace). Just to show the functionality, it then checks if the first file is really available on WASDI (fileExistsOnWasdi).
The next step is to simulate a local file access: to open a file, you need a full local path: this must be requested to WASDI (getPath).
The same function can be used also to obtain a path to use to save your own file: our code just makes a copy of a file in a workspace with another name, using again getPath to have to path to use to save the file. 
This copy is a new file for WASDI: to add it to the workspace use addFileToWASDI: please note that add file to WASDI takes as input only the file name and not the full path.
