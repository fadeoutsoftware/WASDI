# WASDI Python Library

This is a **preliminary version** of the Python Library you can use to access the [WASDI](http://www.wasdi.net) platform functionalities from your Python code.

Visit us at [http://www.wasdi.net](http://www.wasdi.net)

The source code can be found [here](https://github.com/fadeoutsoftware/WASDI/tree/develop/libraries/waspy)

----


## Python tutorial

WASPY is the **WAS**DI **Py**thon Library. 

### Prerequisites:

- a [WASDI](http://www.wasdi.net) registered user (with a username/password, google users are not supported yet)
- at least one workspace
- some EO products in your workspace

### Installation

To start working with WASPY, just install the library using:

```bash
pip install wasdi
```

To quickly check if the installation worked correctly, try running the following code:


```python
import wasdi
print(wasdi.hello())
```

You should see this kind of output:


```json
{"boolValue":null,"doubleValue":null,"intValue":null,"stringValue":"Hello Wasdi!!"}
```


### Configuration

Create a `config.json` file. It is a standard json file, which is used to store the credentials of the user and some other settings. The syntax is:

```json
“VARIABLE_NAME”: value
```

Hint: exploit an editor which can check the syntax (there are many which can be accessed online for free)

The minimal configuration to begin working with WASPY is:

```json
{
  "USER": "yourUser@wasdi.net",
  "PASSWORD": "yourPasswordHere",
  "WORKSPACE": "nameOfTheWorkspaceYouWantToUse"
}
```


For the other available parameters please refer to the Documentation.


### Start WASPY

To start WASPY and check if everything is working, run the following code:


```python
wasdi.init('./config.json')
```

(Adapt the path if the file is not located in your working directory)

The Lib will read the configuration file, load the user and password, log the user in, and then open the workspace specified in the configuration file. To check if everything is working, try to get the list of workspaces available for the user:


```python
wasdi.getWorkspaces()
```

You should be able to see a result similar to the following one:

```python
[{u'ownerUserId': u'yourUser@wasdi.net',
  u'sharedUsers': [],
  u'workspaceId': u'23ab54f3-b453-2b3e-284a-b6a4243f0f2c',
  u'workspaceName': u'nameOfTheWorkspaceYouWantToUse'},
 {u'ownerUserId': u'yourUser@wasdi.net',
  u'sharedUsers': [],
  u'workspaceId': u'103fbf01-2e68-22d3-bd45-2cf95665dac2',
  u'workspaceName': u'theNameOfAnotherWorkspace'}]
```

The configured Workspace is already opened.  The use can open another workspace using:

```python
wasdi.openWorkspace('theNameOfAnotherWorkspace')
```

and the lib replies showing the workspace unique id:

```python
u'9ce787d4-1d59-4146-8df7-3fc9516d4eb3'
```

To get the list of the products available in the workspace, call

```python
wasdi.getProductsByWorkspace('nameOfTheWorkspaceYouWantToUse')
```

and the lib returns a list of the products in the given workspace:

```python
[u'S1A_IW_GRDH_1SDV_20190517T053543_20190517T053608_027263_0312F1_F071.zip',
u'S1B_IW_RAW__0SDV_20190506T052631_20190506T052703_016119_01E53A_D2AD.zip', u'S1A_IW_GRDH_1SDV_20190517T053608_20190517T053633_027263_0312F1_3382.zip']
```

Now try something more, let's search for some Sentinel 1 images. Let's assume we are interested in images taken from "2018-09-01" to "2018-09-02". Also, we'd better specify a bounding box. Assume we're interested in images with *latitude* in `[43, 44]` and *longitude* in `[11, 12]`. In other words, The corresponding code would be:

```python
wasdi.wasdiLog('Let\'s search some images')
aoImages = wasdi.searchEOImages("S1", "2018-09-01", "2018-09-02", 44, 11, 43, 12, None, None, None, None)
wasdi.wasdiLog('Found ' + str(len(aoImages)))
```

The output should be similar to this:

```
 Let's search some images
[INFO] waspy.searchEOImages: search results:
[{'footprint': 'POLYGON ((8.8724 45.3272, 8.4505 43.3746, 11.4656 43.0981, 11.9901 45.0472, 8.8724 45.3272, 8.8724 45.3272))', 'id': 'cba6c104-3006-4af7-a2d1-cbd55f58b939', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(cba6c104-3006-4af7-a2d1-cbd55f58b939)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_RAW__0SDV_20180902T052727_20180902T052759_023515_028F75_7325.zip', 'size': '1.54 GB', 'pseudopath': 'RADAR/LEVEL-0/2018/09/02, S1/1A/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, S1/1A/LEVEL-0/IW_RAW__0S/2018/09/02, S1/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, S1/LEVEL-0/IW_RAW__0S/2018/09/02, 2014-016A/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, 2014-016A/LEVEL-0/IW_RAW__0S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(cba6c104-3006-4af7-a2d1-cbd55f58b939)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T05:12:37.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T05:12:37.000Z, Instrument: null, Mode: null, Satellite: null, Size: 1.54 GB', 'title': 'S1A_IW_RAW__0SDV_20180902T052727_20180902T052759_023515_028F75_7325'}, {'footprint': 'POLYGON ((8.546 43.8206, 8.1314 41.8674, 11.0739 41.5931, 11.5836 43.5434, 8.546 43.8206, 8.546 43.8206))', 'id': 'ab09cbe3-757d-4ad1-ad2c-9ee3dc0da82e', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(ab09cbe3-757d-4ad1-ad2c-9ee3dc0da82e)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_RAW__0SDV_20180902T052752_20180902T052824_023515_028F75_C0E0.zip', 'size': '1.49 GB', 'pseudopath': 'RADAR/LEVEL-0/2018/09/02, S1/1A/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, S1/1A/LEVEL-0/IW_RAW__0S/2018/09/02, S1/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, S1/LEVEL-0/IW_RAW__0S/2018/09/02, 2014-016A/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, 2014-016A/LEVEL-0/IW_RAW__0S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(ab09cbe3-757d-4ad1-ad2c-9ee3dc0da82e)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T05:13:22.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T05:13:22.000Z, Instrument: null, Mode: null, Satellite: null, Size: 1.49 GB', 'title': 'S1A_IW_RAW__0SDV_20180902T052752_20180902T052824_023515_028F75_C0E0'}, {'footprint': 'POLYGON ((11.472471 43.479721, 8.245583 43.886421, 8.576793 45.384708, 11.887283 44.978157, 11.472471 43.479721))', 'id': 'b478c3f0-93e9-4289-9b4c-6d2829a934a6', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(b478c3f0-93e9-4289-9b4c-6d2829a934a6)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_OCN__2SDV_20180902T052731_20180902T052756_023515_028F75_BE31.zip', 'size': '6.42 MB', 'pseudopath': 'RADAR/LEVEL-2/2018/09/02, S1/1A/SAR-C/LEVEL-2/IW_OCN__2S/2018/09/02, S1/1A/LEVEL-2/IW_OCN__2S/2018/09/02, S1/SAR-C/LEVEL-2/IW_OCN__2S/2018/09/02, S1/LEVEL-2/IW_OCN__2S/2018/09/02, 2014-016A/SAR-C/LEVEL-2/IW_OCN__2S/2018/09/02, 2014-016A/LEVEL-2/IW_OCN__2S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(b478c3f0-93e9-4289-9b4c-6d2829a934a6)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T07:28:24.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T07:28:24.000Z, Instrument: null, Mode: null, Satellite: null, Size: 6.42 MB', 'title': 'S1A_IW_OCN__2SDV_20180902T052731_20180902T052756_023515_028F75_BE31'}, {'footprint': 'POLYGON ((11.07582 41.979179, 7.92763 42.386463, 8.246798 43.886196, 11.472451 43.47963, 11.07582 41.979179))', 'id': '1012e123-291b-495a-b5e4-28312943dcba', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(1012e123-291b-495a-b5e4-28312943dcba)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_OCN__2SDV_20180902T052756_20180902T052821_023515_028F75_25A7.zip', 'size': '6.59 MB', 'pseudopath': 'RADAR/LEVEL-2/2018/09/02, S1/1A/SAR-C/LEVEL-2/IW_OCN__2S/2018/09/02, S1/1A/LEVEL-2/IW_OCN__2S/2018/09/02, S1/SAR-C/LEVEL-2/IW_OCN__2S/2018/09/02, S1/LEVEL-2/IW_OCN__2S/2018/09/02, 2014-016A/SAR-C/LEVEL-2/IW_OCN__2S/2018/09/02, 2014-016A/LEVEL-2/IW_OCN__2S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(1012e123-291b-495a-b5e4-28312943dcba)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T07:28:35.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T07:28:35.000Z, Instrument: null, Mode: null, Satellite: null, Size: 6.59 MB', 'title': 'S1A_IW_OCN__2SDV_20180902T052756_20180902T052821_023515_028F75_25A7'}, {'footprint': 'POLYGON ((11.472471 43.479721, 8.245705 43.88641, 8.576919 45.384693, 11.887283 44.978157, 11.472471 43.479721))', 'id': 'ce559c33-e249-447c-87db-60276e8de115', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(ce559c33-e249-447c-87db-60276e8de115)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_GRDH_1SDV_20180902T052731_20180902T052756_023515_028F75_AA37.zip', 'size': '968.87 MB', 'pseudopath': 'RADAR/LEVEL-1/2018/09/02, S1/1A/SAR-C/LEVEL-1/IW_GRDH_1S/2018/09/02, S1/1A/LEVEL-1/IW_GRDH_1S/2018/09/02, S1/SAR-C/LEVEL-1/IW_GRDH_1S/2018/09/02, S1/LEVEL-1/IW_GRDH_1S/2018/09/02, 2014-016A/SAR-C/LEVEL-1/IW_GRDH_1S/2018/09/02, 2014-016A/LEVEL-1/IW_GRDH_1S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(ce559c33-e249-447c-87db-60276e8de115)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T07:30:27.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T07:30:27.000Z, Instrument: null, Mode: null, Satellite: null, Size: 968.87 MB', 'title': 'S1A_IW_GRDH_1SDV_20180902T052731_20180902T052756_023515_028F75_AA37'}, {'footprint': 'POLYGON ((11.075819 41.979179, 7.92775 42.386448, 8.246921 43.886181, 11.472451 43.47963, 11.075819 41.979179))', 'id': 'f395a594-ba58-4fc5-b8a4-82a7a7e95d0a', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(f395a594-ba58-4fc5-b8a4-82a7a7e95d0a)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_GRDH_1SDV_20180902T052756_20180902T052821_023515_028F75_E2FC.zip', 'size': '847.08 MB', 'pseudopath': 'RADAR/LEVEL-1/2018/09/02, S1/1A/SAR-C/LEVEL-1/IW_GRDH_1S/2018/09/02, S1/1A/LEVEL-1/IW_GRDH_1S/2018/09/02, S1/SAR-C/LEVEL-1/IW_GRDH_1S/2018/09/02, S1/LEVEL-1/IW_GRDH_1S/2018/09/02, 2014-016A/SAR-C/LEVEL-1/IW_GRDH_1S/2018/09/02, 2014-016A/LEVEL-1/IW_GRDH_1S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(f395a594-ba58-4fc5-b8a4-82a7a7e95d0a)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T07:32:54.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T07:32:54.000Z, Instrument: null, Mode: null, Satellite: null, Size: 847.08 MB', 'title': 'S1A_IW_GRDH_1SDV_20180902T052756_20180902T052821_023515_028F75_E2FC'}, {'footprint': 'POLYGON ((11.064627 41.935905, 7.978951 42.336258, 8.325854 43.954865, 11.49643 43.554798, 11.064627 41.935905))', 'id': '8a4f2693-2add-4052-ba87-848b05bfe951', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(8a4f2693-2add-4052-ba87-848b05bfe951)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_SLC__1SDV_20180902T052754_20180902T052821_023515_028F75_08A8.zip', 'size': '3.78 GB', 'pseudopath': 'RADAR/LEVEL-1/2018/09/02, S1/1A/SAR-C/LEVEL-1/IW_SLC__1S/2018/09/02, S1/1A/LEVEL-1/IW_SLC__1S/2018/09/02, S1/SAR-C/LEVEL-1/IW_SLC__1S/2018/09/02, S1/LEVEL-1/IW_SLC__1S/2018/09/02, 2014-016A/SAR-C/LEVEL-1/IW_SLC__1S/2018/09/02, 2014-016A/LEVEL-1/IW_SLC__1S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(8a4f2693-2add-4052-ba87-848b05bfe951)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T08:24:36.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T08:24:36.000Z, Instrument: null, Mode: null, Satellite: null, Size: 3.78 GB', 'title': 'S1A_IW_SLC__1SDV_20180902T052754_20180902T052821_023515_028F75_08A8'}, {'footprint': 'POLYGON ((11.451983 43.426788, 8.292464 43.826138, 8.645281 45.443913, 11.896197 45.04446, 11.451983 43.426788))', 'id': '3f18a6f7-b3dc-4041-97bc-db43ab3b5f52', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(3f18a6f7-b3dc-4041-97bc-db43ab3b5f52)/$value', 'preview': None, 'properties': {'offline': 'false', 'downloadable': '', 'filename': 'S1A_IW_SLC__1SDV_20180902T052730_20180902T052757_023515_028F75_0BF8.zip', 'size': '4.33 GB', 'pseudopath': 'RADAR/LEVEL-1/2018/09/02, S1/1A/SAR-C/LEVEL-1/IW_SLC__1S/2018/09/02, S1/1A/LEVEL-1/IW_SLC__1S/2018/09/02, S1/SAR-C/LEVEL-1/IW_SLC__1S/2018/09/02, S1/LEVEL-1/IW_SLC__1S/2018/09/02, 2014-016A/SAR-C/LEVEL-1/IW_SLC__1S/2018/09/02, 2014-016A/LEVEL-1/IW_SLC__1S/2018/09/02', 'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(3f18a6f7-b3dc-4041-97bc-db43ab3b5f52)/$value', 'format': 'application/zip', 'creationDate': '2018-09-03T08:24:37.000Z'}, 'provider': 'ONDA', 'summary': 'Date: 2018-09-03T08:24:37.000Z, Instrument: null, Mode: null, Satellite: null, Size: 4.33 GB', 'title': 'S1A_IW_SLC__1SDV_20180902T052730_20180902T052757_023515_028F75_0BF8'}]
Found 8
```


### Include WASDI in a custom Processor

Let’s assume that the developer has his own EO Product Manipulation Code. At some point, the developer wishes to read his own input file, then make elaborations and finally save an output file. 

Let’s imagine a pseudo-code like this.

```python
# Input and output file name
filename = '~wasdiUser/EO/myfile.zip'
outputfilename = "~wasdiUser/EO/myoutput.tiff"

# Read the file
EOimage = multibandRead(filename, size, precision, offset, interleave, byteorder)

# Elaborate the image somehow
EOimage *= 2

# Save the output
imwrite(EOimage, outputfilename)
```

To port the code onto WASDI, the pseudo-code has to be integrated like this:

```python
import wasdi
import os

filename = 'myfile.zip'
outputFileName = 'myoutput.tiff'

fullInputPath = wasdi.getFullProductPath(filename)

# Read the file
EOproduct = multibandRead(fullInputPath, size, precision, offset, interleave, byteorder)

# Elaborate the image
EOproduct *= 2

# Save the output
# Get The Path
outputPath = wasdi.getSavePath()
fullOutputPath = os.path.join(outputPath, outputFileName)

# Use the save path
imwrite(EOproduct, fullOutputPath)

# Ingest in WASDI
wasdi.addFileToWASDI(outputFileName)
```

We modified the code to start the library and then to receive from WASDI the paths to use. 

The input files are supposed to be in the workspace. In order for this to happen, the user can go the wasdi web application, open the workspace, search the needed image and add it to the workspace.

The `wasdi.getFullProductPath` method has a double goal:

1. as the name suggests, it returns the local path to use back to the developer
2. if the code is running on the client PC, the Wasdi Lib will checks if the file is available locally: in case this checks fails, the lib will automatically download the file from the WASDI cloud to the local PC. \
To disable the auto download feature, is possible to add this parameter to the `config.json` file:\
```json
"DOWNLOADACTIVE":0
```

The choice of a name for the output file is left to the user,  WASPY just provides the folder to use (`wasdi.GetSavePath`). So to save the file we need to get the path and then concatenate the custom file name (`fullOutputPath = os.path.join(outputPath, outputFileName)`).

The last call, `AddFileToWASDI`, has the goal to add the product to the workspace. It takes in input only the file name, without the full path.

When used on the local PC, it will automatically upload the file after writing it on local file system. To inhibit this behavior, just add the following to the `config.json`:\
```json
"UPLOADACTIVE":0
```

### Use Custom parameters

Every processor usually has its own parameters. A typical example can be the name of a file in input, a threshold, the coordinates of an area of interest and so on. To let the developer work with her/his own parameters, WASPY implements an automatic file read. 

Add this line to the configuration file `config.json`:

```json
"PARAMETERSFILEPATH": "<path to a similar file for own parameters>"
```

e.g.

```json
"PARAMETERSFILEPATH": "c:/temp/myparameters.txt"
```

Then create the same file in the right folder and fill it with all the needed parameters, using the same syntax used for `config.json`; e.g.:

```json
"INPUTFILE": "S1A_imported_file.zip",
"THRESHOLD": 5,
"POINT": [44.2, 23.4]
```

The decision about how to encode these parameters is left to the developer. For WASDI these are all strings. In the example above, the developers may know that THRESHOLD is a number, and POINT is couple of coordinates that must to be splitted.

The only limit is that each parameter has to be written in one line.

In WASPY there are these three methods available:

-   `wasdi.getParameter(sKey)`: return the value of the sKey Parameter
-   `wasdi.addParameter(sKey, sValue)`: updates the value of a Parameter (ONLY in memory NOT in the file)
-   `wasdi.refreshParameters()`: reads the parameter file from disk again

Let’s update the code above to use the parameters file. First of all create a parameter file and set the name and path in the `config.json` file. The file (i.e., `parameters.json`) might look like this:

```json
{
  "INPUT_FILE": "S1A_imported_file.zip",
  "OUTPUT_FILE": "FloodedArea.tif"
}
```

Then modify the code to read the parameters without using hard-coded input:

```python
import wasdi
import os

# The input file is supposed to be in the workspace
# Read the file from parameters
filename = wasdi.getParameter("INPUT_FILE")
outputfilename = wasdi.getParameter("OUTPUT_FILE")

fullInputPath = wasdi.getFullProductPath(filename)

# Read the file
EOproduct = multibandRead(fullInputPath, size, precision, offset, interleave, byteorder)

# Elaborate the image
EOproduct  *= 2

# Save the output
# Get The Path
outputPath = wasdi.getSavePath()
fullOutputPath = os.path.join(outputPath, outputFileName)

# Use the save path
imwrite(EOproduct, fullOutputPath)

# Ingest in WASDI
wasdi.addFileToWASDI(outputFileName)
```
