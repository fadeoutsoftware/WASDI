# WASDI Python Library

WASDI is the Web Advanced Space Developer Interface. This software is a **preliminary version** of the Python Library you can use to access the [WASDI](http://www.wasdi.net) platform functionalities from your Python code.

Visit us at [http://www.wasdi.net](http://www.wasdi.net)

The source code can be found [here](https://github.com/fadeoutsoftware/WASDI/tree/develop/libraries/waspy)

----


## Python tutorial

WASPY is the **WAS**DI **Py**thon Library. 


**GET UPDATED DOCUMENTATION, TUTORIALS AND MORE [here](https://wasdi.readthedocs.io/en/latest/index.html)**

### Prerequisites:

mandatory:

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

Now try something more, let's search for some Sentinel 1 images. Let's assume we are interested in images taken from "2018-09-01" to "2018-09-02". Also, we'd better specify a bounding box. Assume we're interested in images with *latitude* in `[43, 44]` and *longitude* in `[11, 12]`. We can think of these coordinates as a rectangle limited by the upper left corner `(44, 11)` and the lower right corner`(43, 12)`. 
 The corresponding code is:

```python
wasdi.wasdiLog('Let\'s search some images')
aoImages = wasdi.searchEOImages("S1", "2018-09-01", "2018-09-02", 44, 11, 43, 12, None, None, None, None)
wasdi.wasdiLog('Found ' + str(len(aoImages)))
```

The output should be similar to this:

```
 Let's search some images
[INFO] waspy.searchEOImages: search results:
[{
		'footprint': 'POLYGON ((8.8724 45.3272, 8.4505 43.3746, 11.4656 43.0981, 11.9901 45.0472, 8.8724 45.3272, 8.8724 45.3272))',
		'id': 'cba6c104-3006-4af7-a2d1-cbd55f58b939',
		'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(cba6c104-3006-4af7-a2d1-cbd55f58b939)/$value',
		'preview': None,
		'properties': {
			'offline': 'false',
			'downloadable': '',
			'filename': 'S1A_IW_RAW__0SDV_20180902T052727_20180902T052759_023515_028F75_7325.zip',
			'size': '1.54 GB',
			'pseudopath': 'RADAR/LEVEL-0/2018/09/02, S1/1A/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, S1/1A/LEVEL-0/IW_RAW__0S/2018/09/02, S1/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, S1/LEVEL-0/IW_RAW__0S/2018/09/02, 2014-016A/SAR-C/LEVEL-0/IW_RAW__0S/2018/09/02, 2014-016A/LEVEL-0/IW_RAW__0S/2018/09/02',
			'link': 'https://catalogue.onda-dias.eu/dias-catalogue/Products(cba6c104-3006-4af7-a2d1-cbd55f58b939)/$value',
			'format': 'application/zip',
			'creationDate': '2018-09-03T05:12:37.000Z'
		},
		'provider': 'ONDA',
		'summary': 'Date: 2018-09-03T05:12:37.000Z, Instrument: null, Mode: null, Satellite: null, Size: 1.54 GB',
		'title': 'S1A_IW_RAW__0SDV_20180902T052727_20180902T052759_023515_028F75_7325'
},
{'(...7 more results similar to this one, omitted for brevity)'}]
Found 8
```

Now we can import one of those products in WASDI: let's download the first one:

```python
sImportWithDict = wasdi.importProduct(None, None, aoImages[0])
```

We can see a list of the products in the workspace as follows:

```python
asProducts = wasdi.getProductsByActiveWorkspace()
wasdi.wasdiLog(asProducts)
```

The second line logs the list of products

### Running an existing workflow

If you wish to run an existing SNAP workflow you can use `wasdi.executeWorkflow`. For example, if you wish to execute a workflow that calibrates and corrects the georeference of a Sentinel 1 image, you may use the workflow called `LISTSinglePreproc` in this way:

```python
asProducts = wasdi.getProductsByActiveWorkspace()
sStatus = wasdi.executeWorkflow([asProducts[0]], ['lovelyOutput'], 'LISTSinglePreproc')
```

Here the first line gets the list of products and the second calls the workflow `LISTSinglePreproc` on the first product of the workspace and creates another product called `lovelyOutput`.

### A more complete example

Now put everything back together. Create a file called [`myProcessor.py`](https://github.com/fadeoutsoftware/WASDI/blob/develop/libraries/waspy/examples/myProcessor.py) (follow the link to download the file) with the following content:

```python
import wasdi


def run(parameters, processId):
    wasdi.wasdiLog('Here\'s the list of your workspaces:')
    aoWorkspaces = wasdi.getWorkspaces()
    wasdi.wasdiLog(aoWorkspaces)
    wasdi.wasdiLog('The ID of currently selected workspace is:')
    sActiveWorkspace = wasdi.getActiveWorkspaceId()
    wasdi.wasdiLog(sActiveWorkspace)

    wasdi.wasdiLog('Let\'s search some images...')
    aoImages = wasdi.searchEOImages("S1", "2018-09-01", "2018-09-02", 44, 11, 43, 12, sProductType='GRD')
    wasdi.wasdiLog('Found ' + str(len(aoImages)) + ' images')

    wasdi.wasdiLog('Download the first one passing the dictionary...')
    sImportWithDict = wasdi.importProduct(None, None, aoImages[0])
    wasdi.wasdiLog('Import with dict returned: ' + sImportWithDict)

    wasdi.wasdiLog('Now, these are the products in your workspace: ')
    asProducts = wasdi.getProductsByActiveWorkspace()
    wasdi.wasdiLog(asProducts)

    wasdi.wasdiLog('Let\'s run a workflow on the first image to rectify its georeference...')
    sStatus = wasdi.executeWorkflow([asProducts[0]], ['lovelyOutput'], 'LISTSinglePreproc')
    if sStatus == 'DONE':
        wasdi.wasdiLog('The product is now in your workspace, look at it on the website')

    wasdi.wasdiLog('It\'s over!')

def WasdiHelp():
    sHelp = "Wasdi Tutorial"
    return sHelp
```

Then create another file to start the processor. Let's call it [`tutorial.py`](https://github.com/fadeoutsoftware/WASDI/blob/develop/libraries/waspy/examples/tutorial.py) (follow the link to download the file), with the following content:

```python
import myProcessor
import wasdi

bInitResult = wasdi.init('config.json')
if bInitResult:
    myProcessor.run(wasdi.getParametersDict(), '')

```

Now, if you run `tutorial.py`, it will call `myProcessor.py`, which will go through the instructions we saw above. Pro tip: keep the browser open in wasdi.net (make sure you are logged in) and open the workspace you are using, to see the evolution of the script in real time.

### Deploy your processor on WASDI

Finally, to deply our processor on WASDI, you need first to create a text file called [`pip.txt`](https://github.com/fadeoutsoftware/WASDI/blob/develop/libraries/waspy/examples/pip.txt) (follow the link to download the file) containg the packages we imported in `myProcessor.py`, one per line. Since we just imported `wasdi`, it should look like this:

```
wasdi
```

Now, create a zip file containing these two files:

- `myProcessor.py`
- `pip.txt`

You can now upload the zip file on wasdi.net from *Edit* -> *Processor* -> *New WASDI App* by giving it a name and completing the other details. You will need to do this just once.
To run it, go to *WADI Apps* -> (select yours) -> no parameters are needed, so just enter `{}` and clic *run*.

----

### More to include WASDI in a custom Processor

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
