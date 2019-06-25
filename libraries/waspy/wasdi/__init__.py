"""
This is WASPY, the WASDI Python lib.
The methods allow to interact with WASDI seamlessly.
Note:
the philosophy of safe programming is adopted as widely as possible, the lib will try to workaround issues such as
faulty input, and print an error rather than raise an exception, so that your program can possibly go on. Please check
the return statues


Created on 11 Jun 2018

@author: p.campanella
"""
# from pandas._libs.join import outer_join_indexer

name = "wasdi"

import requests
import os
import json
import traceback
import re
import zipfile
import time

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
    """
    Prints status
    """
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

    _log('[INFO] user: ' + str(getUser()))
    _log('[INFO] password: ' + str(getPassword()))
    _log('[INFO] active workspace: ' + str(getActiveWorkspaceId()))
    _log('[INFO] parameters file path: ' + str(getParametersFilePath()))
    _log('[INFO] session id: ' + str(getSessionId()))
    _log('[INFO] base path: ' + str(getBasePath()))
    _log('[INFO] download active: ' + str(getDownloadActive()))
    _log('[INFO] upload active: ' + str(getUploadActive()))
    _log('[INFO] verbose: ' + str(getVerbose()))
    _log('[INFO] param dict: ' + str(getParametersDict()))
    _log('[INFO] proc id: ' + str(getProcId()))
    _log('[INFO] base url: ' + str(getBaseUrl()))
    _log('[INFO] is on server: ' + str(getIsOnServer()))


def setVerbose(bVerbose):
    if bVerbose is None:
        print('[ERROR] waspy.setVerbose: passed None, won\'t change')
        return
    if not isinstance(bVerbose, bool):
        print('[ERROR] waspy.setVerbose: passed non boolean, trying to convert')
        try:
            bVerbose = bool(bVerbose)
        except:
            print('[ERROR] waspy.setVerbose: cannot convert argument into boolean, won\'t change')
            return

    global m_bVerbose
    m_bVerbose = bVerbose


def getVerbose():
    global m_bVerbose
    return m_bVerbose


def getParametersDict():
    """
    Get the full Params Dictionary
    :return: a dictionary containing the parameters
    """
    global m_aoParamsDictionary
    return m_aoParamsDictionary


def addParameter(sKey, oValue):
    """
    Adds a parameter
    :param sKey: parameter key
    :param oValue: parameter value
    """
    global m_aoParamsDictionary
    m_aoParamsDictionary[sKey] = oValue


def getParameter(sKey):
    """
    Gets a parameter using its key
    :param sKey: parameter key
    :return: parameter value
    """
    global m_aoParamsDictionary
    try:
        return m_aoParamsDictionary[sKey]
    except:
        return None


def setUser(sUser):
    """
    Sets the WASDI User
    :param sUser: WASDI UserID
    :return:
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


def setParametersFilePath(sParamPath):
    if sParamPath is None:
        print('[ERROR] waspy.setParametersFilePath: passed None as path, won\'t change')
        return
    if len(sParamPath) < 1:
        print('[ERROR] waspy.setParametersFilePath: string passed has zero length, won\'t change')
        return

    global m_sParametersFilePath
    m_sParametersFilePath = sParamPath


def getParametersFilePath():
    global m_sParametersFilePath
    return m_sParametersFilePath


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
    When in development, set True to download locally files from Server.
    Set it to false to NOT donwload data. In this case the developer must check the availability of the files
    """

    if bDownloadActive is None:
        print('[ERROR] waspy.setDownloadActive: passed None, won\'t change')
        return

    global m_bDownloadActive
    m_bDownloadActive = bDownloadActive


def getDownloadActive():
    """
    Get the WASDI API URL
    """
    global m_bDownloadActive
    return m_bDownloadActive


def setUploadActive(bUploadActive):
    """
    When in development, set True to upload local files on Server.
    Set it to false to NOT upload data. In this case the developer must check the availability of the files
    """

    if bUploadActive is None:
        print('[ERROR] waspy.setUploadActive: passed None, won\'t change')
        return

    global m_bUploadActive
    m_bUploadActive = bUploadActive


def getUploadActive():
    """
    Get the WASDI API URL
    """
    global m_bUploadActive
    return m_bUploadActive


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


def _log(sLog):
    if m_bVerbose:
        print(sLog)


def _getStandardHeaders():
    global m_sSessionId
    asHeaders = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    return asHeaders


def _loadConfig(sConfigFilePath):
    """
    Loads configuration from given file
    :param sConfigFilePath: a string containing a path to the configuration file
    """
    if sConfigFilePath is None:
        print("[ERROR] waspy._loadConfigParams: config parameter file name is None, cannot load config")
        return
    if sConfigFilePath == '':
        print("[ERROR] waspy._loadConfigParams: config parameter file name is empty, cannot load config")
        return

    sConfigFilePath = _normPath(sConfigFilePath)

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
                m_sParametersFilePath = _normPath(m_sParametersFilePath)
            if "DOWNLOADACTIVE" in oJson:
                m_bDownloadActive = bool(oJson["DOWNLOADACTIVE"])
            if "UPLOADACTIVE" in oJson:
                m_bUploadActive = bool(oJson["UPLOADACTIVE"])
            if "VERBOSE" in oJson:
                m_bVerbose = bool(oJson["VERBOSE"])

        return True, sTempWorkspaceName, sTempWorkspaceID

    except Exception as oEx:
        print('[ERROR] waspy._loadConfigParams: something went wrong')
        return


