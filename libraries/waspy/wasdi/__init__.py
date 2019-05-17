"""
Created on 11 Jun 2018

@author: p.campanella
"""

name = "wasdi"

import requests
import os
import json
import traceback
import re
import zipfile

m_sUser = None
m_sPassword = None

m_sActiveWorkspace = None

m_sParametersFilePath = None
m_sSessionId = ''
m_sBasePath = None

m_bDownloadActive = True
m_bUploadActive = True
m_bVerbose = True
m_aoParamsDictionary = {}

m_sMyProcId = ''
m_sBaseUrl = 'http://www.wasdi.net/wasdiwebserver/rest'
m_bIsOnServer = False


def printStatus():
    global m_sUser
    global m_sPassword
    global m_sActiveWorkspace
    global m_sParametersFilePath
    global m_sSessionId
    global m_sBasePath
    global m_bDownloadActive
    global m_bUploadActive
    global m_bVerbose
    global m_aoParamsDictionary
    global m_sMyProcId
    global m_sBaseUrl
    global m_bIsOnServer

    log('user: '+str(m_sUser))
    log('password: '+str(m_sPassword))
    log('active workspace: '+str(m_sActiveWorkspace))
    log('parameters file path: '+str(m_sParametersFilePath))
    log('session id: '+str(m_sSessionId))
    log('base path: '+str(m_sBasePath))
    log('download active: '+str(m_bDownloadActive))
    log('upload active: '+str(m_bUploadActive))
    log('verbose: '+str(m_bVerbose))
    log('param dict: '+str(m_aoParamsDictionary))
    log('proc id: '+str(m_sMyProcId))
    log('base url: '+str(m_sBaseUrl))
    log('is on server: '+str(m_bIsOnServer))


def getParametersDict():
    """
    Get the full Params Dictionary
    """
    global m_aoParamsDictionary
    return m_aoParamsDictionary


def addParameter(sKey, oValue):
    """
    Add a sigle Parameter to the Dictionary
    """
    global m_aoParamsDictionary
    m_aoParamsDictionary[sKey] = oValue


def getParameter(sKey):
    """
    Get a Parameter. None if key does not exists
    """
    global m_aoParamsDictionary
    try:
        return m_aoParamsDictionary[sKey]
    except:
        return None

    
def setUser(sUser):
    """
    Set the WASDI User
    """
    global m_sUser
    m_sUser = sUser


def getUser():
    """
    Get the WASDI User
    """    
    global m_sUser
    return m_sUser


def setPassword(sPassword):
    """
    Set the WASDI Password
    """    
    global m_sPassword
    m_sPassword = sPassword


def getPassword():
    """
    Get the WASDI Password
    """    
    global m_sPassword
    return m_sPassword


def setSessionId(sSessionId):
    """
    Set the WASDI Session
    """    
    global m_sSessionId
    m_sSessionId = sSessionId


def getSessionId():
    """
    Get the WASDI Session
    """    
    global m_sSessionId
    return m_sSessionId
    

def setBasePath(sBasePath):
    """
    Set the local Base Path for WASDI
    """    
    global m_sBasePath
    m_sBasePath = sBasePath


def getBasePath():
    """
    Get the local Base Path for WASDI
    """    
    global m_sBasePath
    return m_sBasePath
       

def setBaseUrl(sBaseUrl):
    """
    Set the WASDI API URL
    """    
    global m_sBaseUrl
    m_sBaseUrl = sBaseUrl


def getBaseUrl():
    """
    Get the WASDI API URL
    """    
    global m_sBaseUrl
    return m_sBaseUrl


def setIsOnServer(bIsOnServer):
    """
    Set the Is on Server Flag: keep it false, as default, while developing
    """    
    global m_bIsOnServer
    m_bIsOnServer = bIsOnServer


def getIsOnServer():
    """
    Get the WASDI API URL
    """    
    global m_bIsOnServer
    return m_bIsOnServer


