""""""
import wasdi

"""
WASDI (LU) S.a.r.l.

**Disclaimer ** 
The library is provided "as-is" without warranty

Neither WASDI (LU) S.a.r.l. or any of its partners or agents shall be liable for any direct, indirect, incidental, special, exemplary, or consequential
damages (including, but not limited to, breach of expressed or implied contract; procurement of substitute goods or services; loss of use, data or profits; 
business interruption; or damage to any equipment, software and/or data files) however caused and on any legal theory of liability, whether for contract, 
tort, strict liability, or a combination thereof (including negligence or otherwise) arising in any way out of the direct or indirect use of software, 
even if advised of the possibility of such risk and potential damage.

WASDI (LU) S.a.r.l. uses all reasonable care to ensure that software products and other files that are made available are safe to use when installed,
and that all products are free from any known software virus. For your own protection, you should scan all files for viruses prior to installation.


# WASDI

This is WASPY, the WASDI Python lib.

WASDI is a fully scalable and distributed Cloud based EO analytical platform. The system is cross-cloud and cross DIAS. 
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

Version 0.8.7.5

Last Update: 09/04/2025

Tested with: Python 3.7 - Python 3.13 

Created on 11 Jun 2018

@author: p.campanella
"""
from builtins import str
from time import sleep

name = "wasdi"

import json
import os
import re
import time
import zipfile
import requests
import getpass
import os.path
import inspect
from datetime import datetime
from enum import Enum
import hashlib

# Initialize "Members"

# WASDI User
m_sUser = None
# Password
m_sPassword = None

# Active Workspace Id
m_sActiveWorkspace = None
# Owner of the Active Workspace
m_sWorkspaceOwner = ''
# Active  workspace specific url
m_sWorkspaceBaseUrl = ''

# Path of the parameters file
m_sParametersFilePath = None
# Session Id
m_sSessionId = ''
# Flag to detect if the session is valid
m_bValidSession = False
# Local base path
m_sBasePath = None

# True to activate auto download
m_bDownloadActive = True
# True to activate auto upload
m_bUploadActive = True
# True to enable verbose logs of the lib
m_bVerbose = True
# Dictionary with the input parameters
m_aoParamsDictionary = {}

# Process Workspace Id of this application
m_sMyProcId = ''
# Base url: by default the main wasdi server
m_sBaseUrl = 'https://www.wasdi.net/wasdiwebserver/rest'
#m_sBaseUrl = 'https://test.wasdi.net/wasdiwebserver/rest'
# The launcher set this flag true when working on a wasdi node
m_bIsOnServer = False
# The launcher set this flag true when working on a cloud that is not a wasdi node
m_bIsOnExternalServer = False
# Set true if the app is on server and the data folder mounted is only for the workspace folder
m_bOnlyWorkspaceFolderMounted = False
# Timeout for the http calls
m_iRequestsTimeout = 2 * 60
# Specific timeout for the upload http calls
m_iUploadTimeout = 10 * 60
# Local copy of the payload of the app
m_sPayload = ""
# True to enable the test of the checksum when determining when a file should be downloaded locally or not
m_bEnableChecksumTest = False
# Time spent in sleep while polling for API like wait Process
m_iPollingSleepSeconds = 3

def printStatus():
    """Prints status
    """
    global m_sWorkspaceOwner

    _log('')
    _log('[INFO] waspy.printStatus: user: ' + str(getUser()))
    _log('[INFO] waspy.printStatus: password: ***********')
    _log('[INFO] waspy.printStatus: session id: ' + str(getSessionId()))
    _log('[INFO] waspy.printStatus: active workspace: ' + str(getActiveWorkspaceId()))
    _log('[INFO] waspy.printStatus: workspace owner: ' + str(m_sWorkspaceOwner))
    _log('[INFO] waspy.printStatus: parameters file path: ' + str(getParametersFilePath()))
    _log('[INFO] waspy.printStatus: base path: ' + str(getBasePath()))
    _log('[INFO] waspy.printStatus: download active: ' + str(getDownloadActive()))
    _log('[INFO] waspy.printStatus: upload active: ' + str(getUploadActive()))
    _log('[INFO] waspy.printStatus: enable checksum: ' + str(getEnableChecksum()))
    _log('[INFO] waspy.printStatus: verbose: ' + str(getVerbose()))
    _log('[INFO] waspy.printStatus: param dict: ' + str(getParametersDict()))
    _log('[INFO] waspy.printStatus: proc id: ' + str(getProcId()))
    _log('[INFO] waspy.printStatus: base url: ' + str(getBaseUrl()))
    _log('[INFO] waspy.printStatus: is on server: ' + str(getIsOnServer()))
    _log('[INFO] waspy.printStatus: is on external server: ' + str(getIsOnExternalServer()))
    _log('[INFO] waspy.printStatus: workspace base url: ' + str(getWorkspaceBaseUrl()))

    if m_bValidSession:
        _log('[INFO] waspy.printStatus: session is valid :-)')
    else:
        print('[ERROR] waspy.printStatus: session is not valid :-(' +
              '  ******************************************************************************')


def setVerbose(bVerbose):
    """Sets verbosity
    :param boolean bVerbose: False non verbose, True verbose
    :return:
    """
    if bVerbose is None:
        print('[ERROR] waspy.setVerbose: passed None, won\'t change' +
              '  ******************************************************************************')
        return
    if not isinstance(bVerbose, bool):
        print('[ERROR] waspy.setVerbose: passed non boolean, trying to convert' +
              '  ******************************************************************************')
        try:
            bVerbose = bool(bVerbose)
        except:
            print('[ERROR] waspy.setVerbose: cannot convert argument into boolean, won\'t change' +
                  '  ******************************************************************************')
            return

    global m_bVerbose
    m_bVerbose = bVerbose


def getVerbose():
    """
    Get Verbose Flag

    :return: True or False
    """
    global m_bVerbose
    return m_bVerbose


def getParametersDict():
    """
    Get the full Params Dictionary

    :return: a dictionary containing the parameters
    """
    global m_aoParamsDictionary

    aoReturnDict = dict(m_aoParamsDictionary)

    if "user" in aoReturnDict:
        del aoReturnDict["user"]

    if "sessionid" in aoReturnDict:
        del aoReturnDict["sessionid"]

    if "workspaceid" in aoReturnDict:
        del aoReturnDict["workspaceid"]

    if "password" in aoReturnDict:
        del aoReturnDict["password"]

    return aoReturnDict


def setParametersDict(aoParams):
    """
    Get the full Params Dictionary

    :param aoParams: dictionary of Parameters
    :return: a dictionary containing the parameters
    """
    global m_aoParamsDictionary
    m_aoParamsDictionary = aoParams


def addParameter(sKey, oValue):
    """
    Adds a parameter

    :param sKey: parameter key

    :param oValue: parameter value
    """
    global m_aoParamsDictionary
    m_aoParamsDictionary[sKey] = oValue


def getParameter(sKey, oDefault=None):
    """
    Gets a parameter using its key

    :param sKey: parameter key

    :param oDefault: Default value to return if parameter is not present
    :return: parameter value
    """
    global m_aoParamsDictionary
    try:
        return m_aoParamsDictionary[sKey]
    except:
        return oDefault

def setParameter(sKey, oValue):
    """
    Same as add Parameter
    """
    addParameter(sKey, oValue)

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
    """
    Set The Parameters JSON File Path

    :param: sParamPath Local Path of the parameters file
    """
    if sParamPath is None:
        _log('[ERROR] waspy.setParametersFilePath: passed None as path, won\'t change' +
              '  ******************************************************************************')
        return
    if len(sParamPath) < 1:
        _log('[ERROR] waspy.setParametersFilePath: string passed has zero length, won\'t change' +
              '  ******************************************************************************')
        return

    global m_sParametersFilePath
    m_sParametersFilePath = sParamPath


def getParametersFilePath():
    """
    Get the local parameters file Path

    :return: local paramters file path
    """
    global m_sParametersFilePath
    return m_sParametersFilePath


def getSessionId():
    """
    Get the WASDI Session

    :return: Session Id [String]
    """
    global m_sSessionId
    return m_sSessionId


def setBasePath(sBasePath):
    """
    Set the local Base Path for WASDI

    :param sBasePath: local WASDI base Path. If not set, by default WASDI uses [USERHOME].wasdi
    """
    global m_sBasePath
    m_sBasePath = sBasePath


def getBasePath():
    """
    Get the local Base Path for WASDI

    :return: local base path for WASDI
    """
    global m_sBasePath
    return m_sBasePath


def setBaseUrl(sBaseUrl):
    """
    Set the WASDI API URL

    :param sBaseUrl: WASDI API URL
    """
    global m_sBaseUrl
    m_sBaseUrl = sBaseUrl


def getBaseUrl():
    """
    Get the WASDI API URL

    :return: WASDI API URL
    """
    global m_sBaseUrl
    return m_sBaseUrl


def setWorkspaceBaseUrl(sWorkspaceBaseUrl):
    """
    Set the Workspace specific API URL

    :param sWorkspaceBaseUrl: Workspace API URL
    """
    global m_sWorkspaceBaseUrl
    m_sWorkspaceBaseUrl = sWorkspaceBaseUrl


def getWorkspaceBaseUrl():
    """
    Get the Workspace API URL

    :return: Workspace API URL
    """
    global m_sWorkspaceBaseUrl
    return m_sWorkspaceBaseUrl


def setIsOnServer(bIsOnServer):
    """
    Set the Is on Server Flag: keep it false, as default, while developing

    :param bIsOnServer: set the flag to know if the processor is running on the server or on the local PC
    """
    global m_bIsOnServer
    m_bIsOnServer = bIsOnServer


def getIsOnServer():
    """
    Are we running on a WASDI Server?

    :return: True if it is running on server, False if it is running on the local Machine
    """
    global m_bIsOnServer
    return m_bIsOnServer

def setIsOnExternalServer(bIsOnExternalServer):
    """
    Set the Is on  External Server Flag: keep it false, as default, while developing or when in another infrastructure

    :param bIsOnExternalServer: set the flag to know if the processor is running on an external server
    """
    global m_bIsOnExternalServer
    m_bIsOnExternalServer = bIsOnExternalServer
    
def getIsOnExternalServer():
    """
    Are we running on an External Server?

    :return: True if it is running on an external server, False if it is running on WASDI or on the local Machine
    """
    global m_bIsOnExternalServer
    return m_bIsOnExternalServer

def setDownloadActive(bDownloadActive):
    """
    When in development, set True to download locally files from Server.
    Set it to false to NOT donwload data. In this case the developer must check the availability of the files

    :param bDownloadActive: True (default) to activate autodownload. False to disactivate
    """

    if bDownloadActive is None:
        print('[ERROR] waspy.setDownloadActive: passed None, won\'t change' +
              '  ******************************************************************************')
        return

    global m_bDownloadActive
    m_bDownloadActive = bDownloadActive


def getDownloadActive():
    """
    Get the Download Active Flag

    :return: True if auto download is active, False if it is not active
    """
    global m_bDownloadActive
    return m_bDownloadActive


def setUploadActive(bUploadActive):
    """
    When in development, set True to upload local files on Server.
    Set it to false to NOT upload data. In this case the developer must check the availability of the files

    :param bUploadActive: True to activate Auto Upload, False to disactivate auto upload
    """

    if bUploadActive is None:
        _log('[ERROR] waspy.setUploadActive: passed None, won\'t change' +
              '  ******************************************************************************')
        return

    global m_bUploadActive
    m_bUploadActive = bUploadActive


def getUploadActive():
    """
    Get the Upload Active Flag

    :return: True if Auto Upload is Active, False if it is NOT Active
    """
    global m_bUploadActive
    return m_bUploadActive


def setProcId(sProcID):
    """
    Own Proc Id

    :param sProcID: self processor identifier
    """
    global m_sMyProcId
    m_sMyProcId = sProcID


def getProcId():
    """
    Get the Own Proc Id

    :return: Own Processor Identifier
    """
    global m_sMyProcId
    return m_sMyProcId


def setActiveWorkspaceId(sActiveWorkspace):
    """
    Set the Active Workspace Id

    :param sActiveWorkspace: Active Workspace Id
    """
    global m_sActiveWorkspace
    m_sActiveWorkspace = sActiveWorkspace


def getActiveWorkspaceId():
    """
    Get Active workspace Id

    :return: the WorkspaceId as a String, '' if there is any error
    """
    global m_sActiveWorkspace
    return m_sActiveWorkspace

def getRequestsTimeout():
    """
    :return: the timeout for HTTP requests
    """
    global m_iRequestsTimeout
    return m_iRequestsTimeout


def setRequestsTimeout(iTimeout):
    """
    :param iTimeout: the timeout to be set for HTTP requests
    """
    global m_iRequestsTimeout
    m_iRequestsTimeout = iTimeout

def getUploadTimeout():
    """
    :return: the timeout for HTTP uploads
    """
    global m_iUploadTimeout
    return m_iUploadTimeout

def setUploadTimeout(iTimeout):
    """
    :param iTimeout: the timeout to be set for HTTP requests
    """
    global m_iUploadTimeout
    m_iUploadTimeout = iTimeout

def setEnableChecksum(bEnableChecksum):
    """
    :param bEnableChecksum: true to enable the checksum verification to align remote files
    """
    global m_bEnableChecksumTest
    m_bEnableChecksumTest = bEnableChecksum

def getEnableChecksum():
    """
    Get the value of the Enable Checksum flag
    """
    global m_bEnableChecksumTest
    return m_bEnableChecksumTest


def setOnlyWorkspaceFolderMounted(bOnlyWorkspaceFolderMounted):
    """
    :param bOnlyWorkspaceFolderMounted: true if only the workspace folder is mounted
    """
    global m_bOnlyWorkspaceFolderMounted
    m_bOnlyWorkspaceFolderMounted = bOnlyWorkspaceFolderMounted

def getOnlyWorkspaceFolderMounted():
    """
    Get the value of the Only Workspace Folder Mounted Flag
    """
    global m_bOnlyWorkspaceFolderMounted
    return m_bOnlyWorkspaceFolderMounted

def refreshParameters():
    """
    Refresh parameters, reading the file again
    """
    _loadParams()


def _getEnvironmentVariable(sVariable):
    try:
        sValue = os.environ[sVariable]
        return sValue
    except KeyError:
        return None

def getPollingSleepSeconds():
    """
    :return: the number of seconds to sleep between polling http calls
    """
    global m_iPollingSleepSeconds
    return m_iPollingSleepSeconds


def setPollingSleepSeconds(iPollingSleepSeconds):
    """
    :param iPollingSleepSeconds: the number of seconds to sleep between polling http calls
    """
    global m_iPollingSleepSeconds
    m_iPollingSleepSeconds = iPollingSleepSeconds