def _loadParams():
    """
    Loads parameters from file, if specified in configuration file
    """
    global m_sParametersFilePath
    global m_aoParamsDictionary

    if (m_sParametersFilePath is not None) and (m_sParametersFilePath != ''):
        with open(m_sParametersFilePath) as oJsonFile:
            m_aoParamsDictionary = json.load(oJsonFile)
    else:
        _log('[INFO] wasdi could not load param file. That is fine, you can still load it later, don\'t worry')


def refreshParameters():
    """
    Refresh parameters, reading the file again
    """
    _loadParams()


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
        bConfigOk, sWname, sWId = _loadConfig(sConfigFilePath)
        if bConfigOk is True:
            _loadParams()

    if m_sUser is None:
        print('[ERROR] waspy.init: must initialize user first, but None given')
        return False

    if m_sBasePath is None:
        if m_bIsOnServer is True:
            m_sBasePath = '/data/wasdi/'
        else:
            sHome = os.path.expanduser("~")
            # the empty string at the end adds a separator
            m_sBasePath = os.path.join(sHome, ".wasdi", "")

    if m_sSessionId != '':
        asHeaders = _getStandardHeaders()
        sUrl = m_sBaseUrl + '/auth/checksession'
        oResponse = requests.get(sUrl, headers=asHeaders)
        if (oResponse is not None) and (oResponse.ok is True):
            oJsonResult = oResponse.json()
            try:
                sUser = str(oJsonResult['userId'])
                if sUser == m_sUser:
                    bResult = True
                else:
                    bResult = False
            except:
                bResult = False
        else:
            bResult = False
    else:
        if m_sPassword is None:
            print('[ERROR] waspy.init: must initialize password first, but None given')
            return False
        asHeaders = {'Content-Type': 'application/json'}
        sUrl = m_sBaseUrl + '/auth/login'
        sPayload = '{"userId":"' + m_sUser + '","userPassword":"' + m_sPassword + '" }'
        oResponse = requests.post(sUrl, data=sPayload, headers=asHeaders)
        if oResponse is None:
            print('[ERROR] waspy.init: cannot authenticate')
            bResult = False
        elif oResponse.ok is not True:
            print('[ERROR] waspy.init: cannot authenticate, server replied: ' + str(oResponse.status_code))
            bResult = False
        else:
            oJsonResult = oResponse.json()
            try:
                m_sSessionId = str(oJsonResult['sessionId'])
                bResult = True
            except:
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
    :return: the hello message as Text
    """
    global m_sBaseUrl

    sUrl = m_sBaseUrl + '/wasdi/hello'
    oResult = requests.get(sUrl)
    return oResult.text


def getWorkspaces():
    """
    Get List of user workspaces
    :return: an array of WASDI Workspace JSON Objects.
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

    asHeaders = _getStandardHeaders()

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

    asHeaders = _getStandardHeaders()

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


def setActiveWorkspaceId(sActiveWorkspace):
    global m_sActiveWorkspace
    m_sActiveWorkspace = sActiveWorkspace


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
    asHeaders = _getStandardHeaders()
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

    sFullPath = os.path.join(sFullPath, m_sUser, m_sActiveWorkspace, sProductName)

    if m_bIsOnServer is False:
        if m_bDownloadActive is True:
            if os.path.isfile(sFullPath) is False:
                # Download The File from WASDI
                print('[INFO] waspy.getFullProductPath: LOCAL WASDI FILE MISSING: START DOWNLOAD... PLEASE WAIT')
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

    # empty string at the ends adds a final separator
    sFullPath = os.path.join(sFullPath, m_sUser, m_sActiveWorkspace, "")

    return sFullPath


def getWorkflows():
    """
        Get the list of workflows for the user
        Return None if there is any error
        Return an array of WASDI Workspace JSON Objects if everything is ok, None otherwise. The format is as follows:

        {
            "description":STRING,
            "name": STRING,
            "workflowId": STRING
        }

    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/processing/getgraphsbyusr'

    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResults = oResult.json()
        return oJsonResults
    else:
        return None


def getProcessStatus(sProcessId):
    """
    get the status of a Process
    return the status or '' if there was any error

    STATUS are  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()
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

    if sProcessId is None:
        print('[ERROR] waspy.updateProcessStatus: cannot update status, process ID is None')
        return ''
    if sStatus is None:
        print('[ERROR] waspy.updateProcessStatus: cannot update status, status is None')
        return ''
    if iPerc is None:
        print('[ERROR] waspy.updateProcessStatus: percentage is None')
        return ''

    if iPerc < 0:
        print('[ERROR] waspy.updateProcessStatus: iPerc < 0 not valid')
        return ''
    elif iPerc > 100:
        print('[ERROR] waspy.updateProcessStatus: iPerc > 100 not valid')
        return ''
    elif sStatus not in {'CREATED', 'RUNNING', 'STOPPED', 'DONE', 'ERROR'}:
        print(
            '[ERROR] waspy.updateProcessStatus: sStatus must be a string in: {CREATED,  RUNNING,  STOPPED,  DONE,  ERROR')
        return ''
    elif sProcessId == '':
        return ''

    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()
    payload = {'sProcessId': sProcessId, 'status': sStatus, 'perc': iPerc}

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


