'''
Created on 24 Nov 2022

@author: p.campanella
'''

import wasdi
import os
import sys
import urllib.parse
import json
import traceback


def executeProcessor():

    sProcessId = wasdi.getProcId()
    # First of all be sure to be in the right path
    #sProcessorDirPath = os.path.dirname(os.path.realpath(__file__))
    #os.chdir(sProcessorDirPath)

    if sProcessId is None:
        sProcessId = "test"

    if sProcessId == "":
        sProcessId = ""

    print("[" + sProcessId+ "] wasdi.executeProcessor: processor folder set")
    
    #Init Wasdi
    print("[" + sProcessId+ "] wasdi.executeProcessor: init waspy lib")
    wasdi.setIsOnServer(False)
    wasdi.setIsOnExternalServer(True)
    wasdi.setDownloadActive(True)
    wasdi.setUploadActive(True)

    sForceStatus = 'ERROR'

    #Run the processor
    try:
        import myProcessor        
        wasdi.wasdiLog("wasdi.executeProcessor RUN " + sProcessId)
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
        sFinalStatus = wasdi.getProcessStatus(sProcessId)
        
        if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
            wasdi.wasdiLog("wasdi.executeProcessor Process finished. Forcing status to " + sForceStatus)
            wasdi.updateProcessStatus(sProcessId, sForceStatus, 100)

    return

if __name__ == '__main__':
    sWriteDir = os.environ['WASDI_OUTPUT']

    print("eoepcaProcessorExecutor: Write Dir " + sWriteDir)

    if not sWriteDir.endswith("/"):
        sWriteDir = sWriteDir + "/"

    wasdi.setBasePath(sWriteDir)

    sConfigPath = sWriteDir + "config.json"
    print("eoepcaProcessorExecutor: config path: " + sConfigPath)

    wasdi.init(sConfigPath)

    print("called init, staring processor")
    executeProcessor()
