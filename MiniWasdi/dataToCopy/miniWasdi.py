import os
import json
import time
import wasdi
import requests

def _getEnvironmentVariable(sVariable):
    try:
        sValue = os.environ[sVariable]
        return sValue
    except KeyError:
        return None


def waitProcess(sProcessId):
    """
    Wait for a process to End

    :param sProcessId: Id of the process to wait
    """
    if sProcessId is None:
        return "ERROR"

    if sProcessId == '':
        return "ERROR"

    if sProcessId in {"DONE", "STOPPED", "ERROR", "CREATED", "WAITING", "READY"}:
        return sProcessId

    # Put this processor in WAITING
    sStatus = ''

    try:
        while sStatus not in {"DONE", "STOPPED", "ERROR"}:
            try:
                sStatus = wasdi.getProcessStatus(sProcessId, wasdi.getBaseUrl())

                if sStatus in {"DONE", "STOPPED", "ERROR"}:
                    break

                time.sleep(5)
            except Exception as oInnerEx:
                print("[STARTUP] Exception in the waitProcess loop " + str(oInnerEx))
    except:
        print("[STARTUP] Exception in the waitProcess")
    
    return sStatus

def _upload_processor(sApiUrl, sZipPath, sZipFileName):
    
    # Ensure the name is lowercase
    sName = os.path.splitext(sZipFileName)[0].lower()

    # Default params to upload the processor
    aoQueryParams = {
        "workspace": "",
        "name": sName,
        "version": "1",
        "description": "",
        "type": "local_python312",
        "paramsSample": "%7B%7D",
        "public": 1,
        "timeout": -1,
        "force": True
    }

    # No auth needed in miniwasdi
    aoHeaders = {
        "x-session-token": "",
    }

    with open(sZipPath, "rb") as oZipFile:
        aoFiles = {
            "file": (sZipFileName, oZipFile, "application/zip"),
        }
        oResponse = requests.post(
            sApiUrl,
            headers=aoHeaders,
            params=aoQueryParams,
            files=aoFiles,
            timeout=120,
        )

    print(f"[STARTUP] Upload status for {sZipFileName}: {oResponse.status_code}")
    print(oResponse.text)

    if oResponse.status_code != 200:
        print(f"[STARTUP] Error uploading processor {sZipFileName}: {oResponse.status_code} - {oResponse.text}")
    else:
        sResponseJson = oResponse.json()
        bOk = sResponseJson.get("boolValue", False)
        sProcessObjId = sResponseJson.get("stringValue", "")

        if bOk:
            print(f"[STARTUP] Processor {sZipFileName} uploaded successfully waiting for deployment.")
            waitProcess(sProcessObjId)
            print(f"[STARTUP] Processor {sZipFileName} deployed successfully.")
        else:
            print(f"[STARTUP] Error deploying processor {sZipFileName}: {sProcessObjId}")

def _update_processor(sBaseUrl, sZipPath, sZipFileName):
    
    # Ensure the name is lowercase
    sName = os.path.splitext(sZipFileName)[0].lower()

    # Default params to query the processor
    aoQueryParams = {
        "name": sName
    }

    # No auth needed in miniwasdi
    aoHeaders = {
        "x-session-token": "",
    }

    sUrl = sBaseUrl + "processors/getprocessor"

    oResponse = requests.get(
        sUrl,
        headers=aoHeaders,
        params=aoQueryParams,
        timeout=120
    )

    if (oResponse.status_code != 200):
        print(f"[STARTUP] Error fetching processor {sZipFileName} for update: {oResponse.status_code} - {oResponse.text}")
        return
    
    sResponseJson = oResponse.json()

    dLastZipFileUpdate = os.path.getmtime(sZipPath)*1000.0
    dProcessorUpdate = sResponseJson.get("lastUpdate", 0)

    if dLastZipFileUpdate > dProcessorUpdate:
        print(f"[STARTUP] Processor {sZipFileName} is outdated, updating it.")

        sUrl = sBaseUrl + "processors/updatefiles"

        aoQueryParams = {
            "processorId": sResponseJson.get("processorId", ""),
            "workspace": "",
            "file": sZipFileName
        }

        with open(sZipPath, "rb") as oZipFile:
            aoFiles = {
                "file": (sZipFileName, oZipFile, "application/zip"),
            }
            oResponse = requests.post(
                sUrl,
                headers=aoHeaders,
                params=aoQueryParams,
                files=aoFiles,
                timeout=120,
            )

        print(f"[STARTUP] Update status for {sZipFileName}: {oResponse.status_code}")
        print(oResponse.text)

        if oResponse.status_code != 200:
            print(f"[STARTUP] Error updating processor {sZipFileName}: {oResponse.status_code} - {oResponse.text}")
        else:
            sResponseJson = oResponse.json()
            bOk = sResponseJson.get("boolValue", False)
            sProcessObjId = sResponseJson.get("stringValue", "")

            if bOk:
                print(f"[STARTUP] Processor {sZipFileName} updated successfully waiting for deployment.")
                waitProcess(sProcessObjId)
                print(f"[STARTUP] Processor {sZipFileName} updated successfully.")
            else:
                print(f"[STARTUP] Error updating processor {sZipFileName}: {sProcessObjId}")
    else:
        print(f"[STARTUP] Processor {sZipFileName} is up to date, no need to update.")