def updateProgressPerc(iPerc):
    _log('[INFO] waspy.updateProgressPerc( ' + str(iPerc) + ' )')
    if iPerc is None:
        print('[ERROR] waspy.updateProgressPerc: Passed None, expected a percentage')
        return ''

    if (getProcId() is None) or (len(getProcId()) < 1):
        print('[ERROR] waspy.updateProgressPerc: Cannot update progress: process ID is not known')
        return ''

    if 0 > iPerc or 100 < iPerc:
        print('[WARNING] waspy.updateProgressPerc: passed' + str(iPerc) + ', automatically resetting in [0, 100]')
        if iPerc < 0:
            iPerc = 0
        if iPerc > 100:
            iPerc = 100

    sStatus = "RUNNING"
    sUrl = getBaseUrl() + "/process/updatebyid?sProcessId=" + getProcId() + "&status=" + sStatus + "&perc=" + str(iPerc) + "&sendrabbit=1"
    asHeaders = _getStandardHeaders()
    oResponse = requests.get(sUrl, headers=asHeaders)
    sResult = ""
    if (oResponse is not None) and (oResponse.ok is True):
        oJson = oResponse.json()
        if (oJson is not None) and ("status" in oJson):
            sResult = str(oJson['status'])
    else:
        print('[ERROR] waspy.updateProgressPerc: could not update progress')
    return sResult


