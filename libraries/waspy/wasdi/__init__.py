"""
FADEOUT SOFTWARE 

**Disclaimer ** 
Please consider this is a preliminary version of the lib. It is undergoing many tests but also many changes,
so please be patient and do not trust anyone's life with the library (not yet)

# WASDI

This is WASPY, the WASDI Python lib.

The methods in the module allow to interact with WASDI seamlessly.

Note:
the philosophy of safe programming is adopted as widely as possible, the lib will try to workaround issues such as
faulty input, and print an error rather than raise an exception, so that your program can possibly go on. Please check
the return statues

Last Update: 2020-02-21

Tested with: Python 2.7, Python 3.7

History: see file changelog.md

Created on 11 Jun 2018

@author: Paolo Campanella
@author: Cristiano Nattero
"""

import getpass
import json
import logging
import os
import re
import sys
import time
import traceback
import zipfile
from time import sleep

import requests

from wasdi import waspyLogFormatter

name = "wasdi"

# Initialize "Members"
m_sUser = None
m_sPassword = None

m_sActiveWorkspace = None
m_sWorkspaceOwner = ''

m_sParametersFilePath = None
m_sSessionId = ''
m_bValidSession = False
m_sBasePath = None

m_bDownloadActive = True
m_bUploadActive = True
m_bVerbose = True
m_aoParamsDictionary = {}

m_sMyProcId = ''
m_sBaseUrl = 'http://www.wasdi.net/wasdiwebserver/rest'
# m_sBaseUrl = 'http://178.22.66.96/wasdiwebserver/rest'
m_bIsOnServer = False

# log configuration

m_bLogConfigured = False
m_oLogLevel = logging.DEBUG
m_asFormats = None
m_oLogger = None


def _configureLog():
    # configure logger options
    global m_oLogLevel
    m_oLogLevel = logging.INFO

    _sErrorEmphasizer = ' ***************************************************************'

    global m_asFormats
    m_asFormats = {
        logging.DEBUG: '%(asctime)s [%(levelname)s] %(name)s - %(message)s',
        logging.INFO: '%(asctime)s [%(levelname)s] %(name)s - %(message)s',
        logging.ERROR: f'%(asctime)s [%(levelname)s] %(name)s - %(message)s errorEmphasizer [%(levelname)s]',
        logging.CRITICAL: f'%(asctime)s [%(levelname)s] %(name)s - %(message)s errorEmphasizer [%(levelname)s]'
    }

    _oFormatter = waspyLogFormatter.WaspyLogFormatter(_sErrorEmphasizer, m_asFormats)
    _oHandler = logging.StreamHandler()
    _oHandler.setFormatter(_oFormatter)

    logging.root.addHandler(_oHandler)
    logging.root.setLevel(m_oLogLevel)

    global m_oLogger
    m_oLogger = logging.getLogger(__name__)

    m_bLogConfigured = True


if not m_bLogConfigured:
    _configureLog()


def printStatus():
    """Prints status
    """
    global m_sActiveWorkspace
    global m_sWorkspaceOwner
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

    m_oLogger.info('printStatus: begin testing log level')
    m_oLogger.debug('printStatus: testing DEBUG level')
    m_oLogger.info('printStatus: testing INFO level')
    m_oLogger.warning('printStatus: testing WARNING level')
    m_oLogger.error('printStatus: testing ERROR level (just a test, worry not ;) )')
    m_oLogger.critical('printStatus: testing CRITICAL level (just a test, worry not ;) )')
    m_oLogger.info('printStatus: done testing log level')

    m_oLogger.info(f'printStatus: user: {getUser()}')
    m_oLogger.info(f'printStatus: password: *******************************')
    m_oLogger.info(f'printStatus: session id: {getSessionId()}')
    m_oLogger.info(f'printStatus: active workspace: {getActiveWorkspaceId()}')
    m_oLogger.info(f'printStatus: workspace owner: {m_sWorkspaceOwner}')
    m_oLogger.info(f'printStatus: parameters file path: {getParametersFilePath()}')
    m_oLogger.info(f'printStatus: base path: {getBasePath()}')
    m_oLogger.info(f'printStatus: download active: {getDownloadActive()}')
    m_oLogger.info(f'printStatus: upload active: {getUploadActive()}')
    m_oLogger.info(f'printStatus: verbose: {getVerbose()}')
    m_oLogger.info(f'printStatus: param dict: {getParametersDict()}')
    m_oLogger.info(f'printStatus: proc id: {getProcId()}')
    m_oLogger.info(f'printStatus: base url: {getBaseUrl()}')
    m_oLogger.info(f'printStatus: is on server: {getIsOnServer()}')
    if m_bValidSession:
        m_oLogger.info('printStatus: session is valid :-)')
    else:
        m_oLogger.error('printStatus: session is not valid :-(')


def setVerbose(bVerbose: bool):
    """Sets verbosity

    :param bVerbose: False non verbose, True verbose
    :return:
    """
    if bVerbose is None:
        m_oLogger.error('setVerbose: passed None, won\'t change')
        return
    if not isinstance(bVerbose, bool):
        m_oLogger.warning('setVerbose: passed non boolean, trying to convert')
        try:
            bVerbose = bool(bVerbose)
        except:
            m_oLogger.error('setVerbose: cannot convert argument into boolean, won\'t change')
            return

    global m_bVerbose
    m_bVerbose = bVerbose
    if m_bVerbose:
        global m_oLogLevel
        m_oLogLevel = logging.DEBUG


def getVerbose() -> bool:
    """
    Get Verbose Flag
    :return: True or False
    """
    global m_bVerbose
    return m_bVerbose


def getParametersDict() -> dict:
    """
    Get the full Params Dictionary
    :return: a dictionary containing the parameters
    """
    global m_aoParamsDictionary
    return m_aoParamsDictionary


def setParametersDict(aoParams: dict):
    """
    Get the full Params Dictionary
    :param aoParams: dictionary of Parameters
    :return: a dictionary containing the parameters
    """
    global m_aoParamsDictionary
    m_aoParamsDictionary = aoParams