def setDownloadActive(bDownloadActive):
    """
    When in development, set True to download locally files on Server.
    Set it to false to NOT donwload data. In this case the developer must check the availability of the files
    """    
    global m_bDownloadActive
    m_bDownloadActive = bDownloadActive


def getDownloadActive():
    """
    Get the WASDI API URL
    """    
    global m_bDownloadActive
    return m_bDownloadActive


def setProcId(sProcID):
    """
    Own Proc Id 
    """    
    global m_sMyProcId
    m_sMyProcId = sProcID


def getProcId():
    """
    Get the Own Proc Id
    """    
    global m_sMyProcId
    return m_sMyProcId


def log(sLog):
    if m_bVerbose:
        print(sLog)


def __getStandardHeaders():
    global m_sSessionId
    asHeaders = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    return asHeaders


def __loadConfig(sConfigFilePath):
    """
    Loads configuration from given file
    :param sConfigFilePath: a string containing a path to the configuration file
    """
    if sConfigFilePath is None:
        raise TypeError("[ERROR] waspy.__loadConfigParams: config parameter file name is None, cannot load config")
    if sConfigFilePath == '':
        raise ValueError("[ERROR] waspy.__loadConfigParams: config parameter file name is empty, cannot load config")

    global m_sUser
    global m_sPassword
    global m_sParametersFilePath
    global m_sSessionId
    global m_sBasePath

    global m_bDownloadActive
    global m_bUploadActive
    global m_bVerbose

    try:
        # assume it is a JSON file
        sTempWorkspaceName = None
        sTempWorkspaceID = None
        with open(sConfigFilePath) as oJsonFile:
            oJson = json.load(oJsonFile)
            if "USER" in oJson:
                m_sUser = oJson["USER"]
            if "PASSWORD" in oJson:
                m_sPassword = oJson["PASSWORD"]
            if "WORKSPACE" in oJson:
                sTempWorkspaceName = oJson["WORKSPACE"]
                sTempWorkspaceID = None
            elif "WORKSPACEID" in oJson:
                sTempWorkspaceID = oJson["WORKSPACEID"]
                sTempWorkspaceName = None
            if "BASEPATH" in oJson:
                m_sBasePath = oJson["BASEPATH"]
            if "PARAMETERSFILEPATH" in oJson:
                m_sParametersFilePath = oJson["PARAMETERSFILEPATH"]
            if "DOWNLOADACTIVE" in oJson:
                m_bDownloadActive = bool(oJson["DOWNLOADACTIVE"])
            if "UPLOADACTIVE" in oJson:
                m_bUploadActive = bool(oJson["UPLOADACTIVE"])
            if "VERBOSE" in oJson:
                m_bVerbose = bool(oJson["VERBOSE"])

        return True, sTempWorkspaceName, sTempWorkspaceID

    except Exception as oEx:
        print('[ERROR] waspy.__loadConfigParams: something went wrong')
        raise


def __loadParams():
    """
    Loads parameters from file, if specified in configuration file
    """
    global m_sParametersFilePath
    global m_aoParamsDictionary

    if (m_sParametersFilePath is not None) and (m_sParametersFilePath != ''):
        with open(m_sParametersFilePath) as oJsonFile:
            m_aoParamsDictionary = json.load(oJsonFile)