def setProcessPayload(sProcessId, data):
    """
    Update the status of a process
    return the updated status as a String or '' if there was any problem
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()
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

    asHeaders = _getStandardHeaders()
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

    _log('[INFO] waspy.downloadFile( ' + sFileName + ' )')

    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    asHeaders = _getStandardHeaders()
    payload = {'filename': sFileName}

    sUrl = m_sBaseUrl
    sUrl += '/catalog/downloadbyname?'
    sUrl += 'filename='
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()

    _log('[INFO] waspy.downloadfile: send request to configured url ' + sUrl)

    oResponse = requests.get(sUrl, headers=asHeaders, params=payload, stream=True)

    if (oResponse is not None) and (oResponse.status_code == 200):
        _log('[INFO] waspy.downloadFile: got ok result, downloading')
        sAttachmentName = None
        asResponseHeaders = oResponse.headers
        if asResponseHeaders is not None:
            if 'Content-Disposition' in asResponseHeaders:
                sContentDisposition = asResponseHeaders['Content-Disposition']
                sAttachmentName = sContentDisposition.split('filename=')[1]
                bLoop = True
                while bLoop is True:
                    if sAttachmentName[0] == '.':
                        sAttachmentName = sAttachmentName[1:]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[0] == '/') or (sAttachmentName[0] == '\\'):
                        sAttachmentName = sAttachmentName[1:]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[-1] == '/') or (sAttachmentName[-1] == '\\'):
                        sAttachmentName = sAttachmentName[:-1]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[0] == '\"') or (sAttachmentName[0] == '\''):
                        sAttachmentName = sAttachmentName[1:]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[-1] == '\"') or (sAttachmentName[-1] == '\''):
                        sAttachmentName = sAttachmentName[:-1]
                        bLoop = True
                    else:
                        bLoop = False
        sSavePath = getSavePath()
        sSavePath = os.path.join(sSavePath, sAttachmentName)

        try:
            os.makedirs(os.path.dirname(sSavePath))
        except:  # Guard against race condition
            print('[ERROR] waspy.downloadFile Creating File Path!!')

        _log('[INFO] waspy.downloadFile: downloading local file ' + sSavePath)

        with open(sSavePath, 'wb') as oFile:
            for oChunk in oResponse:
                # _log('.')
                oFile.write(oChunk)
        _log('[INFO] waspy.downloadFile: download done, new file locally available ' + sSavePath)

        if (sAttachmentName is not None) and \
                (sAttachmentName != sFileName) and \
                sAttachmentName.lower().endswith('.zip'):
            sPath = getSavePath()
            _unzip(sAttachmentName, sPath)

    else:
        print('[ERROR] waspy.downloadFile: download error, server code: ' + str(oResponse.status_code))

    return


def wasdiLog(sLogRow):
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    if m_bIsOnServer:
        asHeaders = _getStandardHeaders()
        sUrl = m_sBaseUrl + '/processors/logs/add?processworkspace=' + m_sMyProcId
        oResult = requests.post(sUrl, data=sLogRow, headers=asHeaders)
        if oResult is None:
            print('[WARNING] waspy.wasdiLog: could not log')
        elif oResult.ok is not True:
            print('[WARNING] waspy.wasdiLog: could not log, server returned: ' + str(oResult.status_code))
    else:
        print(sLogRow)


def deleteProduct(sProduct):
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    if sProduct is None:
        print('[ERROR] waspy.deleteProduct: product passed is None')

    asHeaders = _getStandardHeaders
    sUrl = m_sBaseUrl
    sUrl += "/product/delete?sProductName="
    sUrl += sProduct
    sUrl += "&bDeleteFile=true&sWorkspaceId="
    sUrl += m_sActiveWorkspace
    sUrl += "&bDeleteLayer=true"
    oResult = requests.get(sUrl, headers=asHeaders)

    if oResult is None:
        print('[ERROR] waspy.deleteProduct: deletion failed')
        return False
    elif oResult.ok is not True:
        print('[ERROR] waspy.deleteProduct: deletion failed, server returned: ' + str(oResult.status_code))
    else:
        return oResult.ok


def searchEOImages(sPlatform, sDateFrom, sDateTo,
                   fULLat=None, fULLon=None, fLRLat=None, fLRLon=None,
                   sProductType=None, iOrbitNumber=None,
                   sSensorOperationalMode=None, sCloudCoverage=None):
    """
    Search EO images

    :param sPlatform: satellite platform (S1 or S2)
    :param sDateFrom: inital date YYYY-MM-DD
    :param sDateTo: final date YYYY-MM-DD
    :param fULLat: Latitude of Upper-Left corner
    :param fULLon: Longitude of Upper-Left corner
    :param fLRLat: Latitude of Lower-Right corner
    :param fLRLon: Longitude of Lower-Right corner
    :param sProductType: type of EO product
    :param iOrbitNumber: orbit number
    :param sSensorOperationalMode: sensor operational mode
    :param sCloudCoverage: interval of allowed cloud coverage, e.g. "[0 TO 22.5]"
    :return: a list of results
    """
    aoReturnList = []

    if sPlatform is None:
        print("[ERROR] waspy.searchEOImages: platform cannot be None")
        return aoReturnList

    # todo support other platforms
    if (sPlatform != "S1") and (sPlatform != "S2"):
        print("[ERROR] waspy.searchEOImages: platform must be S1 or S2. Received [" + sPlatform + "]")
        return aoReturnList

    if sPlatform == "S1":
        if sProductType is not None:
            if not (sProductType == "SLC" or sProductType == "GRD" or sProductType == "OCN"):
                print("[ERROR] waspy.searchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" +
                      sProductType + "]")
                return aoReturnList

    if sPlatform == "S2":
        if sProductType is not None:
            if not (sProductType == "S2MSI1C" or sProductType == "S2MSI2Ap" or sProductType == "S2MSI2A"):
                print(
                    "[ERROR] waspy.searchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received ["
                    + sProductType + "]")
                return aoReturnList

    if sDateFrom is None:
        print("[ERROR] waspy.searchEOImages: sDateFrom cannot be None")
        return aoReturnList

    # if (len(sDateFrom) < 10) or (sDateFrom[4] != '-') or (sDateFrom[7] != '-'):
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateFrom)):
        print("[ERROR] waspy.searchEOImages: sDateFrom must be in format YYYY-MM-DD")
        return aoReturnList

    if sDateTo is None:
        print("[ERROR] waspy.searchEOImages: sDateTo cannot be None")
        return aoReturnList

    # if len(sDateTo) < 10 or sDateTo[4] != '-' or sDateTo[7] != '-':
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateTo)):
        print("[ERROR] waspy.searchEOImages: sDateTo must be in format YYYY-MM-DD")
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
        sQuery += " AND producttype:" + str(sProductType)

    # If available Sensor Operational Mode
    if (sSensorOperationalMode is not None) and (sPlatform == "S1"):
        sQuery += " AND sensoroperationalmode:" + str(sSensorOperationalMode)

    # If available cloud coverage
    if (sCloudCoverage is not None) and (sCloudCoverage == "S2"):
        sQuery += " AND cloudcoverpercentage:" + str(sCloudCoverage)

    # If available add orbit number
    if iOrbitNumber is not None:
        if isinstance(iOrbitNumber, int):
            sQuery += " AND relativeorbitnumber:" + str(iOrbitNumber)
        else:
            print('[WARNING] waspy.searchEOImages: iOrbitNumber is' + str(iOrbitNumber),
                  ', but it should be an integer')
            try:
                iTmp = int(iOrbitNumber)
                print('[WARNING] waspy.searchEOImages: iOrbitNumber converted to: ' + str(iTmp))
                sQuery += str(iTmp)
            except:
                print('[ERROR] waspy.searchEOImages: could not convert iOrbitNumber to an int, ignoring it')

            # Close the first block
    sQuery += ") "

    # Date Block
    sQuery += "AND ( beginPosition:[" + str(sDateFrom) + "T00:00:00.000Z TO " + str(sDateTo) + "T23:59:59.999Z]"
    sQuery += "AND ( endPosition:[" + str(sDateFrom) + "T00:00:00.000Z TO " + str(sDateTo) + "T23:59:59.999Z]"

    # Close the second block
    sQuery += ") "

    # footprint polygon
    if (fULLat is not None) and (fULLon is not None) and (fLRLat is not None) and (fLRLon is not None):
        sFootPrint = "( footprint:\"intersects(POLYGON(( " + str(fULLon) + " " + str(fLRLat) + "," + \
                     str(fULLon) + " " + str(fULLat) + "," + str(fLRLon) + " " + str(fULLat) + "," + str(fLRLon) + \
                     " " + str(fLRLat) + "," + str(fULLon) + " " + str(fLRLat) + ")))\") AND "
    sQuery = sFootPrint + sQuery

    sQueryBody = "[\"" + sQuery.replace("\"", "\\\"") + "\"]"
    sQuery = "sQuery=" + sQuery + "&offset=0&limit=10&providers=ONDA"

    try:
        sUrl = getBaseUrl() + "/search/querylist?" + sQuery
        asHeaders = _getStandardHeaders()
        oResponse = requests.post(sUrl, data=sQueryBody, headers=asHeaders)
        try:
            # populate list from response
            oJsonResponse = oResponse.json()
            aoReturnList = oJsonResponse
        except Exception as oEx:
            print('[ERROR] waspy.searchEOImages: exception while trying to convert response into JSON object')
            return aoReturnList

        _log("[INFO] waspy.searchEOImages: search results:\n" + repr(aoReturnList))
        return aoReturnList
    except Exception as oEx:
        print('[ERROR] waspy.searchEOImages: an error occured')
        _log(type(oEx))
        traceback.print_exc()
        _log(oEx)

    return aoReturnList


def getFoundProductName(aoProduct):
    if aoProduct is None:
        print('[ERROR] waspy.getFoundProductName: product is None, aborting')
        return ''
    elif "title" not in aoProduct:
        print('[ERROR] waspy.getFoundProductName: title not found in product, aborting')
        return ''
    else:
        return aoProduct['title']


def _fileExistsOnWasdi(sFileName):
    """
    checks hwther a file already exists on WASDI or not
    :param sFileName: file name. Warning: must be the complete path
    :return: True if the file exists, False otherwise
    """
    if sFileName is None:
        print('[ERROR] waspy._fileExistsOnWasdi: file name must not be None')
        return False
    if len(sFileName) < 1:
        print('[ERROR] waspy._fileExistsOnWasdi: File name too short')
        return False

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

    asHeaders = _getStandardHeaders()
    oResult = requests.get(sUrl, headers=asHeaders)

    if oResult is None:
        print('[ERROR] waspy._fileExistsOnWasdi: failed contacting the server')
        return False
    elif oResult.ok is not True:
        print('[ERROR] waspy._fileExistsOnWasdi: failed, server returned: ' + str(oResult.status_code))
        return False
    else:
        return oResult.ok


def _unzip(sAttachmentName, sPath):
    """
    Unzips a file
    :param sAttachmentName: filename to unzip
    :param sPath: both the path where the file is and where it must be unzipped
    :return:
    """
    _log('waspy._unzip( ' + sAttachmentName + ', ' + sPath + ' )')
    if sPath is None:
        print('[ERROR] waspy._unzip: path is None')
        return
    if sAttachmentName is None:
        print('[ERROR] waspy._unzip: attachment to unzip is None')
        return

    try:
        sZipFilePath = os.path.join(sPath, sAttachmentName)
        zip_ref = zipfile.ZipFile(sZipFilePath, 'r')
        zip_ref.extractall(sPath)
        zip_ref.close()
    except:
        print('[ERROR] waspy._unzip: failed unzipping')

    return


# todo split into 2 functions
def importProduct(sFileUrl=None, sBoundingBox=None, asProduct=None):
    """
    Imports a product from a Provider in WASDI
    :param sFileUrl:
    :param sBoundingBox:
    :return: execution status
    """

    _log('waspy.importProduct( ' + str(sFileUrl) + ', ' + str(sBoundingBox) + ', ' + str(asProduct) + ' )')

    sReturn = "ERROR"

    if sFileUrl is None:
        if asProduct is not None and "link" in asProduct:
            sFileUrl = asProduct["link"]
            if "footprint" in asProduct:
                sBoundingBox = asProduct["footprint"]
        else:
            print('[ERROR] waspy.importProduct: cannot import product without url or a dict containing the link')
            return ''

    if sFileUrl is None:
        print('[ERROR] waspy.importProduct: cannot detect the link to get the requested product')
        return ''

    sUrl = getBaseUrl()
    sUrl += "/filebuffer/download?sFileUrl="
    sUrl += sFileUrl
    sUrl += "&sProvider=ONDA&sWorkspaceId="
    sUrl += getActiveWorkspaceId()
    sUrl += "&sBoundingBox="
    sUrl += sBoundingBox

    asHeaders = _getStandardHeaders()

    oResponse = requests.get(sUrl, headers=asHeaders)
    if oResponse is None:
        print('[ERROR] waspy.importProduct: cannot import product')
    elif oResponse.ok is not True:
        print('[ERROR] waspy.importProduct: cannot import product, server returned: ' + str(oResponse.status_code))
    else:
        oJsonResponse = oResponse.json()
        if ("boolValue" in oJsonResponse) and (oJsonResponse["boolValue"] is True):
            if "stringValue" in oJsonResponse:
                sProcessId = str(oJsonResponse["stringValue"])
                sReturn = waitProcess(sProcessId)

    return sReturn


def asynchExecuteProcessor(sProcessorName, aoParams={}):
    """
    Execute a WASDI processor asynchronously
    :param sProcessorName: WASDI processor name
    :param aoParams: a dictionary of parameters for the processor
    :return: processor ID
    """

    _log('[INFO] waspy.asynchExecuteProcessor( ' + str(sProcessorName) + ', ' + str(aoParams) + ' )')

    if sProcessorName is None:
        print('[ERROR] waspy.asynchExecuteProcessor: processor name is None, aborting')
        return ''
    elif len(sProcessorName) <= 0:
        print('[ERROR] waspy.asynchExecuteProcessor: processor name empty, aborting')
        return ''
    if isinstance(aoParams, dict) is not True:
        print('[ERROR] waspy.asynchExecuteProcessor: parameters must be a dictionary but it is not, aborting')
        return ''

    sUrl = getBaseUrl() + \
           "/processors/run?workspace=" + getActiveWorkspaceId() + \
           "&name=" + sProcessorName + \
           "&encodedJson=" + str(aoParams)
    asHeaders = _getStandardHeaders()
    oResponse = requests.get(sUrl, headers=asHeaders, params=aoParams)
    if oResponse is None:
        print('[ERROR] waspy.asynchExecuteProcessor: something broke when contacting the server, aborting')
        return ''
    elif oResponse.ok is True:
        _log('[INFO] waspy.asynchExecuteProcessor: API call OK')
        aoJson = oResponse.json()
        if "processingIdentifier" in aoJson:
            sProcessID = aoJson['processingIdentifier']
            return sProcessID
        else:
            print('[ERROR] waspy.asynchExecuteProcessor: cannot extract processing identifier from response, aborting')
    else:
        print('[ERROR] waspy.asynchExecuteProcessor: server returned status ' + str(oResponse.status_code))

    return ''


def executeProcessor(sProcessorName, aoProcessParams):
    """
    Executes a WASDI Processor
    return the Process Id if every thing is ok
    return '' if there was any problem
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    sEncodedParams = json.dumps(aoProcessParams)
    asHeaders = _getStandardHeaders()
    payload = {'workspace': m_sActiveWorkspace,
               'name': sProcessorName,
               'sProcessorName': sEncodedParams}

    sUrl = m_sBaseUrl + '/processors/run'

    oResult = requests.get(sUrl, headers=asHeaders, params=payload)

    sProcessId = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResults = oResult.json()

        try:
            sProcessId = oJsonResults['processingIdentifier']
        except:
            return sProcessId

    return sProcessId


