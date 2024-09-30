.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _Python WasdiLib:

Python WasdiLib
===========================================

Legal Notice
-------------------
WASDI Sàrl

**Disclaimer**
The library is provided "as-is" without warranty

Neither FadeOut Software (IT) Srl or any of its partners or agents shall be liable for any direct, indirect, incidental, special, exemplary, or consequential
damages (including, but not limited to, breach of expressed or implied contract; procurement of substitute goods or services; loss of use, data or profits;
business interruption; or damage to any equipment, software and/or data files) however caused and on any legal theory of liability, whether for contract,
tort, strict liability, or a combination thereof (including negligence or otherwise) arising in any way out of the direct or indirect use of software,
even if advised of the possibility of such risk and potential damage.

FadeOut Software (IT) Srl uses all reasonable care to ensure that software products and other files that are made available are safe to use when installed,
and that all products are free from any known software virus. For your own protection, you should scan all files for viruses prior to installation.


# WASDI

This is WASPY, the WASDI Python lib.

WASDI is an ESA GSTP Project sponsored by ASI in 2016. The system is a fully scalable and distributed Cloud based EO analytical platform. The system is cross-cloud and cross DIAS.
WASDI is an operating platform that offers services to develop and deploy DIAS based EO on-line applications, designed
to extract value-added information, made and distributed by EO-Experts without any specific IT/Cloud skills.
WASDI offers as well to End-Users the opportunity to run EO applications both from a dedicated user-friendly interface
and from an API based software interface, fulfilling the real-world business needs.
EO-Developers can work using the WASDI Libraries in their usual programming languages and add to the platform these new blocks
in the simplest possible way.

Note:
the philosophy of safe programming is adopted as widely as possible, the lib will try to workaround issues such as
faulty input, and print an error rather than raise an exception, so that your program can possibly go on. Please check
the return statues

Version 0.6.2
Last Update: 10/03/2021




Tested with: Python 2.7, Python 3.7

Methods
-------------------

addChartToPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: addChartToPayload
   :noindex:

addFileToWASDI
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: addFileToWASDI
   :noindex:

addParameter
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: addParameter
   :noindex:

getParameter
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getParameter
   :noindex:

getParametersDict
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getParametersDict
   :noindex:

getParametersFilePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getParametersFilePath
   :noindex:

getSessionId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getSessionId
   :noindex:


getPassword
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getPassword
   :noindex:

getUser
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getUser
   :noindex:

getVerbose
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getVerbose
   :noindex:

getWorkflows
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkflows
   :noindex:



getFoundProductName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getFoundProductName
   :noindex:

getProductBBOX
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductBBOX
   :noindex:


getProcessorPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessorPath
   :noindex:

getProcessesByWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessesByWorkspace
   :noindex:


getBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getBaseUrl
   :noindex:

setWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setWorkspaceBaseUrl
   :noindex:

getWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceBaseUrl
   :noindex:

setIsOnServer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setIsOnServer
   :noindex:

getIsOnServer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getIsOnServer
   :noindex:

setDownloadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setDownloadActive
   :noindex:

getDownloadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getDownloadActive
   :noindex:

setUploadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setUploadActive
   :noindex:

getUploadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getUploadActive
   :noindex:

setProcId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setProcId
   :noindex:

getProcId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcId
   :noindex:

setActiveWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setActiveWorkspaceId
   :noindex:

getActiveWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getActiveWorkspaceId
   :noindex:

refreshParameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: refreshParameters
   :noindex:

init
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: init
   :noindex:

hello
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: hello
   :noindex:

getWorkspaces
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaces
   :noindex:

createWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: createWorkspace
   :noindex:

deleteWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: deleteWorkspace
   :noindex:

getWorkspaceIdByName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceIdByName
   :noindex:

getWorkspaceOwnerByName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceOwnerByName
   :noindex:

getWorkspaceOwnerByWsId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceOwnerByWsId
   :noindex:

getWorkspaceUrlByWsId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceUrlByWsId
   :noindex:

openWorkspaceById
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: openWorkspaceById
   :noindex:

openWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: openWorkspace
   :noindex:

getProductsByWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductsByWorkspace
   :noindex:

getProductsByWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductsByWorkspaceId
   :noindex:

getProductsByActiveWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductsByActiveWorkspace
   :noindex:

getPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getPath
   :noindex:

getFullProductPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getFullProductPath
   :noindex:

getSavePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getSavePath
   :noindex:

getProcessStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessStatus
   :noindex:

deleteProduct
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: deleteProduct
   :noindex:

mosaic
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: mosaic
   :noindex:

printStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: printStatus
   :noindex:

searchEOImages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: searchEOImages
   :noindex:

setVerbose
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setVerbose
   :noindex:

setParametersDict
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setParametersDict
   :noindex:

setUser
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setUser
   :noindex:

setPassword
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setPassword
   :noindex:

setSessionId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setSessionId
   :noindex:

setParametersFilePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setParametersFilePath
   :noindex:


setBasePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setBasePath
   :noindex:

getBasePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getBasePath
   :noindex:

setBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setBaseUrl
   :noindex:

setProcessPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setProcessPayload
   :noindex:

setPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setPayload
   :noindex:

getProcessorPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessorPayload
   :noindex:

getProcessorPayloadAsJson
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessorPayloadAsJson
   :noindex:

setSubPid
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setSubPid
   :noindex:

saveFile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: saveFile
   :noindex:

updateProgressPerc
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: updateProgressPerc
   :noindex:

updateProcessStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: updateProcessStatus
   :noindex:

updateStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: updateStatus
   :noindex:

waitProcess
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: waitProcess
   :noindex:

waitProcesses
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: waitProcesses
   :noindex:



_downloadFile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _downloadFile
   :noindex:

wasdiLog
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: wasdiLog
   :noindex:





fileExistsOnWasdi
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: fileExistsOnWasdi
   :noindex:


importProductByFileUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importProductByFileUrl
   :noindex:

asynchImportProductByFileUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchImportProductByFileUrl
   :noindex:

importProduct
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importProduct
   :noindex:

asynchImportProduct
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchImportProduct
   :noindex:

importProductList
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importProductList
   :noindex:

asynchImportProductList
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchImportProductList
   :noindex:

asynchAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchAddFileToWASDI
   :noindex:

importAndPreprocess
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importAndPreprocess
   :noindex:

asynchExecuteProcessor
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchExecuteProcessor
   :noindex:

executeProcessor
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: executeProcessor
   :noindex:

_uploadFile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _uploadFile
   :noindex:


subset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: subset
   :noindex:
   

multiSubset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: multiSubset
   :noindex:



executeWorkflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: executeWorkflow
   :noindex:

asynchExecuteWorkflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchExecuteWorkflow
   :noindex:

asynchMosaic
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchMosaic
   :noindex:



copyFileToSftp
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: copyFileToSftp
   :noindex:


_log
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _log

_getStandardHeaders
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _getStandardHeaders

_loadConfig
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _loadConfig

_loadParams
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _loadParams

_unzip
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _unzip

_waitForResume
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _waitForResume

_normPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _normPath

_internalAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _internalAddFileToWASDI

_internalExecuteWorkflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _internalExecuteWorkflow

_fileOnNode
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _fileOnNode

_getDefaultCRS
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _getDefaultCRS


Changelog
---------------------------------------
.. toctree::
    :maxdepth: 1
    :hidden:

   changelog.md