def init(sConfigFilePath=None):
    """
    Init WASDI Library. Call it after setting user, password, path and url or use it with a config file

    :param sConfigFilePath: local path of the config file. In None or the file does not exists, WASDI will ask for login in the console
    :return: True if login was successful, False otherwise
    """
    global m_sUser
    global m_sPassword
    global m_sBaseUrl
    global m_sSessionId
    global m_sBasePath
    global m_bValidSession
    global m_sMyProcId

    sWorkspaceName = None
    sWorkspaceId = None
    m_bValidSession = False

    sEnvBaseUrl = _getEnvironmentVariable('WASDI_WEBSERVER_URL')

    if sEnvBaseUrl is not None:
        if sEnvBaseUrl!="":
            if sEnvBaseUrl.endswith("/"):
                sEnvBaseUrl = sEnvBaseUrl[:-1]
            m_sBaseUrl = sEnvBaseUrl
            print('[INFO] waspy.init: Base Url read by WASDI_WEBSERVER_URL: ' + m_sBaseUrl)

    # P.Campanella 2022/08/30: if there is no config file, try the default notebook one
    if sConfigFilePath is None:
        if os.path.exists("/home/appwasdi/notebook/notebook_config.cfg"):
            sConfigFilePath = "/home/appwasdi/notebook/notebook_config.cfg"

    if sConfigFilePath is not None:
        bConfigOk, sWorkspaceName, sWorkspaceId = _loadConfig(sConfigFilePath)

        if bConfigOk is True:
            _loadParams()

    if m_sUser is None:
        m_sUser = _getEnvironmentVariable("WASDI_USER")
        if m_sUser is not None:
            print('[INFO] waspy.init: user read in the env WASDI_USER variable')

    #Default on local pc
    sHome = os.path.expanduser("~")
    sCredentialsConfigFile = sHome + "/.wasdi/credentials.json"

    # Check if there is the credentials config file in the user home
    if m_sUser is None and m_sPassword is None:
        if not getIsOnServer() and os.path.exists(sCredentialsConfigFile):
            with open(sCredentialsConfigFile,"r") as oInFile:
                oReadConfig = json.load(oInFile)
                if oReadConfig is not None:
                    if "USER" in oReadConfig and "PASSWORD" in oReadConfig:
                        m_sUser = oReadConfig["USER"]
                        m_sPassword = oReadConfig["PASSWORD"]

    if m_sUser is None and m_sPassword is None:
        m_sUser = input('[INFO] waspy.init: Please Insert WASDI User:')

        m_sPassword = getpass.getpass(prompt='[INFO] waspy.init: Please Insert WASDI Password:', stream=None)

        m_sUser = m_sUser.rstrip()
        m_sPassword = m_sPassword.rstrip()

        if sWorkspaceId is None and sWorkspaceName is None:
            sWorkspaceName = input('[INFO] waspy.init: Please Insert Active Workspace Name (Enter to jump):')

        if not getIsOnServer():
            sAnswer = input('[WARNING] do you want to store your WASDI credentials? Will be saved not-encrypted in your user home folder [yes/no]')
            
            if sAnswer == "yes":
                oWriteConfig = {}
                oWriteConfig["USER"] = m_sUser
                oWriteConfig["PASSWORD"] = m_sPassword
                sJsonContent = json.dumps(oWriteConfig)
                with open(sCredentialsConfigFile,"w") as oOutFile:
                    oOutFile.write(sJsonContent)


    if m_sUser is None:
        print('[ERROR] waspy.init: must initialize user first, but None given' +
              '  ******************************************************************************')
        return False

    if m_sBasePath is None:
        # If it was not on config:
        if getIsOnServer() is True:
            #Default on server
            m_sBasePath = '/data/wasdi/'
        else:
            # the empty string at the end adds a separator
            m_sBasePath = os.path.join(sHome, ".wasdi", "")

        sEnvBasePath = _getEnvironmentVariable('WASDI_BASE_PATH')

        if sEnvBasePath is not None:
            if sEnvBasePath != "":
                if not sEnvBasePath.endswith("/"):
                    sEnvBasePath = sEnvBasePath + "/"
                m_sBasePath = sEnvBasePath
                print('[INFO] waspy.init: Base Path read by WASDI_BASE_PATH: ' + m_sBasePath)

    sOnlyWsFolderMounted = _getEnvironmentVariable('WASDI_ONLY_WS_FOLDER')

    if sOnlyWsFolderMounted is not None:
        if sOnlyWsFolderMounted == "1":
            print('[INFO] waspy.init: Only Workspace Folder detect from  WASDI_ONLY_WS_FOLDER')
            setOnlyWorkspaceFolderMounted(True)

    # Check if we have the session id in env
    if not m_sSessionId or m_sSessionId=='':
        m_sSessionId = _getEnvironmentVariable("WASDI_SESSION_ID")
        if m_sSessionId is not None:
            print('[INFO] waspy.init: session id read in the env WASDI_SESSION_ID variable')
        if m_sSessionId is None:
            m_sSessionId = ""

    # Check if we have the my proc id in env
    if not m_sMyProcId or m_sMyProcId == '':
        m_sMyProcId = _getEnvironmentVariable("WASDI_PROCESS_WORKSPACE_ID")
        if m_sMyProcId is not None:
            print('[INFO] waspy.init: process workspace id read in the env WASDI_SESSION_ID variable')
        if m_sMyProcId is None:
            m_sMyProcId = ""

    if m_sSessionId is not None and m_sSessionId != '':
        asHeaders = _getStandardHeaders()
        sUrl = m_sBaseUrl + '/auth/checksession'

        oResponse = None
        
        try:
            oResponse = requests.get(sUrl, headers=asHeaders, timeout=m_iRequestsTimeout)
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.init: there was an error contacting the API " + str(oEx))

        if (oResponse is not None) and (oResponse.ok is True):
            oJsonResult = oResponse.json()
            try:
                sUser = str(oJsonResult['userId'])
                if sUser == m_sUser:
                    m_bValidSession = True
                else:
                    m_bValidSession = False
            except:
                m_bValidSession = False
        else:
            m_bValidSession = False
    else:
        if m_sPassword is None:
            print('[ERROR] waspy.init: must initialize password first, but None given' +
                  '  ******************************************************************************')
            return False

        asHeaders = {'Content-Type': 'application/json'}
        sUrl = m_sBaseUrl + '/auth/login'
        sPayload = '{"userId":"' + m_sUser + '","userPassword":"' + m_sPassword + '" }'
        oResponse = None
        try:
            oResponse = requests.post(sUrl, data=sPayload, headers=asHeaders, timeout=m_iRequestsTimeout)
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.init: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            print('[ERROR] waspy.init: cannot authenticate' +
                  '  ******************************************************************************')
            m_bValidSession = False
        elif oResponse.ok is not True:
            print('[ERROR] waspy.init: cannot authenticate, server replied: ' + str(oResponse.status_code) +
                  '  ******************************************************************************')
            m_bValidSession = False
        else:
            oJsonResult = oResponse.json()
            try:
                m_sSessionId = str(oJsonResult['sessionId'])
                _log('[INFO] waspy.init: returned session is: ' + str(m_sSessionId) + '\n')
                if m_sSessionId is not None and m_sSessionId != '' and m_sSessionId != 'None':
                    m_bValidSession = True
                else:
                    m_bValidSession = False
            except:
                m_bValidSession = False

    if m_bValidSession is True:
        _log('[INFO] waspy.init: WASPY successfully initiated :-)')
        sActuallyActiveWorkspace = getActiveWorkspaceId()
        if (sActuallyActiveWorkspace is None) or (len(sActuallyActiveWorkspace) < 1):

            # Check if the workspace id is NOT set by config
            if sWorkspaceId is None and sWorkspaceName is None:
                # And in case if we have it in env
                sWorkspaceId = _getEnvironmentVariable("WASDI_WORKSPACE_ID")
                if sWorkspaceId is not None:
                    print('[INFO] waspy.init: workspace id read in the env WASDI_WORKSPACE_ID variable')

            if sWorkspaceName is not None:
                openWorkspace(sWorkspaceName)
            elif sWorkspaceId is not None:
                openWorkspaceById(sWorkspaceId)
    else:
        print('[ERROR] waspy.init: could not init WASPY :-(' +
              '  ******************************************************************************')

    printStatus()
    return m_bValidSession