# todo extend to a list of processes
def waitProcess(sProcessId):
    if sProcessId is None:
        _log('Passed None, expected a process ID')
        return "ERROR"

    if sProcessId == '':
        _log('Passed empty, expected a process ID')
        return "ERROR"

    sStatus = ''

    while sStatus not in {"DONE", "STOPPED", "ERROR"}:
        sStatus = getProcessStatus(sProcessId)
        time.sleep(2)

    return sStatus


def uploadFile(sFileName):
    """
    Uploads a file to WASDI
    :param sFileName: name of file inside working directory OR path to file RELATIVE to working directory
    :return: True if succeded, False otherwise
    """

    _log('[INFO] waspy.upload( ' + str(sFileName) + ' )')

    if sFileName is None:
        print('[ERROR] waspy.upload: the given file name is None, cannot upload')
        return False
    if sFileName.startswith('.'):
        sFileName = sFileName[1:]
    if sFileName.startswith('/') or sFileName.startswith('\\'):
        sFileName = sFileName[1:]

    sFileName = _normPath(sFileName)

    bResult = False

    sBasePath = getBasePath()
    sFullPath = os.path.join(sBasePath, sFileName)

    return bResult


def _normPath(sPath):
    """
    Normalizes path by adjusting separator
    :param sPath: a path to be normalized
    :return: the normalized path
    """

    if sPath is None:
        print('[ERROR] waspy._normPath: passed path is None')
        return None

    sPath = sPath.replace('/', os.path.sep)
    sPath = sPath.replace('\\', os.path.sep)

    return sPath