def addParameter(sKey: str, oValue: object):
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
    :param sParamPath Local Path of the parameters file
    """
    if sParamPath is None:
        m_oLogger.error('setParametersFilePath: passed None as path, won\'t change')
        return
    if len(sParamPath) < 1:
        m_oLogger.error('setParametersFilePath: string passed has zero length, won\'t change')
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


def setIsOnServer(bIsOnServer):
    """
    Set the Is on Server Flag: keep it false, as default, while developing
    :param bIsOnServer: set the flag to know if the processor is running on the server or on the local PC
    """
    global m_bIsOnServer
    m_bIsOnServer = bIsOnServer


def getIsOnServer():
    """
    Get the WASDI API URL
    :return: True if it is running on server, False if it is running on the local Machine
    """
    global m_bIsOnServer
    return m_bIsOnServer


def setDownloadActive(bDownloadActive):
    """
    When in development, set True to download locally files from Server.
    Set it to false to NOT donwload data. In this case the developer must check the availability of the files
    :param bDownloadActive: True (default) to activate autodownload. False to disactivate
    """

    if bDownloadActive is None:
        m_oLogger.error('setDownloadActive: passed None, won\'t change')
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
        m_oLogger.error('setUploadActive: passed None, won\'t change')
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
        m_oLogger.error('_loadConfigParams: config parameter file name is None, cannot load config')
        return
    if sConfigFilePath == '':
        m_oLogger.error('_loadConfigParams: config parameter file name is empty, cannot load config')
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
                try:
                    if m_bVerbose:
                        global m_oLogLevel
                        m_oLogLevel = logging.DEBUG
                except Exception:
                    pass
            if 'LOGLEVEL' in oJson:
                sLogLevel = str(oJson['LOGLEVEL']).upper()
                if sLogLevel == "DEBUG":
                    m_oLogLevel = logging.DEBUG
                elif sLogLevel == "INFO":
                    m_oLogLevel = logging.INFO
                elif sLogLevel == "WARNING":
                    m_oLogLevel = logging.WARNING
                elif sLogLevel == "ERROR":
                    m_oLogLevel = logging.ERROR
                elif sLogLevel == "CRITICAL":
                    m_oLogLevel = logging.CRITICAL
                else:
                    m_oLogger.error(f'_loadConfigParams: unrecognized log level: {sLogLevel}')
                m_oLogger.setLevel(m_oLogLevel)

        return True, sTempWorkspaceName, sTempWorkspaceID

    except Exception as oEx:
        m_oLogger.error('_loadConfigParams: something went wrong')
        return


def _loadParams():
    """
    Loads parameters from file, if specified in configuration file
    """
    global m_sParametersFilePath
    global m_aoParamsDictionary

    bParamLoaded = False
    if (m_sParametersFilePath is not None) and (m_sParametersFilePath != ''):
        try:
            with open(m_sParametersFilePath) as oJsonFile:
                m_aoParamsDictionary = json.load(oJsonFile)
                bParamLoaded = True
        except:
            pass

    if not bParamLoaded:
        m_oLogger.info('wasdi could not load param file. That is fine, you can still load it later, don\'t worry')


def refreshParameters():
    """
    Refresh parameters, reading the file again
    """
    _loadParams()


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

    sWname = None
    sWId = None
    m_bValidSession = False
    if sConfigFilePath is not None:
        bConfigOk, sWname, sWId = _loadConfig(sConfigFilePath)
        if bConfigOk is True:
            _loadParams()

    if m_sUser is None and m_sPassword is None:

        if sys.version_info > (3, 0):
            m_sUser = input('[INFO] waspy.init: Please Insert WASDI User:')
        else:
            m_sUser = raw_input('[INFO] waspy.init: Please Insert WASDI User:')

        m_sPassword = getpass.getpass(prompt='[INFO] waspy.init: Please Insert WASDI Password:', stream=None)

        m_sUser = m_sUser.rstrip()
        m_sPassword = m_sPassword.rstrip()

        if sys.version_info > (3, 0):
            sWname = input('[INFO] waspy.init: Please Insert Active Workspace Name (Enter to jump):')
        else:
            sWname = raw_input('[INFO] waspy.init: Please Insert Active Workspace Name (Enter to jump):')

    if m_sUser is None:
        m_oLogger.error('init: must initialize user first, but None given')
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
        sUrl = f'{m_sBaseUrl}/auth/checksession'
        oResponse = requests.get(sUrl, headers=asHeaders)
        if (oResponse is not None) and (oResponse.ok is True):
            oJsonResult = oResponse.json()
            try:
                sUser = f"{oJsonResult['userId']}"
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
            m_oLogger.error('init: must initialize password first, but None given')
            return False
        asHeaders = {'Content-Type': 'application/json'}
        sUrl = m_sBaseUrl + '/auth/login'
        sPayload = '{"userId":"' + m_sUser + '","userPassword":"' + m_sPassword + '" }'
        oResponse = requests.post(sUrl, data=sPayload, headers=asHeaders)
        if oResponse is None:
            m_oLogger.error('init: cannot authenticate')
            m_bValidSession = False
        elif oResponse.ok is not True:
            m_oLogger.error(f'init: cannot authenticate, server replied: {oResponse.status_code}')
            m_bValidSession = False
        else:
            oJsonResult = oResponse.json()
            try:
                m_sSessionId = f"{oJsonResult['sessionId']}"
                m_oLogger.info(f'init: returned session is: {m_sSessionId}')
                if m_sSessionId is not None and m_sSessionId != '' and m_sSessionId != 'None':
                    m_bValidSession = True
                else:
                    m_bValidSession = False
            except:
                m_bValidSession = False

    if m_bValidSession is True:
        m_oLogger.info('init: WASPY successfully initiated :-)')
        sW = getActiveWorkspaceId()
        if (sW is None) or (len(sW) < 1):
            if sWname is not None:
                openWorkspace(sWname)
            elif sWId is not None:
                openWorkspaceById(sWId)
    else:
        m_oLogger.error('init: could not init WASPY :-(')

    printStatus()
    return m_bValidSession


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

    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()

        openWorkspaceById(oJsonResult["stringValue"])

        return oJsonResult["stringValue"]
    else:
        return None


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

    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceName'] == sName:
                    return oWorkspace['ownerUserId']
            except:
                return ''

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

    oResult = requests.get(sUrl, headers=asHeaders)

    if (oResult is not None) and (oResult.ok is True):
        oJsonResult = oResult.json()

        for oWorkspace in oJsonResult:
            try:
                if oWorkspace['workspaceId'] == sWsId:
                    return oWorkspace['ownerUserId']
            except:
                return ''

    return ''


def openWorkspaceById(sWorkspaceId):
    """
    Open a workspace by Id
    :param sWorkspaceId: Workspace Id
    :return: the WorkspaceId as a String, '' if there is any error
    """
    global m_sActiveWorkspace
    global m_sWorkspaceOwner

    m_sActiveWorkspace = sWorkspaceId
    m_sWorkspaceOwner = getWorkspaceOwnerByWsId(sWorkspaceId)

    return m_sActiveWorkspace


def setActiveWorkspaceId(sActiveWorkspace):
    """
    Set the Active Workspace Id
    :param sActiveWorkpsace: Active Workspace Id
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


def openWorkspace(sWorkspaceName):
    """
    Open a workspace
    :param sWorkspaceName: Workspace Name
    :return: the WorkspaceId as a String, '' if there is any error
    """
    global m_sActiveWorkspace
    global m_sWorkspaceOwner

    m_sActiveWorkspace = getWorkspaceIdByName(sWorkspaceName)
    m_sWorkspaceOwner = getWorkspaceOwnerByName(sWorkspaceName)

    return m_sActiveWorkspace


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
    Get the list of products in the active workspace
    :return: the list is an array of string. Can be empty if there is any error
    """
    global m_sActiveWorkspace

    return getProductsByWorkspaceId(m_sActiveWorkspace)


def getPath(sFile):
    """
    Get Local File Path. If the file exists and needed the file will be automatically downloaded.
    Returns the full local path where to read or write sFile
    :param sFile name of the file
    :return: Local path where to read or write sFile 
    """

    if fileExistsOnWasdi(sFile) is True:
        return getFullProductPath(sFile)
    else:
        return getSavePath() + sFile


def getFullProductPath(sProductName):
    """
    Get the full local path of a product given the product name. If auto download is true and the code is running locally, WASDI will download the image and keep the file on the local PC
    Use the output of this API to get the full path to open a file
    :param sProductName: name of the product to get the path open (WITH the final extension)
    :return: local path of the Product File
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

    # Normalize the path and extract the name
    sProductName = os.path.basename(os.path.normpath(sProductName))
    sFullPath = os.path.join(sFullPath, m_sWorkspaceOwner, m_sActiveWorkspace, sProductName)

    # If we are on the local PC
    if m_bIsOnServer is False:
        # If the download is active
        if m_bDownloadActive is True:
            # If there is no local file
            if os.path.isfile(sFullPath) is False:
                # If the file exists on server
                if fileExistsOnWasdi(sProductName) is True:
                    # Download The File from WASDI
                    m_oLogger.info('getFullProductPath: LOCAL WASDI FILE MISSING: START DOWNLOAD... PLEASE WAIT')
                    downloadFile(sProductName)
                    m_oLogger.info('getFullProductPath: DONWLOAD COMPLETED')

    return sFullPath


def getSavePath():
    """
    Get the local base save path for a product. To save use this path + fileName. Path already include '/' as last char
    :return: local path to use to save files (with '/' as last char)
    """
    global m_sBasePath
    global m_sActiveWorkspace
    global m_sUser

    if m_bIsOnServer is True:
        sFullPath = '/data/wasdi/'
    else:
        sFullPath = m_sBasePath

    # empty string at the ends adds a final separator
    sFullPath = os.path.join(sFullPath, m_sWorkspaceOwner, m_sActiveWorkspace, "")

    return sFullPath


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
    :param sProcessId: Id of the process to query
    :return: the status or '' if there was any error

    STATUS are  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
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


