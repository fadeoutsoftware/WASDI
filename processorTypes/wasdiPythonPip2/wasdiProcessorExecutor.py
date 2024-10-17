'''
Created on 14 Jun 2019

@author: p.campanella
'''

import wasdi
import os
import sys
import urllib.parse
import json
import traceback

m_sProcId = ""

def log(sLogString):
	print("[" + m_sProcId + "] wasdiProcessorExecutor PIP2 Engine v.2.1.3 - " + sLogString)

def executeProcessor(parameters, processId):

    global m_sProcId
    m_sProcId = processId

    # First of all be sure to be in the right path
    dir_path = os.path.dirname(os.path.realpath(__file__))
    os.chdir(dir_path)
    
    log("processor folder set")
    
    #Try to get the user
    try:
        sUser = parameters['user']
        wasdi.setUser(sUser)
        log("User available in params. Got " + sUser)
    except:
        log("user not available in parameters.")

    #Try to get the session id
    try:
        sSessionId = parameters['sessionid']
        wasdi.setSessionId(sSessionId)
        log("Session available in params " + sSessionId)
    except:
        log("Session not available in parameters.")
    
    #Try to set the proc id
    try:
        wasdi.setProcId(processId)
        log("Proc Id " + processId)
    except:
        log("Proc Id not available")

    sWorkspaceId = ""
    #Try to get the workspace id
    try:
        sWorkspaceId = parameters['workspaceid']
        wasdi.setActiveWorkspaceId(sWorkspaceId)
        log("Workspace Id available in params " + sWorkspaceId)
    except:
        log("Workspace Id not available in parameters.")
    
    
    #Try to get the base url
    try:
        sBaseUrl = parameters['baseurl']
        wasdi.setBaseUrl(sBaseUrl)
        log("Base Url in params " + sBaseUrl)
    except:
        log("Using default or ENV  base url")
    
    #Init Wasdi
    log("Init waspy lib")
    wasdi.setIsOnServer(True)
    wasdi.setDownloadActive(False)
    
    if not wasdi.init():
        log("init FAILED")

    wasdi.openWorkspaceById(sWorkspaceId)

    sForceStatus = 'ERROR'
    
    wasdi.setProcId(processId)
    wasdi.setParametersDict(parameters)
    
    #Run the processor
    try:
        import myProcessor        
        wasdi.wasdiLog("wasdi.executeProcessor RUN " + processId)
        myProcessor.run()
        wasdi.wasdiLog("wasdi.executeProcessor Done")
        
        sForceStatus = 'DONE'
        
    except Exception as oEx:
        wasdi.wasdiLog("wasdi.executeProcessor EXCEPTION")
        wasdi.wasdiLog(repr(oEx))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog("wasdi.executeProcessor generic EXCEPTION")
    finally:
        sFinalStatus = wasdi.getProcessStatus(processId)
        
        if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
            wasdi.wasdiLog("wasdi.executeProcessor Process finished. Forcing status to " + sForceStatus)
            wasdi.updateProcessStatus(processId, sForceStatus, 100)

    return

def handleAllExceptions(e_type, e_value, e_traceback):
    try:
        global m_sProcId

        if e_type is not None:
            print("wasdi.handleAllExceptions: " + str(e_type))
            print("wasdi.handleAllExceptions: " + str(e_value))

        if m_sProcId is not None:
            sForceStatus = "ERROR"
            sFinalStatus = wasdi.getProcessStatus(processId)

            if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
                wasdi.wasdiLog("wasdi.handleAllExceptions Process finished. Forcing status to " + sForceStatus)
                wasdi.updateProcessStatus(processId, sForceStatus, 100)
        else:
            print("handleAllExceptions: no proc id available.")
    except:
        print("handleAllExceptions: exception in exception. I surrender.")

if __name__ == '__main__':
    
    aoParameters = {}
    processId = ""
    
    if len(sys.argv)>=2:
        sEncodedParams = sys.argv[1]
        sDecodedParams = urllib.parse.unquote(sEncodedParams)
        aoParameters = json.loads(sDecodedParams)
        
    if len(sys.argv)>=3:
        processId = sys.argv[2]

    sys.excepthook = handleAllExceptions
    executeProcessor(aoParameters, processId)