def addFileToWASDI(sFileName):
    return _internalAddFileToWASDI(sFileName, False)


def asynchAddFileToWASDI(sFileName):
    return _internalAddFileToWASDI(sFileName, True)


def _internalAddFileToWASDI(sFileName, bAsynch=None):
    _log('[INFO] waspy._internalAddFileToWASDI( ' + str(sFileName) + ', ' + str(bAsynch) + ' )')

    if sFileName is None:
        print('[ERROR] waspy._internalAddFileToWASDI: file name is None, aborting')
        return ''
    if not isinstance(sFileName, str):
        print('[WARNING] waspy._internalAddFileToWASDI: file name is not a string, trying conversion')
        try:
            sFileName = str(sFileName)
        except:
            print('[ERROR] waspy._internalAddFileToWASDI: cannot convert file name into string, aborting')
            return ''
    if len(sFileName) < 1:
        print('[ERROR] waspy._internalAddFileToWASDI: file name has zero length, aborting')
        return ''

    if bAsynch is None:
        print('[WARNING] waspy._internalAddFileToWASDI: asynch flag is None, assuming False')
        bAsynch = False
    if not isinstance(bAsynch, bool):
        print('[WARNING] waspy._internalAddFileToWASDI: asynch flag is not a boolean, trying conversion')
        try:
            bAsynch = bool(bAsynch)
        except:
            print('[ERROR] waspy._internalAddFileToWASDI: could not convert asynch flag into bool, aborting')
            return ''

    sResult = ''
    try:
        if getUploadActive() is True:
            sFilePath = os.path.join(getSavePath(), sFileName)
            if _fileExistsOnWasdi(sFilePath) is False:
                _log('[INFO] waspy._internalAddFileToWASDI: remote file is missing, uploading')
                try:
                    uploadFile(sFileName)
                    _log('[INFO] waspy._internalAddFileToWASDI: file uploaded, keep on working!')
                except:
                    print('[ERROR] waspy._internalAddFileToWASDI: could not proceed with upload')

        sUrl = getBaseUrl() + "/catalog/upload/ingestinws?file=" + sFileName + "&workspace=" + getActiveWorkspaceId()
        asHeaders = _getStandardHeaders()
        oResponse = requests.get(url=sUrl, headers=asHeaders)
        if oResponse is None:
            print('[ERROR] waspy._internalAddFileToWASDI: cannot contact server')
        elif oResponse.ok is not True:
            print('[ERROR] waspy._internalAddFileToWASDI: failed, server replied ' + str(oResponse.status_code))
        else:
            oJson = oResponse.json()
            if 'stringValue' in oJson:
                sProcessId = str(oJson['stringValue'])
                if bAsynch is True:
                    sResult = sProcessId
                else:
                    sResult = waitProcess(sProcessId)
    except:
        print('[ERROR] waspy._internalAddFileToWASDI: something broke alongside')

    return sResult