def init(sConfigFilePath=None):
    """
    Init WASDI Library. Call it after setting user, password, path and url or use it with a config file
    Return True if login was successful, False otherwise
    """    
    global m_sUser
    global m_sPassword
    global m_sBaseUrl
    global m_sSessionId
    global m_sBasePath

    sWname = None
    sWId = None
    bResult = False
    if sConfigFilePath is not None:
        bConfigOk, sWname, sWId = __loadConfig(sConfigFilePath)
        if bConfigOk is True:
            __loadParams()

    if m_sUser is None:
        raise TypeError('Must initialize user first')

    if m_sBasePath is None:
        if m_bIsOnServer is True:
            m_sBasePath = '/data/wasdi/'
        else:
            sHome = os.path.expanduser("~")
            m_sBasePath = os.path.join(sHome, ".wasdi", "")

    if m_sSessionId != '':
        asHeaders = __getStandardHeaders()
        sUrl = m_sBaseUrl + '/auth/checksession'
        oResponse = requests.get(sUrl, headers=asHeaders)
        if (oResponse is not None) and (oResponse.ok is True):
            oJsonResult = oResponse.json()
            try:
                sUser = oJsonResult['userId']
                if sUser == m_sUser:
                    bResult = True
                else:
                    bResult = False
            except:
                bResult = False
        else:
            bResult = False
    else:
        asHeaders = {'Content-Type': 'application/json'}
        sUrl = m_sBaseUrl + '/auth/login'
        sPayload = '{"userId":"' + m_sUser + '","userPassword":"' + m_sPassword + '" }'
        oResponse = requests.post(sUrl, data=sPayload, headers=asHeaders)

        if oResponse.ok is True:
            oJsonResult = oResponse.json()
            try:
                m_sSessionId = oJsonResult['sessionId']
                bResult = True
            except:
                bResult = False
        else:
            bResult = False

    if bResult is True:
        sW = getActiveWorkspaceId()
        if (sW is None) or (len(sW) < 1):
            if sWname is not None:
                openWorkspace(sWname)
            elif sWId is not None:
                openWorkspaceById(sWId)

    printStatus()
    return bResult


def hello():
    """
    Hello Wasdi to test the connection.
    Return the hello message as Text
    """    
    global m_sBaseUrl
    
    sUrl = m_sBaseUrl + '/wasdi/hello'
    oResult = requests.get(sUrl)
    return oResult.text


def getWorkspaces():
    """
    Get List of user workspaces
    Return an array of WASDI Workspace JSON Objects.
    Each Object is like this
    {
        "ownerUserId":STRING,
        "sharedUsers":[STRING],
        "workspaceId":STRING,
        "workspaceName":STRING    
    }
    """    
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = __getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/byuser'
        
    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()
        return oJsonResult
    else:
        return None


def getWorkspaceIdByName(sName):
    """
    Get Id of a Workspace from the name
    Return the WorkspaceId as a String, '' if there is any error
    """    
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = __getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceName'] == sName:
                    return oWorkspace['workspaceId']
            except:
                return ''

    return ''


def openWorkspaceById(sWorkspaceId):
    """
    Open a workspace by Id
    return the WorkspaceId as a String, '' if there is any error
    """    
    global m_sActiveWorkspace
    
    m_sActiveWorkspace = sWorkspaceId
    
    return m_sActiveWorkspace


def getActiveWorkspaceId():
    """
    Get Active workspace Id
    return the WorkspaceId as a String, '' if there is any error
    """    
    global m_sActiveWorkspace
    
    return m_sActiveWorkspace


def openWorkspace(sWorkspaceName):
    """
    Open a workspace
    return the WorkspaceId as a String, '' if there is any error
    """    
    global m_sActiveWorkspace
    
    m_sActiveWorkspace = getWorkspaceIdByName(sWorkspaceName)
    
    return m_sActiveWorkspace


def getProductsByWorkspace(sWorkspaceName):
    """
    Get the list of products in a workspace
    the list is an array of string. Can be empty if there is any error
    """

    sWorkspaceId = getWorkspaceIdByName(sWorkspaceName)

    return getProductsByWorkspaceId(sWorkspaceId)


def getProductsByWorkspaceId(sWorkspaceId):
    """
    Get the list of products in a workspace (by Id)
    the list is an array of string. Can be empty if there is any error
    """
    global m_sBaseUrl
    global m_sActiveWorkspace

    m_sActiveWorkspace = sWorkspaceId

    asHeaders = __getStandardHeaders()
    payload = {'sWorkspaceId': sWorkspaceId}

    sUrl = m_sBaseUrl + '/product/byws'

    asProducts = []

    oResult = requests.get(sUrl, headers=asHeaders, params=payload)

    if oResult.ok is True:
        oJsonResults = oResult.json()

        for oProduct in oJsonResults:
            try:
                asProducts.append(oProduct['fileName'])
            except:
                continue

    return asProducts


