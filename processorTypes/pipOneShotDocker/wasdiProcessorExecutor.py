'''
Created on 24 Nov 2022

@author: p.campanella
'''

import wasdi
import os
import urllib.parse
import json
import traceback

m_sProcId = ""

def log(sLogString):
	print("[" + m_sProcId + "] wasdiProcessorExecutor PIP One Shot Engine v.2.1.3 - " + sLogString)

def executeProcessor(bIsOnServer = True):
    # We need the proc id for logs
    global m_sProcId
    
    #Init Wasdi
    log("wasdi.executeProcessor: init waspy lib")
    wasdi.setIsOnServer(bIsOnServer)
    wasdi.setIsOnExternalServer(not bIsOnServer)
    wasdi.setDownloadActive(True)
    wasdi.setUploadActive(True)

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

if __name__ == '__main__':
    try:
        # Read the process Id
        sProcId = os.environ['WASDI_PROCESS_WORKSPACE_ID']

        if sProcId is not None:
            m_sProcId = sProcId
        else:
            m_sProcId = "N.A."

        log("Starting Proc Id " + m_sProcId)

        # Read the encoded parameters
        sEncodedParams = os.environ['WASDI_ONESHOT_ENCODED_PARAMS']

        if sEncodedParams is None:
            log("no params available")
            sEncodedParams = "%7B%7D"

        sDecodedParams = urllib.parse.unquote(sEncodedParams)
        aoParameters = json.loads(sDecodedParams)

        wasdi.setParametersDict(aoParameters)

        log("Added parameters")

        # Read the User
        sUserId = os.environ['WASDI_USER']

        if sUserId is None:
            log("User Id not available")

        # Read the Session Id
        sSessionId = os.environ['WASDI_SESSION_ID']

        if sSessionId is None:
            log("Session Id not available")

        # Read the Workspace Id
        sWorkspaceId = os.environ['WASDI_WORKSPACE_ID']

        if sWorkspaceId is None:
            log("Workspace Id not available")

        # The lib will read all the data from env
        if not wasdi.init():
            log("There was an error in init, we try to execute but it will likely not work")
            wasdi.wasdiLog("There was an error in init, we try to execute but it will likely not work")
        else:
            log("Init done, staring processor")
            wasdi.wasdiLog("Init done, staring processor")


        # Read the on-server flag
        bIsOnServer = True
        sOnServer = os.environ['WASDI_ONESHOT_ON_SERVER']

        if sOnServer is not None:
            if sOnServer == "0" or sOnServer.lower() == "false":
                bIsOnServer = False

        executeProcessor(bIsOnServer)

    except Exception as oEx:
        wasdi.wasdiLog("pipOneShot: EXCEPTION")
        wasdi.wasdiLog(repr(oEx))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog("wasdi.executeProcessor generic EXCEPTION")