def subset(sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE):
    _log('[INFO] waspy.subset( ' + str(sInputFile) + ', ' + str(sOutputFile) + ', ' +
          str(dLatN) + ', ' + str(dLonW) + ', ' + str(dLatS) + ', ' + str(dLonE) + ' )')

    if sInputFile is None:
        print('[ERROR] waspy.subset: input file must not be None, aborting')
        return ''
    if len(sInputFile) < 1:
        print('[ERROR] waspy.subset: input file name must not have zero length, aborting')
        return ''
    if sOutputFile is None:
        print('[ERROR] waspy.subset: output file must not be None, aborting')
        return ''
    if len(sOutputFile) < 1:
        print('[ERROR] waspy.subset: output file name len must not have zero length, aborting')
        return ''

    sUrl = m_sBaseUrl + "/processing/geometric/subset?sSourceProductName=" + sInputFile + "&sDestinationProductName=" + \
           sOutputFile + "&sWorkspaceId=" + m_sActiveWorkspace
    sSubsetSetting = "{ \"latN\":" + dLatN + ", \"lonW\":" + dLonW + ", \"latS\":" + dLatS + ", \"lonE\":" + dLonE + " }"
    asHeaders = _getStandardHeaders()
    oResponse = requests.get(sUrl, data=sSubsetSetting, headers=asHeaders)
    if oResponse is None:
        print('[ERROR] waspy.subset: cannot contact server')
        return ''
    if oResponse.ok is not True:
        print('[ERROR] waspy.subset: failed, server returned ' + str(oResponse.status_code))
        return ''
    else:
        oJson = oResponse.json()
        if oJson is not None:
            if 'stringValue' in oJson:
                sProcessId = oJson['stringValue']
                return waitProcess(sProcessId)

    return ''


def executeWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName):
    return _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, False)


def asynchExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName):
    return _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, True)


def _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, bAsynch=False):
    """
    Internal call to execute workflow

    :param asInputFileNames:
    :param asOutputFileNames:
    :param sWorkflowName:
    :param bAsynch:
    :return: processID if asynch, status of the executed process if synch, empty string in case of failure
    """

    _log('[INFO] waspy._internalExecuteWorkflow( ' + str(asInputFileNames) + ', ' +
          str(asOutputFileNames) + ', ' + str(sWorkflowName) + ', ' + str(bAsynch) + ' )')

    if asInputFileNames is None:
        print('[ERROR] waspy._internalExecuteWorkflow: input file names None, aborting')
        return ''
    elif len(asInputFileNames) <= 0:
        print('[ERROR] waspy._internalExecuteWorkflow: no input file names, aborting')
        return ''

    if asOutputFileNames is None:
        print('[ERROR] waspy._internalExecuteWorkflow: output file names None, aborting')
        return ''
    # elif len(asOutputFileNames) <= 0:
    #     print('[ERROR] waspy._internalExecuteWorkflow: no output file names, aborting')
    #     return ''

    if sWorkflowName is None:
        print('[ERROR] waspy._internalExecuteWorkflow: workspace name is None, aborting')
        return ''
    elif len(sWorkflowName) <= 0:
        print('[ERROR] waspy._internalExecuteWorkflow: workflow name too short, aborting')
        return ''

    sProcessId = ''
    sWorkflowId = None
    sUrl = getBaseUrl() + "/processing/graph_id?workspace=" + getActiveWorkspaceId()

    # get a list of workflows, with entries in this form: :
    #   {  "description":STRING,
    #       "name": STRING,
    #       "workflowId": STRING }
    aoWorkflows = getWorkflows()
    aoDictPayload = None
    if aoWorkflows is None:
        print('[ERROR] waspy._internalExecuteWorkflow: workflow list is None, aborting')
        return ''
    elif len(aoWorkflows) <= 0:
        print('[ERROR] waspy._internalExecuteWorkflow: workflow list is empty, aborting')
        return ''
    else:
        for asWorkflow in aoWorkflows:
            if asWorkflow is not None:
                if "name" in asWorkflow:
                    if asWorkflow["name"] == sWorkflowName:
                        if "workflowId" in asWorkflow:
                            # replace \' with \" everywhere
                            aoDictPayload = {}
                            aoDictPayload["description"] = asWorkflow["description"]
                            aoDictPayload["name"] = asWorkflow["name"]
                            aoDictPayload["workflowId"] = asWorkflow["workflowId"]
                            break
    if aoDictPayload is None:
        print('[ERROR] waspy._internalExecuteWorkflow: workflow name not found, aborting')
        return ''

    try:
        aoDictPayload["inputFileNames"] = asInputFileNames
        aoDictPayload["outputFileNames"] = asOutputFileNames
    except:
        print('[ERROR] waspy._internalExecuteWorkflow: payload could not be generated, aborting')
        return ''

    _log('[INFO] waspy._internalExecuteWorkflow: about to HTTP put to ' + str(sUrl) + ' with payload ' +
          str(aoDictPayload))
    asHeaders = _getStandardHeaders()
    oResponse = requests.post(sUrl, headers=asHeaders, data=json.dumps(aoDictPayload))
    if oResponse is None:
        print('[ERROR] waspy._internalExecuteWorkflow: communication with the server failed, aborting')
        return ''
    elif oResponse.ok is True:
        _log('[INFO] waspy._internalExecuteWorkflow: server replied OK')
        asJson = oResponse.json()
        if "stringValue" in asJson:
            sProcessId = asJson["stringValue"]
            if bAsynch is True:
                return sProcessId
            else:
                return waitProcess(sProcessId)
        else:
            print('[ERROR] waspy._internalExecuteWorkflow: cannot find process ID in response, aborting')
            return ''
    else:
        print('[ERROR] waspy._internalExecuteWorkflow: server returned status ' + str(oResponse.status_code))
        print(oResponse.content)
    return ''