def getProductsByActiveWorkspace():
    """
    Get the list of products in a workspace
    the list is an array of string. Can be empty if there is any error
    """
    global m_sActiveWorkspace

    return getProductsByWorkspaceId(m_sActiveWorkspace)


def getFullProductPath(sProductName):
    """
    Get the full local path of a product given the product name.
    Use the output of this API to open the file
    """    
    global m_sBasePath
    global m_sActiveWorkspace
    global m_sUser
    global m_bIsOnServer
    global m_bDownloadActive
    
    if m_bIsOnServer is True:
        sFullPath = '/data/wasdi/'
    else:
        sFullPath = m_sBasePath

    if not (sFullPath.endswith('/') or sFullPath.endswith('\\')):
        sFullPath = sFullPath + '/'
    
    sFullPath = sFullPath + m_sUser + '/' + m_sActiveWorkspace + '/' + sProductName

    if m_bIsOnServer is False:
        if m_bDownloadActive is True:
            if os.path.isfile(sFullPath) is False:
                # Download The File from WASDI
                print('LOCAL WASDI FILE MISSING: START DOWNLOAD... PLEASE WAIT')
                downloadFile(sProductName)
    
    return sFullPath


def getSavePath():
    """
    Get the local base save path for a product. To save use this path + fileName. Path already include '/' as last char
    """    
    global m_sBasePath
    global m_sActiveWorkspace
    global m_sUser

    if m_bIsOnServer is True:
        sFullPath = '/data/wasdi/'
    else:
        sFullPath = m_sBasePath

    if not (sFullPath.endswith('/') or sFullPath.endswith('\\')):
        sFullPath = sFullPath + '/'
    
    sFullPath = sFullPath + m_sUser + '/' + m_sActiveWorkspace + '/'
    
    return sFullPath


def getWorkflows():
    """
        Get the list of workflows for the user
        Return None if there is any error
        Return an array of WASI Workspace JSON Objects if everything is ok:
        
        {
            "description":STRING,
            "name": STRING,
            "workflowId": STRING
        }        
        
    """    
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = __getStandardHeaders()
    
    sUrl = m_sBaseUrl + '/processing/getgraphsbyusr'
        
    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResults = oResult.json()
        return oJsonResults
    else:
        return None


def executeWorkflow(sInputFileName, sOutputFileName, sWorkflowName):
    """
    Executes a WASDI SNAP Workflow
    return the Process Id if every thing is ok
    return '' if there was any problem
    """    
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace
    
    sWorkflowId = ''
    aoWorkflows = getWorkflows()
        
    for oWorkflow in aoWorkflows:
        try:
            if oWorkflow['name'] == sWorkflowName:
                sWorkflowId = oWorkflow['workflowId']
                break
        except:
            continue

    asHeaders = __getStandardHeaders()
    payload = {'workspace': m_sActiveWorkspace, 'source': sInputFileName, 'destination': sOutputFileName,
               'workflowId': sWorkflowId}

    sUrl = m_sBaseUrl + '/processing/graph_id'
    
    oResult = requests.get(sUrl, headers=asHeaders, params=payload)

    sProcessId = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResults = oResult.json()
        
        try:
            if oJsonResults['boolValue'] is True:
                sProcessId = oJsonResults['stringValue']
        except:
            return sProcessId
    
    return sProcessId


