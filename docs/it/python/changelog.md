


### [0.6.5] - 2021-09-02

 Added
- REQUEST TIMEOUT: added a timeout to all the requests calls. It is 2 min by default. Can be configured in config.json, key REQUESTSTIMEOUT, value is the number of seconds
 Fixed
- searchEOImages: fixed file name in VIIRS (replace .part with _part)


### [0.6.4] - 2021-05-21

 Fixed
- searchEOImages: fixed file name in VIIRS (replace .part with _part)

### [0.6.3] - 2021-05-21

 Added
- searchEOImages: support to Landsat8, VIIRS, ENVISAT

 Changed

- added support to relative path in copy to sftp

### [0.6.2] - 2021-03-10

 Changed

- moved to https

### [0.6.1] - 2021-01-14

 Added
- getProcessesByWorkspace: gets a list of the processes executed in the active workspace

 Fixed

- import `__builtin__` or `builtins` depending on the version of Python being used

 Changed

- removed log in `updateProcessStatus` when the percent == -1, as that value is assigned by default by other calls

### [0.6.0] - 2020-10-28

 Added

- support to the auto upload and download by nodes
- getProcessorPath method to have the absolute path of the running processor
- fileName, provider and relativeOrbit properties to the search EO Images results
- Support for string and json bounding box in the searchEOImages method
- importImage methods use the provider specified in the image properties if not set

 Fixed

- Removed internal libraries logs

### [0.5.1] - 2020-05-27

 Fixed

- add File to WASDI did a double file Ingest
- Auto upload was active also on server
- Moved upload and download as private methods

### [0.5.0] - 2020-05-15

 Added

- support to the optmized Distributed Architecture

### [0.4.2] - 2020-04-30

 Added

- added deleteWorkspace method

### [0.4.1] - 2020-04-22

 Added

- added log in searchEOImages

### [0.4.0] - 2020-04-22

 Added

- check for availability of params file
- executeProcessor supporting POST other than GET

 Fixed

- bug in getProcessorPayload due to string concatenation with non string

### [0.3.5] - 2020-04-21

 Added

- get payload given process id

 Changed

- added retry and logs to the executeProcess
- added check to the getProcess Status to return ERROR if processId is null or empty
- use of optimized API to get process status

### [0.3.3] - 2020-04-10

 Added

- Added big tiff support to multiSubset (added flag, False by default)
- Added wasdi.copyFileToSftp to copy a file from a workpsace to the user wasdi sftp folder

### [0.3.2] - 2020-04-02

 Added

- Updated waitProcesses to use Massive API
- Updated importAndPreprocess to start all downloads in asynch way from the beginning
- Fixed log in update Progress Perc


### [0.3.1] - 2020-03-26

 Added

- Private API to set the subprocess id

### [0.3.0] - 2020-03-20

 Added

- Support to distributed WASDI nodes

### [0.2.12] - 2020-03-18

 Added

- (Automatic) upload (& ingestion) of files in wasdi

### [0.2.11] - 2020-03-11

 Added

- users can now log at different levels: DEBUG, INFO, WARNING, ERROR and CRITICAL, using respectively: debugLog, infoLog, warningLog, errorLog and criticalLog

 Fixed

- solved error in getProductBBOX internal url construction (it was introduced in last update)

### [0.2.10] - 2020-03-06

 Added

- DEBUG log at the beginning of each method (except those that would log anyway)

 Changed

- improved 'pythonicyty' of IFs
- improved exception handling
- improved clarity of log messages

### [0.2.9] - 2020-02-24

 Changed

- Separate changelog according to [keepachangelog](https://keepachangelog.com/)
- Introduced use of python logging instead of prints and _log
    - Log at DEBUG level each time a method is accessed
    - Log (at ERROR or WARNING, as appropriate), each time an exception is caught

 Fixed

- minor errors in f-strings construction


### [0.2.8] - 2020-02-05

 Fixed

- Solved a bug in the _waitForResume private method.

### [0.2.7] - 2020-01-25

 Added

- Support to Provider selection for search and import
- Generic getPath method for both writing and reading
- Exception handling in getProductBBOX
- Limit to 10 tiles in multiSubset

### [0.2.3] - 2020-01-23

 Added

- Support to WAITING and READY Process State

### [0.1.34] - 2019-12/20

 Added

- createWorkspace

 Fixed

- asynchExecuteProcess bug

 Changed

- Reviewed comment based documentation of all methods

### [0.1.32] - 2019-12-19

 Fixed

- import bug on a not requested package

### [0.1.31] - 2019-12-18

 Added

- multiSubset support
- console input of user, pw and workspace if config is not specified

### [0.1.30] - 2019-12-10

 Added

- asynch version of the import Products Method
- import Product for a list of files
- get Product Bounding Box
- first version of importAndPreprocess Version

 Fixed

- bug on Verbose Flag

### [0.1.29] - 2019-11-05

 Fixed

- possible infinite loop in addFileToWASDI

### [0.1.28] - 2019-10-28

 Added

- support to .vrt format for mosaic

### [0.1.26] - 2019-10-24

 Added

- try and catch to importProduct

 Changed

- getFullProductPath works also for non existing files

### [0.1.23] - 2019-10-23

 Fixed

- deleteProduct bug (did not get standard headers)

### [0.1.22] - 2019-10-16

 Changed

- updated mosaic to last gdal-supported version

### [0.1.21] - 2019-10-15

 Changed

- moved fileExistInWasdi from protected to public

### [0.1.20] - 2019-10-15

 Added

- possibility to run synch and asynch workflows without the need to use the array of input and ouput files if not needed: user can pass just strings

### [0.1.19] - 15/10/2019

 Removed

- unwanted import from wasdi lib

### [0.1.18] - 2019-10-15

 Changed

- Splitted importEO product in two version: one with product dictionary object and one with url and bbox

 Fixed

- waitProcesses syntax for python 2 compatibility

### [0.1.17]- 2019-10-15

 Added

- waitProcesses to wait for more than one asynch process
- getParamter version with a second optional parameter to use as default

 Fixed

- bug about cloud coverage in search EO Images

### [0.1.16] - 2019-09-16

 Added

- setPayload to set the payload of the actual running processor.

 Fixed

- getFullProductPath bug to support many files on the same folder

### [0.1.15]

 Fixed

- Path generation for execution on shared workspaces