def updateProcessStatus(sProcessId, sStatus, iPerc=-1):
    """
    Update the status of a process
    :param sProcessId: Id of the process to update. 
    :param sStatus: Status of the process. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
    :param iPerc: percentage of complete of the processor. Use -1 to ignore Percentage. Use a value between 0 and 100 to set it. 
    :return: the updated status as a String or '' if there was any problem
    """

    if sProcessId is None:
        m_oLogger.error('updateProcessStatus: cannot update status, process ID is None')
        return ''
    elif sProcessId == '':
        return ''

    if sStatus is None:
        m_oLogger.error('updateProcessStatus: cannot update status, status is None')
        return ''
    elif sStatus not in {'CREATED', 'RUNNING', 'STOPPED', 'DONE', 'ERROR', 'WAITING', 'READY'}:
        m_oLogger.error('updateProcessStatus: sStatus must be a string in: ' +
                        '{CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY')
        return ''

    if iPerc is None:
        m_oLogger.error('updateProcessStatus: percentage is None')
        return ''

    if iPerc < 0:
        if iPerc != -1:
            m_oLogger.error('updateProcessStatus: iPerc < 0 not valid')
            return ''
        else:
            m_oLogger.info('updateProcessStatus: iPerc = -1 - Not considered')
    elif iPerc > 100:
        m_oLogger.error('updateProcessStatus: iPerc > 100 not valid')
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


def updateStatus(sStatus, iPerc=-1):
    """
    Update the status of the running process
    :param sStatus: new status. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
    :param iPerc: new Percentage.-1 By default, means no change percentage. Use a value between 0 and 100 to set it.
    :return: the updated status as a String or '' if there was any problem
    """
    try:

        if m_bIsOnServer is False:
            m_oLogger.info("updateStatus: Running Locally, will not update status on server")
            return sStatus

        return updateProcessStatus(getProcId(), sStatus, iPerc)
    except Exception as oEx:
        m_oLogger.error(f'updateStatus: exception {oEx}')
        return ''


def updateProgressPerc(iPerc):
    """
    Update the actual progress Percentage of the processor
    :param iPerc: new Percentage. Use a value between 0 and 100 to set it.
    :return: updated status of the process or '' if there was any error
    """
    try:
        m_oLogger.info(f'updateProgressPerc( {iPerc} )')
        if iPerc is None:
            m_oLogger.error('updateProgressPerc: Passed None, expected a percentage')
            return ''

        if (getProcId() is None) or (len(getProcId()) < 1):
            m_oLogger.error('updateProgressPerc: Cannot update progress: process ID is not known')
            return ''

        if 0 > iPerc or 100 < iPerc:
            m_oLogger.warning(f'updateProgressPerc: passed {iPerc}, automatically resetting in [0, 100]')
            if iPerc < 0:
                iPerc = 0
            if iPerc > 100:
                iPerc = 100

        if m_bIsOnServer is False:
            m_oLogger.info("Running locally, will not updateProgressPerc on server")
            return "RUNNING"

        sStatus = "RUNNING"
        sUrl = f'{getBaseUrl()}/process/updatebyid?sProcessId={getProcId()}&status={sStatus}&perc={iPerc}&sendrabbit=1'
        asHeaders = _getStandardHeaders()
        oResponse = requests.get(sUrl, headers=asHeaders)
        sResult = ''
        if (oResponse is not None) and (oResponse.ok is True):
            oJson = oResponse.json()
            if (oJson is not None) and ("status" in oJson):
                sResult = f"{oJson['status']}"
        else:
            m_oLogger.error('updateProgressPerc: could not update progress')
        return sResult
    except Exception as oEx:
        m_oLogger.error(f'updateProgressPerc: exception: {oEx}')
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
    except Exception as oEx:
        m_oLogger.error(f'setProcessPayload: exception {oEx}')
        return ''


def setPayload(data):
    """
    Set the payload of the actual running process.
    The payload is saved only when run on Server. In local mode is just a print.
    :param data: data to save in the payload. Suggestion is to use JSON
    return None
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sMyProcId
    global m_bIsOnServer

    if m_bIsOnServer is True:
        setProcessPayload(m_sMyProcId, data)
    else:
        m_oLogger.info(f'setPayload{data}')


def saveFile(sFileName):
    """
    Ingest a new file in the Active WASDI Workspace.
    The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
    To work be sure that the file is on the server
    :param Name of the file to add to the workpsace
    :return: Status of the operation
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
    Downloads a file from WASDI
    :param sFileName: file to download
    :return: None
    """

    m_oLogger.info('downloadFile( ' + sFileName + ' )')

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

    m_oLogger.info('downloadfile: send request to configured url ' + sUrl)

    oResponse = requests.get(sUrl, headers=asHeaders, params=payload, stream=True)

    if (oResponse is not None) and (oResponse.status_code == 200):
        m_oLogger.info('downloadFile: got ok result, downloading')
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

        if os.path.exists(os.path.dirname(sSavePath)) == False:
            try:
                os.makedirs(os.path.dirname(sSavePath))
            except:  # Guard against race condition
                m_oLogger.error('downloadFile: cannot create File Path, aborting')
                return

        m_oLogger.info(f'downloadFile: downloading local file {sSavePath}')

        with open(sSavePath, 'wb') as oFile:
            for oChunk in oResponse:
                # m_logger.info('.')
                oFile.write(oChunk)
        m_oLogger.info(f'downloadFile: download done, new file locally available {sSavePath}')

        if (sAttachmentName is not None) and \
                (sAttachmentName != sFileName) and \
                sAttachmentName.lower().endswith('.zip'):
            sPath = getSavePath()
            _unzip(sAttachmentName, sPath)

    else:
        m_oLogger.error(f'downloadFile: download error, server code: {oResponse.status_code}')

    return


def wasdiLog(sLogRow):
    """
    Writes one row of Log
    :param sLogRow: text to log
    :return: None
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    sForceLogRow = str(sLogRow)

    if m_bIsOnServer:
        asHeaders = _getStandardHeaders()
        sUrl = m_sBaseUrl + '/processors/logs/add?processworkspace=' + m_sMyProcId
        oResult = requests.post(sUrl, data=sForceLogRow, headers=asHeaders)
        if oResult is None:
            m_oLogger.warning('wasdiLog: could not log')
        elif oResult.ok is not True:
            m_oLogger.warning(f'wasdiLog: could not log, server returned: {oResult.status_code}')
    else:
        m_oLogger.info(sForceLogRow)


