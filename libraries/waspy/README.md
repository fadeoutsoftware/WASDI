# WASDI Python Library

This is a **preliminary version** of the Python Library you can use to access the [WASDI](http://www.wasdi.net) platform functionalities from your Python code.

Visit us at http://www.wasdi.net

----


## Python tutorial

WASPY is the **WAS**DI **Py**thon Library. You need WASDI registered user (with a username/password, don't create it using google) to use it. To start working with it, just install the library using:


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



### Create a `config.json` file

The `config.json` file is a standard json file, which is used to store the credentials of the user and some other settings. The syntax is:

```json
“VARIABLE_NAME”: value
```

The minimal configuration to begin working with WASPY is:

```json
{
  "USER": "yourUser@wasdi.net",
  "PASSWORD": "yourPasswordHere",
  "WORKSPACE": "nameOfTheWorkspaceYouWantToUse",
  "PARAMETERSFILEPATH": "<path to a json file w/ your own parameters>"
}
```


For the other available parameters please refer to the Documentation.


### Start WASPY

To start WASPY and check if everything is working, run the following code:


```python
wasdi.init('./config.json')
```


The Lib will read the configuration file, load the user and password, log the user in, and then open the workspace specified in the configuration file. To check if everything is working, try to get the list of workspaces available for the user:


```python
getWorkspaces()
```


you should see a result like this:


```
[{u'ownerUserId': u'yourUser@wasdi.net',
  u'sharedUsers': [],
  u'workspaceId': u'23ab54f3-b453-2b3e-284a-b6a4243f0f2c',
  u'workspaceName': u'aNiceNameForThisWorkspace'},
 {u'ownerUserId': u'yourUser@wasdi.net',
  u'sharedUsers': [],
  u'workspaceId': u'103fbf01-2e68-22d3-bd45-2cf95665dac2',
  u'workspaceName': u'theNameOfAnotherWorkspace'}]
```

The configured Workspace is already opened.  The use can open another workspace using:

```python
openWorkspace('aNiceNameForThisWorkspace')
```

and the lib replies showing the workspace unique id:

```python
u'9ce787d4-1d59-4146-8df7-3fc9516d4eb3'
```

To get the list of the products available in the workspace, call

```python
getProductsByWorkspace('aNiceNameForThisWorkspace')
```

and the lib returns a list of the products in the given workspace:

```
[u'S1A_IW_GRDH_1SDV_20190517T053543_20190517T053608_027263_0312F1_F071.zip',
u'S1B_IW_RAW__0SDV_20190506T052631_20190506T052703_016119_01E53A_D2AD.zip', u'S1A_IW_GRDH_1SDV_20190517T053608_20190517T053633_027263_0312F1_3382.zip']
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
import os

filename = 'myfile.zip'
outputFileName = 'myoutput.tiff'

fullInputPath = getFullProductPath(filename)

# Read the file
EOproduct = multibandRead(fullInputPath, size, precision, offset, interleave, byteorder)
# Elaborate the image
EOproduct *= 2

# Save the output
# Get The Path
outputPath = getSavePath()
fullOutputPath = os.path.join(outputPath, outputFileName)
# Use the save path
imwrite(EOproduct, fullOutputPath)
# Ingest in WASDI
AddFileToWASDI(outputFileName)
```

We modified the code to start the library and then to receive from WASDI the paths to use. 

The input files are supposed to be in the workspace. In order for this to happen, the user can go the wasdi web application, open the workspace, search the needed image and add it to the workspace.

The `getFullProductPath` method has a double goal:

1. as the name suggests, it returns the local path to use back to the developer
2. if the code is running on the client PC, the Wasdi Lib will checks if the file is available locally: in case this checks fails, the lib will automatically download the file from the WASDI cloud to the local PC. \
To disable the auto download feature, is possible to add this parameter to the `config.json` file: \
`"DOWNLOADACTIVE":0`

To save a file the name is left to the user,  WASPY just provides the folder to use (`GetSavePath`). So to save the file we need to get the path and then concatenate the custom file name.

The last call, `AddFileToWASDI`, has the goal to add the product to the workspace. It takes in input only the file name, without the full path.

When used on the local PC, it will automatically upload the file after writing it on local file system. To inhibit this behavior, just add the following to the `config.json`:\
```json
"UPLOADACTIVE":0
```

### Use Custom parameters

Every processor usually has its own parameters. A typical example can be the name of a file in input, a threshold, the coordinates of an area of interest and so on. To let the developer work with her/his own parameters, WASPY implements an automatic file read. 

Add this line to the configuration file `config.json`:

```json
"PARAMETERSFILEPATH": <path to a similar file for own parameters>
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

*   `getParameter(sKey)`: return the value of the sKey Parameter
*   `addParameter(sKey, sValue)`: updates the value of a Parameter (ONLY in memory NOT in the file)
*   `refreshParameters()`: reads the parameter file from disk again

Let’s update the code above to use the parameters file. First of all create a parameter file and set the name and path in the `config.json` file. The file (i.e., `parameters.json`) becomes:

```json
{
  "INPUT_FILE": "S1A_imported_file.zip",
  "OUTPUT_FILE": "FloodedArea.tif"
}
```

Then modify the code to read the parameters without using hard-coded input:

```python
# THE INPUT FILE IS SUPPOSED TO BE IN THE WORKSPACE
# READ THE FILE FROM PARAMETERS
filename = getParameter("INPUT_FILE")
outputfilename = getParameter("OUTPUT_FILE")

fullInputPath = getFullProductPath(filename)

# Read the file
EOproduct = multibandRead(fullInputPath, size, precision, offset, interleave, byteorder)
# Elaborate the image
EOproduct  *= 2

# Save the output
# Get The Path
outputPath = getSavePath()
fullOutputPath = os.path.join(outputPath, outputFileName)
# Use the save path
imwrite(EOproduct, fullOutputPath)
# Ingest in WASDI
addFileToWASDI(outputFileName)
```