def _upload_workflow(sApiUrl, sXmlPath, sXmlFileName):
    sName = os.path.splitext(sXmlFileName)[0]

    aoQueryParams = {
        "workspace": "",
        "name": sName,
        "description": "",
        "public": True,
    }

    aoHeaders = {
        "x-session-token": "",
    }

    with open(sXmlPath, "rb") as oXmlFile:
        aoFiles = {
            "file": (sXmlFileName, oXmlFile, "application/xml"),
        }
        oResponse = requests.post(
            sApiUrl,
            headers=aoHeaders,
            params=aoQueryParams,
            files=aoFiles,
            timeout=120,
        )

    print(f"[STARTUP] Upload status for {sXmlFileName}: {oResponse.status_code}")
    print(f"[STARTUP] {oResponse.text}")


def _update_workflow(sBaseUrl, sXmlPath, sXmlFileName, dLastUpdate, sWorkflowId):

    dLastXmlFileUpdate = os.path.getmtime(sXmlPath)*1000.0

    if dLastXmlFileUpdate > dLastUpdate:
        print(f"[STARTUP] Workflow {sXmlFileName} is outdated, updating it.")

        sUrl = sBaseUrl + "workflows/updatefile"

        aoHeaders = {
            "x-session-token": "",
        }

        aoQueryParams = {
            "workflowid": sWorkflowId
        }        

        with open(sXmlPath, "rb") as oXmlFile:
            aoFiles = {
                "file": (sXmlFileName, oXmlFile, "application/xml"),
            }

            oResponse = requests.post(
                sUrl,
                headers=aoHeaders,
                params=aoQueryParams,
                files=aoFiles,
                timeout=120,
            )

        print(f"[STARTUP] Update status for {sXmlFileName}: {oResponse.status_code}")
        print(f"[STARTUP] {oResponse.text}")
    else:
        print(f"[STARTUP] Workflow {sXmlFileName} is up to date, no need to update.")


def _wait_for_server(sBaseUrl, iMaxWaitSeconds=180, iPollSeconds=3):
    sHelloUrl = sBaseUrl.rstrip("/") + "/wasdi/hello"
    iDeadline = time.time() + iMaxWaitSeconds

    print("[STARTUP] Waiting for WASDI to start")

    while time.time() < iDeadline:
        try:
            oResponse = requests.get(sHelloUrl, timeout=5)
            if oResponse.status_code == 200:
                print("[STARTUP] WASDI is ready.")
                return
            print("[STARTUP] WASDI not ready yet. ")
        except requests.RequestException as oEx:
            print("[STARTUP] WASDI not reachable yet. ")

        time.sleep(iPollSeconds)

    raise RuntimeError("[STARTUP] Timed out waiting for WASDI readiness")
    
