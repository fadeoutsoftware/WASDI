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


def executeProcessor(parameters, processId): 
    
    # First of all be sure to be in the right path
    dir_path = os.path.dirname(os.path.realpath(__file__))
    os.chdir(dir_path)
    
    print("wasdi.executeProcessor: processor folder set")
    
    
    #Try to get the user
    try:
        sUser = parameters['user']
        wasdi.setUser(sUser)
        print("wasdi.executeProcessor User available in params. Got " + sUser)
    except:
        print('wasdi.executeProcessor user not available in parameters.')
        
    #Try to get the password
    try:
        sPassword = parameters['password']
        wasdi.setPassword(sPassword)
        print("wasdi.executeProcessor Pw available in params")
    except:
        print('wasdi.executeProcessor password not available in parameters.')
        
    #Try to get the session id
    try:
        sSessionId = parameters['sessionid']
        wasdi.setSessionId(sSessionId)
        print("wasdi.executeProcessor Session available in params " + sSessionId)
    except:
        print('wasdi.executeProcessor Session not available in parameters.')        
    
    #Try to set the proc id
    try:
        wasdi.setProcId(processId)
        print("wasdi.executeProcessor set Proc Id " + processId)
    except:
        print('wasdi.executeProcessor Proc Id not available')
        
    #Try to get the workspace id
    try:
        sWorkspaceId = parameters['workspaceid']
        wasdi.openWorkspaceById(sWorkspaceId)
        print("wasdi.executeProcessor Workspace Id available in params " + sWorkspaceId)
    except:
        print('wasdi.executeProcessor Workspace Id not available in parameters.')        
    
    #Init Wasdi
    print("wasdi.executeProcessor: init waspy lib")
    wasdi.setIsOnServer(True)
    wasdi.setDownloadActive(False)
    
    if wasdi.init() == False:
        print("wasdi.executeProcessor: init FAILED")
        
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
        
        if (sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR'):
            wasdi.wasdiLog("wasdi.executeProcessor Process finished. Forcing status to " + sForceStatus)
            wasdi.updateProcessStatus(processId, sForceStatus, 100)
            
        
    return

if __name__ == '__main__':
    
    aoParameters = {}
    processId = ""
    
    if (len(sys.argv)>=2):
        sEncodedParams = sys.argv[1]
        sDecodedParams = urllib.parse.unquote(sEncodedParams)
        aoParameters = json.loads(sDecodedParams)
        
    if (len(sys.argv)>=3):
        processId = sys.argv[2]
    
    executeProcessor(aoParameters, processId)