def deleteProduct(sProduct):
    """
    Delete a Product from a Workspace
    :param sProduct: Name of the product to delete (WITH EXTENSION)
    :return: True if the file has been deleted, False if there was any error
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    if sProduct is None:
        m_oLogger.error('deleteProduct: product passed is None')

    asHeaders = _getStandardHeaders()
    sUrl = m_sBaseUrl
    sUrl += "/product/delete?sProductName="
    sUrl += sProduct
    sUrl += "&bDeleteFile=true&sWorkspaceId="
    sUrl += m_sActiveWorkspace
    sUrl += "&bDeleteLayer=true"
    oResult = requests.get(sUrl, headers=asHeaders)

    if oResult is None:
        m_oLogger.error('deleteProduct: deletion failed')
        return False
    elif oResult.ok is not True:
        m_oLogger.error(f'deleteProduct: deletion failed, server returned: {oResult.status_code}')
    else:
        return oResult.ok


def searchEOImages(sPlatform, sDateFrom, sDateTo,
                   fULLat=None, fULLon=None, fLRLat=None, fLRLon=None,
                   sProductType=None, iOrbitNumber=None,
                   sSensorOperationalMode=None, sCloudCoverage=None,
                   sProvider=None):
    """
    Search EO images

    :param sPlatform: satellite platform (S1 or S2)
    :param sDateFrom: inital date YYYY-MM-DD
    :param sDateTo: final date YYYY-MM-DD
    :param fULLat: Latitude of Upper-Left corner
    :param fULLon: Longitude of Upper-Left corner
    :param fLRLat: Latitude of Lower-Right corner
    :param fLRLon: Longitude of Lower-Right corner
    :param sProductType: type of EO product; If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
    :param iOrbitNumber: orbit number
    :param sSensorOperationalMode: sensor operational mode
    :param sCloudCoverage: interval of allowed cloud coverage, e.g. "[0 TO 22.5]"
    :param sProvider: WASDI Data Provider to query. Null means default node provider
    :return: a list of results represented as a Dictionary with many properties. 
    """
    aoReturnList = []

    if sPlatform is None:
        m_oLogger.error('searchEOImages: platform cannot be None')
        return aoReturnList

    # todo support other platforms
    if (sPlatform != "S1") and (sPlatform != "S2"):
        m_oLogger.error(f'searchEOImages: platform must be S1 or S2. Received [ {sPlatform} ]')
        return aoReturnList

    if sPlatform == "S1":
        if sProductType is not None:
            if not (sProductType == "SLC" or sProductType == "GRD" or sProductType == "OCN"):
                m_oLogger.error("searchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" +
                                sProductType)
                return aoReturnList

    if sPlatform == "S2":
        if sProductType is not None:
            if not (sProductType == "S2MSI1C" or sProductType == "S2MSI2Ap" or sProductType == "S2MSI2A"):
                m_oLogger.error("searchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received ["
                                + sProductType + "]")
                return aoReturnList

    if sDateFrom is None:
        m_oLogger.error("searchEOImages: sDateFrom cannot be None")
        return aoReturnList

    # if (len(sDateFrom) < 10) or (sDateFrom[4] != '-') or (sDateFrom[7] != '-'):
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateFrom)):
        m_oLogger.error("searchEOImages: sDateFrom must be in format YYYY-MM-DD")
        return aoReturnList

    if sDateTo is None:
        m_oLogger.error("searchEOImages: sDateTo cannot be None")
        return aoReturnList

    # if len(sDateTo) < 10 or sDateTo[4] != '-' or sDateTo[7] != '-':
    if not bool(re.match(r"\d\d\d\d\-\d\d\-\d\d", sDateTo)):
        m_oLogger.error("searchEOImages: sDateTo must be in format YYYY-MM-DD")
        return aoReturnList

    if sCloudCoverage is not None:
        # Force to be a String
        sCloudCoverage = str(sCloudCoverage)
        sCloudCoverage = sCloudCoverage.upper()

    # create query string:

    # platform name
    sQuery = "( platformname:"
    if sPlatform == "S2":
        sQuery += "Sentinel-2 "
    elif sPlatform == "S1":
        sQuery += "Sentinel-1"

    # If available add product type
    if sProductType is not None:
        sQuery += f" AND producttype:{sProductType}"

    # If available Sensor Operational Mode
    if (sSensorOperationalMode is not None) and (sPlatform == "S1"):
        sQuery += f" AND sensoroperationalmode:{sSensorOperationalMode}"

    # If available cloud coverage
    if (sCloudCoverage is not None) and (sPlatform == "S2"):
        sQuery += f" AND cloudcoverpercentage:{sCloudCoverage}"

    # If available add orbit number
    if iOrbitNumber is not None:
        if isinstance(iOrbitNumber, int):
            sQuery += f" AND relativeorbitnumber:{iOrbitNumber}"
        else:
            m_oLogger.warning(f'searchEOImages: iOrbitNumber is {iOrbitNumber}, but it should be an integer')
            try:
                iTmp = int(iOrbitNumber)
                m_oLogger.warning(f'searchEOImages: iOrbitNumber converted to: {iTmp}')
                sQuery += str(iTmp)
            except:
                m_oLogger.warning('searchEOImages: could not convert iOrbitNumber to an int, ignoring it')

            # Close the first block
    sQuery += ") "

    # Date Block
    sQuery += f"AND ( beginPosition:[{sDateFrom}T00:00:00.000Z TO {sDateTo}T23:59:59.999Z]"
    sQuery += f"AND ( endPosition:[{sDateFrom}T00:00:00.000Z TO {sDateTo}T23:59:59.999Z]"

    # Close the second block
    sQuery += ") "

    # footprint polygon
    if (fULLat is not None) and (fULLon is not None) and (fLRLat is not None) and (fLRLon is not None):
        sFootPrint = f'( footprint:"intersects(POLYGON(( '
        sFootPrint += f"{fULLon} {fLRLat},{fULLon} {fULLat},{fLRLon} {fULLat},{fLRLon}{fLRLat},{fULLon} {fLRLat}"
        sFootPrint += ')))") AND '
    sQuery = sFootPrint + sQuery

    sQueryBody = '["' + sQuery.replace('\"' '\\\"') + '"]'

    if sProvider is None:
        sProvider = "ONDA"

    sQuery = "sQuery=" + sQuery + "&offset=0&limit=10&providers=" + sProvider

    try:
        sUrl = getBaseUrl() + "/search/querylist?" + sQuery
        asHeaders = _getStandardHeaders()
        oResponse = requests.post(sUrl, data=sQueryBody, headers=asHeaders)
        try:
            # populate list from response
            oJsonResponse = oResponse.json()
            aoReturnList = oJsonResponse
        except Exception as oEx:
            m_oLogger.error('searchEOImages: exception while trying to convert response into JSON object')
            return aoReturnList

        m_oLogger.info("waspy.searchEOImages: search results:\n" + repr(aoReturnList))
        return aoReturnList
    except Exception as oEx:
        m_oLogger.error('searchEOImages: an error occured')
        m_oLogger.error(f'searchEOImages: {type(oEx)}')
        traceback.print_exc()
        m_oLogger.error(f'searchEOImages{oEx}')

    return aoReturnList


def getFoundProductName(aoProduct):
    """
    Get The name of a product from a Dictionary returned by Search EO Images
    :param aoProduct: dictionary representing the product as returned by Search EO Images
    :return: product name or '' if there was any error
    """
    if aoProduct is None:
        m_oLogger.error('getFoundProductName: product is None, aborting')
        return ''
    elif "title" not in aoProduct:
        m_oLogger.error('getFoundProductName: title not found in product, aborting')
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
        m_oLogger.error('fileExistsOnWasdi: file name must not be None')
        return False
    if len(sFileName) < 1:
        m_oLogger.error('fileExistsOnWasdi: File name too short')
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
        m_oLogger.error('fileExistsOnWasdi: failed contacting the server')
        return False
    elif oResult.ok is not True:
        m_oLogger.error(f'fileExistsOnWasdi: failed, server returned: {oResult.status_code}')
        return False
    else:
        return oResult.ok


def _unzip(sAttachmentName, sPath):
    """
    Unzips a file
    :param sAttachmentName: filename to unzip
    :param sPath: both the path where the file is and where it must be unzipped
    :return: None
    """
    m_oLogger.info('_unzip( ' + sAttachmentName + ', ' + sPath + ' )')
    if sPath is None:
        m_oLogger.error('_unzip: path is None')
        return
    if sAttachmentName is None:
        m_oLogger.error('_unzip: attachment to unzip is None')
        return

    try:
        sZipFilePath = os.path.join(sPath, sAttachmentName)
        zip_ref = zipfile.ZipFile(sZipFilePath, 'r')
        zip_ref.extractall(sPath)
        zip_ref.close()
    except:
        m_oLogger.error('_unzip: failed unzipping')

    return


def getProductBBOX(sFileName):
    """
    Gets the bounding box of a file
    :param sFileName: name of the file to query for bounding box
    :return: Bounding Box if available as a String comma separated in form SOUTH,WEST,EST,NORTH
    """

    sUrl = f'{getBaseUrl()}/product/byname?sProductName={sFileName}"&workspace="{getActiveWorkspaceId()}'
    asHeaders = _getStandardHeaders()

    oResponse = requests.get(sUrl, headers=asHeaders)

    try:
        if oResponse is None:
            m_oLogger.error('getProductBBOX: cannot get bbox for product')
        elif oResponse.ok is not True:
            m_oLogger.error(f'getProductBBOX: cannot get bbox product, server returned: {oResponse.status_code}')
        else:
            oJsonResponse = oResponse.json()
            if "bbox" in oJsonResponse:
                return oJsonResponse["bbox"]

    except:
        return ""

    return ""


def importProductByFileUrl(sFileUrl=None, sBoundingBox=None, sProvider=None):
    """
    Imports a product from a Provider in WASDI, starting from the File URL.
    :param sFileUrl: url of the file to import
    :param sBoundingBox: declared bounding box of the file to import
    :param sProvider: WASDI Data Provider to use. Use None for Default
    :return: execution status as a STRING. Can be DONE, ERROR, STOPPED.
    """

    m_oLogger.info(f'importProductByFileUrl( {sFileUrl}, {sBoundingBox} )')

    sReturn = "ERROR"

    if sFileUrl is None:
        m_oLogger.error('importProductByFileUrl: cannot find a link to download the requested product')
        return sReturn

    if sProvider is None:
        sProvider = "ONDA"

    sUrl = f'{getBaseUrl()}/filebuffer/download?sFileUrl={sFileUrl}&sProvider={sProvider}&sWorkspaceId={getActiveWorkspaceId()}'

    if sBoundingBox is not None:
        sUrl += f"&sBoundingBox={sBoundingBox}"

    if m_bIsOnServer:
        sUrl += f"&parent={getProcId()}"

    asHeaders = _getStandardHeaders()

    oResponse = requests.get(sUrl, headers=asHeaders)
    if oResponse is None:
        m_oLogger.error('importProductByFileUrl: cannot import product')
    elif oResponse.ok is not True:
        m_oLogger.error(f'importProductByFileUrl: cannot import product, server returned: {oResponse.status_code}')
    else:
        oJsonResponse = oResponse.json()
        if ("boolValue" in oJsonResponse) and (oJsonResponse["boolValue"] is True):
            if "stringValue" in oJsonResponse:
                sProcessId = str(oJsonResponse["stringValue"])
                sReturn = waitProcess(sProcessId)

    return sReturn


def asynchImportProductByFileUrl(sFileUrl=None, sBoundingBox=None, sProvider=None):
    """
    Asynch Import of a product from a Provider in WASDI, starting from file URL
    :param sFileUrl: url of the file to import
    :param sBoundingBox: declared bounding box of the file to import
    :param sProvider: WASDI Data Provider. Use None for default
    :return: ProcessId of the Download Operation or "ERROR" if there is any problem
    """

    m_oLogger.info(f'importProductByFileUrl( {sFileUrl}), {sBoundingBox} )')

    sReturn = "ERROR"

    if sFileUrl is None:
        m_oLogger.error('importProductByFileUrl: cannot find a link to download the requested product')
        return sReturn

    if sProvider is None:
        sProvider = "ONDA"

    sUrl = f'{getBaseUrl()}/filebuffer/download?sFileUrl={sFileUrl}&sProvider={sProvider}&sWorkspaceId={getActiveWorkspaceId()}'
    if sBoundingBox is not None:
        sUrl += f"&sBoundingBox={sBoundingBox}"

    if m_bIsOnServer:
        sUrl += f"&parent={getProcId()}"

    asHeaders = _getStandardHeaders()

    oResponse = requests.get(sUrl, headers=asHeaders)
    if oResponse is None:
        m_oLogger.error('importProductByFileUrl: cannot import product')
    elif oResponse.ok is not True:
        m_oLogger.error(f'importProductByFileUrl: cannot import product, server returned: {oResponse.status_code}')
    else:
        oJsonResponse = oResponse.json()
        if ("boolValue" in oJsonResponse) and (oJsonResponse["boolValue"] is True):
            if "stringValue" in oJsonResponse:
                sReturn = str(oJsonResponse["stringValue"])

    return sReturn


def importProduct(asProduct, sProvider=None):
    """
    Imports a product from a Provider in WASDI starting from the object returned by searchEOImages
    :param asProduct: product dictionary as returned by searchEOImages
    :param sProvider: WASDI Data Provider. Use None for default
    :return: execution status as a STRING. Can be DONE, ERROR, STOPPED.
    """

    if asProduct is None:
        m_oLogger.error("importProduct: input asPRoduct is none")
        return "ERROR"

    m_oLogger.info(f'importProduct( {asProduct} )')

    try:
        sBoundingBox = None
        sFileUrl = asProduct["link"]
        if "footprint" in asProduct:
            sBoundingBox = asProduct["footprint"]

        return importProductByFileUrl(sFileUrl, sBoundingBox, sProvider)
    except Exception as e:
        m_oLogger.error(f"importProduct: exception {e}")
        return "ERROR"


def asynchImportProduct(asProduct, sProvider=None):
    """
    Asynch Import a product from a Provider in WASDI starting from the object returned by searchEOImages
    :param asProduct: product dictionary as returned by searchEOImages
    :param sProvider: WASDI Data Provider. Use None for default
    :return: ProcessId of the Download Operation or "ERROR" if there is any problem
    """

    if asProduct is None:
        m_oLogger.error("importProduct: input asPRoduct is none")
        return "ERROR"

    m_oLogger.info(f'importProduct( {asProduct} )')

    try:
        sBoundingBox = None
        sFileUrl = asProduct["link"]
        if "footprint" in asProduct:
            sBoundingBox = asProduct["footprint"]

        return asynchImportProductByFileUrl(sFileUrl, sBoundingBox, sProvider)
    except Exception as e:
        m_oLogger.error(f"importProduct: exception {e}")
        return "ERROR"


def importProductList(aasProduct, sProvider=None):
    """
    Imports a list of product from a Provider in WASDI starting from an array of objects returned by searchEOImages
    :param aasProduct: Array of product dictionary as returned by searchEOImages
    :param sProvider: WASDI Data Provider. Use None for default 
    :return: execution status as an array of  STRINGs, one for each product in input. Can be DONE, ERROR, STOPPED.
    """

    if aasProduct is None:
        m_oLogger.error("importProductList: input asPRoduct is none")
        return "ERROR"

    m_oLogger.info(f'importProductList( {aasProduct} )')

    asReturnList = []

    # For Each product in input
    for asProduct in aasProduct:
        try:
            # Get BBOX and Link from the dictionary
            sBoundingBox = None
            sFileUrl = asProduct["link"]
            if "footprint" in asProduct:
                sBoundingBox = asProduct["footprint"]

            # Start the download propagating the Asynch Flag
            sReturn = asynchImportProductByFileUrl(sFileUrl, sBoundingBox, sProvider)

            # Append the process id to the list
            asReturnList.append(sReturn)
        except Exception as e:
            # Error!!
            m_oLogger.error(f"importProductList: exception {e}")
            asReturnList.append("ERROR")

    return waitProcesses(asReturnList)


def asynchImportProductList(aasProduct, sProvider=None):
    """
    Asynch Import a list of product from a Provider in WASDI starting from an array of objects returned by searchEOImages
    :param aasProduct: Array of product dictionary as returned by searchEOImages
    :param sProvider: WASDI Data Provider. Use None for default
    :return: array of the ProcessId of the Download Operations. An element can be "ERROR" if there was any problem
    """

    if aasProduct is None:
        m_oLogger.error("importProductList: input asPRoduct is none")
        return "ERROR"

    m_oLogger.info(f'importProductList( {aasProduct} )')

    asReturnList = []

    # For Each product in input
    for asProduct in aasProduct:
        try:
            # Get BBOX and Link from the dictionary
            sBoundingBox = None
            sFileUrl = asProduct["link"]
            if "footprint" in asProduct:
                sBoundingBox = asProduct["footprint"]

            # Start the download propagating the Asynch Flag
            sReturn = asynchImportProductByFileUrl(sFileUrl, sBoundingBox, sProvider)
            # Append the process id to the list
            asReturnList.append(sReturn)
        except Exception as e:
            # Error!!
            m_oLogger.error(f"importProductList: exception {e}")
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

    # For each image found
    for oImage in aoImages:

        # Get the file name
        sFile = oImage["properties"]["filename"]
        wasdiLog("Importing Image " + sFile)

        # Import in WASDI
        sStatus = importProduct(oImage, sProvider)

        # Import done?
        if sStatus == "DONE":
            # Add the file to the list of original files
            asOriginalFiles.append(sFile)
            # Generate the output name
            sOutputFile = sFile.replace(".zip", sPreProcSuffix)

            # Add the pre processed file to the list
            asPreProcessedFiles.append(sOutputFile)

            wasdiLog(sFile + " imported, starting workflow to get " + sOutputFile)

            # Is already there for any reason?
            if not fileExistsOnWasdi(sOutputFile):
                # No, start the workflow
                sProcId = asynchExecuteWorkflow(sFile, sOutputFile, sWorkflow)
                asRunningProcList.append(sProcId)
        else:
            wasdiLog("File " + sFile + " not imported in wasdi. Jump to the next")

    # Checkpoint: wait for all asynch workflows to finish
    wasdiLog("All image imported, waiting for all workflows to finish")
    waitProcesses(asRunningProcList)


def asynchExecuteProcessor(sProcessorName, aoParams={}):
    """
    Execute a WASDI processor asynchronously
    :param sProcessorName: WASDI processor name
    :param aoParams: a dictionary of parameters for the processor
    :return: processor ID
    """

    m_oLogger.info(f'asynchExecuteProcessor( {sProcessorName}, {aoParams} )')

    if sProcessorName is None:
        m_oLogger.error('asynchExecuteProcessor: processor name is None, aborting')
        return ''
    elif len(sProcessorName) <= 0:
        m_oLogger.error('asynchExecuteProcessor: processor name empty, aborting')
        return ''
    if isinstance(aoParams, dict) is not True:
        m_oLogger.error('asynchExecuteProcessor: parameters must be a dictionary but it is not, aborting')
        return ''

    sEncodedParams = json.dumps(aoParams)
    asHeaders = _getStandardHeaders()
    aoWasdiParams = {
        'workspace': m_sActiveWorkspace,
        'name': sProcessorName,
        'encodedJson': sEncodedParams
    }

    if m_bIsOnServer:
        aoWasdiParams['parent'] = getProcId()

    sUrl = getBaseUrl() + "/processors/run"

    oResponse = requests.get(sUrl, headers=asHeaders, params=aoWasdiParams)
    if oResponse is None:
        m_oLogger.error('asynchExecuteProcessor: something broke when contacting the server, aborting')
        return ''
    elif oResponse.ok is True:
        m_oLogger.info('asynchExecuteProcessor: API call OK')
        aoJson = oResponse.json()
        if "processingIdentifier" in aoJson:
            sProcessID = aoJson['processingIdentifier']
            return sProcessID
        else:
            m_oLogger.error('asynchExecuteProcessor: cannot extract processing identifier from response, aborting')
    else:
        m_oLogger.error(f'asynchExecuteProcessor: server returned status {oResponse.status_code}')

    return ''


def executeProcessor(sProcessorName, aoProcessParams):
    """
    Executes a WASDI Processor
    :param sProcessorName: WASDI processor name
    :param aoParams: a dictionary of parameters for the processor    
    :return: the Process Id if every thing is ok, '' if there was any problem
    """
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace

    sEncodedParams = json.dumps(aoProcessParams)
    asHeaders = _getStandardHeaders()
    aoParams = {
        'workspace': m_sActiveWorkspace,
        'name': sProcessorName,
        'encodedJson': sEncodedParams
    }

    if m_bIsOnServer:
        aoParams['parent'] = getProcId()

    sUrl = m_sBaseUrl + '/processors/run'

    oResult = requests.get(sUrl, headers=asHeaders, params=aoParams)

    sProcessId = ''

    if (oResult is not None) and (oResult.ok is True):
        oJsonResults = oResult.json()

        try:
            sProcessId = oJsonResults['processingIdentifier']
        except:
            return sProcessId

    return sProcessId


def waitProcess(sProcessId):
    """
    Wait for a process to End
    :param sProcessId: Id of the process to wait
    :return: output status of the process
    """
    if sProcessId is None:
        m_oLogger.error('waitProcess: Passed None, expected a process ID')
        return "ERROR"

    if sProcessId == '':
        m_oLogger.error('waitProcess: Passed empty, expected a process ID')
        return "ERROR"

    # Put this processor in WAITING
    updateStatus("WAITING")

    try:
        sStatus = ''

        while sStatus not in {"DONE", "STOPPED", "ERROR"}:
            sStatus = getProcessStatus(sProcessId)
            time.sleep(2)
    except:
        m_oLogger.error("Exception in the waitProcess")

    # Wait to be resumed
    _waitForResume()

    return sStatus


def _waitForResume():
    if m_bIsOnServer:
        # Put this processor as READY
        updateStatus("READY")

        try:
            # Wait for the WASDI Scheduler to resume us
            m_oLogger.info("Waiting for the scheduler to resume this process")
            sStatus = ''

            while sStatus not in {"RUNNING", "DONE", "STOPPED", "ERROR"}:
                sStatus = getProcessStatus(getProcId())
                time.sleep(2)

            m_oLogger.info("Process Resumed, let's go!")
        except Exception as oE:
            m_oLogger.error(f'_waitForResume: {oE}')


def waitProcesses(asProcIdList):
    """
    Wait for a list of processes to wait.
    The list of processes is an array of strings, each with a proc id to wait
    
    :param asProcIdList: list of strings, procId, to wait
    
    :return list of strings with the same number of elements in input, with the exit status of the processes
    """

    # Initialize the return list
    asReturnStatus = []

    # Check the input
    if asProcIdList is None:
        m_oLogger.warning("waitProcesses asProcIdList is none, return empty list")
        return asReturnStatus;

    if not isinstance(asProcIdList, list):
        m_oLogger.warning("waitProcesses asProcIdList is not a list, return empty list")
        return asReturnStatus;

    # Temporary List
    asProcessesToCheck = asProcIdList.copy();

    # Get the number of processes
    iProcessCount = len(asProcessesToCheck)

    iTotalTime = 0
    asNewList = []

    # Put this process in WAITING
    updateStatus("WAITING")

    # While there are processes to wait for
    while (iProcessCount > 0):

        # For all the processes
        iProcessIndex = 0

        for iProcessIndex in range(0, iProcessCount):

            # Get the id
            sProcessId = asProcessesToCheck[iProcessIndex]

            if sProcessId == "ERROR":
                sStatus = "ERROR"
            elif sProcessId == "":
                sStatus = "ERROR"
            else:
                # Get the status
                sStatus = getProcessStatus(sProcessId)

                # Check if is done
            if sStatus == "DONE" or sStatus == "STOPPED" or sStatus == "ERROR":
                # Ok one less
                m_oLogger.info("Process " + sProcessId + " finished with status " + sStatus)
            else:
                # Not yet, we still need to wait this
                asNewList.append(sProcessId)

        # Update the list 
        asProcessesToCheck = asNewList.copy()
        # Clean the temp one
        asNewList = []
        # Get the new total of proc to wait
        iProcessCount = len(asProcessesToCheck)
        # Sleep a little bit
        sleep(2)
        # Trace the time needed
        iTotalTime = iTotalTime + 2

    # We are done. Get again all the result to ensure the coordinations of the IN/OUT arrays 
    iProcessCount = len(asProcIdList)
    iProcessIndex = 0

    for iProcessIndex in range(0, iProcessCount):
        # Get Proc id
        sProcessId = asProcIdList[iProcessIndex]

        if sProcessId == "ERROR":
            sStatus = "ERROR"
        elif sProcessId == "":
            sStatus = "ERROR"
        else:
            # Get the status
            sStatus = getProcessStatus(sProcessId)

            # Save status in the output list
        asReturnStatus.append(sStatus)

    # Wait to be resumed
    _waitForResume()

    # Return the list of status
    return asReturnStatus


def uploadFile(sFileName):
    """
    Uploads a file to WASDI
    :param sFileName: name of file inside working directory OR path to file RELATIVE to working directory
    :return: True if succeded, False otherwise
    """

    m_oLogger.info(f'upload( {sFileName} )')

    if sFileName is None:
        m_oLogger.error('upload: the given file name is None, cannot upload')
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
        m_oLogger.error('_normPath: passed path is None')
        return None

    sPath = sPath.replace('/', os.path.sep)
    sPath = sPath.replace('\\', os.path.sep)

    return sPath


def addFileToWASDI(sFileName):
    """
    Add a file to the wasdi workspace
    :param sFileName: Name (with extension) of the file to add
    :return: status of the operation
    """
    return _internalAddFileToWASDI(sFileName, False)


def asynchAddFileToWASDI(sFileName):
    """
    Triggers the ingestion of File Name in the workspace
    :param: sFileName: Name (with extension) of the file to add
    :return: Process Id of the ingestion
    """
    return _internalAddFileToWASDI(sFileName, True)


def _internalAddFileToWASDI(sFileName, bAsynch=None):
    m_oLogger.info(f'_internalAddFileToWASDI( {sFileName}, {bAsynch} )')

    if sFileName is None:
        m_oLogger.error('_internalAddFileToWASDI: file name is None, aborting')
        return ''
    if not isinstance(sFileName, str):
        m_oLogger.warning('_internalAddFileToWASDI: file name is not a string, trying conversion')
        try:
            sFileName = str(sFileName)
        except:
            m_oLogger.error('_internalAddFileToWASDI: cannot convert file name into string, aborting')
            return ''
    if len(sFileName) < 1:
        m_oLogger.error('_internalAddFileToWASDI: file name has zero length, aborting')
        return ''

    if bAsynch is None:
        m_oLogger.warning('_internalAddFileToWASDI: asynch flag is None, assuming False')
        bAsynch = False
    if not isinstance(bAsynch, bool):
        m_oLogger.warning('_internalAddFileToWASDI: asynch flag is not a boolean, trying casting')
        try:
            bAsynch = bool(bAsynch)
        except:
            m_oLogger.error('_internalAddFileToWASDI: could not convert asynch flag into bool, aborting')
            return ''

    sResult = ''
    try:
        if getUploadActive() is True:
            sFilePath = os.path.join(getSavePath(), sFileName)
            if fileExistsOnWasdi(sFilePath) is False:
                m_oLogger.info('_internalAddFileToWASDI: remote file is missing, uploading')
                try:
                    uploadFile(sFileName)
                    m_oLogger.info('_internalAddFileToWASDI: file uploaded, keep on working!')
                except:
                    m_oLogger.error('_internalAddFileToWASDI: could not proceed with upload')

        sUrl = getBaseUrl() + "/catalog/upload/ingestinws?file=" + sFileName + "&workspace=" + getActiveWorkspaceId()

        if m_bIsOnServer:
            sUrl += "&parent="
            sUrl += getProcId()

        asHeaders = _getStandardHeaders()
        oResponse = requests.get(url=sUrl, headers=asHeaders)
        if oResponse is None:
            m_oLogger.error('_internalAddFileToWASDI: cannot contact server')
        elif oResponse.ok is not True:
            m_oLogger.error(f'_internalAddFileToWASDI: failed, server replied {oResponse.status_code}')
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
                    m_oLogger.error('_internalAddFileToWASDI: impossible to ingest the file in WASDI')
    except:
        m_oLogger.error('_internalAddFileToWASDI: something broke alongside')

    return sResult


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
    m_oLogger.info(f'subset( {sInputFile}, {sOutputFile}, {dLatN}, {dLonW}, {dLatS}, {dLonE} )')

    if sInputFile is None:
        m_oLogger.error('subset: input file must not be None, aborting')
        return ''
    if len(sInputFile) < 1:
        m_oLogger.error('subset: input file name must not have zero length, aborting')
        return ''
    if sOutputFile is None:
        m_oLogger.error('subset: output file must not be None, aborting')
        return ''
    if len(sOutputFile) < 1:
        m_oLogger.error('subset: output file name len must not have zero length, aborting')
        return ''

    sUrl = f'{m_sBaseUrl}/processing/geometric/subset?sSourceProductName={sInputFile}'
    sUrl += f'&sDestinationProductName={sOutputFile}&sWorkspaceId={m_sActiveWorkspace}'

    if m_bIsOnServer:
        sUrl += "&parent="
        sUrl += getProcId()

    # sSubsetSetting = "{ \"latN\":" + dLatN + ", \"lonW\":" + dLonW + ", \"latS\":" + dLatS + ", \"lonE\":" + dLonE + " }"
    sSubsetSetting = "{ "
    sSubsetSetting += f'"latN":{dLatN},"lonW":{dLonW}, "latS":{dLatS}, "lonE":{dLonE}'
    sSubsetSetting += " }"
    asHeaders = _getStandardHeaders()
    oResponse = requests.get(sUrl, data=sSubsetSetting, headers=asHeaders)
    if oResponse is None:
        m_oLogger.error('subset: cannot contact server')
        return ''
    if oResponse.ok is not True:
        m_oLogger.error(f'subset: failed, server returned {oResponse.status_code}')
        return ''
    else:
        oJson = oResponse.json()
        if oJson is not None:
            if 'stringValue' in oJson:
                sProcessId = oJson['stringValue']
                return waitProcess(sProcessId)

    return ''


def multiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE):
    """
    Creates a Many Subsets from an image. MAX 10 TILES PER CALL
    :param sInputFile: Input file 
    :param sOutputFile: Array of Output File Names
    :param dLatN: Array of Latitude north of the subset
    :param dLonW: Array of Longitude west of the subset
    :param dLatS: Array of Latitude South of the subset
    :param dLonE: Array of Longitude Est of the subset
    """

    m_oLogger.info(f'multiSubset( {sInputFile}, {asOutputFiles}, {adLatN}, {adLonW}, {adLatS}, {adLonE} )')

    if sInputFile is None:
        m_oLogger.error('multiSubset: input file must not be None, aborting')
        return ''
    if len(sInputFile) < 1:
        m_oLogger.error('multiSubset: input file name must not have zero length, aborting')
        return ''
    if asOutputFiles is None:
        m_oLogger.error('multiSubset: output files must not be None, aborting')
        return ''
    if len(asOutputFiles) < 1:
        m_oLogger.error('multiSubset: output file names len must not have zero length, aborting')
        return ''

    if len(asOutputFiles) > 10:
        m_oLogger.error('multiSubset: max allowed 10 tiles per call')
        return ''

    sUrl = f'{m_sBaseUrl}/processing/geometric/multisubset?sSourceProductName={sInputFile}&sDestinationProductName={sInputFile}&sWorkspaceId={m_sActiveWorkspace}'

    if m_bIsOnServer:
        sUrl += "&parent="
        sUrl += getProcId()

    aoBody = {
        "outputNames": asOutputFiles,
        "latNList": adLatN,
        "lonWList": adLonW,
        "latSList": adLatS,
        "lonEList": adLonE
    }

    sSubsetSetting = json.dumps(aoBody)
    asHeaders = _getStandardHeaders()

    oResponse = requests.post(sUrl, headers=asHeaders, data=sSubsetSetting)

    if oResponse is None:
        m_oLogger.error('multiSubset: cannot contact server')
        return ''

    if oResponse.ok is not True:
        m_oLogger.error(f'multiSubset: failed, server returned {oResponse.status_code}')
        return ''
    else:
        oJson = oResponse.json()
        if oJson is not None:
            if 'stringValue' in oJson:
                sProcessId = oJson['stringValue']
                return waitProcess(sProcessId)

    return ''


def executeWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName):
    """
    Execute a SNAP Workflow available in WASDI (you can use WASDI to upload your SNAP Graph XML and use from remote)
    :param asInputFileNames: array of the inputs of the workflow. Must correspond to the number of inputs of the workflow.
    :param asOutputFileNames: array of the  outputs of the workflow. Must correspond to the number of inputs of the workflow.
    :param sWorkflowName: Name of the workflow to run
    :return: final status of the executed Workflow
    """
    return _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, False)


def asynchExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName):
    """
    Trigger the asynch execution of a SNAP Workflow available in WASDI (you can use WASDI to upload your SNAP Graph XML and use from remote)
    :param asInputFileNames: array of the inputs of the workflow. Must correspond to the number of inputs of the workflow.
    :param asOutputFileNames: array of the  outputs of the workflow. Must correspond to the number of inputs of the workflow.
    :param sWorkflowName: Name of the workflow to run
    :return: Process Id of the started workflow
    """
    return _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, True)


def _internalExecuteWorkflow(asInputFileNames, asOutputFileNames, sWorkflowName, bAsynch=False):
    """
    Internal call to execute workflow

    :param asInputFileNames: name of the file in input (string WITH extension) or array of strings of the files in input (WITH extension)
    :param asOutputFileNames: name of the file in output (string WITH extension) or array of strings of the files in output (WITH extension)
    :param sWorkflowName: name of the SNAP workflow uploaded in WASDI
    :param bAsynch: true to run asynch, false to run synch
    :return: processID if asynch, status of the executed process if synch, empty string in case of failure
    """

    m_oLogger.info(f'_internalExecuteWorkflow( {asInputFileNames}, {asOutputFileNames}, {sWorkflowName}, {bAsynch} )')

    # if we got only a single file input, let transform it in an array
    if not isinstance(asInputFileNames, list):
        asInputFileNames = [asInputFileNames]

    if not isinstance(asOutputFileNames, list):
        asOutputFileNames = [asOutputFileNames]

    if asInputFileNames is None:
        m_oLogger.error('_internalExecuteWorkflow: input file names None, aborting')
        return ''
    elif len(asInputFileNames) <= 0:
        m_oLogger.error('_internalExecuteWorkflow: no input file names, aborting')
        return ''

    if asOutputFileNames is None:
        m_oLogger.error('_internalExecuteWorkflow: output file names None, aborting')
        return ''
    # elif len(asOutputFileNames) <= 0:
    #     m_logger.error('_internalExecuteWorkflow: no output file names, aborting')
    #     return ''

    if sWorkflowName is None:
        m_oLogger.error('_internalExecuteWorkflow: workspace name is None, aborting')
        return ''
    elif len(sWorkflowName) <= 0:
        m_oLogger.error('_internalExecuteWorkflow: workflow name too short, aborting')
        return ''

    sProcessId = ''
    sWorkflowId = None
    sUrl = getBaseUrl() + "/processing/graph_id?workspace=" + getActiveWorkspaceId()

    if m_bIsOnServer:
        sUrl += "&parent="
        sUrl += getProcId()

    # get a list of workflows, with entries in this form: :
    #   {  "description":STRING,
    #       "name": STRING,
    #       "workflowId": STRING }
    aoWorkflows = getWorkflows()
    aoDictPayload = None
    if aoWorkflows is None:
        m_oLogger.error('_internalExecuteWorkflow: workflow list is None, aborting')
        return ''
    elif len(aoWorkflows) <= 0:
        m_oLogger.error('_internalExecuteWorkflow: workflow list is empty, aborting')
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
        m_oLogger.error('_internalExecuteWorkflow: workflow name not found, aborting')
        return ''

    try:
        aoDictPayload["inputFileNames"] = asInputFileNames
        aoDictPayload["outputFileNames"] = asOutputFileNames
    except:
        m_oLogger.error('_internalExecuteWorkflow: payload could not be generated, aborting')
        return ''

    m_oLogger.info(f'_internalExecuteWorkflow: about to HTTP put to {sUrl} with payload {aoDictPayload}')
    asHeaders = _getStandardHeaders()
    oResponse = requests.post(sUrl, headers=asHeaders, data=json.dumps(aoDictPayload))
    if oResponse is None:
        m_oLogger.error('_internalExecuteWorkflow: communication with the server failed, aborting')
        return ''
    elif oResponse.ok is True:
        m_oLogger.info('_internalExecuteWorkflow: server replied OK')
        asJson = oResponse.json()
        if "stringValue" in asJson:
            sProcessId = asJson["stringValue"]
            if bAsynch is True:
                return sProcessId
            else:
                return waitProcess(sProcessId)
        else:
            m_oLogger.error('_internalExecuteWorkflow: cannot find process ID in response, aborting')
            return ''
    else:
        m_oLogger.error(f'_internalExecuteWorkflow: server returned status {oResponse.status_code}')
        m_oLogger.error(str(oResponse.content))
    return ''


def asynchMosaic(asInputFiles, sOutputFile, iNoDataValue=None, iIgnoreInputValue=None):
    """
    Start a mosaic out of a set of images in asynch way

    :param asInputFiles: List of input files to mosaic
    :param sOutputFile: Name of the mosaic output file
    :param iNoDataValue: Value to use as noData. Use -1 to ignore
    :param iIgnoreInputValue: Value to ignore from the input files of the mosaic. Use -1 to ignore
    :return: Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
    """

    return mosaic(asInputFiles, sOutputFile, iNoDataValue, iIgnoreInputValue, True)


def mosaic(
        asInputFiles: list,
        sOutputFile: str,
        iNoDataValue: int = None,
        iIgnoreInputValue: int = None,
        bAsynch: bool = False
) -> str:
    """
    Creates a mosaic out of a set of images

    :param asInputFiles: List of input files to mosaic
    :param sOutputFile: Name of the mosaic output file
    :param iNoDataValue: Value to use as noData. Use -1 to ignore
    :param iIgnoreInputValue: Value to ignore from the input files of the mosaic. Use -1 to ignore
    :param bAsynch: True to return after the triggering, False to wait the process to finish
    :return: Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
    """
    asBands = []
    fPixelSizeX = -1.0
    fPixelSizeY = -1.0
    sCrs = None
    fSouthBound = -1.0
    fNorthBound = -1.0
    fEastBound = -1.0
    fWestBound = -1.0
    sOverlappingMethod = "MOSAIC_TYPE_OVERLAY"
    bShowSourceProducts = False
    sElevationModelName = "ASTER 1sec GDEM"
    sResamplingName = "Nearest"
    bUpdateMode = False
    bNativeResolution = True
    sCombine = "OR"

    m_oLogger.info(f'mosaic( {asInputFiles}, {sOutputFile}, {iNoDataValue}, {iIgnoreInputValue}, {bAsynch} )')

    if asInputFiles is None:
        m_oLogger.error('mosaic: list of input files is None, aborting')
        return ''
    elif len(asInputFiles) <= 0:
        m_oLogger.error('mosaic: list of input files is empty, aborting')
        return ''

    if sOutputFile is None:
        m_oLogger.error('mosaic: name of output file is None, aborting')
        return ''
    elif isinstance(sOutputFile, str) is False:
        m_oLogger.error(f'mosaic: output file name must be a string, but a {type(sOutputFile)} was passed, aborting')
        return ''
    elif len(sOutputFile) <= 0:
        m_oLogger.error('mosaic: output file name is empty, aborting')
        return ''

    sUrl = f'{getBaseUrl()}/processing/geometric/mosaic?sDestinationProductName={sOutputFile}&sWorkspaceId={getActiveWorkspaceId()}'

    if m_bIsOnServer:
        sUrl += f"&parent={getProcId()}"

    sOutputFormat = "GeoTIFF"
    if sOutputFile.endswith(".dim"):
        sOutputFormat = "BEAM-DIMAP"
    if sOutputFile.endswith(".vrt"):
        sOutputFormat = "VRT"

    if sCrs is None:
        sCrs = _getDefaultCRS()

    try:
        aoMosaicSettings = {
            'crs': sCrs,
            'southBound': fSouthBound,
            'eastBound': fEastBound,
            'westBound': fWestBound,
            'northBound': fNorthBound,
            'pixelSizeX': fPixelSizeX,
            'pixelSizeY': fPixelSizeY,
            'noDataValue': iNoDataValue,
            'inputIgnoreValue': iIgnoreInputValue,
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
        m_oLogger.error('mosaic: cannot build DTO, please check your input. Aborting')
        return ''

    asHeaders = _getStandardHeaders()
    oResponse = requests.post(sUrl, data=json.dumps(aoMosaicSettings), headers=asHeaders)
    if oResponse is None:
        m_oLogger.error('mosaic: cannot contact server, aborting')
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
        m_oLogger.error(f'mosaic: server responded with status: {oResponse.status_code}, aborting')
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
    m_oLogger.info('WASPY - The WASDI Python Library. Include in your code for space development processors. Visit '
                   'www.wasdi.net')