def run():
    print("[STARTUP] Welcome to MiniWasdi 1.0")

    # Read the config file path from the env
    sConfigFile = _getEnvironmentVariable("WASDI_CONFIG_FILE")

    # Assign a default config file path if not set
    if (sConfigFile is None):
        sConfigFile = "/etc/wasdi/wasdiConfig.json"

    print("[STARTUP] Using config file: " + sConfigFile)

    # Initialize the base Url
    sBaseUrl = "http://127.0.0.1:8080/wasdiwebserver/rest/"

    # Read the config file
    with open(sConfigFile) as oJsonFile:
        aoConfig = json.load(oJsonFile)
        # Update the base Url if specified in the config
        sBaseUrl = aoConfig.get("baseUrl", sBaseUrl)
    
    # Ensure the base Url ends with a slash
    if not sBaseUrl.endswith("/"):
        sBaseUrl = sBaseUrl + "/"
    
    # Wait for the mini server to be ready before proceeding
    _wait_for_server(sBaseUrl)

    sCleanHistory = _getEnvironmentVariable("WASDI_CLEAR_HISTORY")

    if sCleanHistory is not None:
        if sCleanHistory.lower() == "true" or sCleanHistory == "1":
            print("[STARTUP] WASDI_CLEAR_HISTORY is set to true, cleaning the history.")
            sCleanQueueUrl = sBaseUrl + "admin/cleanProcessesQueue"
            try:
                oResponse = requests.delete(sCleanQueueUrl, timeout=30)
                if oResponse.status_code == 200:
                    print("[STARTUP] Created processes cleaned successfully.")
                else:
                    print(f"[STARTUP] Error cleaning created processes: {oResponse.status_code} - {oResponse.text}")
            except Exception as oEx:
                print(f"[STARTUP] Exception while cleaning created processes: {str(oEx)}")

            sCleanPastProcessesUrl = sBaseUrl + "admin/cleanOldProcesses"
            try:
                oResponse = requests.delete(sCleanPastProcessesUrl, timeout=30)
                if oResponse.status_code == 200:
                    print("[STARTUP] History cleaned successfully.")
                else:
                    print(f"[STARTUP] Error cleaning history: {oResponse.status_code} - {oResponse.text}")
            except Exception as oEx:
                print(f"[STARTUP] Exception while cleaning history: {str(oEx)}")

            print("[STARTUP] History cleaned, continuing with the startup.")
        else:
            print("[STARTUP] WASDI_CLEAR_HISTORY is set to not known value (" + sCleanHistory + "), not cleaning the history.")
    else:
        print("[STARTUP] WASDI_CLEAR_HISTORY is not set, not cleaning the history.")

    # Get the  base path of WASDI
    sBasePath = aoConfig["paths"]["downloadRootPath"]
    if not sBasePath.endswith("/"):
        sBasePath = sBasePath + "/"

    # Init the library
    wasdi.setUser("user")
    wasdi.setSessionId("********")
    wasdi.setBaseUrl(sBaseUrl.rstrip("/"))
    wasdi.setBasePath(sBasePath)
    wasdi.setIsOnServer(True)
    wasdi.setVerbose(False)
    wasdi.init()

    # Base Path of the apps and workflows to be deployed
    sInstallPath = sBasePath + "install/"
    sNewProcessorPath = sInstallPath + "processors"

    # Base Path of the apps already deployed on the server
    sApplicationsPath = sBasePath + "processors/"

    # Array of the names of the processors to be deployed
    asProcessors = []

    # Read all the zip files in the new processors path
    if os.path.exists(sNewProcessorPath) and os.path.isdir(sNewProcessorPath):
        asProcessors = os.listdir(sNewProcessorPath)

    # Url of the API to upload the processors
    sNewProcessorUrl = sBaseUrl + "processors/uploadprocessor"

    # For all the processors to be deployed
    for sProcessor in asProcessors:

        # Check if the processor is a zip file
        sProcessorPath = os.path.join(sNewProcessorPath, sProcessor)

        if os.path.isfile(sProcessorPath):
            if (sProcessor.upper().endswith(".ZIP")):

                print("[STARTUP] Found processor: " + sProcessor)

                # Get the name from the zip
                sName = os.path.splitext(sProcessor)[0].lower()

                # If it is already deployed we should find the folder and the venv.
                sExistingProcessorPath = os.path.join(sApplicationsPath, sName)
                bAlreadyDeployed = False
                if os.path.exists(sExistingProcessorPath):
                    sVenv = os.path.join(sExistingProcessorPath, "venv")
                    if os.path.exists(sVenv):
                        bAlreadyDeployed = True

                # Do we need to deploy?
                if not bAlreadyDeployed:
                    # Let's upload the processor
                    _upload_processor(sNewProcessorUrl, sProcessorPath, sProcessor)
                else:
                    # No it is already deployed: let's check if we need to update it
                    _update_processor(sBaseUrl, sProcessorPath, sProcessor)

    # Path of the new workflows to be deployed
    sNewWorkflowsPath = sInstallPath + "workflows"

    # List of the workflows to be deployed
    asWorkflows = []

    if os.path.exists(sNewWorkflowsPath) and os.path.isdir(sNewWorkflowsPath):
        asWorkflows = os.listdir(sNewWorkflowsPath)

    # Url of the API to upload the workflows
    sNewWorkflowUrl = sBaseUrl + "workflows/uploadfile"

    # List of the workflows already deployed on the server, to avoid duplicates
    aoExistingWorkflows = []

    try:
        # Get the existing workflows to avoid duplicates
        aoExistingWorkflows = wasdi.getWorkflows()
    except Exception as oEx:
        print("[STARTUP] Error fetching existing workflows, will not check for duplicates. Error: " + str(oEx))

    # For all the workflows to be deployed
    for sWorkflow in asWorkflows:
        # Check if the workflow is a xml file
        sWorkflowPath = os.path.join(sNewWorkflowsPath, sWorkflow)
        if os.path.isfile(sWorkflowPath):
            if (sWorkflow.upper().endswith(".XML")):

                bAlreadyDeployed = False

                # Check if the workflow is already deployed, if we have the list of existing workflows
                if aoExistingWorkflows is not None:
                    for oExistingWorkflow in aoExistingWorkflows:
                        if oExistingWorkflow.get("name", "").lower() == os.path.splitext(sWorkflow)[0].lower():
                            bAlreadyDeployed = True
                            break
                
                if bAlreadyDeployed:
                    _update_workflow(sBaseUrl, sWorkflowPath, sWorkflow, oExistingWorkflow.get("lastUpdate", 0), oExistingWorkflow.get("workflowId", ""))
                else:
                    # If it is not already deployed, let's upload it
                    print("[STARTUP] Upload workflow: " + sWorkflow)
                    _upload_workflow(sNewWorkflowUrl, sWorkflowPath, sWorkflow)

    # Now the start up is done: read what the user wants to start and start it
    sApplicationToStart = _getEnvironmentVariable("WASDI_RUN_APPLICATION")
    sParams = _getEnvironmentVariable("WASDI_PARAMS")
    sWorkspace = _getEnvironmentVariable("WASDI_WORKSPACE")

    # Sanity check the params, if not set use empty params
    if sParams is None:
        sParams = ""
    
    if sParams == "":
        sParams = "{}"
    
    # Object version of the params
    aoParams = {}

    try:
        aoParams = json.loads(sParams)
    except Exception as oEx:
        print("[STARTUP] Error parsing WASDI_PARAMS, using empty params. Error: " + str(oEx))
    
    print("[STARTUP] WASDI_PARAMS value: " + sParams)

    bUseWorkspaceId = False
    # Sanity check the workspace, if not set use empty workspace
    if sWorkspace is None:
        sWorkspace = ""
        sWorkspaceId = _getEnvironmentVariable("WASDI_WORKSPACE_ID")
        if sWorkspaceId is not None and sWorkspaceId != "":
            bUseWorkspaceId = True


    aoWorkspaces = wasdi.getWorkspaces()
    
    if sWorkspace == "":
        sWorkspace = "Default Workspace"

    bCreate = True

    if aoWorkspaces is not None:
        for oWorkspace in aoWorkspaces:
            if bUseWorkspaceId:
                if oWorkspace.get("workspaceId", "") == sWorkspaceId:
                    bCreate = False
                    sWorkspace = oWorkspace.get("workspaceName", "")
                    break
            else:
                if oWorkspace.get("workspaceName", "") == sWorkspace:
                    bCreate = False
                    break

    if bCreate:                
        # We need a new workspace
        sWsId = wasdi.createWorkspace(sWorkspace)
        wasdi.setActiveWorkspaceId(sWsId)
    else:
        wasdi.openWorkspace(sWorkspace)

    if sApplicationToStart is not None:
        sProcId =  wasdi.executeProcessor(sApplicationToStart, aoParams)
        print("[STARTUP] Started Application with Id = " + sProcId)
        waitProcess(sProcId)
    else:
        print("[STARTUP] No application specified to start, startup complete.")


if __name__ == '__main__':
    run()