def getProcessStatus(sProcessId):
    """
    get the status of a Process
    return the status or '' if there was any error
    
    STATUS are  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
    """    
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = __getStandardHeaders()
    payload = {'sProcessId': sProcessId}
    
    sUrl = m_sBaseUrl + '/process/byid'
    
    oResult = requests.get(sUrl, headers=asHeaders, params=payload)
    
    sStatus = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()
        
        try:
            sStatus = oJsonResult['status']
        except:
            sStatus = ''
    
    return sStatus


def updateProcessStatus(sProcessId, sStatus, iPerc):
    """
    Update the status of a process
    return the updated status as a String or '' if there was any problem
    """    
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = __getStandardHeaders()
    payload = {'sProcessId': sProcessId, 'status': sStatus, 'perc': iPerc}

    if iPerc < 0:
        log('iPerc < 0 not valid')
        return ''
    elif iPerc > 100:
        print('iPerc > 100 not valid')
        return ''
    elif not (
            sStatus == 'CREATED' or
            sStatus == 'RUNNING' or
            sStatus == 'STOPPED' or
            sStatus == 'DONE' or
            sStatus == 'ERROR'
    ):
        print('sStatus must be a string like one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR')
        return ''
    elif sProcessId == '':
        return ''
    
    sUrl = m_sBaseUrl + '/process/updatebyid'
    
    oResult = requests.get(sUrl, headers=asHeaders, params=payload)
    
    sStatus = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()
        try:
            sStatus = oJsonResult['status']
        except:
            sStatus = ''
    
    return sStatus


def setProcessPayload(sProcessId, data):
    """
    Update the status of a process
    return the updated status as a String or '' if there was any problem
    """    
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = __getStandardHeaders()
    payload = {'sProcessId': sProcessId, 'payload': json.dumps(data)}
    
    sUrl = m_sBaseUrl + '/process/setpayload'
    
    oResult = requests.get(sUrl, headers=asHeaders, params=payload)
    
    sStatus = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()
        try:
            sStatus = oJsonResult['status']
        except:
            sStatus = ''
    
    return sStatus


def saveFile(sFileName):
    """
    Ingest a new file in the Active WASDI Workspace.
    The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS 
    To work be sure that the file is on the server
    """    
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    asHeaders = __getStandardHeaders()
    payload = {'file': sFileName, 'workspace': m_sActiveWorkspace}

    sUrl = m_sBaseUrl + '/catalog/upload/ingestinws'
    
    oResult = requests.get(sUrl, headers=asHeaders, params=payload)
    
    sProcessId = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()
        try:
            if oJsonResult['boolValue'] is True:
                sProcessId = oJsonResult['stringValue']
        except:
            sProcessId = ''

    return sProcessId


def downloadFile(sFileName):
    """
    Ingest a new file in the Active WASDI Workspace.
    The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS 
    To work be sure that the file is on the server
    """

    log('wasdi.downloadFile( ' + sFileName + ' )')

    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    asHeaders = __getStandardHeaders()
    payload = {'filename': sFileName}

    sUrl = m_sBaseUrl
    sUrl += '/catalog/downloadbyname?'
    sUrl += 'filename='
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()
    
    log('WASDI: send request to configured url ' + sUrl)
    
    oResponse = requests.get(sUrl, headers=asHeaders, params=payload, stream=True)

    if (oResponse is not None) and (oResponse.status_code == 200):
        log('WASDI: got ok result, downloading')
        sAttachmentName = None
        asResponseHeaders = oResponse.headers
        if asResponseHeaders is not None:
            if 'Content-Disposition' in asResponseHeaders:
                sContentDisposition = asResponseHeaders['Content-Disposition']
                sAttachmentName = sContentDisposition.split('filename=')[1]
                if (sAttachmentName[0] == '/') or (sAttachmentName[0] == '\\'):
                    sAttachmentName = sAttachmentName[1:]
                if (sAttachmentName[-1] == '/') or (sAttachmentName[-1] == '\\'):
                    sAttachmentName = sAttachmentName[:-1]
        sSavePath = getSavePath()
        sSavePath += sAttachmentName
        
        try:
            os.makedirs(os.path.dirname(sSavePath))
        except:  # Guard against race condition
            log('Error Creating File Path!!')
        
        log('WASDI: downloading local file ' + sSavePath)

        with open(sSavePath, 'wb') as oFile:
            for oChunk in oResponse:
                log('.')
                oFile.write(oChunk)
        log('WASDI: download Done new file locally available ' + sSavePath)

        if (sAttachmentName is not None) and\
                (sAttachmentName != sFileName) and\
                sAttachmentName.lower().endswith('.zip'):
            sPath = getSavePath()
            __unzip(sAttachmentName, sPath)

    else:
        log('WASDI: download error server code: ' + str(oResponse.status_code))
        
    return


