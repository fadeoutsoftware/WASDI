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


addFileToWASDI
^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: addFileToWASDI

printStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: printStatus

setVerbose
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setVerbose

getVerbose
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getVerbose

getParametersDict
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getParametersDict

setParametersDict
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setParametersDict

addParameter
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: addParameter

getParameter
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getParameter

setUser
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setUser

getUser
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getUser

setPassword
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setPassword

getPassword
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getPassword

setSessionId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setSessionId

setParametersFilePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setParametersFilePath

getParametersFilePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getParametersFilePath

getSessionId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getSessionId

setBasePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setBasePath

getBasePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getBasePath

setBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setBaseUrl

getBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getBaseUrl

setWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setWorkspaceBaseUrl

getWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceBaseUrl

setIsOnServer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setIsOnServer

getIsOnServer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getIsOnServer

setDownloadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setDownloadActive

getDownloadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getDownloadActive

setUploadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setUploadActive

getUploadActive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getUploadActive

setProcId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setProcId

getProcId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcId

setActiveWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setActiveWorkspaceId

getActiveWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getActiveWorkspaceId

refreshParameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: refreshParameters

init
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: init

hello
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: hello

getWorkspaces
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaces

createWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: createWorkspace

deleteWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: deleteWorkspace

getWorkspaceIdByName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceIdByName

getWorkspaceOwnerByName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceOwnerByName

getWorkspaceOwnerByWsId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceOwnerByWsId

getWorkspaceUrlByWsId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkspaceUrlByWsId

openWorkspaceById
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: openWorkspaceById

openWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: openWorkspace

getProductsByWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductsByWorkspace

getProductsByWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductsByWorkspaceId

getProductsByActiveWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductsByActiveWorkspace

getPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getPath

getFullProductPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getFullProductPath

getSavePath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getSavePath

getProcessStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessStatus

updateProcessStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: updateProcessStatus

updateStatus
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: updateStatus

waitProcess
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: waitProcess

waitProcesses
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: waitProcesses

updateProgressPerc
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: updateProgressPerc

setProcessPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setProcessPayload

setPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setPayload

getProcessorPayload
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessorPayload

getProcessorPayloadAsJson
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessorPayloadAsJson

setSubPid
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: setSubPid

saveFile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: saveFile

_downloadFile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _downloadFile

wasdiLog
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: wasdiLog

deleteProduct
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: deleteProduct

searchEOImages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: searchEOImages

getFoundProductName
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getFoundProductName

fileExistsOnWasdi
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: fileExistsOnWasdi

getProductBBOX
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProductBBOX

importProductByFileUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importProductByFileUrl

asynchImportProductByFileUrl
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchImportProductByFileUrl

importProduct
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importProduct

asynchImportProduct
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchImportProduct

importProductList
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importProductList

asynchImportProductList
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchImportProductList

importAndPreprocess
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: importAndPreprocess

asynchExecuteProcessor
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchExecuteProcessor

executeProcessor
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: executeProcessor

_uploadFile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: _uploadFile

addFileToWASDI
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: addFileToWASDI

asynchAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchAddFileToWASDI

subset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: subset

multiSubset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: multiSubset

getWorkflows
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getWorkflows

executeWorkflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: executeWorkflow

asynchExecuteWorkflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchExecuteWorkflow

asynchMosaic
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: asynchMosaic

mosaic
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: mosaic

copyFileToSftp
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: copyFileToSftp

getProcessorPath
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessorPath

getProcessesByWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. automodule:: wasdi
   :members: getProcessesByWorkspace

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


Process finished with exit code 0




.. Logging Utils
.. ^^^^^^^^^^^^^^^^^^^^ Reference for subsection with different modules. Check how to remove comments from automodule

.. .. automodule:: wasdi :members: setVerbose, util





