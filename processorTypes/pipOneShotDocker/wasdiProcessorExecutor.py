'''
Created on 24 Feb 2024

@author: p.campanella
'''

import wasdi
import os
import urllib.parse
import json
import traceback
import subprocess
import re
import time

m_sProcId = ""

def _getEnvironmentVariable(sVariable):
    try:
        sValue = os.environ[sVariable]
        return sValue
    except KeyError:
        return None

def log(sLogString):
    print("[" + m_sProcId + "] wasdiProcessorExecutor PIP One Shot Engine v.2.1.3 - " + sLogString)

def executeProcessor():
    # We need the proc id for logs
    global m_sProcId

    sForceStatus = 'ERROR'

    #Run the processor
    try:
        import myProcessor
        wasdi.wasdiLog("wasdi.executeProcessor RUN " + m_sProcId)
        myProcessor.run()
        wasdi.wasdiLog("wasdi.executeProcessor Done")

        sForceStatus = 'DONE'

    except Exception as oEx2:
        wasdi.wasdiLog("wasdi.executeProcessor EXCEPTION")
        wasdi.wasdiLog(repr(oEx2))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog("wasdi.executeProcessor generic EXCEPTION")
    finally:
        sFinalStatus = wasdi.getProcessStatus(m_sProcId)

        if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
            wasdi.wasdiLog("wasdi.executeProcessor Process finished. Forcing status to " + sForceStatus)
            wasdi.updateProcessStatus(m_sProcId, sForceStatus, 100)

    return

def refresh_package_list(sRefreshPackageList: str):

    # We need the proc id for logs
    global m_sProcId

    sForceStatus = 'ERROR'

    #Run the processor
    try:
        aoPackagesList = {}

        log("Get Outdated packages")
        aoOutdatedDependencies = pm_list_packages("o")
        aoPackagesList["outdated"] = aoOutdatedDependencies

        log("Get Updated packages")
        aoUpdatedDependencies = pm_list_packages("u")
        aoPackagesList["uptodate"] = aoUpdatedDependencies

        log("Get Manager Version")
        oManagerVersion = pm_manager_version()
        aoPackagesList["packageManager"] = oManagerVersion

        sFullPath = wasdi.getPath(sRefreshPackageList)

        log('Saving Packages Info file in ' + sFullPath)

        with open(sFullPath, 'w') as oFile:
            sJsonContent = json.dumps(aoPackagesList)
            oFile.write(sJsonContent)
            oFile.flush()
            oFile.close()
            log("Check closed: " + str(oFile.closed))

        if os.path.exists(sFullPath):
            log("Packages File written")
        else:
            log("ERROR Packages File NOT written")

        log('Packages list done')

        sForceStatus = 'DONE'

    except Exception as oEx2:
        wasdi.wasdiLog("wasdi.executeProcessor EXCEPTION")
        wasdi.wasdiLog(repr(oEx2))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog("wasdi.executeProcessor generic EXCEPTION")
    finally:
        sFinalStatus = wasdi.getProcessStatus(m_sProcId)

        if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
            wasdi.wasdiLog("wasdi.executeProcessor Process finished. Forcing status to " + sForceStatus)
            wasdi.updateProcessStatus(m_sProcId, sForceStatus, 100)


def pm_list_packages(sFlag: str):
    sCommand: str = 'pip list'
    if sFlag != '':
        sCommand = sCommand + ' -' + sFlag

    sOutput: str = __execute_pip_command_and_get_output(sCommand)
    log("Got output\n " + sOutput)
    aoDependencies: list = __parse_list_command_output(sOutput)
    return  aoDependencies

def pm_manager_version():
    log('/packageManager/managerVersion/')

    command: str = 'pip -V'

    sCommandOutput: str = __execute_pip_command_and_get_output(command)

    start: str = 'pip '
    end: str = ' from '

    sVersionFromOutput = re.search('%s(.*)%s' % (start, end), sCommandOutput).group(1)

    asVersion: list = sVersionFromOutput.split('.')

    oVersion = {}
    oVersion["name"] = "pip"
    oVersion["version"] = sVersionFromOutput
    oVersion["major"] = asVersion[0]
    oVersion["minor"] = asVersion[1]
    oVersion["patch"] = asVersion[2]

    return oVersion