def wasdiLog(sLogRow):
    
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    if m_bIsOnServer:
        asHeaders = __getStandardHeaders()
        sUrl = m_sBaseUrl + '/processors/logs/add?processworkspace=' + m_sMyProcId
        oResult = requests.post(sUrl, data=sLogRow, headers=asHeaders)

    else:
        print(sLogRow)    


def deleteProduct(sProduct):
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    asHeaders = __getStandardHeaders
    sUrl = m_sBaseUrl
    sUrl += "/product/delete?sProductName="
    sUrl += sProduct
    sUrl += "&bDeleteFile=true&sWorkspaceId="
    sUrl += m_sActiveWorkspace
    sUrl += "&bDeleteLayer=true"
    oResult = requests.get(sUrl, headers=asHeaders)

    return oResult.ok


def searchEOImages(sPlatform, sDateFrom, sDateTo,
                   dULLat, dULLon, dLRLat, dLRLon,
                   sProductType, iOrbitNumber,
                   sSensorOperationalMode, sCloudCoverage):
    """
    Search EO images

    :param sPlatform: satellite platform (S1 or S2)
    :param sDateFrom: inital date
    :param sDateTo: final date
    :param dULLat: Latitude of Upper-Left corner
    :param dULLon: Longitude of Upper-Left corner
    :param dLRLat: Latitude of Lower-Right corner
    :param dLRLon: Longitude of Lower-Right corner
    :param sProductType: type of EO product
    :param iOrbitNumber: orbit number
    :param sSensorOperationalMode: sensor operational mode
    :param sCloudCoverage: interval of allowed cloud coverage, e.g. "[0 TO 22.5]"
    :return: a list of results
    """
    aoReturnList = []

    if sPlatform is None:
        log("searchEOImages: platform cannot be None")
        return aoReturnList

    # todo support other platforms
    if (sPlatform != "S1") and (sPlatform != "S2"):
        log("searchEOImages: platform must be S1 or S2. Received [" + sPlatform + "]")
        return aoReturnList

    if sPlatform == "S1":
        if sProductType is not None:
            if not (sProductType == "SLC" or sProductType == "GRD" or sProductType == "OCN"):
                log("searchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" + sProductType + "]")

    if sPlatform == "S2":
        if sProductType is not None:
            if not (sProductType == "S2MSI1C" or sProductType == "S2MSI2Ap" or sProductType == "S2MSI2A"):
                log("searchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received ["
                    + sProductType + "]")

    if sDateFrom is None:
        log("searchEOImages: sDateFrom cannot be None")
        return aoReturnList

    # if (len(sDateFrom) < 10) or (sDateFrom[4] != '-') or (sDateFrom[7] != '-'):
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateFrom)):
        log("searchEOImages: sDateFrom must be in format YYYY-MM-DD")
        return aoReturnList

    if sDateTo is None:
        log("searchEOImages: sDateTo cannot be None")
        return aoReturnList

    # if len(sDateTo) < 10 or sDateTo[4] != '-' or sDateTo[7] != '-':
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateTo)):
        log("searchEOImages: sDateTo must be in format YYYY-MM-DD")
        return aoReturnList

    # create query string:

    # platform name
    sQuery = "( platformname:"
    if sPlatform == "S2":
        sQuery += "Sentinel-2 "
    elif sPlatform == "S1":
        sQuery += "Sentinel-1"

    # If available add product type
    if sProductType is not None:
        sQuery += " AND producttype:" + sProductType

    # If available Sensor Operational Mode
    if (sSensorOperationalMode is not None) and (sPlatform == "S1"):
        sQuery += " AND sensoroperationalmode:" + sSensorOperationalMode

    # If available cloud coverage
    if (sCloudCoverage is not None) and (sCloudCoverage == "S2"):
        sQuery += " AND cloudcoverpercentage:" + sCloudCoverage

    # If available add orbit number
    if iOrbitNumber is not None:
        sQuery += " AND relativeorbitnumber:" + iOrbitNumber

    # Close the first block
    sQuery += ") "

    # Date Block
    sQuery += "AND ( beginPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]"
    sQuery += "AND ( endPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]"

    # Close the second block
    sQuery += ") "

    # footprint polygon
    if (dULLat is not None) and (dULLon is not None) and (dLRLat is not None) and (dLRLon is not None):
        sFootPrint = "( footprint:\"intersects(POLYGON(( " + dULLon + " " +dLRLat + "," +\
                     dULLon + " " + dULLat + "," + dLRLon + " " + dULLat + "," + dLRLon +\
                     " " + dLRLat + "," + dULLon + " " +dLRLat + ")))\") AND ";
    sQuery = sFootPrint + sQuery

    sQueryBody = "[\"" + sQuery.replace("\"", "\\\"") + "\"]";
    sQuery = "sQuery=" + sQuery + "&offset=0&limit=10&providers=ONDA"

    try:
        sUrl = getBaseUrl() + "/search/querylist?" + sQuery
        # todo write standard headers, maybe make a function
        asHeaders = __getStandardHeaders()
        oResponse = requests.post(sUrl, data=sQueryBody, headers=asHeaders)
        try:
            # populate list from response
            oJsonResponse = oResponse.json()
            aoReturnList = oJsonResponse
        except Exception as oEx:
            log('[ERROR] waspy.searchEOImages: exception while trying to convert response into JSON object')
            raise

        log("" + repr(aoReturnList))
        return aoReturnList
    except Exception as oEx:
        log(type(oEx))
        traceback.print_exc()
        log(oEx)

    return aoReturnList


