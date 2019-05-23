import wasdi
from datetime import datetime
from datetime import timedelta

def run(parameters, processId):
    wasdi.wasdiLog('eDrift Check Images tool start')
    
    
    sArchiveStartDate = parameters.get('ARCHIVE_START_DATE', "2019-05-01")
    sArchiveEndDate = parameters.get('ARCHIVE_END_DATE', "2019-05-03")
    sDelete = parameters.get('DELETE', "1")
    sSimulate = parameters.get('SIMULATE', "0")
    sBbox = parameters.get('BBOX', "29.0,92.0,10.0,100.0")
    sOrbits = parameters.get('ORBITS', "33,41,62,70,77,99,106,135,143,172")
    sGridStep = parameters.get('GRIDSTEP', "1,1")
    sLastDays = parameters.get('LASTDAYS', "0")
    sPreprocWorkflow = parameters.get('PREPROCWORKFLOW', "LISTSinglePreproc2")
    sMosaicBaseName = parameters.get('MOSAICBASENAME', "MY")
    sMosaicXStep = parameters.get('MOSAICXSTEP', "0.00018")
    sMosaicYStep = parameters.get('MOSAICYSTEP', "0.00018")
    
    wasdi.wasdiLog('eDrift Archive Generator: Start Archive from ' + sArchiveStartDate + ' to ' + sArchiveEndDate)
    
    oStartDay = datetime.today()
    oEndDay = datetime.today()
            
    try:
        oStartDay = datetime.strptime(sArchiveStartDate , '%Y-%m-%d')
    except:
        wasdi.wasdiLog('Start Date not valid, assuming today')
        
    try:
        oEndDay = datetime.strptime(sArchiveEndDate, '%Y-%m-%d')
    except:
        wasdi.wasdiLog('End Date not valid, assuming today')

    oActualDate = oStartDay
    oTimeDelta = timedelta(days=1)
    oTimeDelta2 = oEndDay-oStartDay
    
    iDays = oTimeDelta2.days;
    
    if (iDays == 0):
        iDays = 1
    
    iStep = 100/iDays
    iProgress = 0
    
    while oActualDate <= oEndDay:
        
        sDate = oActualDate.strftime("%Y-%m-%d")
        
        wasdi.wasdiLog('-------------STARTING CHAIN FOR DATE ' + sDate)
        
        aoChainParams = {}
        aoChainParams["ENDDATE"] = sDate
        aoChainParams["DELETE"] = sDelete
        aoChainParams["SIMULATE"] = sSimulate
        aoChainParams["BBOX"] = sBbox
        aoChainParams["ORBITS"] = sOrbits
        aoChainParams["GRIDSTEP"] = sGridStep 
        aoChainParams["LASTDAYS"] = sLastDays
        aoChainParams["PREPROCWORKFLOW"] = sPreprocWorkflow
        aoChainParams["MOSAICBASENAME"] = sMosaicBaseName
        aoChainParams["MOSAICXSTEP"] = sMosaicXStep
        aoChainParams["MOSAICYSTEP"] = sMosaicYStep
        
        #TODO wasdi.executeProcess
        sProcessId = ""
        
        wasdi.wasdiLog('Chain started waiting for end')
        
        wasdi.waitProcess(sProcessId) 
        
        iProgress = iProgress+iStep
        wasdi.updateProgressPerc(iProgress)
        wasdi.wasdiLog('Chain done for day ' + sDate)
        
        oActualDate += oTimeDelta
    
    wasdi.updateProcessStatus(processId, "eDrift Archive Generator DONE", 100)
    
    

            
    
def WasdiHelp():
    sHelp = "eDRIFT Archive Generator Utility\n"
    sHelp += "Tool to generate an eDRIFT flood Archive\n"
    sHelp += "Takes as input all the params of the chain, a start date and an end date\n"
    sHelp += "Triggers the chain for each day in the interval with the supplied params\n"
    sHelp += "Parameters are:\n"
    sHelp += 'ARCHIVE_START_DATE: bbox in lat lon string:"ULLat,ULLon,LRLat,LRLon'
    sHelp += "ARCHIVE_END_DATE: xstep,ystep\n"    
    sHelp += "DELETE: flag delete for the chain"
    sHelp += "SIMULATE: flag simulate for the chain"
    sHelp += "BBOX: bbox for the chain"
    sHelp += "ORBITS: orbits for the chain"
    sHelp += "GRIDSTEP: grid step for the chain" 
    sHelp += "LASTDAYS: last days for the chain"
    sHelp += "PREPROCWORKFLOW: pre proc workflow for the chain"
    sHelp += "MOSAICBASENAME: mosaic base name for the chain"
    sHelp += "MOSAICXSTEP: mosaic x step for the chain"
    sHelp += "MOSAICYSTEP: mosaic y step for the chain"

    return sHelp