def __execute_pip_command_and_get_output(command: str) -> str:
    log('__execute_pip_command_and_get_output: ' + command)

    oPipProcess = subprocess.run(command + ' > tmp', shell=True, capture_output=True)

    sOutput = open('tmp', 'r').read()
    os.remove('tmp')

    stderr: str = oPipProcess.stderr.decode("utf-8")

    if stderr != '':
        if sOutput == '':
            sOutput = stderr
        else:
            sOutput += stderr

    return sOutput

def __parse_list_command_output(output: str) -> list:
    asLines: list = output.splitlines()

    sHeader: str = asLines[0]

    log('__parse_list_command_output')

    asHeaders: list = sHeader.split()

    for i in range(len(asHeaders)):
        asHeaders[i] = asHeaders[i].lower()

    aoDependencies: list = []

    for sLine in asLines[2:]:
        asColumns = sLine.split()

        if len(asHeaders) == 2:
            aoDependencies.append({
                "managerName": "pip",
                "packageName": asColumns[0],
                "currentVersion": asColumns[1]})
        elif len(asHeaders) == 4:
            aoDependencies.append({
                "managerName": "pip",
                "packageName": asColumns[0],
                "currentVersion": asColumns[1],
                "latestVersion": asColumns[2],
                "type": asColumns[3]})

    return aoDependencies

if __name__ == '__main__':
    try:
        # Read the process Id
        sProcId = _getEnvironmentVariable('WASDI_PROCESS_WORKSPACE_ID')

        if sProcId is not None:
            m_sProcId = sProcId
        else:
            m_sProcId = "N.A."

        log("Assigned Proc Id " + m_sProcId)

        # Read the encoded parameters
        sEncodedParams = _getEnvironmentVariable('WASDI_ONESHOT_ENCODED_PARAMS')

        if sEncodedParams is None:
            log("No params available, use empty one")
            sEncodedParams = "%7B%7D"

        if sEncodedParams == "":
            log("No params available, use empty one")
            sEncodedParams = "%7B%7D"

        sDecodedParams = urllib.parse.unquote(sEncodedParams)
        aoParameters = json.loads(sDecodedParams)

        wasdi.setParametersDict(aoParameters)

        # Read the User
        sUserId = _getEnvironmentVariable('WASDI_USER')

        if sUserId is None:
            log("User Id not available")

        # Read the Session Id
        sSessionId = _getEnvironmentVariable('WASDI_SESSION_ID')

        if sSessionId is None:
            log("Session Id not available")

        # Read the Workspace Id
        sWorkspaceId = _getEnvironmentVariable('WASDI_WORKSPACE_ID')

        if sWorkspaceId is None:
            log("Workspace Id not available")

        # Read the on-server flag
        bIsOnServer = True
        sOnServer = _getEnvironmentVariable('WASDI_ONESHOT_ON_SERVER')

        if sOnServer is not None:
            if sOnServer == "0" or sOnServer.lower() == "false":
                bIsOnServer = False

        # set the server flags
        wasdi.setIsOnServer(bIsOnServer)
        wasdi.setIsOnExternalServer(not bIsOnServer)
        wasdi.setDownloadActive(True)
        wasdi.setUploadActive(True)

        log("wasdi.executeProcessor: init waspy lib")
        # The lib will read all the data from env
        if not wasdi.init():
            log("There was an error in init, we try to execute but it will likely not work")
            wasdi.wasdiLog("There was an error in init, we try to execute but it will likely not work")
        else:
            log("Init done")
            wasdi.wasdiLog("Init done")

        bRun = True
        sRefreshPackageList = _getEnvironmentVariable('WASDI_ONESHOT_REFRESH_PACKAGE_LIST')

        if sRefreshPackageList is not None:
            if sRefreshPackageList != "":
                log("Refreshing package list in temp file " + sRefreshPackageList)
                refresh_package_list(sRefreshPackageList)
                bRun = False

        if bRun:
            wasdi.wasdiLog("Starting Processor")
            executeProcessor()

    except Exception as oEx:
        wasdi.wasdiLog("wasdi.executeProcessor: EXCEPTION")
        wasdi.wasdiLog(repr(oEx))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog("wasdi.executeProcessor generic EXCEPTION")