def __fileExistsOnWasdi(sFileName):
    """
    checks hwther a file already exists on WASDI or not
    :param sFileName: file name. Warning: must be the complete path
    :return: True if the file exists, False otherwise
    """
    if sFileName is None:
        raise TypeError('File name must not be None')
    if len(sFileName) < 1:
        raise ValueError('File name too short')

    sBaseUrl = getBaseUrl()
    sSessionId = getSessionId()
    sActiveWorkspace = getActiveWorkspaceId()

    sUrl = m_sBaseUrl
    sUrl += "/catalog/checkdownloadavaialibitybyname?token="
    sUrl += sSessionId
    sUrl += "&filename="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += sActiveWorkspace

    asHeaders = __getStandardHeaders()
    oResult = requests.get(sUrl, headers=asHeaders)

    return oResult.ok


def __unzip(sAttachmentName, sPath):
    """
    Unzips a file
    :param sAttachmentName: filename to unzip
    :param sPath: both the path where the file is and where it must be unzipped
    :return:
    """
    log('wasdi.__unzip( ' + sAttachmentName + ', ' + sPath + ' )')
    if sPath is None:
        raise TypeError('No path no party!')
    if sAttachmentName:
        raise TypeError('No attachment to unzip!')

    sZipFilePath = sPath + sAttachmentName
    zip_ref = zipfile.ZipFile(sZipFilePath, 'r')
    zip_ref.extractall(sPath)
    zip_ref.close()


if __name__ == '__main__':
    log('WASPY - The WASDI Python Library. Include in your code for space development processors. Visit www.wasdi.net')