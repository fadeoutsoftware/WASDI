"""
Created on 11 Jun 2018

@author: p.campanella
"""
from _overlapped import NULL

name = "wasdi"

import requests
import os
import json

m_sUser = 'urs'
m_sPassword = 'pw'
m_sBasePath = '/data/wasdi/'
m_sSessionId = ''
m_sActiveWorkspace = ''
m_sBaseUrl = 'http://www.wasdi.net/wasdiwebserver/rest'
m_bIsOnServer = False
m_bDownloadActive = True
m_sMyProcId = ''
m_aoParamsDictionary = {}
m_bUploadActive = True
m_bVerbose = False
m_sParametersFilePath = ''


def getParams():
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


def setMyProcId(sProcID):
    """
    Own Proc Id 
    """
    global m_sMyProcId
    m_sMyProcId = sProcID


def getMyProcId():
    """
    Get the Own Proc Id
    """
    global m_sMyProcId
    return m_sMyProcId


def log(sLog):
    if m_bVerbose:
        print(sLog)


def init():
    """
    Init WASDI Library. Call it after setting user, password, path and url or use it with a config file
    Return True if login was successful, False otherwise
    """

    global m_sUser
    global m_sPassword
    global m_sBaseUrl
    global m_sSessionId

    if m_sSessionId != '':
        headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
        sUrl = m_sBaseUrl + '/auth/checksession'
        oResult = requests.get(sUrl, headers=headers)

        if (oResult is not None) and (oResult.ok is True):
            oJsonResult = oResult.json()
            try:
                sUser = oJsonResult['userId']

                if sUser == m_sUser:
                    return True
                else:
                    return False
            except:
                return False
        else:
            return False
    else:
        headers = {'Content-Type': 'application/json'}

        sUrl = m_sBaseUrl + '/auth/login'

        sPayload = '{"userId":"' + m_sUser + '","userPassword":"' + m_sPassword + '" }'

        oResult = requests.post(sUrl, data=sPayload, headers=headers)

        if oResult.ok is True:
            oJsonResult = oResult.json()
            try:
                m_sSessionId = oJsonResult['sessionId']
                return True
            except:
                return False
        else:
            return False


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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = requests.get(sUrl, headers=headers)

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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = requests.get(sUrl, headers=headers)

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
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    sWorkspaceId = getWorkspaceIdByName(sWorkspaceName)

    m_sActiveWorkspace = sWorkspaceId

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'sWorkspaceId': sWorkspaceId}

    sUrl = m_sBaseUrl + '/product/byws'

    asProducts = []

    oResult = requests.get(sUrl, headers=headers, params=payload)

    if oResult.ok is True:
        oJsonResults = oResult.json()

        for oProduct in oJsonResults:
            try:
                asProducts.append(oProduct['fileName'])
            except:
                continue

    return asProducts


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

    if m_bIsOnServer:
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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}

    sUrl = m_sBaseUrl + '/processing/getgraphsbyusr'

    oResult = requests.get(sUrl, headers=headers)

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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'workspace': m_sActiveWorkspace, 'source': sInputFileName, 'destination': sOutputFileName,
               'workflowId': sWorkflowId}

    sUrl = m_sBaseUrl + '/processing/graph_id'

    oResult = requests.get(sUrl, headers=headers, params=payload)

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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'sProcessId': sProcessId}

    sUrl = m_sBaseUrl + '/process/byid'

    oResult = requests.get(sUrl, headers=headers, params=payload)

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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'sProcessId': sProcessId, 'status': sStatus, 'perc': iPerc}

    if iPerc < 0:
        print('iPerc < 0 not valid')
        return ''
    elif iPerc > 100:
        print('iPerc > 100 not valid')
        return ''
    elif not (
            sStatus == 'CREATED' or\
            sStatus == 'RUNNING' or\
            sStatus == 'STOPPED' or\
            sStatus == 'DONE' or\
            sStatus == 'ERROR'
    ):
        print('sStatus must be a string like one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR')
        return ''
    elif sProcessId == '':
        return ''

    sUrl = m_sBaseUrl + '/process/updatebyid'

    oResult = requests.get(sUrl, headers=headers, params=payload)

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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'sProcessId': sProcessId, 'payload': json.dumps(data)}

    sUrl = m_sBaseUrl + '/process/setpayload'

    oResult = requests.get(sUrl, headers=headers, params=payload)

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

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'file': sFileName, 'workspace': m_sActiveWorkspace}

    sUrl = m_sBaseUrl + '/catalog/upload/ingestinws'

    oResult = requests.get(sUrl, headers=headers, params=payload)

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

    # todo auto unzip

    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    headers = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    payload = {'filename': sFileName}

    sUrl = m_sBaseUrl + '/catalog/downloadbyname'

    print('WASDI: send request to configured url ' + sUrl)

    oResult = requests.get(sUrl, headers=headers, params=payload, stream=True)

    if (oResult is not None) and (oResult.status_code == 200):
        print('WASDI: got ok result, downloading')

        sSavePath = getSavePath()
        sSavePath += sFileName

        try:
            os.makedirs(os.path.dirname(sSavePath))
        except:  # Guard against race condition
            print('Error Creating File Path!!')

        print('WASDI: downloading local file ' + sSavePath)

        with open(sSavePath, 'wb') as oFile:
            for oChunk in oResult:
                print('.')
                oFile.write(oChunk)
        print('WASDI: download Done new file locally available ' + sSavePath)
    else:
        print('WASDI: download error server code: ' + oResult.status_code)

    return


def wasdiLog(sLogRow):
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    if m_bIsOnServer:
        sHeaders = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
        sUrl = m_sBaseUrl + '/processors/logs/add?processworkspace=' + m_sMyProcId
        oResult = requests.post(sUrl, data=sLogRow, headers=sHeaders)

    else:
        print(sLogRow)


def deleteProduct(sProduct):
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    sHeaders = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    sUrl = m_sBaseUrl + "/product/delete?sProductName=" + sProduct + "&bDeleteFile=true&sWorkspaceId=" + m_sActiveWorkspace + "&bDeleteLayer=true";
    oResult = requests.get(sUrl, headers=sHeaders)


if __name__ == '__main__':
    sTx = 'WASPY - The WASDI Python Library. Include in your code for space development processors. Visit www.wasdi.net'
    print(sTx)
