import wasdi
from datetime import datetime
from datetime import timedelta
from __builtin__ import int

def run(parameters, processId):
    wasdi.wasdiLog('eDrift Archive Generator start')
    
    
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
    sMosaicXStep = parameters.get('MOSAICXSTEP', "-1.0")
    sMosaicYStep = parameters.get('MOSAICYSTEP', "-1.0")
    
    sHSBA_DEPTH_IN = parameters.get('HSBA_DEPTH_IN', "-1")
    sASHMAN_COEFF = parameters.get('ASHMAN_COEFF', "2.4")
    sMIN_PIXNB_BIMODD = parameters.get('MIN_PIXNB_BIMODD', "10000")
    sBLOBS_SIZE = parameters.get('BLOBS_SIZE', "150")
    
    sNODATAVALUE = parameters.get('NODATAVALUE', "-9999")
    sINPUTIGNOREVALUE = parameters.get('INPUTIGNOREVALUE', "0")
    sFLOODNODATAVALUE = parameters.get('FLOODNODATAVALUE', "255")
    sFLOODINPUTIGNOREVALUE = parameters.get('FLOODINPUTIGNOREVALUE', "-9999")
    sMOSAICNODATAVALUE = parameters.get('MOSAICNODATAVALUE', "255")
    sMOSAICINPUTIGNOREVALUE = parameters.get('MOSAICINPUTIGNOREVALUE', "255")
    sAPPLYMAPCONVERSION = parameters.get('APPLYMAPCONVERSION', "1")
            
    wasdi.wasdiLog('eDrift Archive Generator: Start Archive Generation from ' + sArchiveStartDate + ' to ' + sArchiveEndDate)
    
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
    
    iStep = 100.0/float(iDays+1)
    iProgress = 0.0
    
    
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
        aoChainParams["HSBA_DEPTH_IN"] = sHSBA_DEPTH_IN
        aoChainParams["ASHMAN_COEFF"] = sASHMAN_COEFF
        aoChainParams["MIN_PIXNB_BIMODD"] = sMIN_PIXNB_BIMODD
        aoChainParams["BLOBS_SIZE"] = sBLOBS_SIZE
        
        
        aoChainParams['NODATAVALUE'] = sNODATAVALUE
        aoChainParams['INPUTIGNOREVALUE'] = sINPUTIGNOREVALUE
        aoChainParams['FLOODNODATAVALUE'] = sFLOODNODATAVALUE
        aoChainParams['FLOODINPUTIGNOREVALUE'] = sFLOODINPUTIGNOREVALUE
        aoChainParams['MOSAICNODATAVALUE'] = sMOSAICNODATAVALUE
        aoChainParams['MOSAICINPUTIGNOREVALUE'] = sMOSAICINPUTIGNOREVALUE
        aoChainParams['APPLYMAPCONVERSION'] = sAPPLYMAPCONVERSION
        
        
        sProcessId = wasdi.executeProcessor("mosaic_tile", aoChainParams)
        
        wasdi.wasdiLog('Chain started waiting for end')
        
        wasdi.waitProcess(sProcessId) 
        
        iProgress = iProgress+iStep
        wasdi.updateProgressPerc(int(iProgress))
        wasdi.wasdiLog('Chain done for day ' + sDate)
        
        oActualDate += oTimeDelta
    
    wasdi.updateProcessStatus(processId, "DONE", 100)
    
    
def WasdiHelp():
    sHelp = "eDRIFT Archive Generator Utility<br>"
    sHelp += "Tool to generate an eDRIFT flood Archive<br>"
    sHelp += "Takes as input all the params of the chain, a start date and an end date<br>"
    sHelp += "Triggers the chain for each day in the interval with the supplied params<br>"
    sHelp += "Parameters are:<br>"
    sHelp += 'ARCHIVE_START_DATE: bbox in lat lon string:"ULLat,ULLon,LRLat,LRLon<br>'
    sHelp += "ARCHIVE_END_DATE: xstep,ystep<br>"    
    sHelp += "DELETE: flag delete for the chain<br>"
    sHelp += "SIMULATE: flag simulate for the chain<br>"
    sHelp += "BBOX: bbox for the chain<br>"
    sHelp += "ORBITS: orbits for the chain<br>"
    sHelp += "GRIDSTEP: grid step for the chain<br>" 
    sHelp += "LASTDAYS: last days for the chain<br>"
    sHelp += "PREPROCWORKFLOW: pre proc workflow for the chain<br>"
    sHelp += "MOSAICBASENAME: mosaic base name for the chain<br>"
    sHelp += "MOSAICXSTEP: mosaic x step for the chain (-1 => Native)<br>"
    sHelp += "MOSAICYSTEP: mosaic y step for the chain (-1 => Native)<br>"
    sHelp += "HSBA_DEPTH_IN: hbsa Depth in for the flood detection<br>"
    sHelp += "ASHMAN_COEFF: ashman coefficient for the flood detection<br>"
    sHelp += "MIN_PIXNB_BIMODD: min number of pixel for bimodal mask  for the flood detection<br>"
    sHelp += "BLOBS_SIZE: blob size for the flood detection<br>"
    
    sHelp += 'NODATAVALUE: no data value to use for sentinel mosaic<br>'
    sHelp += 'INPUTIGNOREVALUE: input no data value for the sentinel mosaic and tiles<br>'
    sHelp += 'FLOODNODATAVALUE: output no data value for flood maps<br>'
    sHelp += 'FLOODINPUTIGNOREVALUE: input no data value for the flood maps<br>'
    sHelp += 'MOSAICNODATAVALUE: no data value for flood mosaic<br>'
    sHelp += 'MOSAICINPUTIGNOREVALUE: input no data value for flood mosaic<br>'
    sHelp += 'APPLYMAPCONVERSION: 1 to apply seadrif palette. 0 to leave values<br>'
    
    return sHelp