def hello():
    """
    Hello Wasdi to test the connection.

    :return: the hello message as Text
    """
    global m_sBaseUrl

    sUrl = m_sBaseUrl + '/wasdi/hello'

    oResult = None

    try:
        oResult = requests.get(sUrl, timeout = getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.hello: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ""

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

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkspaces: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return None

    if oResult.ok:
        oJsonResult = oResult.json()
        return oJsonResult
    else:
        wasdiLog("[ERROR] waspy.getWorkspaces: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
        return None

def createWorkspace(sName=None):
    """
    Create a new workspaces and set it as ACTIVE Workspace

    :param sName: Name of the workspace to create. Null by default
    :return: Workspace Id as a String if it is a success, None otherwise
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/create'

    if sName is not None:
        sUrl = sUrl + "?name=" + sName

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.createWorkspace: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return None

    if oResult.ok:
        oJsonResult = oResult.json()

        openWorkspaceById(oJsonResult["stringValue"])

        return oJsonResult["stringValue"]
    else:
        wasdiLog("[ERROR] waspy.createWorkspace: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
        return None


def deleteWorkspace(sWorkspaceId):
    """
    Delete a workspace

    :param sWorkspaceId: Id of the workspace to delete
    :return: True if workspace could be deleted, False otherwise
    """
    asHeaders = _getStandardHeaders()

    if sWorkspaceId is None:
        _log('[ERROR] waspy.deleteWorkspace: sWorkspaceId passed is None' +
              '  ******************************************************************************')
        return False

    bDeleteLayer = True
    bDeleteFile = True

    sActualWorkspaceId = getActiveWorkspaceId()

    openWorkspaceById(sWorkspaceId)

    try:
        sUrl = getWorkspaceBaseUrl() + '/ws/delete?workspace=' + sWorkspaceId + '&deletelayer=' + str(
            bDeleteLayer) + "&deletefile=" + str(bDeleteFile)

        oResult = None

        try:
            oResult = requests.delete(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR]  waspy.deleteWorkspace there was an error contacting the API " + str(oEx))

        if oResult is None:
            return False

        if oResult.ok:
            return True
        else:
            wasdiLog("[ERROR] waspy.deleteWorkspace: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
            return False        
    finally:
        openWorkspaceById(sActualWorkspaceId)


def getWorkspaceIdByName(sName):
    """
    Get Id of a Workspace from the name

    :param sName: Workspace Name
    :return: the WorkspaceId as a String, '' if there is any error
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkspaceIdByName: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ''

    if oResult.ok:
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceName'] == sName:
                    return oWorkspace['workspaceId']
            except:
                return ''
    else:
        wasdiLog("[ERROR] waspy.getWorkspaceIdByName: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)

    return ''

def getWorkspaceNameById(sWorkspaceId):
    """
    Get Name of a Workspace from the id

    :param sWorkspaceId: Workspace Id
    :return: the Workspace Name as a String, '' if there is any error
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkspaceNameById: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ''

    if oResult.ok:
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceId'] == sWorkspaceId:
                    return oWorkspace['workspaceName']
            except:
                return ''
    else:
        wasdiLog("[ERROR] waspy.getWorkspaceNameById: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)

    return ''


def getWorkspaceOwnerByName(sName):
    """
    Get user Id of the owner of Workspace from the name

    :param sName: Name of the workspace
    :return: the userId as a String, '' if there is any error
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkspaceOwnerByName: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ''

    if oResult.ok:
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceName'] == sName:
                    return oWorkspace['ownerUserId']
            except:
                return ''
    else:
        wasdiLog("[ERROR] waspy.getWorkspaceOwnerByName: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return ''


def getWorkspaceOwnerByWsId(sWsId):
    """
    Get user Id of the owner of Workspace from the Workspace Id

    :param sWsId: Workspace Id
    :return: the userId as a String, '' if there is any error
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/byuser'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkspaceOwnerByWsId: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ''

    if oResult.ok:
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceId'] == sWsId:
                    return oWorkspace['ownerUserId']
            except:
                return ''
    else:
        wasdiLog("[ERROR] waspy.getWorkspaceOwnerByWsId: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return ''


def getWorkspaceUrlByWsId(sWsId):
    """
    Get Base Url of a Workspace from the Workspace Id

    :param sWsId: Workspace Id
    :return: the Workspace Base Url as a String, '' if there is any error
    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/ws/getws?workspace=' + sWsId

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkspaceUrlByWsId: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ''

    if oResult.ok:
        oJsonResult = oResult.json()
        try:
            return oJsonResult['apiUrl']
        except:
            return ''
    else:
        wasdiLog("[ERROR] waspy.getWorkspaceUrlByWsId: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return ''


def openWorkspaceById(sWorkspaceId):
    """
    Open a workspace by Id

    :param sWorkspaceId: Workspace Id
    :return: the WorkspaceId as a String, '' if there is any error
    """
    global m_sActiveWorkspace
    global m_sWorkspaceOwner
    global m_sWorkspaceBaseUrl

    m_sActiveWorkspace = sWorkspaceId
    m_sWorkspaceOwner = getWorkspaceOwnerByWsId(sWorkspaceId)
    m_sWorkspaceBaseUrl = getWorkspaceUrlByWsId(sWorkspaceId)

    if not m_sWorkspaceBaseUrl:
        m_sWorkspaceBaseUrl = getBaseUrl()

    _createWorkspaceFolder()

    return m_sActiveWorkspace


def openWorkspace(sWorkspaceName):
    """
    Open a workspace

    :param sWorkspaceName: Workspace Name
    :return: the WorkspaceId as a String, '' if there is any error
    """
    global m_sActiveWorkspace
    global m_sWorkspaceOwner
    global m_sWorkspaceBaseUrl

    m_sActiveWorkspace = getWorkspaceIdByName(sWorkspaceName)
    m_sWorkspaceOwner = getWorkspaceOwnerByName(sWorkspaceName)
    m_sWorkspaceBaseUrl = getWorkspaceUrlByWsId(m_sActiveWorkspace)
    if not m_sWorkspaceBaseUrl:
        m_sWorkspaceBaseUrl = getBaseUrl()

    _createWorkspaceFolder()

    return m_sActiveWorkspace

def _createWorkspaceFolder():
    """
    Creates the workspace folder
    """
    sWorkspacePath = ""

    try:
        sWorkspacePath = _internalGetPath("")
        if not os.path.exists(sWorkspacePath):
                os.makedirs(sWorkspacePath)
    except:
        _log('[ERROR] waspy._createWorkspaceFolder: cannot create Workspace Path !!! folder ' + sWorkspacePath)


def getProductsByWorkspace(sWorkspaceName):
    """
    Get the list of products in a workspace by Name

    :param sWorkspaceName: Name of the workspace
    :return: the list is an array of string. Can be empty if there is any error
    """

    sWorkspaceId = getWorkspaceIdByName(sWorkspaceName)
    return getProductsByWorkspaceId(sWorkspaceId)


def getProductsByWorkspaceId(sWorkspaceId):
    """
    Get the list of products in a workspace by Id

    :param sWorkspaceId: Workspace Id
    :return: the list is an array of string. Can be empty if there is any error
    """
    global m_sBaseUrl

    asHeaders = _getStandardHeaders()
    payload = {'workspace': sWorkspaceId}

    sUrl = m_sBaseUrl + '/product/namesbyws'

    asProducts = []

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, params=payload, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getProductsByWorkspaceId: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return asProducts

    if oResult.ok:
        oJsonResults = oResult.json()

        for sProduct in oJsonResults:
            try:
                asProducts.append(sProduct)
            except:
                continue
    else:
        wasdiLog("[ERROR] waspy.getProductsByWorkspaceId: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return asProducts


def getDetailedProductsByWorkspaceId(sId=None):
    if sId is None or sId == '':
        _log('waspy.getDetailedProductsByActiveWorkspace: defaulting')
        sId = getActiveWorkspaceId()

    sUrl = getBaseUrl() + '/product/byws' + '?workspace=' + sId
    asHeaders = _getStandardHeaders()
    oResponse = None
    try:
        oResponse = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oE:
        wasdi.wasdiLog('[ERROR] waspy.getProductDetailsByWorkspaceId: got ' + str(type(oE)) + ': ' + str(oE) +
                       ' while geting products list, aborting')
    aoProducts = []

    if oResponse is None:
        return aoProducts

    try:
        if not oResponse.ok:
            wasdi.wasdiLog('[ERROR] waspy.getProductDetailsByWorkspaceId: API Return Code ' + oResponse.status_code +' - Reason ' + oResponse.reason)
            return aoProducts
    except Exception as oE:
        wasdi.wasdiLog(
            '[ERROR] waspy.getProductDetailsByWorkspaceId: could not parse response due to ' + str(type(oE)) + ': ' + str(oE))
        return aoProducts

    try:
        aoProducts = oResponse.json()
    except Exception as oE:
        wasdiLog('[ERROR] waspy.getProductDetailsByWorkspaceId: could not convert response due to ' + str(type(oE)) + ': ' + str(oE))

    return aoProducts


def getProductsByActiveWorkspace():
    """
    Get the list of products in the active workspace

    :return: the list is an array of string. Can be empty if there is any error
    """
    global m_sActiveWorkspace

    return getProductsByWorkspaceId(m_sActiveWorkspace)


def getPath(sFile=''):
    """
    Get Local File Path. If the file exists and needed the file will be automatically downloaded.
    Returns the full local path where to read or write sFile

    :param: sFile name of the file
    :return: Local path where to read or write sFile 
    """

    if fileExistsOnWasdi(sFile) is True:
        return getFullProductPath(sFile)
    else:
        return getSavePath() + str(sFile)

def _internalGetPath(sProductName):
    """
    Iternal get path. Resolve the path of a Product
    """
    global m_sActiveWorkspace
    global m_sWorkspaceOwner
    if getOnlyWorkspaceFolderMounted() and (getIsOnServer() or getIsOnExternalServer()):
        return os.path.join(getBasePath(), sProductName)
    else:
        sWorkspaceOwner = m_sWorkspaceOwner
        if sWorkspaceOwner is None:
            sWorkspaceOwner = ""
        if sWorkspaceOwner == "":
            sWorkspaceOwner = m_sUser

        return os.path.join(getBasePath(), sWorkspaceOwner, m_sActiveWorkspace, sProductName)


def getFullProductPath(sProductName):
    """
    Get the full local path of a product given the product name. If auto download is true and the code is running locally, WASDI will download the image and keep the file on the local PC
    Use the output of this API to get the full path to open a file

    :param sProductName: name of the product to get the path open (WITH the final extension)
    :return: local path of the Product File
    """
    global m_sActiveWorkspace
    global m_bDownloadActive
    global m_sWorkspaceOwner

    # Normalize the path and extract the name
    if sProductName.startswith(getSavePath()):
        sProductName = sProductName.replace(getSavePath(),"")
    #sProductName = os.path.basename(os.path.normpath(sProductName))

    # Get the full path
    sFullPath = _internalGetPath(sProductName)

    # Do we need to download it?
    bDownloadFileFromServer = False

    if os.path.isfile(sFullPath):
        # There is a local file: do we need to look the checksum?
        if getEnableChecksum():
            # Yes
            aoProperties = getProductProperties(sProductName)
            if aoProperties is not None:
                if "checksum" in aoProperties:
                    sLocalChecksum = getMD5Checksum(sFullPath)
                    sRemoteChecksum = aoProperties["checksum"]
                    if sLocalChecksum != sRemoteChecksum:
                        wasdiLog(
                            '[INFO] waspy.getFullProductPath: Local file exists but looks different from the on in the server')
                        bDownloadFileFromServer = True
    else:
        # If we are on the local PC
        if getIsOnServer() is False:
            # If the download is active
            if m_bDownloadActive is True:
                # If the file exists on server
                if fileExistsOnWasdi(sProductName) is True:
                    wasdiLog('[INFO] waspy.getFullProductPath: Local file does not exists')
                    bDownloadFileFromServer = True
        else:
            try:
                # If the file exists on server
                if fileExistsOnWasdi(sProductName) is True:
                    wasdiLog('[WARNING] waspy.getFullProductPath: WASDI FILE ON ANOTHER NODE: START DOWNLOAD... PLEASE WAIT')
                    bDownloadFileFromServer = True
            except:
                wasdiLog('[ERROR] waspy.getFullProductPath: error downloading the file from the workspace node')

    if bDownloadFileFromServer:
        # Download The File from WASDI
        wasdiLog('[INFO] waspy.getFullProductPath: LOCAL WASDI FILE MISSING: START DOWNLOAD... PLEASE WAIT')
        _downloadFile(sProductName)
        wasdiLog('[INFO] waspy.getFullProductPath: DONWLOAD COMPLETED')

    return sFullPath


def getSavePath():
    """
    Get the local base save path for a product. To save use this path + fileName. Path already include '/' as last char

    :return: local path to use to save files (with '/' as last char)
    """

    sFullPath = _internalGetPath("")

    return sFullPath


def getProcessStatus(sProcessId, sDestinationWorkspaceUrl = None):
    """
    get the status of a Process

    :param sProcessId: Id of the process to query
    :param sDestinationWorkspaceUrl: allow to ask for a status of a Process That is not in the actual Active Node
    :return: the status or 'ERROR' if there was any error

    STATUS are  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
    """
    global m_sBaseUrl
    global m_sSessionId

    if sProcessId is None:
        wasdiLog('[ERROR] waspy.getProcessStatus: Passed None, expected a process ID' +
                 '  ******************************************************************************')
        return "ERROR"

    if sProcessId == '':
        wasdiLog('[ERROR] waspy.getProcessStatus: Passed empty, expected a process ID' +
                 '  ******************************************************************************')
        return "ERROR"

    asHeaders = _getStandardHeaders()
    payload = {'procws': sProcessId}

    if sDestinationWorkspaceUrl is not None and sDestinationWorkspaceUrl!="":
        sUrl = sDestinationWorkspaceUrl + '/process/getstatusbyid'
    else:
        sUrl = getWorkspaceBaseUrl() + '/process/getstatusbyid'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, params=payload, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getProcessStatus: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ""

    sStatus = ''

    if oResult.ok:
        try:
            sStatus = oResult.text
        except Exception as oE:
            wasdiLog('[ERROR] waspy.getProcessStatus: ' + str(oE))
            sStatus = 'ERROR'
    else:
        wasdiLog("[ERROR] waspy.getProcessStatus: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return sStatus


def updateProcessStatus(sProcessId, sStatus, iPerc=-1):
    """
    Update the status of a process

    :param sProcessId: Id of the process to update.

    :param sStatus: Status of the process. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY

    :param iPerc: percentage of complete of the processor. Use -1 to ignore Percentage. Use a value between 0 and 100 to set it.

    :return: the updated status as a String or '' if there was any problem
    """

    if sProcessId is None:
        wasdiLog('[ERROR] waspy.updateProcessStatus: cannot update status, process ID is None' +
                 '  ******************************************************************************')
        return ''
    elif sProcessId == '':
        return ''

    if sStatus is None:
        wasdiLog('[ERROR] waspy.updateProcessStatus: cannot update status, status is None' +
                 '  ******************************************************************************')
        return ''
    elif sStatus not in {'CREATED', 'RUNNING', 'STOPPED', 'DONE', 'ERROR', 'WAITING', 'READY'}:
        wasdiLog(
            '[ERROR] waspy.updateProcessStatus: sStatus must be a string in: ' +
            '{CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY' +
            '  ******************************************************************************')
        return ''

    if iPerc is None:
        wasdiLog('[ERROR] waspy.updateProcessStatus: percentage is None' +
                 '  ******************************************************************************')
        return ''

    if iPerc < 0:
        if iPerc != -1:
            wasdiLog('[ERROR] waspy.updateProcessStatus: iPerc < 0 not valid' +
                     '  ******************************************************************************')
            return ''
        else:
            # _log('[INFO] waspy.updateProcessStatus: iPerc = -1 - Not considered')
            pass
    elif iPerc > 100:
        wasdiLog('[ERROR] waspy.updateProcessStatus: iPerc > 100 not valid' +
                 '  ******************************************************************************')
        return ''

    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()
    payload = {'procws': sProcessId, 'status': sStatus, 'perc': iPerc}

    sUrl = getWorkspaceBaseUrl() + '/process/updatebyid'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, params=payload, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.updateProcessStatus there was an error contacting the API " + str(oEx))

    if oResult is None:
        return ""

    sStatus = ''

    if oResult.ok:
        oJsonResult = oResult.json()
        try:
            sStatus = oJsonResult['status']
        except:
            sStatus = ''
    else:
        wasdiLog("[ERROR] waspy.updateProcessStatus: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return sStatus


def updateStatus(sStatus, iPerc=-1):
    """
    Update the status of the running process

    :param sStatus: new status. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY

    :param iPerc: new Percentage.-1 By default, means no change percentage. Use a value between 0 and 100 to set it.
    :return: the updated status as a String or '' if there was any problem
    """
    try:

        if getIsOnServer() is False and getIsOnExternalServer() is False:
            _log("[INFO] waspy.updateStatus: Running Locally, will not update status on server")
            return sStatus

        return updateProcessStatus(getProcId(), sStatus, iPerc)
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.updateStatus: exception " + str(oEx))
        return ''


def waitProcess(sProcessId, sDestinationWorkspaceUrl=None):
    """
    Wait for a process to End

    :param sProcessId: Id of the process to wait
    :param sDestinationWorkspaceUrl: base url of the destination workspace
    :return: output status of the process
    """
    if sProcessId is None:
        _log('[ERROR] waspy.waitProcess: Passed None, expected a process ID' +
             '  ******************************************************************************')
        return "ERROR"

    if sProcessId == '':
        _log('[ERROR] waspy.waitProcess: Passed empty, expected a process ID' +
             '  ******************************************************************************')
        return "ERROR"

    if sProcessId in {"DONE", "STOPPED", "ERROR", "CREATED", "WAITING", "READY"}:
        _log('[INFO] waspy.waitProcess: process ID is indeed alrady a status, returning it' +
             '  ******************************************************************************')
        return sProcessId

    # Put this processor in WAITING
    updateStatus("WAITING")

    sStatus = ''

    try:
        while sStatus not in {"DONE", "STOPPED", "ERROR"}:
            try:
                sStatus = getProcessStatus(sProcessId, sDestinationWorkspaceUrl)

                if sStatus in {"DONE", "STOPPED", "ERROR"}:
                    break

                time.sleep(getPollingSleepSeconds())
            except Exception as oInnerEx:
                _log("waspy.waitProcess: [ERROR] Exception in the waitProcess loop " + str(oInnerEx))
    except:
        _log("waspy.waitProcess: [ERROR] Exception in the waitProcess")

    # Wait to be resumed
    _waitForResume()

    return sStatus


def waitProcesses(asProcIdList):
    """
    Wait for a list of processes to wait.
    The list of processes is an array of strings, each with a proc id to wait
    

    :param asProcIdList: list of strings, procId, to wait
    
    :return: list of strings with the same number of elements in input, with the exit status of the processes
    """

    asHeaders = _getStandardHeaders()

    sUrl = getWorkspaceBaseUrl() + '/process/statusbyid'

    asReturnStatus = []

    # Check the input
    if asProcIdList is None:
        wasdiLog("[WARNING] waspy.waitProcesses asProcIdList is none, return empty list")
        return asReturnStatus

    if not isinstance(asProcIdList, list):
        wasdiLog("[WARNING] waspy.waitProcesses asProcIdList is not a list, return empty list")
        return asReturnStatus

    if len(asProcIdList) == 0:
        wasdiLog("[WARNING] waspy.waitProcesses asProcIdList is empty, return empty list")
        return asReturnStatus

    iTotalTime = 0

    # Pre-filter the list: skip None, empty or "status" processes Id
    asFilteredProcs = []
    asFilteredIndexes = []

    iIndex = 0

    for sProcId in asProcIdList:
        iIndex = iIndex + 1
        if sProcId is None:
            # Not valid
            continue
        if sProcId == "":
            # Empty
            continue
        if sProcId in  {"DONE", "STOPPED", "ERROR", "CREATED", "WAITING", "READY"}:
            # This is already a result
            continue
        asFilteredProcs.append(str(sProcId))
        asFilteredIndexes.append(iIndex-1)

    if len(asFilteredProcs) == 0:
        wasdiLog("[INFO] waspy.waitProcesses asProcIdList are all empty, None or already status. Return immediatly")
        return  asProcIdList

    # Put this process in WAITING
    updateStatus("WAITING")

    bAllDone = False

    while not bAllDone:

        oResult = None
        try:
            oResult = requests.post(sUrl, data=json.dumps(asFilteredProcs), headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.waitProcesses: there was an error contacting the API " + str(oEx))

        if oResult is not None:
            if oResult.ok:
                asResultStatus = oResult.json()
                asReturnStatus = asResultStatus

                bAllDone = True

                for sProcStatus in asResultStatus:
                    if not (sProcStatus == "DONE" or sProcStatus == "ERROR" or sProcStatus == "STOPPED"):
                        bAllDone = False
                        break
            else:
                wasdiLog("[ERROR] waspy.waitProcesses: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            


        if not bAllDone:
            # Sleep a little bit
            sleep(getPollingSleepSeconds())
            # Trace the time needed
            iTotalTime = iTotalTime + getPollingSleepSeconds()

    # Wait to be resumed
    _waitForResume()

    asFullReturnStatus = []

    for iIndex in range(len(asProcIdList)):
        if iIndex in asFilteredIndexes:
            asFullReturnStatus.append(str(asReturnStatus[asFilteredIndexes.index(iIndex)]))
        else:
            asFullReturnStatus.append(str(asProcIdList[iIndex]))

    # Return the list of status
    return asFullReturnStatus


def updateProgressPerc(iPerc):
    """
    Update the actual progress Percentage of the processor

    :param iPerc: new Percentage. Use a value between 0 and 100 to set it. The value must be an integer
    :return: updated status of the process or '' if there was any error
    """
    try:
        _log('[INFO] waspy.updateProgressPerc( ' + str(iPerc) + ' )')
        if iPerc is None:
            wasdiLog('[ERROR] waspy.updateProgressPerc: Passed None, expected a percentage' +
                     '  ******************************************************************************')
            return ''

        if 0 > iPerc or 100 < iPerc:
            wasdiLog(
                '[WARNING] waspy.updateProgressPerc: passed' + str(iPerc) + ', automatically resetting in [0, 100]')
            if iPerc < 0:
                iPerc = 0
            if iPerc > 100:
                iPerc = 100

        if getIsOnServer() is False and getIsOnExternalServer() is False:
            _log("[INFO] waspy.updateProgressPerc: Running locally, will not updateProgressPerc on server")
            return "RUNNING"
        else:
            if (getProcId() is None) or (len(getProcId()) < 1):
                wasdiLog('[ERROR] waspy.updateProgressPerc: Cannot update progress: process ID is not known' +
                         '  ******************************************************************************')
                return ''

        sStatus = "RUNNING"
        sUrl = getWorkspaceBaseUrl() + "/process/updatebyid?procws=" + getProcId() + "&status=" + sStatus + "&perc=" + str(
            iPerc) + "&sendrabbit=1"
        asHeaders = _getStandardHeaders()

        oResponse = None

        try:
            oResponse = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.updateProgressPerc: there was an error contacting the API " + str(oEx))

        sResult = ""

        if oResponse is None:
            return sResult

        if oResponse.ok:
            oJson = oResponse.json()
            if (oJson is not None) and ("status" in oJson):
                sResult = str(oJson['status'])
        else:
            wasdiLog("[ERROR] waspy.updateProgressPerc: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)            

        return sResult
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.updateProgressPerc: exception " + str(oEx))
        return ''


def setProcessPayload(sProcessId, data):
    """
    Saves the Payload of a process

    :param sProcessId: Id of the process

    :param data: data to write in the payload. Suggestion to use a JSON
    :return: the updated status as a String or '' if there was any problem
    """
    global m_sBaseUrl
    global m_sSessionId

    try:
        asHeaders = _getStandardHeaders()

        sUrl = getWorkspaceBaseUrl() + '/process/setpayload?procws=' + sProcessId

        oResult = None

        try:
            oResult = requests.post(sUrl, data=json.dumps(data), headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.setProcessPayload: there was an error contacting the API " + str(oEx))

        sStatus = ''

        if oResult is None:
            return sStatus

        if oResult.ok:
            oJsonResult = oResult.json()
            try:
                sStatus = oJsonResult['status']
            except:
                sStatus = ''
        else:
            wasdiLog("[ERROR] waspy.setProcessPayload: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

        return sStatus
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.setProcessPayload: exception " + str(oEx))
        return ''


def setPayload(data):
    """
    Sets the payload of the current running process.
    The payload is saved only when run on Server. In local mode is just a print.

    :param data: data to save in the payload. Suggestion is to use JSON
    return None
    """
    global m_sPayload

    try:
        m_sPayload = json.dumps(data)
    except:
        _log('waspy.setPayload() error saving m_sPayload')

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        setProcessPayload(getProcId(), data)
    else:
        _log('waspy.setPayload( ' + str(data))


def getProcessorPayload(sProcessObjId, bAsJson=False):
    """
    Retrieves the payload

    :param sProcessObjId: a valid processor obj id

    :param bAsJson: flag to indicate whether the payload is a json object: if True, then a dictionary is returned
    :return: the processor payload if present, None otherwise
    """
    global m_sPayload

    try:
        if sProcessObjId is None:
            wasdiLog('[WARNING] waspy.getProcessorPayload: process obj id is None, aborting')
            return None

        sUrl = getWorkspaceBaseUrl() + '/process/payload'
        asParams = {'procws': sProcessObjId}
        asHeaders = _getStandardHeaders()

        oResponse = None

        try:
            oResponse = requests.get(url=sUrl, headers=asHeaders, params=asParams, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.getProcessorPayload: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            wasdiLog('[ERROR] waspy.getProcessorPayload: response is None, failing')
            return None

        if oResponse.ok:
            if bAsJson:
                return oResponse.json()
            else:
                return oResponse.text
        else:
            wasdiLog(
                '[ERROR] waspy.getProcessorPayload: API Return Code: ' + str(oResponse.status_code) + ': ' + str(
                    oResponse.text))
    except Exception as oE:
        wasdiLog('[ERROR] waspy.getProcessorPayload: ' + str(oE))
    return None


def getProcessorPayloadAsJson(sProcessObjId):
    """
    Retrieves the payload in json format using getProcessorPayload

    :param sProcessObjId: a valid processor obj id
    :return: the processor payload if present as a dictionary, None otherwise
    """
    return getProcessorPayload(sProcessObjId, True)

def addChartToPayload(aoXValues, aoYValues, sChartType ="line", sChartName ="", sSerieName ="", sXAxisName = False, sYAxisName = False):
    """
    Adds a chart to the payload

    param aoXValues Array of X Axis Values
    param aoYValues Array of Y Axis Values
    param sChartType Chart Type. The vaid types are enumered in wasdi.ChartType
    param sChartName Name to assign to the chart
    param sSerieName Name to assign to the serie
    param sXAxisName X Axis name. False to hide it.
    param sYAxisName Y Axis name. False to hide it.
    """
    global m_sPayload

    if aoXValues is None:
        wasdiLog('[ERROR] waspy.addChartToPayload: aoXValues is None, return')
        return False

    if aoYValues is None:
        wasdiLog('[ERROR] waspy.addChartToPayload: aoYValues is None, return')
        return False

    if m_sPayload is None:
        m_sPayload = ""

    if m_sPayload == "":
        m_sPayload = "{}"

    try:
        aoPayload = json.loads(m_sPayload)
        if not "wasdi_dashboard" in aoPayload:
            aoPayload["wasdi_dashboard"] = []

        oChart = {}

        if type(sChartType) == str:
            oChart["chart-type"] = sChartType
        elif isinstance(sChartType, wasdi.ChartType):
            oChart["chart-type"] = sChartType.value
        else:
            oChart["chart-type"] = str(sChartType)

        oChart["chart-name"] = sChartName
        oChart["x-axis-name"] = sXAxisName
        oChart["y-axis-name"] = sYAxisName
        oChart["chart-data"] = []

        oChartData = {}
        oChartData["name"] = sSerieName
        oChartData["series"] = []

        iXValuesLength = len(aoXValues)
        iYValuesLength = len(aoYValues)

        iMax = iXValuesLength
        if iYValuesLength<iMax:
            iMax = iYValuesLength

        for iCount in range(0,iMax):
            oChartDataValue = {}
            oChartDataValue["value"] = aoYValues[iCount]
            oChartDataValue["name"] = aoXValues[iCount]
            oChartData["series"].append(oChartDataValue)

        oChart["chart-data"].append(oChartData)

        aoPayload["wasdi_dashboard"].append(oChart)

        setPayload(aoPayload)
    except:
        wasdiLog('[ERROR] waspy.addChartToPayload: error, return')
        return False


def setSubPid(sProcessId, iSubPid):
    """
    Set the sub pid

    :param sProcessId: Id of the process

    :param iSubPid: PID of the physical process
    :return: the updated status as a String or '' if there was any problem
    """
    global m_sBaseUrl
    global m_sSessionId

    try:
        asHeaders = _getStandardHeaders()
        payload = {'procws': sProcessId, 'subpid': iSubPid}

        sUrl = getWorkspaceBaseUrl() + '/process/setsubpid'

        oResult = None

        try:
            oResult = requests.get(sUrl, headers=asHeaders, params=payload, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.setSubPid: there was an error contacting the API " + str(oEx))

        sStatus = ''

        if oResult is None:
            return sStatus

        if oResult.ok:
            oJsonResult = oResult.json()
            try:
                sStatus = oJsonResult['status']
            except:
                sStatus = ''
        else:
            wasdiLog("[ERROR] waspy.setSubPid: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

        return sStatus
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.setSubPid: exception " + str(oEx))
        return ''


def saveFile(sFileName):
    """
    Ingest a new file in the Active WASDI Workspace.
    The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
    To work be sure that the file is on the server

    :param: Name of the file to add to the workpsace
    :return: Status of the operation
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    asHeaders = _getStandardHeaders()
    payload = {'file': sFileName, 'workspace': m_sActiveWorkspace}

    # sUrl = m_sBaseUrl + '/catalog/upload/ingestinws'
    sUrl = getWorkspaceBaseUrl() + '/catalog/upload/ingestinws'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, params=payload, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.saveFile: there was an error contacting the API " + str(oEx))

    sProcessId = ''

    if oResult is None:
        return sProcessId

    if oResult.ok:
        oJsonResult = oResult.json()
        try:
            if oJsonResult['boolValue'] is True:
                sProcessId = oJsonResult['stringValue']
        except:
            sProcessId = ''
    else:
        wasdiLog("[ERROR] waspy.saveFile: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            

    return sProcessId


def _downloadFile(sFileName):
    """
    Download a file from WASDI

    :param sFileName: file to download
    :return: None
    """

    _log('[INFO] waspy.downloadFile( ' + sFileName + ' )')

    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    asHeaders = _getStandardHeaders()
    payload = {'filename': sFileName}

    sUrl = getWorkspaceBaseUrl()
    sUrl += '/catalog/downloadbyname?'
    sUrl += 'filename='
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()
    sUrl += "&procws="
    sUrl += getProcId()

    _log('[INFO] waspy.downloadfile: send request to configured url ' + sUrl)

    oResponse = None

    try:
        oResponse = requests.get(sUrl, headers=asHeaders, params=payload, stream=True, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy._downloadFile: there was an error contacting the API " + str(oEx))

    if oResponse is None:
        return

    if oResponse.status_code == 200:
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

        asParts = sFileName.split("/")
        sFolders = ""
        if asParts is not  None:
            if len(asParts) > 1:
                for sPart in asParts[:-1]:
                    sFolders +=  sPart + "/"

        if sFolders != "":
            sAttachmentName = sFolders + sAttachmentName

        sSavePath = os.path.join(sSavePath, sAttachmentName)

        if os.path.exists(os.path.dirname(sSavePath)) == False:
            try:
                os.makedirs(os.path.dirname(sSavePath))
            except:  # Guard against race condition
                _log('[ERROR] waspy.downloadFile: cannot create File Path, aborting' +
                      '  ******************************************************************************')
                return

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
        wasdiLog("[ERROR] waspy.downloadFile: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)

    return


def wasdiLog(sLogRow):
    """
    Write one row of Log

    :param sLogRow: text to log
    :return: None
    """

    sForceLogRow = str(sLogRow)

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        asHeaders = _getStandardHeaders()
        sUrl = getWorkspaceBaseUrl() + '/processors/logs/add?processworkspace=' + getProcId()

        oResult = None

        try:
            oResult = requests.post(sUrl, data=sForceLogRow, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            _log("[ERROR] waspy.wasdiLog: there was an error contacting the API " + str(oEx))

        if oResult is None:
            print('[WARNING] waspy.wasdiLog: could not log')
            _log(sForceLogRow)
            return

        if oResult.ok is not True:
            wasdiLog("[ERROR] waspy.wasdiLog: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
            return
    else:
        _log(sForceLogRow)


def deleteProduct(sProduct):
    """
    Delete a Product from a Workspace
    NOTE: the method DOES NOT delete the pyshical file on your local Disk if the app is running in your environment.

    :param sProduct: Name of the product to delete (WITH EXTENSION)
    :return: True if the file has been deleted, False if there was any error
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    if sProduct is None:
        _log('[ERROR] waspy.deleteProduct: product passed is None' +
              '  ******************************************************************************')
        return False

    asHeaders = _getStandardHeaders()
    sUrl = getWorkspaceBaseUrl()
    sUrl += "/product/delete?name="
    sUrl += sProduct
    sUrl += "&deletefile=true&workspace="
    sUrl += m_sActiveWorkspace
    sUrl += "&deletelayer=true"

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.deleteProduct: there was an error contacting the API " + str(oEx))

    if oResult is None:
        wasdiLog('[ERROR] waspy.deleteProduct: deletion failed' +
                 '  ******************************************************************************')
        return False

    if oResult.ok is not True:
        wasdiLog("[ERROR] waspy.deleteProduct: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
    else:
        return oResult.ok


def searchEOImages(sPlatform, sDateFrom=None, sDateTo=None,
                   fULLat=None, fULLon=None, fLRLat=None, fLRLon=None,
                   sProductType=None, iOrbitNumber=None,
                   sSensorOperationalMode=None, sCloudCoverage=None,
                   sProvider=None, oBoundingBox=None, aoParams=None, sFileName=None):
    """
    Search EO images

    :param sPlatform: satellite platform:(S1|S2|S3|S5P|VIIRS|L8|ENVI|ERA5)

    :param sDateFrom: inital date YYYY-MM-DD

    :param sDateTo: final date YYYY-MM-DD

    :param fULLat: Latitude of Upper-Left corner

    :param fULLon: Longitude of Upper-Left corner

    :param fLRLat: Latitude of Lower-Right corner

    :param fLRLon: Longitude of Lower-Right corner

    :param sProductType: type of EO product; Can be null. FOR "S1" -> "SLC","GRD", "OCN". FOR "S2" -> "S2MSI1C","S2MSI2Ap","S2MSI2A". FOR "VIIRS" -> "VIIRS_1d_composite","VIIRS_5d_composite". FOR "L8" -> "L1T","L1G","L1GT","L1GS","L1TP". For "ENVI" -> "ASA_IM__0P", "ASA_WS__0P"

    :param iOrbitNumber: orbit number

    :param sSensorOperationalMode: sensor operational mode

    :param sCloudCoverage: interval of allowed cloud coverage, e.g. "[0 TO 22.5]"

    :param sProvider: WASDI Data Provider to query (AUTO|LSA|ONDA|CREODIAS|SOBLOO|VIIRS|SENTINEL). None means default node provider = AUTO.
    
    :param oBoundingBox: alternative to the float lat-lon corners: an object expected to have these attributes: oBoundingBox["northEast"]["lat"], oBoundingBox["southWest"]["lng"], oBoundingBox["southWest"]["lat"], oBoundingBox["northEast"]["lng"]
    
    :param aoParams: dictionary of search keys to add to the query. The system will add key=value to the query sent to WASDI. The parameters for each collection can be found on the on line documentation
    
    :param sFileName: name of a specific file to search

    :return: a list of results represented as a Dictionary with many properties. The dictionary has the "fileName" and "relativeOrbit" properties among the others 
    """
    aoReturnList = []

    if sPlatform is None:
        _log('[ERROR] waspy.searchEOImages: platform cannot be None' +
              '  ******************************************************************************')
        return aoReturnList

    if sPlatform == "S1":
        if sProductType is not None:
            if not (sProductType == "SLC" or sProductType == "GRD" or sProductType == "OCN"):
                wasdiLog("[WARNING] waspy.searchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" +
                         sProductType +
                         '  ******************************************************************************')

    if sPlatform == "S2":
        if sProductType is not None:
            if not (sProductType == "S2MSI1C" or sProductType == "S2MSI2Ap" or sProductType == "S2MSI2A"):
                wasdiLog(
                    "[WARNING] waspy.searchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received ["
                    + sProductType + "]" +
                    '  ******************************************************************************')

    if sPlatform == "VIIRS":
        if sProductType is not None:
            if not (sProductType == "VIIRS_5d_composite" or sProductType == "VIIRS_1d_composite" or sProductType == "VNP21A1D" or sProductType == "VNP21A1N"):
                wasdiLog(
                    "[WARNING] waspy.searchEOImages: Available Product Types for VIIRS; VIIRS_1d_composite, VIIRS_5d_composite. Received ["
                    + sProductType + "]" +
                    '  ******************************************************************************')

    if sPlatform == "L8":
        if sProductType is not None:
            if not (
                    sProductType == "L1T" or sProductType == "L1G" or sProductType == "L1GT" or sProductType == "L1GS" or sProductType == "L1TP"):
                wasdiLog(
                    "[WARNING] waspy.searchEOImages: Available Product Types for L8; L1T, L1G, L1GT, L1GS, L1TP. Received ["
                    + sProductType + "]" +
                    '  ******************************************************************************')

    if sPlatform == "ENVI":
        if sProductType is not None:
            if not (sProductType == "ASA_IM__0P" or sProductType == "ASA_WS__0P"):
                wasdiLog(
                    "[WARNING] waspy.searchEOImages: Available Product Types for VIIRS; ASA_IM__0P, ASA_WS__0P. Received ["
                    + sProductType + "]" +
                    '  ******************************************************************************')

    if sDateFrom is None:
        wasdiLog("[WARNING] waspy.searchEOImages: sDateFrom is None, assume very old one 04/10/1957" +
                 '  ******************************************************************************')
        sDateFrom = "1957-10-04"

    # if (len(sDateFrom) < 10) or (sDateFrom[4] != '-') or (sDateFrom[7] != '-'):
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateFrom)):
        wasdiLog("[ERROR] waspy.searchEOImages: sDateFrom must be in format YYYY-MM-DD" +
                 '  ******************************************************************************')
        return aoReturnList

    if sDateTo is None:
        wasdiLog("[WARNING] waspy.searchEOImages: sDateTo is None, assume today" +
                 '  ******************************************************************************')
        oToday = datetime.today()
        sDateTo = oToday.strftime("%Y-%m-%d")

    # if len(sDateTo) < 10 or sDateTo[4] != '-' or sDateTo[7] != '-':
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateTo)):
        wasdiLog("[ERROR] waspy.searchEOImages: sDateTo must be in format YYYY-MM-DD" +
                 '  ******************************************************************************')
        return aoReturnList

    if oBoundingBox is not None:
        if isinstance(oBoundingBox, str):
            asBBox = oBoundingBox.split(",")

            if len(asBBox) != 4:
                wasdiLog(
                    "[WARNING] waspy.searchEOImages: BoundingBox is a string but in bad format. Must be LATN;LONW;LATS;LONE")
            else:
                try:
                    fTempLatN = float(asBBox[0])
                    fTempLonW = float(asBBox[1])
                    fTempLatS = float(asBBox[2])
                    fTempLonE = float(asBBox[3])

                    fULLat = fTempLatN
                    fULLon = fTempLonW
                    fLRLat = fTempLatS
                    fLRLon = fTempLonE
                except:
                    wasdiLog(
                        "[WARNING] waspy.searchEOImages: BoundingBox is a string but in bad format, not all are floats")
        else:
            try:
                fTempLatN = oBoundingBox["northEast"]["lat"]
                fTempLonW = oBoundingBox["southWest"]["lng"]
                fTempLatS = oBoundingBox["southWest"]["lat"]
                fTempLonE = oBoundingBox["northEast"]["lng"]

                fULLat = fTempLatN
                fULLon = fTempLonW
                fLRLat = fTempLatS
                fLRLon = fTempLonE
            except:
                wasdiLog("[WARNING] waspy.searchEOImages: exception decoding BoundingBox")

    if sCloudCoverage is not None:
        # Force to be a String
        sCloudCoverage = str(sCloudCoverage)
        sCloudCoverage = sCloudCoverage.upper()

    # create query string:

    sQuery = ""

    if sFileName is not None:
        sQuery += sFileName

    # platform name
    sQuery += "( platformname:"
    if sPlatform == "S2":
        sQuery += "Sentinel-2 "
    elif sPlatform == "S1":
        sQuery += "Sentinel-1"
    elif sPlatform == "S3":
        sQuery += "Sentinel-3"
    elif sPlatform == "VIIRS":
        sQuery += "VIIRS"
    elif sPlatform == "L8":
        sQuery += "Landsat-*"
    elif sPlatform == "ENVI":
        sQuery += "Envisat"
    elif sPlatform == "S5P":
        sQuery += "Sentinel-5P"
    elif sPlatform == "ERA5":
        sQuery += "ERA5"
    else:
        sQuery += sPlatform

    # If available add product type
    if sProductType is not None:
        sQuery += " AND producttype:" + str(sProductType)
    else:
        if sPlatform == "VIIRS":
            sQuery += " AND producttype:VIIRS_1d_composite"

    # If available Sensor Operational Mode
    if (sSensorOperationalMode is not None) and (sPlatform == "S1"):
        sQuery += " AND sensoroperationalmode:" + str(sSensorOperationalMode)

    # If available cloud coverage
    if (sCloudCoverage is not None) and (sPlatform == "S2"):
        sQuery += " AND cloudcoverpercentage:" + str(sCloudCoverage)

    # If available add orbit number
    if iOrbitNumber is not None:
        if isinstance(iOrbitNumber, int):
            sQuery += " AND relativeorbitnumber:" + str(iOrbitNumber)
        else:
            _log('[WARNING] waspy.searchEOImages: iOrbitNumber is' + str(iOrbitNumber),
                  ', but it should be an integer')
            try:
                iTmp = int(iOrbitNumber)
                wasdiLog('[WARNING] waspy.searchEOImages: iOrbitNumber converted to: ' + str(iTmp))
                sQuery += str(iTmp)
            except:
                wasdiLog('[WARNING] waspy.searchEOImages: could not convert iOrbitNumber to an int, ignoring it' +
                         '  ******************************************************************************')

    if aoParams is not None:
        try:
            for sKey in aoParams:
                sQuery += " AND " + sKey + ":" + aoParams[sKey]
        except:
            wasdiLog('[WARNING] waspy.searchEOImages: exception adding generic params')

    # Close the first block
    sQuery += ") "

    # Date Block
    sQuery += "AND ( beginPosition:[" + str(sDateFrom) + "T00:00:00.000Z TO " + str(sDateTo) + "T23:59:59.999Z]"
    sQuery += "AND endPosition:[" + str(sDateFrom) + "T00:00:00.000Z TO " + str(sDateTo) + "T23:59:59.999Z]"

    # Close the second block
    sQuery += ") "

    # footprint polygon
    if (fULLat is not None) and (fULLon is not None) and (fLRLat is not None) and (fLRLon is not None):
        sFootPrint = "( footprint:\"intersects(POLYGON(( " + str(fULLon) + " " + str(fLRLat) + "," + \
                     str(fULLon) + " " + str(fULLat) + "," + str(fLRLon) + " " + str(fULLat) + "," + str(fLRLon) + \
                     " " + str(fLRLat) + "," + str(fULLon) + " " + str(fLRLat) + ")))\") AND "
        sQuery = sFootPrint + sQuery

    sQueryBody = "[\"" + sQuery.replace("\"", "\\\"") + "\"]"

    if sProvider is None:
        sProvider = "AUTO"

    sQuery = "providers=" + sProvider

    try:
        sUrl = getBaseUrl() + "/search/querylist?" + sQuery
        _log("[INFO] searchEOImages: Start Provider Query")
        asHeaders = _getStandardHeaders()

        oResponse = None

        try:
            oResponse = requests.post(sUrl, data=sQueryBody, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.searchEOImages: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            return aoReturnList

        _log("[INFO] searchEOImages: Query Done, starting conversion")
        try:
            # populate list from response
            oJsonResponse = oResponse.json()
            _log("[INFO] searchEOImages: Conversion done")
            aoReturnList = oJsonResponse
        except Exception as oEx:
            wasdiLog('[ERROR] waspy.searchEOImages: exception while trying to convert response into JSON object' +
                     '  ******************************************************************************')
            return aoReturnList

        # For each got result
        for oSearchResult in aoReturnList:

            oSearchResult["fileName"] = ""
            oSearchResult["relativeOrbit"] = -1
            oSearchResult["provider"] = sProvider

            # Initialize the fileName property
            if oSearchResult["title"] is not None:

                # Se the file name
                if sPlatform == "S1" or sPlatform == "S2":
                    oSearchResult["fileName"] = oSearchResult["title"] + ".zip"
                elif sPlatform == "VIIRS":
                    oSearchResult["fileName"] = oSearchResult["title"].replace(".part", "_part")
                    oSearchResult["title"] = oSearchResult["title"].replace(".part", "_part")
                else:
                    oSearchResult["fileName"] = oSearchResult["title"]

            # Initialized the relative orbit
            if oSearchResult["properties"] is not None:
                if "relativeorbitnumber" in oSearchResult["properties"]:
                    # Set the relative Orbit
                    oSearchResult["relativeOrbit"] = oSearchResult["properties"]["relativeorbitnumber"]

        return aoReturnList
    except Exception as oEx:
        wasdiLog('[ERROR] waspy.searchEOImages: an error occured' +
                 '  ******************************************************************************')
        wasdiLog(type(oEx))
        wasdiLog(oEx)

    return aoReturnList


def getFoundProductName(aoProduct):
    """
    Get The name of a product from a Dictionary returned by Search EO Images

    :param aoProduct: dictionary representing the product as returned by Search EO Images
    :return: product name or '' if there was any error
    """
    if aoProduct is None:
        wasdiLog('[ERROR] waspy.getFoundProductName: product is None, aborting' +
                 '  ******************************************************************************')
        return ''
    elif "title" not in aoProduct:
        wasdiLog('[ERROR] waspy.getFoundProductName: title not found in product, aborting' +
                 '  ******************************************************************************')
        return ''
    else:
        return aoProduct['title']


def fileExistsOnWasdi(sFileName):
    """
    checks if a file already exists on WASDI or not

    :param sFileName: file name with extension
    :return: True if the file exists, False otherwise
    """

    if sFileName is None:
        wasdiLog('[ERROR] waspy.fileExistsOnWasdi: file name must not be None' +
                 '  ******************************************************************************')
        return False
    if len(sFileName) < 1:
        wasdiLog('[ERROR] waspy.fileExistsOnWasdi: File name too short' +
                 '  ******************************************************************************')
        return False

    sSessionId = getSessionId()
    sActiveWorkspace = getActiveWorkspaceId()

    sUrl = getWorkspaceBaseUrl()
    sUrl += "/catalog/checkdownloadavaialibitybyname?token="
    sUrl += sSessionId
    sUrl += "&filename="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += sActiveWorkspace
    sUrl += "&procws="
    sUrl += getProcId()

    asHeaders = _getStandardHeaders()

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.fileExistsOnWasdi: there was an error contacting the API " + str(oEx))

    if oResult is None:
        wasdiLog('[ERROR] waspy.fileExistsOnWasdi: failed contacting the server' +
                 '  ******************************************************************************')
        return False

    if oResult.status_code <200 or oResult.status_code >299:
        wasdiLog("[ERROR] waspy.fileExistsOnWasdi: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
        return False
    else:
        try:
            oJsonResponse = oResult.json()
            if "boolValue" in oJsonResponse:
                return oJsonResponse["boolValue"]
            else:
                return False
        except:
            return False


def getProductBBOX(sFileName):
    """
    Gets the bounding box of a file

    :param sFileName: name of the file to query for bounding box
    :return: Bounding Box if available as a String comma separated in form SOUTH,WEST,EST,NORTH
    """

    sUrl = getBaseUrl()
    sUrl += "/product/byname?name="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()

    asHeaders = _getStandardHeaders()

    oResponse = None

    try:
        oResponse = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getProductBBOX: there was an error contacting the API " + str(oEx))

    try:
        if oResponse is None:
            wasdiLog('[ERROR] waspy.getProductBBOX: cannot get bbox for product' +
                     '  ******************************************************************************')
            return ""

        if oResponse.ok is not True:
            wasdiLog("[ERROR] waspy.getProductBBOX: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)
            return ""
        else:
            oJsonResponse = oResponse.json()
            if ("bbox" in oJsonResponse):
                return oJsonResponse["bbox"]

    except:
        return ""

    return ""

def importProductByFileUrl(sFileUrl=None, sName=None, sBoundingBox=None, sProvider=None, sVolumeName=None, sVolumePath=None, sPlatformType=None):
    """
    Imports a product from a Provider in WASDI, starting from the File URL.

    :param sFileUrl: url of the file to import
    
    :param sName: Name of the file to import as returned by the Data Provider

    :param sBoundingBox: declared bounding box of the file to import

    :param sProvider: WASDI Data Provider to use. Use None for Default

    :param sVolumeName: if the file is in a Volume, the name of the volume

    :param sVolumePath: if the file is in a Volume, the path of the file in the volume

    :param sPlatformType: the platform (aka Mission) of the file to ingest
    
    :return: execution status as a STRING. Can be DONE, ERROR, STOPPED.
    """

    sReturn = "ERROR"

    try:
        sProcessId = asynchImportProductByFileUrl(sFileUrl, sName, sBoundingBox, sProvider, sVolumeName, sVolumePath, sPlatformType)
        sReturn = waitProcess(sProcessId)
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.importProductByFileUrl: there was an error importing a product " + str(oEx))

    return sReturn


def asynchImportProductByFileUrl(sFileUrl=None, sName=None, sBoundingBox=None, sProvider=None, sVolumeName=None, sVolumePath=None, sPlatformType=None):
    """
    Asynch Import of a product from a Provider in WASDI, starting from file URL

    :param sFileUrl: url of the file to import as returned by the data provider
    
    :param sName: Name of the file to import as returned by the Data Provider 

    :param sBoundingBox: declared bounding box of the file to import

    :param sProvider: WASDI Data Provider. Use None for default

    :param sVolumeName: if the file is in a Volume, the name of the volume

    :param sVolumePath: if the file is in a Volume, the path of the file in the volume

    :param sPlatformType: the platform (aka Mission) of the file to ingest
    
    :return: ProcessId of the Download Operation, "DONE" if the file is imported or "ERROR" if there is any problem
    """

    sReturn = "ERROR"

    if sProvider is None:
        sProvider = "AUTO"

    sUrl = getBaseUrl()
    sUrl += "/filebuffer/download"

    asHeaders = _getStandardHeaders()

    oImageImportViewModel = {}
    oImageImportViewModel["fileUrl"] = sFileUrl
    oImageImportViewModel["name"] = sName
    oImageImportViewModel["provider"] = sProvider
    oImageImportViewModel["workspace"] = getActiveWorkspaceId()
    oImageImportViewModel["bbox"] = sBoundingBox
    oImageImportViewModel["volumeName"] = sVolumeName
    oImageImportViewModel["volumePath"] = sVolumePath
    oImageImportViewModel["platform"] = sPlatformType

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        oImageImportViewModel["parent"] = getProcId()

    oResponse = None

    try:
        sEncodedBody = json.dumps(oImageImportViewModel)
        oResponse = requests.post(sUrl,data=sEncodedBody, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.asynchImportProductByFileUrl: there was an error contacting the API " + str(oEx))

    if oResponse is None:
        wasdiLog('[ERROR] waspy.importProductByFileUrl: cannot import product' +
                 '  ******************************************************************************')
        return sReturn

    if oResponse.ok is not True:
        wasdiLog("[ERROR] waspy.saveFile: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)
    else:
        oJsonResponse = oResponse.json()
        if ("boolValue" in oJsonResponse) and (oJsonResponse["boolValue"] is True):
            if "stringValue" in oJsonResponse:
                sReturn = str(oJsonResponse["stringValue"])

    return sReturn


def importProduct(oProduct, sProvider=None):
    """
    Imports a product from a Provider in WASDI starting from the object returned by searchEOImages

    :param oProduct: product dictionary as returned by searchEOImages

    :param sProvider: WASDI Data Provider. Use None for default
    :return: execution status as a STRING. Can be DONE, ERROR, STOPPED.
    """

    if oProduct is None:
        wasdiLog("[ERROR] waspy.importProduct: input asPRoduct is none")
        return "ERROR"

    _log('[INFO] waspy.importProduct( ' + str(oProduct) + ' )')

    try:

        sFileUrl = oProduct["link"]

        sBoundingBox = None
        if "footprint" in oProduct:
            sBoundingBox = oProduct["footprint"]

        if sProvider is None:
            if "provider" in oProduct:
                sProvider = oProduct["provider"]

        sName = None
        if "title" in oProduct:
            sName = oProduct['title']

        sPlatform = None
        if "platform" in oProduct:
            sPlatform = oProduct["platform"]

        return importProductByFileUrl(sFileUrl=sFileUrl, sName=sName, sBoundingBox=sBoundingBox, sProvider=sProvider, sVolumeName=oProduct["volumeName"], sVolumePath=oProduct["volumePath"], sPlatformType=sPlatform)
    except Exception as e:
        wasdiLog("[ERROR] waspy.importProduct: exception " + str(e))
        return "ERROR"


def asynchImportProduct(oProduct, sProvider=None):
    """
    Asynch Import a product from a Provider in WASDI starting from the object returned by searchEOImages

    :param oProduct: product dictionary as returned by searchEOImages

    :param sProvider: WASDI Data Provider. Use None for default
    :return: ProcessId of the Download Operation or "ERROR" if there is any problem
    """

    if oProduct is None:
        wasdiLog("[ERROR] waspy.asynchImportProduct: input asPRoduct is none")
        return "ERROR"

    _log('[INFO] waspy.asynchImportProduct( ' + str(oProduct) + ' )')

    try:

        sFileUrl = oProduct["link"]

        sBoundingBox = None
        if "footprint" in oProduct:
            sBoundingBox = oProduct["footprint"]

        if sProvider is None:
            if "provider" in oProduct:
                sProvider = oProduct["provider"]

        sName = None
        if "title" in oProduct:
            sName = oProduct["title"]

        sPlatform = None
        if "platform" in oProduct:
            sPlatform = oProduct["platform"]

        return asynchImportProductByFileUrl(sFileUrl=sFileUrl, sName=sName, sBoundingBox=sBoundingBox,
                                            sProvider=sProvider, sVolumeName=oProduct["volumeName"], sVolumePath=oProduct["volumePath"], sPlatformType=sPlatform)
    except Exception as e:
        wasdiLog("[ERROR] waspy.asynchImportProduct: exception " + str(e))
        return "ERROR"


def importProductList(aoProducts, sProvider=None):
    """
    Imports a list of product from a Provider in WASDI starting from an array of objects returned by searchEOImages

    :param aoProducts: Array of product dictionary as returned by searchEOImages

    :param sProvider: WASDI Data Provider. Use None for default
    :return: execution status as an array of  STRINGs, one for each product in input. Can be CREATED, DONE, ERROR, STOPPED, WAITING, READY
    """

    if aoProducts is None:
        wasdiLog("[ERROR] waspy.importProductList: input asPRoduct is none")
        return "ERROR"

    asReturnList = []

    # For Each product in input
    for oProduct in aoProducts:
        try:
            # Get BBOX and Link from the dictionary
            sBoundingBox = None
            sFileUrl = oProduct["link"]
            if "footprint" in oProduct:
                sBoundingBox = oProduct["footprint"]

            sName = None
            if "title" in oProduct:
                sName = oProduct["title"]

            sActualProvider = sProvider

            if sActualProvider is None:
                if "provider" in oProduct:
                    sActualProvider = oProduct["provider"]

            sPlatform = None
            if "platform" in oProduct:
                sPlatform = oProduct["platform"]

            # Start the download propagating the Asynch Flag
            sReturn = asynchImportProductByFileUrl(sFileUrl=sFileUrl, sName=sName, sBoundingBox=sBoundingBox,
                                                   sProvider=sActualProvider, sVolumeName=oProduct["volumeName"], sVolumePath=oProduct["volumePath"], sPlatformType=sPlatform)

            # Append the process id to the list
            asReturnList.append(sReturn)
        except Exception as e:
            # Error!!
            wasdiLog("[ERROR] waspy.importProductList: exception " + str(e))
            asReturnList.append("ERROR")

    return waitProcesses(asReturnList)


def asynchImportProductList(aoProducts, sProvider=None):
    """
    Asynch Import a list of product from a Provider in WASDI starting from an array of objects returned by searchEOImages

    :param aoProducts: Array of product dictionary as returned by searchEOImages

    :param sProvider: WASDI Data Provider. Use None for default
    :return: array of the ProcessId of the Download Operations. An element can be "ERROR" if there was any problem
    """

    if aoProducts is None:
        wasdiLog("[ERROR] waspy.asynchImportProductList: input asPRoduct is none")
        return "ERROR"

    _log('[INFO] waspy.asynchImportProductList( ' + str(aoProducts) + ' )')

    asReturnList = []

    # For Each product in input
    for oProduct in aoProducts:
        try:
            # Get BBOX and Link from the dictionary
            sBoundingBox = None
            sFileUrl = oProduct["link"]
            if "footprint" in oProduct:
                sBoundingBox = oProduct["footprint"]

            sActualProvider = sProvider

            if sActualProvider is None:
                if "provider" in oProduct:
                    sActualProvider = oProduct["provider"]

            sName = None
            if "title" in oProduct:
                sName = oProduct["title"]

            sPlatform = None
            if "platform" in oProduct:
                sPlatform = oProduct["platform"]

            # Start the download propagating the Asynch Flag
            sReturn = asynchImportProductByFileUrl(sFileUrl=sFileUrl, sName=sName, sBoundingBox=sBoundingBox,
                                                   sProvider=sProvider, sVolumeName=oProduct["volumeName"], sVolumePath=oProduct["volumePath"], sPlatformType=sPlatform)
            # Append the process id to the list
            asReturnList.append(sReturn)
        except Exception as e:
            # Error!!
            wasdiLog("[ERROR] waspy.asynchImportProductList: exception " + str(e))
            asReturnList.append("ERROR")

    # In the ASYNCH MODE return the list of process Id
    return asReturnList


def importAndPreprocess(aoImages, sWorkflow, sPreProcSuffix="_proc.tif", sProvider=None):
    """
    Imports in WASDI and apply a SNAP Workflow to an array of EO Images as returned by searchEOImages

    :param aoImages: array of images to import as returned by searchEOImages

    :param sWorkflow: name of the workflow to apply to each imported images

    :param sProvider: WASDI Data Provider. Use None for default

    :param sPreProcSuffix: suffix to use for the name of the output of the workflows
    :return: 
    """
    asOriginalFiles = []
    asPreProcessedFiles = []
    asRunningProcList = []

    asRunningDownloadList = []

    # For each image found
    for oImage in aoImages:

        # Get the file name
        sFile = oImage["title"] + ".zip"
        _log("[INFO] waspy.importAndPreprocess: Importing Image " + sFile)

        # Import in WASDI
        sImportProcId = asynchImportProduct(oImage, sProvider)

        if sImportProcId != "ERROR":
            asRunningDownloadList.append(sImportProcId)
            asOriginalFiles.append(sFile)

    # Flag to know if we are waiting for a donwload
    bWaitingDonwload = True

    # While there are download to wait for
    while bWaitingDonwload:

        # Suppose they are done
        bWaitingDonwload = False

        # For each running process
        for iImports in range(len(asRunningDownloadList)):

            # Get the status
            sImportProcId = asRunningDownloadList[iImports]

            if sImportProcId == "ERROR":
                continue

            if sImportProcId == "DONE":
                sImportStatus = sImportProcId
            else:
                sImportStatus = getProcessStatus(sImportProcId)

            if sImportStatus == "DONE":
                # Yes, start the workflow
                sFile = asOriginalFiles[iImports]
                # Generate the output name
                sOutputFile = sFile.replace(".zip", sPreProcSuffix)

                _log("[INFO]  waspy.importAndPreprocess: " + sFile + " imported, starting workflow to get " + sOutputFile)

                # Is already there for any reason?
                if not fileExistsOnWasdi(sOutputFile) and not sOutputFile in asPreProcessedFiles:
                    # No, start the workflow
                    sProcId = asynchExecuteWorkflow(sFile, sOutputFile, sWorkflow)
                    asRunningProcList.append(sProcId)
                    asPreProcessedFiles.append(sOutputFile)

                asRunningDownloadList[iImports] = "DONE"
            elif sImportStatus == "ERROR" or sImportStatus == "STOPPED":
                asRunningDownloadList[iImports] = sImportStatus
            else:
                bWaitingDonwload = True

        if bWaitingDonwload:
            time.sleep(getPollingSleepSeconds())

            # Checkpoint: wait for all asynch workflows to finish
    _log("[INFO] waspy.importAndPreprocess: All image imported, waiting for all workflows to finish")
    waitProcesses(asRunningProcList)


def asynchExecuteProcessor(sProcessorName, aoParams={}):
    """
    Legacy: use executeProcessor
    Executes a WASDI Processor asynchronously. The method try up to three time if there is any problem.

    :param sProcessorName: WASDI processor name

    :param aoParams: a dictionary of parameters for the processor
    :return: the Process Id if every thing is ok, '' if there was any problem
    """
    return executeProcessor(sProcessorName, aoParams)


def executeProcessor(sProcessorName, aoProcessParams):
    """
    Executes a WASDI Processor asynchronously. The method try up to three time if there is any problem.

    :param sProcessorName: WASDI processor name

    :param aoParams: a dictionary of parameters for the processor
    :return: the Process Id if every thing is ok, '' if there was any problem
    """
    global m_sActiveWorkspace

    if sProcessorName is None:
        wasdiLog('[ERROR] waspy.executeProcessor: processor name is None, aborting' +
                 '  ******************************************************************************')
        return ''
    elif len(sProcessorName) <= 0:
        wasdiLog('[ERROR] waspy.executeProcessor: processor name empty, aborting' +
                 '  ******************************************************************************')
        return ''
    if isinstance(aoProcessParams, dict) is not True:
        wasdiLog('[ERROR] waspy.executeProcessor: parameters must be a dictionary but it is not, aborting' +
                 '  ******************************************************************************')
        return ''

        # Prepare API headers and params
    sEncodedParams = json.dumps(aoProcessParams)

    asHeaders = _getStandardHeaders()

    sUrl = getBaseUrl() + '/processors/run?workspace=' + m_sActiveWorkspace + '&name=' + sProcessorName

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl = sUrl + '&parent=' + getProcId()

    # Try up to three time
    iMaxRetry = 3

    for iAttempt in range(iMaxRetry):

        wasdiLog("[INFO] waspy.executeProcessor: execute Processor Attempt # " + str(iAttempt + 1))

        oResult = None

        try:
            oResult = requests.post(sUrl, data=sEncodedParams, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.executeProcessor: there was an error contacting the API " + str(oEx))

        if oResult is None:
            wasdiLog('[ERROR] waspy.executeProcessor: something broke when contacting the server')
        else:
            if oResult.ok is True:
                _log('[INFO] waspy.executeProcessor: API call OK')
                aoJson = oResult.json()
                if "processingIdentifier" in aoJson:
                    sProcessID = aoJson['processingIdentifier']
                    return sProcessID
                elif "message" in aoJson:
                    sMessage = aoJson['message']
                    wasdiLog('[ERROR] waspy.executeProcessor: cannot run the processor: ' + sMessage)
                else:
                    wasdiLog('[ERROR] waspy.executeProcessor: cannot extract processing identifier from response, aborting')
            else:
                wasdiLog("[ERROR] waspy.executeProcessor: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)            


        wasdiLog("[ERROR]: waspy.executeProcessor: Error triggering the new process.")
        time.sleep(getPollingSleepSeconds())

    wasdiLog("[ERROR]: waspy.executeProcessor: process not triggered, too many errors")

    # If we exit from the cycle, we do not have any result for our client...
    return ''


def executeAndWaitProcessor(sProcessorName, aoProcessParams):
    """
    Executes a WASDI Processor waiting for it to finish. The method try up to three time if there is any problem.

    :param sProcessorName: WASDI processor name

    :param aoParams: a dictionary of parameters for the processor
    :return: the final status of the process if every thing is ok, '' if there was any problem
    """

    sStatus = ""
    try:
        sProcId = wasdi.executeProcessor(sProcessorName, aoProcessParams)

        if sProcId != "":
            sStatus = wasdi.waitProcess(sProcId)
        
    except Exception as oEx:
        wasdi.wasdi("[ERROR]: waspy.executeAndWaitProcessor: exception " + str(oEx))
    
    return sStatus

def _uploadFile(sFileName):
    """
    Uploads a file to WASDI

    :param sFileName: name of file inside working directory OR path to file RELATIVE to working directory
    :return: True if succeded, False otherwise
    """

    _log('upload ' + sFileName)

    if getIsOnServer() is True:
        return True

    bResult = False
    try:
        if sFileName is None:
            wasdiLog('[ERROR] waspy._uploadFile: the given file name is None, cannot upload')
            return False

        sFileProperName = os.path.basename(sFileName)

        sFullPath = getPath(sFileName)

        sUrl = getWorkspaceBaseUrl() + '/product/uploadfilebylib?workspace=' + getActiveWorkspaceId() + '&name=' + sFileProperName
        asHeaders = _getStandardHeaders()
        if 'Content-Type' in asHeaders:
            del (asHeaders['Content-Type'])

        _log('waspy._uploadFile: uploading file to wasdi...')
        oResponse = None
        try:
            with open(sFullPath, 'rb') as oFile:
                oResponse = requests.post(sUrl, files={'file': (sFileName, oFile)}, headers=asHeaders, timeout=getUploadTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy._uploadFile: there was an error contacting the API " + str(oEx))
        try:
            if oResponse.ok:
                _log('waspy._uploadFile: upload complete :-)')
                bResult = True
            else:
                wasdiLog('[ERROR] waspy._uploadFile: upload failed with code {oResponse.status_code}: {oResponse.text}')
        except Exception as oE:
            wasdiLog('[ERROR] waspy._uploadFile: upload failed due to ' + str(type(oE)) + str(oE))
    except Exception as oE:
        wasdiLog('[ERROR] waspy._uploadFile: ' + str(oE))
    # finally:
    # os.chdir(getScriptPath())
    return bResult


def addFileToWASDI(sFileName, sStyle=""):
    """
    Add a file to the wasdi workspace

    :param sFileName: Name (with extension) of the file to add

    :param sStyle: name of a valid WMS style
    :return: status of the operation
    """
    return _internalAddFileToWASDI(sFileName, False, sStyle)


def asynchAddFileToWASDI(sFileName, sStyle=""):
    """
    Triggers the ingestion of File Name in the workspace

    :param: sFileName: Name (with extension) of the file to add

    :param sStyle: name of a valid WMS style
    :return: Process Id of the ingestion
    """
    return _internalAddFileToWASDI(sFileName, True, sStyle)


def subset(sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE):
    """
    Creates a Subset of an image:

    :param sInputFile: Input file

    :param sOutputFile: Output File

    :param dLatN: Latitude north of the subset

    :param dLonW: Longitude west of the subset

    :param dLatS: Latitude South of the subset

    :param dLonE: Longitude Est of the subset
    """
    _log('[INFO] waspy.subset( ' + str(sInputFile) + ', ' + str(sOutputFile) + ', ' +
         str(dLatN) + ', ' + str(dLonW) + ', ' + str(dLatS) + ', ' + str(dLonE) + ' )')

    if sInputFile is None:
        wasdiLog('[ERROR] waspy.subset: input file must not be None, aborting' +
                 '  ******************************************************************************')
        return ''
    if len(sInputFile) < 1:
        wasdiLog('[ERROR] waspy.subset: input file name must not have zero length, aborting' +
                 '  ******************************************************************************')
        return ''
    if sOutputFile is None:
        wasdiLog('[ERROR] waspy.subset: output file must not be None, aborting' +
                 '  ******************************************************************************')
        return ''
    if len(sOutputFile) < 1:
        wasdiLog('[ERROR] waspy.subset: output file name len must not have zero length, aborting' +
                 '  ******************************************************************************')
        return ''

    sUrl = m_sBaseUrl + "/processing/subset?source=" + sInputFile + "&name=" + \
           sOutputFile + "&workspace=" + m_sActiveWorkspace

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parent="
        sUrl += getProcId()

    sSubsetSetting = "{ \"latN\":" + dLatN + ", \"lonW\":" + dLonW + ", \"latS\":" + dLatS + ", \"lonE\":" + dLonE + " }"
    asHeaders = _getStandardHeaders()

    oResponse = None

    try:
        oResponse = requests.get(sUrl, data=sSubsetSetting, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.subset: there was an error contacting the API " + str(oEx))

    if oResponse is None:
        wasdiLog('[ERROR] waspy.subset: cannot contact server' +
                 '  ******************************************************************************')
        return ''

    if oResponse.ok is not True:
        wasdiLog("[ERROR] waspy.subset: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)
        return ''
    else:
        oJson = oResponse.json()
        if oJson is not None:
            if 'stringValue' in oJson:
                sProcessId = oJson['stringValue']
                return waitProcess(sProcessId)

    return ''


def multiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, bBigTiff=False):
    """
    Creates a Many Subsets from an image. MAX 10 TILES PER CALL

    :param sInputFile: Input file

    :param sOutputFile: Array of Output File Names

    :param dLatN: Array of Latitude north of the subset

    :param dLonW: Array of Longitude west of the subset

    :param dLatS: Array of Latitude South of the subset

    :param dLonE: Array of Longitude Est of the subset
    """

    _log('[INFO] waspy.multiSubset( ' + str(sInputFile) + ', ' + str(asOutputFiles) + ', ' +
         str(adLatN) + ', ' + str(adLonW) + ', ' + str(adLatS) + ', ' + str(adLonE) + ' )')

    if sInputFile is None:
        wasdiLog('[ERROR] waspy.multiSubset: input file must not be None, aborting' +
                 '  ******************************************************************************')
        return ''
    if len(sInputFile) < 1:
        wasdiLog('[ERROR] waspy.multiSubset: input file name must not have zero length, aborting' +
                 '  ******************************************************************************')
        return ''
    if asOutputFiles is None:
        wasdiLog('[ERROR] waspy.multiSubset: output files must not be None, aborting' +
                 '  ******************************************************************************')
        return ''
    if len(asOutputFiles) < 1:
        wasdiLog('[ERROR] waspy.multiSubset: output file names len must not have zero length, aborting' +
                 '  ******************************************************************************')
        return ''

    if len(asOutputFiles) > 10:
        wasdiLog('[ERROR] waspy.multiSubset: max allowed 10 tiles per call' +
                 '  ******************************************************************************')
        return ''

    sUrl = m_sBaseUrl + "/processing/multisubset?source=" + sInputFile + "&name=" + \
           sInputFile + "&workspace=" + getActiveWorkspaceId()

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parent="
        sUrl += getProcId()

    aoBody = {}

    aoBody["outputNames"] = asOutputFiles;
    aoBody["latNList"] = adLatN;
    aoBody["lonWList"] = adLonW;
    aoBody["latSList"] = adLatS;
    aoBody["lonEList"] = adLonE;

    if bBigTiff:
        aoBody["bigTiff"] = True

    sSubsetSetting = json.dumps(aoBody)
    asHeaders = _getStandardHeaders()

    # Try up to three time
    iMaxRetry = 3

    for iAttempt in range(iMaxRetry):

        wasdiLog("[INFO] waspy.multiSubset: execute Multi Subset Attempt # " + str(iAttempt + 1))

        oResponse = None

        try:
            oResponse = requests.post(sUrl, headers=asHeaders, data=sSubsetSetting, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.multiSubset: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            wasdiLog('[ERROR] waspy.multiSubset: cannot contact server')
        else:
            if oResponse.ok is not True:
                wasdiLog("[ERROR] waspy.multiSubset: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)
            else:
                oJson = oResponse.json()
                if oJson is not None:
                    if 'stringValue' in oJson:
                        sProcessId = oJson['stringValue']
                        return waitProcess(sProcessId)

        wasdiLog("[ERROR]: waspy.multiSubset: Error triggering the Multi Subset.")
        time.sleep(getPollingSleepSeconds())        

    wasdiLog("[ERROR]: waspy.multiSubset: Multi Subset not triggered, too many errors")
    return ''


def getWorkflows():
    """
        Get the list of workflows for the user

        :return: None if there is any error; an array of WASDI Workspace JSON Objects if everything is ok. The format is as follows:

        {
            "description":STRING,
            "name": STRING,
            "workflowId": STRING
        }

    """
    global m_sBaseUrl
    global m_sSessionId

    asHeaders = _getStandardHeaders()

    sUrl = m_sBaseUrl + '/workflows/getbyuser'

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getWorkflows: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return None

    if oResult.ok:
        oJsonResults = oResult.json()
        return oJsonResults
    else:
        wasdiLog("[ERROR] waspy.getWorkflows: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
        return None


def executeSen2Cor(sProductName):
    """
    Synchronous execution of the sen2Cor convertion on the product specified
    :return:final status of the executed Sen2Cor
    """

    return _internalExecuteSen2Cor(sProductName, "", False)


def asynchExecuteSen2Cor(sProductName):
    """
    Execute the sen2Cor convertion on the product specified
    :return: The processId of the conversion
    """

    return _internalExecuteSen2Cor(sProductName, "", True)


def _internalExecuteSen2Cor(sProductName, sWorkspaceId, bAsynch):
    _log('[INFO] waspy._internalExecuteSen2Cor( ' + str(sProductName) + ', ' +
         str(sWorkspaceId) + ', ' + str(bAsynch) + ')')

    if sProductName is None:
        wasdiLog('[ERROR] waspy._internalExecuteSen2Cor: no product specified, aborting' +
                 '  ******************************************************************************')
        return ''

    if sWorkspaceId is None:
        wasdiLog('[ERROR] waspy._internalExecuteSen2Cor: no workspace specified, aborting' +
                 '  ******************************************************************************')
        return ''

    sProcessId = ''
    sUrl = getBaseUrl() + "/processing/conversion/sen2cor?workspace=" + getActiveWorkspaceId()

    sUrl += "&productName="
    sUrl += sProductName
    
    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parentId="
        sUrl += getProcId()

    asHeaders = _getStandardHeaders()

    oResponse = None

    try:
        oResponse = requests.post(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy._internalExecuteSen2Cor: there was an error contacting the API " + str(oEx))

    if oResponse is None:
        wasdiLog('[ERROR] waspy._internalExecuteSen2Cor: communication with the server failed, aborting' +
                 '  ******************************************************************************')
        return ''

    if oResponse.ok is True:
        _log('[INFO] waspy._internalExecuteSen2Cor: server replied OK')
        asJson = oResponse.json()
        if "stringValue" in asJson:
            sProcessId = asJson["stringValue"]
            if bAsynch is True:
                return sProcessId
            else:
                return waitProcess(sProcessId)
        else:
            wasdiLog('[ERROR] waspy._internalExecuteSen2Cor: cannot find process ID in response, aborting' +
                     '  ******************************************************************************')
            return ''
    else:
        wasdiLog("[ERROR] waspy._internalExecuteSen2Cor: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)
        return ''


def executeWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, aoTemplateParams = None):
    """
    Execute a SNAP Workflow available in WASDI (you can use WASDI to upload your SNAP Graph XML and use from remote)

    :param asInputFileNames: array of the inputs of the workflow. Must correspond to the number of inputs of the workflow.

    :param asOutputFileNames: array of the  outputs of the workflow. Must correspond to the number of inputs of the workflow.

    :param sWorkflowName: Name of the workflow to run

    :param aoTemplateParams: Dictionary with strings KEY-VALUE that will be used to fill potential parameters in the Workflow XML.
     Wasdi will search the XML for the strings in the keys and replace with the value here provided

    :return: final status of the executed Workflow
    """
    return _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, False, aoTemplateParams)


def asynchExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, aoTemplateParams = None):
    """
    Trigger the asynch execution of a SNAP Workflow available in WASDI (you can use WASDI to upload your SNAP Graph XML and use from remote)

    :param asInputFileNames: array of the inputs of the workflow. Must correspond to the number of inputs of the workflow.

    :param asOutputFileNames: array of the  outputs of the workflow. Must correspond to the number of inputs of the workflow.

    :param sWorkflowName: Name of the workflow to run

    :param aoTemplateParams: Dictionary with strings KEY-VALUE that will be used to fill potential parameters in the Workflow XML.
     Wasdi will search the XML for the strings in the keys and replace with the value here provided

    :return: Process Id of the started workflow
    """
    return _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, True, aoTemplateParams)


def asynchMosaic(asInputFiles, sOutputFile, iNoDataValue=None, iIgnoreInputValue=None, fPixelSizeX=None,
                 fPixelSizeY=None):
    """
    Start a mosaic out of a set of images in asynch way


    :param asInputFiles: List of input files to mosaic

    :param sOutputFile: Name of the mosaic output file

    :param iNoDataValue: Value to use as noData. Use -1 to ignore

    :param iIgnoreInputValue: Value to ignore from the input files of the mosaic. Use -1 to ignore
    
    :param fPixelSizeX: double value of the output pixel X resolution
    
    :param fPixelSizeY: double value of the output pixel Y resolution

    :return: Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
    """

    return mosaic(asInputFiles, sOutputFile, iNoDataValue, iIgnoreInputValue, fPixelSizeX=fPixelSizeX,
                  fPixelSizeY=fPixelSizeY, bAsynch=True)


def mosaic(asInputFiles, sOutputFile, iNoDataValue=None, iIgnoreInputValue=None, fPixelSizeX=None, fPixelSizeY=None,
           bAsynch=False):
    """
    Creates a mosaic out of a set of images


    :param asInputFiles: List of input files to mosaic

    :param sOutputFile: Name of the mosaic output file

    :param iNoDataValue: Value to use as noData. Use -1 to ignore

    :param iIgnoreInputValue: Value to ignore from the input files of the mosaic. Use -1 to ignore
    
    :param fPixelSizeX: double value of the output pixel X resolution
    
    :param fPixelSizeY: double value of the output pixel Y resolution

    :param bAsynch: True to return after the triggering, False to wait the process to finish
    
    :return: Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
    """
    if fPixelSizeX is None:
        fPixelSizeX = -1.0

    if fPixelSizeY is None:
        fPixelSizeY = -1.0

    _log('[INFO]  waspy.mosaic( ' +
         str(asInputFiles) + ', ' +
         str(sOutputFile) + ', ' +
         str(iNoDataValue) + ', ' +
         str(iIgnoreInputValue) + ', ' +
         str(bAsynch) + ' )'
         )

    if asInputFiles is None:
        wasdiLog('[ERROR] waspy.mosaic: list of input files is None, aborting')
        return ''
    elif len(asInputFiles) <= 0:
        wasdiLog('[ERROR] waspy.mosaic: list of input files is empty, aborting')
        return ''

    if sOutputFile is None:
        wasdiLog('[ERROR] waspy.mosaic: name of output file is None, aborting')
        return ''
    elif isinstance(sOutputFile, str) is False:
        wasdiLog('[ERROR] waspy.mosaic: output file name must be a string, but a ' + str(type(sOutputFile)) +
                 ' was passed, aborting')
        return ''
    elif len(sOutputFile) <= 0:
        wasdiLog('[ERROR] waspy.mosaic: output file name is empty, aborting')
        return ''

    sUrl = getBaseUrl() + "/processing/mosaic?name=" + sOutputFile + "&workspace=" + \
           getActiveWorkspaceId()

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parent="
        sUrl += getProcId()

    sOutputFormat = "GeoTIFF"
    if sOutputFile.endswith(".dim"):
        sOutputFormat = "BEAM-DIMAP"
    if (sOutputFile.endswith(".vrt")):
        sOutputFormat = "VRT"

    # todo check input type is appropriate
    try:
        aoMosaicSettings = {
            'pixelSizeX': fPixelSizeX,
            'pixelSizeY': fPixelSizeY,
            'noDataValue': iNoDataValue,
            'inputIgnoreValue': iIgnoreInputValue,
            'outputFormat': sOutputFormat,
            'sources': asInputFiles,
            'variableExpressions': []
        }
    except:
        wasdiLog('[ERROR] waspy.mosaic: cannot build DTO, please check your input. Aborting')
        return ''

    asHeaders = _getStandardHeaders()

    oResponse = None

    try:
        oResponse = requests.post(sUrl, data=json.dumps(aoMosaicSettings), headers=asHeaders,
                                  timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.mosaic: there was an error contacting the API " + str(oEx))

    if oResponse is None:
        wasdiLog('[ERROR] waspy.mosaic: cannot contact server, aborting')
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
        wasdiLog("[ERROR] waspy.mosaic: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)

    return ''


def copyFileToSftp(sFileName, bAsynch=None, sRelativePath=None):
    """
    Copy a file from a workspace to the WASDI user's SFTP Folder
    

    :param sFileName: FIle name (with extension, without path) to copy in the SFTP folder

    :param bAsynch: True to return after the triggering, False to wait the process to finish
    :return: Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure    
    """

    _log('[INFO] waspy.copyFileToSftp( ' + str(sFileName) + ', ' + str(bAsynch) + ' )')

    if sFileName is None:
        wasdiLog('[ERROR] waspy.copyFileToSftp: file name is None, aborting' +
                 '  ******************************************************************************')
        return ''
    if not isinstance(sFileName, str):
        wasdiLog('[WARNING] waspy.copyFileToSftp: file name is not a string, trying conversion' +
                 '  ******************************************************************************')
        try:
            sFileName = str(sFileName)
        except:
            wasdiLog('[ERROR] waspy.copyFileToSftp: cannot convert file name into string, aborting' +
                     '  ******************************************************************************')
            return ''
    if len(sFileName) < 1:
        wasdiLog('[ERROR] waspy.copyFileToSftp: file name has zero length, aborting' +
                 '  ******************************************************************************')
        return ''

    if bAsynch is None:
        wasdiLog('[WARNING] waspy.copyFileToSftp: asynch flag is None, assuming False')
        bAsynch = False
    if not isinstance(bAsynch, bool):
        wasdiLog('[WARNING] waspy.copyFileToSftp: asynch flag is not a boolean, trying casting')
        try:
            bAsynch = bool(bAsynch)
        except:
            wasdiLog('[ERROR] waspy.copyFileToSftp: could not convert asynch flag into bool, aborting' +
                     '  ******************************************************************************')
            return ''

    sResult = ''
    try:
        if getUploadActive() is True:
            if fileExistsOnWasdi(sFileName) is False:
                _log('[INFO] waspy.copyFileToSftp: remote file is missing, uploading')
                try:
                    _uploadFile(sFileName)
                    _log('[INFO] waspy.moveFileToSftp: file uploaded, keep on working!')
                except:
                    wasdiLog('[ERROR] waspy.copyFileToSftp: could not proceed with upload' +
                             '  ******************************************************************************')

        sUrl = getWorkspaceBaseUrl() + "/catalog/copytosfpt?file=" + sFileName + "&workspace=" + getActiveWorkspaceId()

        if getIsOnServer() is True or getIsOnExternalServer() is True:
            sUrl += "&parent="
            sUrl += getProcId()

        if sRelativePath is not None:
            sUrl += "&path="
            sUrl += str(sRelativePath)

        asHeaders = _getStandardHeaders()

        oResponse = None

        try:
            oResponse = requests.get(url=sUrl, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy.copyFileToSftp: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            wasdiLog('[ERROR] waspy.copyFileToSftp: cannot contact server' +
                     '  ******************************************************************************')
            return sResult

        if oResponse.ok is not True:
            wasdiLog("[ERROR] waspy.copyFileToSftp: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)
        else:
            oJson = oResponse.json()
            if 'stringValue' in oJson:
                bOk = bool(oJson['boolValue'])
                if bOk:
                    sProcessId = str(oJson['stringValue'])
                    if bAsynch is True:
                        sResult = sProcessId
                    else:
                        sResult = waitProcess(sProcessId)
                else:
                    wasdiLog('[ERROR] waspy.copyFileToSftp: impossible to move file in the user WASDI sftp folder')
    except:
        wasdiLog('[ERROR] waspy.copyFileToSftp: something broke alongside' +
                 '  ******************************************************************************')

    return sResult


def getProcessorPath():
    """
    Get the local path of the processor (where myProcessor.py is located)

    :return: Local path of the processor
    """

    try:
        # get the caller's stack frame and extract its file path
        oFrameInfo = inspect.stack()[1]
        sCallerFilePath = oFrameInfo[1]
        # drop the reference to the stack frame to avoid reference cycles
        del oFrameInfo

        # make the path absolute
        sCallerFilePath = os.path.dirname(os.path.abspath(sCallerFilePath))
        sCallerFilePath = sCallerFilePath + "/"

        return sCallerFilePath
    except:
        return "./"


def getProcessesByWorkspace(iStartIndex=0, iEndIndex=20, sStatus=None, sOperationType=None, sName=None):
    """
    Get a paginated list of processes in the active workspace

    :param iStartIndex: start index of the process (0 by default is the last one)

    :param iEndIndex: end index of the process (20 by default)

    :param sStatus: status filter. None by default. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY

    :param sOperationType: Operation Type Filter. None by default. Can be RUNPROCESSOR, RUNIDL, RUNMATLAB, INGEST, DOWNLOAD, GRAPH, DEPLOYPROCESSOR

    :param sName: Name filter. The name meaning depends by the operation type. None by default. For RUNPROCESSOR, RUNIDL and RUNMATLAB is the name of the application
    """

    sWorkspaceId = getActiveWorkspaceId()
    asHeaders = _getStandardHeaders()
    aoPayload = {'workspace': sWorkspaceId, 'startindex': iStartIndex, 'endindex': iEndIndex}

    if sStatus is not None:
        aoPayload['status'] = sStatus

    if sOperationType is not None:
        aoPayload['operationType'] = sOperationType

    if sName is not None:
        aoPayload['namePattern'] = sName

    sUrl = getWorkspaceBaseUrl() + '/process/byws'

    asProcesses = []

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, params=aoPayload, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getProcessesByWorkspace: there was an error contacting the API " + str(oEx))

    if oResult is None:
        return asProcesses

    if oResult.ok is True:
        oJsonResults = oResult.json()

        for oProcess in oJsonResults:
            try:
                asProcesses.append(oProcess)
            except:
                return asProcesses
    else:
        wasdiLog("[ERROR] waspy.getProcessesByWorkspace: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)

    return asProcesses


def bboxStringToObject(sBbox):
    """
    Convert the WASDI String BBOX format "N,W,S,W" to Object format {"northEast": {"lat":, lng":}, {"southWest":{"lat":, "lng":} } 
    """
    oBbox = {}

    try:
        asBbox = sBbox.split(",")
        oBbox["northEast"] = {}
        oBbox["northEast"]["lat"] = float(asBbox[0])
        oBbox["northEast"]["lng"] = float(asBbox[3])
        oBbox["southWest"] = {}
        oBbox["southWest"]["lat"] = float(asBbox[2])
        oBbox["southWest"]["lng"] = float(asBbox[1])

    except Exception as oEx:
        wasdiLog("[ERROR] waspy.bboxStringToObject: " + str(oEx))

    return oBbox


def bboxObjectToString(oBbox):
    """
    Convert the WASDI Object BBOX format {"northEast": {"lat":, lng":}, {"southWest":{"lat":, "lng":} } to the String format "N,W,S,W"  
    """

    try:
        sBbox = str(oBbox["northEast"]["lat"]) + "," + str(oBbox["southWest"]["lng"]) + "," + str(
            oBbox["southWest"]["lat"]) + "," + str(oBbox["northEast"]["lng"])
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.bboxObjectToString: " + str(oEx))

    return sBbox


def _log(sLog):
    """
    Internal Log function

    :param sLog: text row to log
    """

    if getVerbose():
        print(sLog)


def _getStandardHeaders():
    """
    Get the standard headers for a WASDI API Call, setting also the session token

    :return: dictionary of headers to add to the REST API
    """
    global m_sSessionId
    asHeaders = {'Content-Type': 'application/json', 'x-session-token': m_sSessionId}
    return asHeaders


def _loadConfig(sConfigFilePath):
    """
    Loads configuration from given file

    :param sConfigFilePath: a string containing a path to the configuration file
    """
    if sConfigFilePath is None:
        print('[ERROR] waspy._loadConfigParams: config parameter file name is None, cannot load config' +
              '  ******************************************************************************')
        return
    if sConfigFilePath == '':
        print('[ERROR] waspy._loadConfigParams: config parameter file name is empty, cannot load config' +
              '  ******************************************************************************')
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
            if 'BASEURL' in oJson:
                setBaseUrl(oJson['BASEURL'])
            if 'REQUESTSTIMEOUT' in oJson:
                setRequestsTimeout(oJson['REQUESTSTIMEOUT'])
            if 'SESSIONID' in oJson:
                setSessionId(oJson['SESSIONID'])               
            if 'MYPROCID' in oJson:
                setProcId(oJson['MYPROCID'])
            if 'ENABLECHECKSUM' in oJson:
                setEnableChecksum(bool(oJson["ENABLECHECKSUM"]))
            if 'POLLINGSLEEPSECONDS' in oJson:
                setPollingSleepSeconds(int(oJson['POLLINGSLEEPSECONDS']))

        return True, sTempWorkspaceName, sTempWorkspaceID

    except Exception as oEx:
        wasdiLog('[ERROR] waspy._loadConfigParams: something went wrong' +
                 '  ******************************************************************************')
        wasdiLog(str(oEx))
        return False, None, None


def _loadParams():
    """
    Loads parameters from file, if specified in configuration file
    """
    global m_sParametersFilePath
    global m_aoParamsDictionary

    bParamLoaded = False
    if (m_sParametersFilePath is not None) and (m_sParametersFilePath != ''):
        try:
            if not os.path.isfile(m_sParametersFilePath):
                wasdiLog('[WARNING] _loadParams: parameters file not found')
            with open(m_sParametersFilePath) as oJsonFile:
                m_aoParamsDictionary = json.load(oJsonFile)
                bParamLoaded = True
        except Exception as oE:
            wasdiLog('[WARNING] _loadParams: could not open file due to: ' + str(oE))

    if not bParamLoaded:
        wasdiLog(
            '[INFO] _loadParams: wasdi could not load param file. That is fine, you can still load it later, don\'t worry')


def _unzip(sAttachmentName, sPath):
    """
    Unzips a file

    :param sAttachmentName: filename to unzip

    :param sPath: both the path where the file is and where it must be unzipped
    :return: None
    """
    _log('[INFO] waspy._unzip( ' + sAttachmentName + ', ' + sPath + ' )')
    if sPath is None:
        _log('[ERROR] waspy._unzip: path is None' +
              '  ******************************************************************************')
        return
    if sAttachmentName is None:
        _log('[ERROR] waspy._unzip: attachment to unzip is None' +
              '  ******************************************************************************')
        return

    try:
        sZipFilePath = os.path.join(sPath, sAttachmentName)
        zip_ref = zipfile.ZipFile(sZipFilePath, 'r')
        zip_ref.extractall(sPath)
        zip_ref.close()
    except:
        _log('[ERROR] waspy._unzip: failed unzipping' +
              '  ******************************************************************************')

    return


def _waitForResume():
    if getIsOnServer():
        # Put this processor as READY
        updateStatus("READY")

        try:
            # Wait for the WASDI Scheduler to resume us
            _log("[INFO] waspy._waitForResume: Waiting for the scheduler to resume this process")
            sStatus = ''

            while sStatus not in {"RUNNING", "DONE", "STOPPED", "ERROR"}:
                sStatus = getProcessStatus(getProcId())
                time.sleep(getPollingSleepSeconds())

            _log("[INFO] waspy._waitForResume: Process Resumed, let's go!")
        except:
            _log("waspy._waitForResume: Exception in the _waitForResume")


def _normPath(sPath):
    """
    Normalizes path by adjusting separator

    :param sPath: a path to be normalized
    :return: the normalized path
    """

    if sPath is None:
        _log('[ERROR] waspy._normPath: passed path is None' +
              '  ******************************************************************************')
        return None

    sPath = sPath.replace('/', os.path.sep)
    sPath = sPath.replace('\\', os.path.sep)

    return sPath


def _internalAddFileToWASDI(sFileName, bAsynch=None, sStyle=""):
    _log('[INFO] waspy._internalAddFileToWASDI( ' + str(sFileName) + ', ' + str(bAsynch) + ' )')

    if sFileName is None:
        wasdiLog('[ERROR] waspy._internalAddFileToWASDI: file name is None, aborting' +
                 '  ******************************************************************************')
        return ''
    if not isinstance(sFileName, str):
        wasdiLog('[WARNING] waspy._internalAddFileToWASDI: file name is not a string, trying conversion' +
                 '  ******************************************************************************')
        try:
            sFileName = str(sFileName)
        except:
            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: cannot convert file name into string, aborting' +
                     '  ******************************************************************************')
            return ''
    if len(sFileName) < 1:
        wasdiLog('[ERROR] waspy._internalAddFileToWASDI: file name has zero length, aborting' +
                 '  ******************************************************************************')
        return ''

    if bAsynch is None:
        _log('[WARNING] waspy._internalAddFileToWASDI: asynch flag is None, assuming False')
        bAsynch = False
    if not isinstance(bAsynch, bool):
        _log('[WARNING] waspy._internalAddFileToWASDI: asynch flag is not a boolean, trying casting')
        try:
            bAsynch = bool(bAsynch)
        except:
            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: could not convert asynch flag into bool, aborting' +
                     '  ******************************************************************************')
            return ''

    if not isinstance(sStyle, str):
        wasdiLog('[WARNING] waspy._internalAddFileToWASDI: style is not a string, trying conversion' +
                 '  ******************************************************************************')
        try:
            sStyle = str(sStyle)
        except:
            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: cannot convert style name into string, set empty')
            sStyle = ""

    try:
        sBasePath = getSavePath()
        if sFileName.startswith(str(sBasePath)):
            wasdiLog('[WARNING] waspy._internalAddFileToWASDI: the input file has the base path in it. We remove it')
            sFileName = sFileName.replace(str(sBasePath),"")
    except:
        wasdiLog('[WARNING] waspy._internalAddFileToWASDI: exception trying to clean the base path on file name')

    sResult = ''
    try:
        if getIsOnServer() is False:
            if getUploadActive() is True:
                if fileExistsOnWasdi(sFileName) is False:
                    if os.path.exists(getPath(sFileName)) is True:
                        _log('[INFO] waspy._internalAddFileToWASDI: remote file is missing, uploading')
                        try:
                            _uploadFile(sFileName)
                            _log('[INFO] waspy._internalAddFileToWASDI: file uploaded, keep on working!')
                        except:
                            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: could not proceed with upload' +
                                    '  ******************************************************************************')
                    else:
                        wasdiLog('[WARNING] waspy._internalAddFileToWASDI: the file ' + sFileName + ' does not exists neither locally and in the cloud')    
        else:
            try:
                # We are on the server: do I have the file?
                if os.path.exists(getPath(sFileName)) is True:
                    # Does it exists on the target node?
                    if _fileOnNode(sFileName) is False:
                        wasdiLog('[WARNING] waspy._internalAddFileToWASDI: uploading the file to the workspace node')
                        try:
                            _uploadFile(sFileName)
                            wasdiLog('[WARNING] waspy._internalAddFileToWASDI: file uploaded, keep on working!')
                        except:
                            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: could not proceed with upload' +
                                     '  ******************************************************************************')
                else:
                    wasdiLog('[WARNING] waspy._internalAddFileToWASDI: the file ' + sFileName + ' does not exists')
            except:
                wasdiLog('[ERROR] waspy._internalAddFileToWASDI: could not send the file the workspace node')

        sUrl = getWorkspaceBaseUrl() + "/catalog/upload/ingestinws?file=" + sFileName + "&workspace=" + getActiveWorkspaceId()

        # If present, add the default product style
        if sStyle is not None:
            if sStyle != "":
                sUrl = sUrl + "&style=" + sStyle

        if getIsOnServer() is True or getIsOnExternalServer() is True:
            sUrl += "&parent="
            sUrl += getProcId()

        asHeaders = _getStandardHeaders()

        oResponse = None

        try:
            oResponse = requests.get(url=sUrl, headers=asHeaders, timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy._internalAddFileToWASDI: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: cannot contact server' +
                     '  ******************************************************************************')
            return sResult

        if oResponse.ok is not True:
            wasdiLog('[ERROR] waspy._internalAddFileToWASDI: failed, server replied ' + str(oResponse.status_code) +
                     '  ******************************************************************************')
        else:
            oJson = oResponse.json()
            if 'stringValue' in oJson:
                bOk = bool(oJson['boolValue'])
                if bOk:
                    sProcessId = str(oJson['stringValue'])
                    if bAsynch is True:
                        sResult = sProcessId
                    else:
                        sResult = waitProcess(sProcessId)
                else:
                    wasdiLog('[ERROR] waspy._internalAddFileToWASDI: impossible to ingest the file in WASDI')
    except:
        wasdiLog('[ERROR] waspy._internalAddFileToWASDI: something broke alongside' +
                 '  ******************************************************************************')

    return sResult


def _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, bAsynch=False, aoTemplateParams = None):
    """
    Internal call to execute workflow


    :param asInputFileNames: name of the file in input (string WITH extension) or array of strings of the files in input (WITH extension)

    :param asOutputFileNames: name of the file in output (string WITH extension) or array of strings of the files in output (WITH extension)

    :param sWorkflowName: name of the SNAP workflow uploaded in WASDI

    :param bAsynch: true to run asynch, false to run synch

    :param aoTemplateParams: Dictionary with strings KEY-VALUE that will be used to fill potential parameters in the Workflow XML.
     Wasdi will search the XML for the strings in the keys and replace with the value here provided

    :return: processID if asynch, status of the executed process if synch, empty string in case of failure
    """

    _log('[INFO] waspy._internalExecuteWorkflow( ' + str(asInputFileNames) + ', ' +
         str(asOutputFileNames) + ', ' + str(sWorkflowName) + ', ' + str(bAsynch) + ' )')

    # if we got only a single file input, let transform it in an array
    if not isinstance(asInputFileNames, list):
        asInputFileNames = [asInputFileNames]

    if not isinstance(asOutputFileNames, list):
        asOutputFileNames = [asOutputFileNames]

    if asInputFileNames is None:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: input file names None, aborting' +
                 '  ******************************************************************************')
        return ''
    elif len(asInputFileNames) <= 0:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: no input file names, aborting' +
                 '  ******************************************************************************')
        return ''

    if asOutputFileNames is None:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: output file names None, aborting' +
                 '  ******************************************************************************')
        return ''
    # elif len(asOutputFileNames) <= 0:
    #     _log('[ERROR] waspy._internalExecuteWorkflow: no output file names, aborting')
    #     return ''

    if sWorkflowName is None:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: workspace name is None, aborting' +
                 '  ******************************************************************************')
        return ''
    elif len(sWorkflowName) <= 0:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: workflow name too short, aborting' +
                 '  ******************************************************************************')
        return ''

    sProcessId = ''
    sUrl = getBaseUrl() + "/workflows/run?workspace=" + getActiveWorkspaceId()

    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parent="
        sUrl += getProcId()

    # get a list of workflows, with entries in this form: :
    #   {  "description":STRING,
    #       "name": STRING,
    #       "workflowId": STRING }
    aoWorkflows = getWorkflows()
    aoDictPayload = None
    if aoWorkflows is None:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: workflow list is None, aborting' +
                 '  ******************************************************************************')
        return ''
    elif len(aoWorkflows) <= 0:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: workflow list is empty, aborting' +
                 '  ******************************************************************************')
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
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: workflow name not found, aborting')
        return ''

    try:
        aoDictPayload["inputFileNames"] = asInputFileNames
        aoDictPayload["outputFileNames"] = asOutputFileNames

        if aoTemplateParams is not None:
            aoDictPayload["templateParams"] = aoTemplateParams
    except:
        wasdiLog('[ERROR] waspy._internalExecuteWorkflow: payload could not be generated, aborting' +
                 '  ******************************************************************************')
        return ''

    _log('[INFO] waspy._internalExecuteWorkflow: about to HTTP put to ' + str(sUrl) + ' with payload ' + str(
        aoDictPayload))
    asHeaders = _getStandardHeaders()

    # Try up to three time
    iMaxRetry = 3

    for iAttempt in range(iMaxRetry):

        wasdiLog("[INFO] waspy._internalExecuteWorkflow: execute Workflow Attempt # " + str(iAttempt + 1))
        
        oResponse = None

        try:
            oResponse = requests.post(sUrl, headers=asHeaders, data=json.dumps(aoDictPayload), timeout=getRequestsTimeout())
        except Exception as oEx:
            wasdiLog("[ERROR] waspy._internalExecuteWorkflow: there was an error contacting the API " + str(oEx))

        if oResponse is None:
            wasdiLog('[ERROR] waspy._internalExecuteWorkflow: communication with the server failed')
        else:
            if oResponse.ok is True:
                _log('[INFO] waspy._internalExecuteWorkflow: server replied OK')
                asJson = oResponse.json()
                if "stringValue" in asJson:
                    sProcessId = asJson["stringValue"]
                    if bAsynch is True:
                        return sProcessId
                    else:
                        return waitProcess(sProcessId)
                else:
                    wasdiLog('[ERROR] waspy._internalExecuteWorkflow: cannot find process ID in response')
            else:
                wasdiLog("[ERROR] waspy._internalExecuteWorkflow: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)

        
        wasdiLog("[ERROR]: waspy._internalExecuteWorkflow: Error triggering the workflow.")
        time.sleep(getPollingSleepSeconds())        

    wasdiLog("[ERROR]: waspy._internalExecuteWorkflow: workflow not triggered, too many errors")
    return ''


def _fileOnNode(sFileName):
    """
    checks if a file already exists on the node of the workspace or not

    :param sFileName: file name with extension
    :return: True if the file exists, False otherwise
    """

    if sFileName is None:
        wasdiLog('[ERROR] waspy._fileOnNode: file name must not be None' +
                 '  ******************************************************************************')
        return False
    if len(sFileName) < 1:
        wasdiLog('[ERROR] waspy._fileOnNode: File name too short' +
                 '  ******************************************************************************')
        return False

    sSessionId = getSessionId()
    sActiveWorkspace = getActiveWorkspaceId()

    sUrl = getWorkspaceBaseUrl()
    sUrl += "/catalog/fileOnNode?token="
    sUrl += sSessionId
    sUrl += "&filename="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += sActiveWorkspace

    asHeaders = _getStandardHeaders()

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy._fileOnNode: there was an error contacting the API " + str(oEx))

    if oResult is None:
        wasdiLog('[ERROR] waspy._fileOnNode: failed contacting the server' +
                 '  ******************************************************************************')
        return False

    if not oResult.ok and not 500 == oResult.status_code:
        wasdiLog("[ERROR] waspy._fileOnNode: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)
        return False
    else:
        try:
            oJsonResponse = oResult.json()

            if oJsonResponse['boolValue'] is not None:
                return oJsonResponse['boolValue']
            else:
                return False
        except:
            return False


def _getDefaultCRS():
    return (
            "GEOGCS[\"WGS84(DD)\", \r\n" +
            "          DATUM[\"WGS84\", \r\n" +
            "            SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \r\n" +
            "          PRIMEM[\"Greenwich\", 0.0], \r\n" +
            "          UNIT[\"degree\", 0.017453292519943295], \r\n" +
            "          AXIS[\"Geodetic longitude\", EAST], \r\n" +
            "          AXIS[\"Geodetic latitude\", NORTH]]"
    )


def asynchPublishBand(sProduct, sBand):
    """
    publishes a band of a given product

    :param sProduct: the product containing the desired band
    :param sBand: the band name in the product

    :return: a string containing the processObjId of the publishBand operation
    """

    # validate input
    if sProduct is None or sProduct == '':
        wasdiLog('[ERROR] asynchPublishBand: Product  is not a valid file name, aborting')
        return None

    if sBand is None or sBand == '':
        wasdiLog('[ERROR] asynchPublishBand: Band is not a valid band name, aborting')
        return None

    aoProducts = getDetailedProductsByWorkspaceId()
    asProducts = [oProduct['fileName'] for oProduct in aoProducts]
    if sProduct not in asProducts:
        wasdiLog('[ERROR] asynchPublishBand: ' + sProduct + ' not found in workspace, aborting')
        return None

    aoCandidates = [oProd for oProd in aoProducts if oProd['fileName'] == sProduct]
    if len(aoCandidates) < 1:
        wasdiLog('[ERROR] asynchPublishBand: could not find product on WASDI, aborting')
        return None

    oProduct = aoCandidates[0]
    if oProduct is None:
        wasdiLog('[ERROR] asynchPublishBand: product is None, aborting')
        return None

    if 'bandsGroups' not in oProduct:
        wasdiLog('[ERROR] asynchPublishBand: bandsGroup not present, aborting')
        return None

    if 'bands' not in oProduct['bandsGroups']:
        wasdiLog('[ERROR] asynchPublishBand: bandsGroup has not bands, aborting')
        return None

    aoBands = [oBand for oBand in oProduct['bandsGroups']['bands'] if oBand['name']==sBand]
    if len(aoBands) < 1:
        wasdiLog('[ERROR] asynchPublishBand: band not found, these are available:' +
                 str(oProduct['bandsGroups']['bands']) +
                 ', aborting')
        return None


    # call publish band
    oResult = None
    try:
        sUrl = getBaseUrl() + '/filebuffer/publishband?' + \
               'fileUrl=' + sProduct + \
               '&workspace=' + getActiveWorkspaceId() + \
               '&band=' + sBand
        asHeaders = _getStandardHeaders()
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oE:
        wasdiLog('[ERROR] asynchPublishBand: error in trying to publish band: ' + str(type(oE)) + ': ' + str(oE))

    if oResult is None:
        return None

    if oResult.ok:

        try:
            oJsonResult = oResult.json()
            return oJsonResult
        except Exception as oE:
            wasdiLog('[ERROR] asynchPublishBand: error in trying to get the proc id: ' + str(type(oE)) + ': ' + str(oE))
    else:
        wasdiLog("[ERROR] waspy.asynchPublishBand: API Return Code " + str(oResult.status_code) + " - Reason: " + oResult.reason)


    return None


def publishBand(sProduct, sBand):
    """
    publishes a band of a given product

    :param sProduct: the product containing the desired band
    :param sBand: the band name in the product

    :return: a string containing the final status of the operation: "DONE", "STOPPED", "ERROR"
    """

    oJsonResult = asynchPublishBand(sProduct, sBand)

    try:
        if oJsonResult is not None:
            sProcessId = oJsonResult["payload"]
            return waitProcess(sProcessId)
        else:
            return "ERROR"
    except:
        return "ERROR"

def getlayerWMS(sProduct, sBand):
    """
    Starts a publish band process and wait for the result to be available.
    The function then return a JSON containing the geoServerUrl and the LayerId to be used on external application/
    mpa visualizations
    Waits for a total of 10 attempts waiting 3 seconds each.
    :param sProduct: The product for which the WMS layers details are required
    :param sBand: The band required
    :return: A JSON string with the following structure { server : [GeoServerUrl] , layerId : [LayerId] }
    """
    iCountRetries = 10
    oPublishedBandResponse = asynchPublishBand(sProduct, sBand)
    while not ('payload' in oPublishedBandResponse and 'layerId' in oPublishedBandResponse['payload']):
        if iCountRetries == 0:
            wasdiLog('[ERROR] getlayerWMS: reached the maximum number of attempt ( ' + str(type(iCountRetries)) + ') Aborting operation')
            return
        oPublishedBandResponse = asynchPublishBand(sProduct, sBand)
        time.sleep(getPollingSleepSeconds())
        iCountRetries -= 1
        wasdiLog('[INFO] getlayerWMS: waiting for the band to be available... (' + str(type(iCountRetries)) + ' attempts left before aborting)')
    oPayload = oPublishedBandResponse["payload"]
    oResult = dict()
    oResult["server"] = oPayload["geoserverUrl"]
    oResult["layerId"] = oPayload["layerId"]
    return json.dumps(oResult)


def asynchGetFileFromWorkspaceName(sSourceWorkspaceName, sFileName):
    """
    Trigger the operation to Take a file from another workspace and put it in the actual one.
    sSourceWorkspaceId: Id of the Workspace that must have the file
    sFileName: File to import in the actual Active Workspace

    Return an empty string in case of problems or the process Obj Id of the import process if ok
    """
    sWorkspaceId = getWorkspaceIdByName(sSourceWorkspaceName)
    return asynchGetFileFromWorkspaceId(sWorkspaceId, sFileName)

def getFileFromWorkspaceName(sSourceWorkspaceName, sFileName):
    """
    Takes a file from another workspace and put it in the actual one.
    sSourceWorkspaceName: Name of the Workspace that must have the file
    sFileName: File to import in the actual Active Workspace
    """
    sWorkspaceId = getWorkspaceIdByName(sSourceWorkspaceName)
    return getFileFromWorkspaceId(sWorkspaceId, sFileName)

def getFileFromWorkspaceId(sSourceWorkspaceId, sFileName):
    """
    Takes a file from another workspace and put it in the actual one.
    sSourceWorkspaceId: Id of the Workspace that must have the file
    sFileName: File to import in the actual Active Workspace

    Return true if the file is taken, false in case of problems
    """

    sProcId = asynchGetFileFromWorkspaceId(sSourceWorkspaceId, sFileName)
    if sProcId != "":
        if sProcId == "PRODUCT_ALREADY_PRESENT":
            return True
        sStatus = waitProcess(sProcId)
        if sStatus == "DONE":
            return True
    return  False

def asynchGetFileFromWorkspaceId(sSourceWorkspaceId, sFileName):
    """
    Trigger the operation to Take a file from another workspace and put it in the actual one.
    sSourceWorkspaceId: Id of the Workspace that must have the file
    sFileName: File to import in the actual Active Workspace

    Return an empty string in case of problems or the process Obj Id of the import process if ok
    """
    if sFileName is None:
        wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: file name must not be None' +
                 '  ******************************************************************************')
        return ""
    if len(sFileName) < 1:
        wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: File name too short' +
                 '  ******************************************************************************')
        return ""

    if sSourceWorkspaceId is None:
        wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: SourceWorkspaceId name must not be None' +
                 '  ******************************************************************************')
        return ""
    if len(sSourceWorkspaceId) < 1:
        wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: SourceWorkspaceId name too short' +
                 '  ******************************************************************************')
        return ""

    sUrl = getBaseUrl()
    sUrl += "/filebuffer/share?originWorkspaceId="
    sUrl += sSourceWorkspaceId
    sUrl += "&destinationWorkspaceId="
    sUrl += getActiveWorkspaceId()
    sUrl += "&productName="
    sUrl += sFileName
    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parent="
        sUrl += getProcId()

    asHeaders = _getStandardHeaders()

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.asynchGetFileFromWorkspaceId: there was an error contacting the API " + str(oEx))

    if oResult is None:
        wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: failed contacting the server' +
                 '  ******************************************************************************')
        return ""

    if not oResult.ok and not 500 == oResult.status_code:
        wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: unexpected failure, server returned: ' + str(oResult.status_code) +
                 '  ******************************************************************************')
        return ""
    else:
        aoJson = oResult.json()
        if "stringValue" in aoJson:
            sProcessID = aoJson['stringValue']
            return sProcessID
        else:
            wasdiLog('[ERROR] waspy.asynchGetFileFromWorkspaceId: cannot extract processing identifier from response, aborting')
            return ""

def asynchSendFileToWorkspaceName(sDestinationWorkspaceName, sFileName):
    """
    Trigger the operation to send a file in another workspace
    sDestinationWorkspaceName: Name of the Workspace where we need to copy the file
    sFileName: File to send to the new workspace
    """
    sWorkspaceId = getWorkspaceIdByName(sDestinationWorkspaceName)
    return asynchSendFileToWorkspaceId(sWorkspaceId, sFileName)


def sendFileToWorkspaceName(sDestinationWorkspaceName, sFileName):
    """
    Takes a file from another workspace and put it in the actual one.
    sDestinationWorkspaceName: Name of the Workspace that must have the file
    sFileName: File to import in the actual Active Workspace
    """
    sWorkspaceId = getWorkspaceIdByName(sDestinationWorkspaceName)
    return sendFileToWorkspaceId(sWorkspaceId, sFileName)

def sendFileToWorkspaceId(sDestinationWorkspaceId, sFileName):
    """
    Sends a file from another workspace
    sDestinationWorkspaceId: Id of the Workspace where to send the file
    sFileName: File to send

    Return true if the file is sent, false in case of problems
    """

    sProcId = asynchSendFileToWorkspaceId(sDestinationWorkspaceId, sFileName)

    sDestinationWorkspaceUrl = getWorkspaceUrlByWsId(sDestinationWorkspaceId)
    if sDestinationWorkspaceUrl is None:
        sDestinationWorkspaceUrl = getBaseUrl()

    if sProcId != "":
        if sProcId == "PRODUCT_ALREADY_PRESENT":
            return True
        sStatus = waitProcess(sProcId, sDestinationWorkspaceUrl)
        if sStatus == "DONE":
            return True
    return  False

def asynchSendFileToWorkspaceId(sDestinationWorkspaceId, sFileName):
    """
    Trigger the operation to Send a file from another workspace
    sDestinationWorkspaceId: Id of the Workspace where to send the file
    sFileName: File to send
    """
    if sFileName is None:
        wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: file name must not be None' +
                 '  ******************************************************************************')
        return ""
    if len(sFileName) < 1:
        wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: File name too short' +
                 '  ******************************************************************************')
        return ""

    if sDestinationWorkspaceId is None:
        wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: sDestinationWorkspaceId name must not be None' +
                 '  ******************************************************************************')
        return ""
    if len(sDestinationWorkspaceId) < 1:
        wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: sDestinationWorkspaceId name too short' +
                 '  ******************************************************************************')
        return ""

    sUrl = getBaseUrl()
    sUrl += "/filebuffer/share?originWorkspaceId="
    sUrl += getActiveWorkspaceId()
    sUrl += "&destinationWorkspaceId="
    sUrl += sDestinationWorkspaceId
    sUrl += "&productName="
    sUrl += sFileName
    if getIsOnServer() is True or getIsOnExternalServer() is True:
        sUrl += "&parent="
        sUrl += getProcId()

    asHeaders = _getStandardHeaders()

    oResult = None

    try:
        oResult = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.asynchSendFileToWorkspaceId: there was an error contacting the API " + str(oEx))

    if oResult is None:
        wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: failed contacting the server' +
                 '  ******************************************************************************')
        return ""

    if not oResult.ok and not 500 == oResult.status_code:
        wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: unexpected failure, server returned: ' + str(oResult.status_code) +
                 '  ******************************************************************************')
        return ""
    else:
        aoJson = oResult.json()
        if "stringValue" in aoJson:
            sProcessID = aoJson['stringValue']
            return sProcessID
        else:
            wasdiLog('[ERROR] waspy.asynchSendFileToWorkspaceId: cannot extract processing identifier from response, aborting')
            return ""

def getProductBandNames(sFileName):
    """
        Gets the list of bands of a file as strings

        :param sFileName: name of the file to query for list of bands
        :return: Array of strings containing the names of the bands
        """

    sUrl = getBaseUrl()
    sUrl += "/product/byname?name="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()

    asHeaders = _getStandardHeaders()

    oResponse = None

    try:
        oResponse = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getProductBandNames: there was an error contacting the API " + str(oEx))

    try:
        if oResponse is None:
            wasdiLog('[ERROR] waspy.getProductBandNames: cannot get product band names')
            return []

        if oResponse.ok is not True:
            wasdiLog('[ERROR] waspy.getProductBandNames: cannot get product band names, server returned: ' + str(oResponse.status_code) + '  ')
        else:
            oJsonResponse = oResponse.json()
            if "bandsGroups" in oJsonResponse:
                asBands = []

                for oBand in oJsonResponse["bandsGroups"]["bands"]:
                    asBands.append(oBand["name"])

                return asBands

    except:
        return []

    return []

def getProductProperties(sFileName):
    """
        Gets the properties of a product

        :param sFileName: name of the file to query for properties
        :return: An object with properties: String fileName; String friendlyName; long lastUpdateTimestampMs; String checksum; String style. None in case of error.
    """

    sUrl = getWorkspaceBaseUrl()
    sUrl += "/catalog/properties?file="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()

    if getEnableChecksum():
        sUrl += "&getchecksum=true"

    asHeaders = _getStandardHeaders()

    try:
        oResponse = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.getProductProperties: there was an error contacting the API " + str(oEx))
        return None

    try:
        if oResponse is None:
            wasdiLog('[ERROR] waspy.getProductProperties: cannot get properties for product')
            return None

        if oResponse.ok is not True:
            wasdiLog('[ERROR] waspy.getProductProperties: cannot get product properties, server returned: ' + str(oResponse.status_code) + '  ')
        else:
            oJsonResponse = oResponse.json()
            return oJsonResponse
    except:
        return None

    return None

def getMD5Checksum(sFileName):
    """
    Compute the MD5 Checksum of a file
    """
    oMd5Hash = hashlib.md5()
    with open(sFileName, 'rb') as oFile:

        while True:
            oChunk = oFile.read(4096)
            if not oChunk: break
            oMd5Hash.update(oChunk)

    return oMd5Hash.hexdigest()


def setProductStyle(sFileName, sStyle):
    """
        Set the default style of a product

        :param sFileName: name of the file to update (NO FULL PATH!)
        :param sStyle: name of the style that must be uploaded in WASDI
        :return: Array of strings containing the names of the bands
        """

    sUrl = getBaseUrl()
    sUrl += "/product/byname?name="
    sUrl += sFileName
    sUrl += "&workspace="
    sUrl += getActiveWorkspaceId()

    asHeaders = _getStandardHeaders()

    oResponse = None

    try:
        oResponse = requests.get(sUrl, headers=asHeaders, timeout=getRequestsTimeout())
    except Exception as oEx:
        wasdiLog("[ERROR] waspy.setProductStyle: there was an error contacting the API " + str(oEx))

    try:
        if oResponse is None:
            wasdiLog('[ERROR] waspy.setProductStyle: cannot set the product style')
            return []

        if oResponse.ok is not True:
            wasdiLog('[ERROR] waspy.setProductStyle: cannot get the product view model, server returned: ' + str(oResponse.status_code) + '  ')
        else:
            oJsonResponse = oResponse.json()

            sPayload = '{"style":"' + sStyle + '","fileName":"' + oJsonResponse["fileName"] + '" }'

            sUrl = getBaseUrl()
            sUrl += "/product/update?&workspace="
            sUrl += getActiveWorkspaceId()

            oResponse = requests.post(sUrl, data=sPayload, headers=asHeaders, timeout=getRequestsTimeout())

            if oResponse is None:
                wasdiLog('[ERROR] waspy.setProductStyle: cannot update the style')
                return []

            if oResponse.ok is not True:
                wasdiLog("[ERROR] waspy.asynchPublishBand: API Return Code " + str(oResponse.status_code) + " - Reason: " + oResponse.reason)

    except:
        return

    return

class ChartType(Enum):
    line = "line"
    lineArea = "line-area"
    lineStacked = "line-stacked"
    barHorizontal = "bar-horizontal"
    barVertical = "bar-vertical"
    pie = "pie"


if __name__ == '__main__':
    _log('WASPY - The WASDI Python Library. Include in your code to develop space processors. Visit www.wasdi.net')