def mosaic(asInputFiles, sOutputFile, asBands=None,
           fPixelSizeX=-1.0, fPixelSizeY=-1.0, sCrs=None,
           fSouthBound=-1.0, fNorthBound=-1.0, fEastBound=-1.0, fWestBound=-1.0,
           sOverlappingMethod="MOSAIC_TYPE_OVERLAY", bShowSourceProducts=False, sElevationModelName="ASTER 1sec GDEM",
           sResamplingName="Nearest", bUpdateMode=False, bNativeResolution=True, sCombine="OR", bAsynch=False):
    """
    Constructs a mosaic out of a set of images

    :param asInputFiles: List of input files to mosaic
    :param sOutputFile: Name of the mosaic output file
    :param asBands: List of the bands to use for the mosaic
    :param fPixelSizeX: X Pixel Size
    :param fPixelSizeY: Y Pixel Size
    :param sCrs: WKT of the CRS to use
    :param fSouthBound: South Bound
    :param fNorthBound: North Bound
    :param fEastBound: East Bound
    :param fWestBound: West Bound
    :param sOverlappingMethod: Overlapping Method
    :param bShowSourceProducts: Show Source Products Flag
    :param sElevationModelName: DEM Model Name
    :param sResamplingName: Resampling Method Name
    :param bUpdateMode: Update Mode Flag
    :param bNativeResolution: Native Resolution Flag
    :param sCombine: Combine verb
    :param bAsynch: True to return after the triggering, False to wait the process to finish
    :return: Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
    """

    _log('[INFO]  waspy.mosaic( ' +
          str(asInputFiles) + ', ' +
          str(sOutputFile) + ', ' +
          str(asBands) + ', ' +
          str(fPixelSizeX) + ', ' +
          str(fPixelSizeY) + ', ' +
          str(sCrs) + ', ' +
          str(fSouthBound) + ', ' +
          str(fNorthBound) + ', ' +
          str(fEastBound) + ', ' +
          str(fWestBound) + ', ' +
          str(sOverlappingMethod) + ', ' +
          str(bShowSourceProducts) + ', ' +
          str(sElevationModelName) + ', ' +
          str(sResamplingName) + ', ' +
          str(bUpdateMode) + ', ' +
          str(bNativeResolution) + ', ' +
          str(sCombine) + ', ' +
          str(bAsynch) + ' )'
          )

    if asInputFiles is None:
        print('[ERROR] waspy.mosaic: list of input files is None, aborting')
        return ''
    elif len(asInputFiles) <= 0:
        print('[ERROR] waspy.mosaic: list of input files is empty, aborting')
        return ''

    if sOutputFile is None:
        print('[ERROR] waspy.mosaic: name of output file is None, aborting')
        return ''
    elif isinstance(sOutputFile, str) is False:
        print('[ERROR] waspy.mosaic: output file name must be a string, but a ' + str(type(sOutputFile)) +
              ' was passed, aborting')
        return ''
    elif len(sOutputFile) <= 0:
        print('[ERROR] waspy.mosaic: output file name is empty, aborting')
        return ''

    sUrl = getBaseUrl() + "/processing/geometric/mosaic?sDestinationProductName=" + sOutputFile + "&sWorkspaceId=" + \
           getActiveWorkspaceId()

    sOutputFormat = "GeoTIFF"
    if sOutputFile.endswith(".dim"):
        sOutputFormat = "BEAM-DIMAP"

    if sCrs is None:
        sCrs = _getDefaultCRS()

    # todo check input type is appropriate
    try:
        aoMosaicSettings = {
            'crs': sCrs,
            'southBound': fSouthBound,
            'eastBound': fEastBound,
            'northBound': fNorthBound,
            'westBound': fWestBound,
            'pixelSizeX': fPixelSizeX,
            'pixelSizeY': fPixelSizeY,
            'overlappingMethod': sOverlappingMethod,
            'showSourceProducts': bShowSourceProducts,
            'elevationModelName': sElevationModelName,
            'resamplingName': sResamplingName,
            'updateMode': bUpdateMode,
            'nativeResolution': bNativeResolution,
            'combine': sCombine,
            'outputFormat': sOutputFormat,
            'sources': asInputFiles,
            'variableNames': asBands,
            'variableExpressions': []
        }
    except:
        print('[ERROR] waspy.mosaic: cannot build DTO, please check your input. Aborting')
        return ''

    asHeaders = _getStandardHeaders()
    oResponse = requests.post(sUrl, data=json.dumps(aoMosaicSettings), headers=asHeaders)
    if oResponse is None:
        print('[ERROR] waspy.mosaic: cannot contact server, aborting')
        return ''
    if oResponse.ok is True:
        asJson = oResponse.json()
        if 'stringValue' in asJson:
            sProcessId = str(asJson['stringValue'])
            if bAsynch is False:
                return waitProcess(sProcessId)
            else:
                return sProcessId
    else:
        print('[ERROR] waspy.mosaic: server respondend with status: ' + str(oResponse.status_code) + ', aborting')
        return ''

    return ''


def _getDefaultCRS():
    return (
            "GEOGCS[\"WGS84(DD)\", \r\n" +
            "		  DATUM[\"WGS84\", \r\n" +
            "			SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \r\n" +
            "		  PRIMEM[\"Greenwich\", 0.0], \r\n" +
            "		  UNIT[\"degree\", 0.017453292519943295], \r\n" +
            "		  AXIS[\"Geodetic longitude\", EAST], \r\n" +
            "		  AXIS[\"Geodetic latitude\", NORTH]]"
    )


if __name__ == '__main__':
    _log(
        'WASPY - The WASDI Python Library. Include in your code for space development processors. Visit www.wasdi.net'
    )

