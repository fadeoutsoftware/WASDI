from datetime import datetime
from datetime import timedelta
import wasdi

def run(parameters, processId):
    
    wasdi.wasdiLog('eDrift Check Days start')
    
    #Per Date
    sEndDate = parameters.get('enddate', "0")
    #Purge all the date
    sStartDate = parameters.get('startdate', "0")
    #Sentinel Images
    sBBox = parameters.get('bbox', "0")
    #Masks
    sOrbits = parameters.get('orbits', "0")
    
    wasdi.wasdiLog('START DATE ' + sStartDate)
    wasdi.wasdiLog('END DATE ' + sEndDate)
    wasdi.wasdiLog('BBOX ' + sBBox)
    wasdi.wasdiLog('ORBITS ' + sOrbits)
    wasdi.wasdiLog('ACTIVE WORKSPACE = ' + wasdi.getActiveWorkspaceId())
        
    adBBOX = sBBox.split(",")
    aiOrbits = sOrbits.split(",")
    
    oStartDay = datetime.today()
    oEndDay = datetime.today()
    
    if (len(adBBOX)!=4):
        wasdi.wasdiLog("Boundig Box Must have 4 values: UpLeft Lat, UpLeft Lon, LowRight Lat, Low Right Lon")
        wasdi.updateProcessStatus(processId, "ERROR", 100)
        return
    
    if (len(aiOrbits)<1):
        wasdi.wasdiLog("Set one or more orbits (comma separated)")
        wasdi.updateProcessStatus(processId, "ERROR", 100)
        return    
        
    try:
        oStartDay = datetime.strptime(sStartDate , '%Y-%m-%d')
    except:
        wasdi.wasdiLog('Start Date not valid, assuming today')
        
    try:
        oEndDay = datetime.strptime(sEndDate, '%Y-%m-%d')
    except:
        wasdi.wasdiLog('End Date not valid, assuming today')

    oActualDate = oStartDay
    oTimeDelta = timedelta(days=1)
    oTimeDelta2 = oEndDay-oStartDay
    
    iDays = oTimeDelta2.days;
    
    if (iDays == 0):
        iDays=1
    
    if (iDays < 0):
        oSwapDate = oStartDay
        oStartDay = oEndDay
        oEndDay = oSwapDate
        iDays = -1*iDays        
    
    iUpdateStep = 100/iDays;
    iUpdateOrbitStep = iUpdateStep/len(aiOrbits)
    
    iProgress = 0;
    
    aoResults = []
    
    while oActualDate <= oEndDay:
        
        sDate = oActualDate.strftime("%Y-%m-%d")
        
        wasdi.wasdiLog('-------------DATE ' + sDate)
        
        aoDay = {}
        aoDay["date"] = sDate
        aoDay["orbits"] = []
        
        for iOrbit in aiOrbits:
            wasdi.wasdiLog('Searching for Orbit ' + str(iOrbit))
            aoReturnList = wasdi.searchEOImages("S1", sDate, sDate, adBBOX[0], adBBOX[1], adBBOX[2], adBBOX[3], "GRD", iOrbit, None, None)
            wasdi.wasdiLog('Found ' + str(len(aoReturnList)))
            if (len(aoReturnList)>0):
                oDayOrbit = {}
                
                oDayOrbit["orbit"] = iOrbit
                oDayOrbit["images"] = len(aoReturnList)
                aoDay["orbits"].append(oDayOrbit)
                
            iProgress += iUpdateOrbitStep
            if (iProgress>100):
                iProgress = 100
            wasdi.updateProcessStatus(processId, "RUNNING", iProgress)
        
        aoResults.append(aoDay)
        
        oActualDate += oTimeDelta
        
    wasdi.setProcessPayload(processId, aoResults)
    wasdi.wasdiLog('eDRIFT Check Images Tool done')
                
def WasdiHelp():
    sHelp = "eDRIFT Check S1 Images Utility\n"
    sHelp += "Tool to check availalbe S1 images of defined orbits in the defined bbox day by day \n"
    sHelp += "Parameters are:\n"
    sHelp += "startdate: YYYY-MM-DD start searching day\n"
    sHelp += "enddate: YYYY-MM-DD end searching day\n"
    sHelp += 'bbox: bbox in lat lon string:"ULLat,ULLon,LRLat,LRLon"\n'
    sHelp += 'orbits: oribts comma separated: "orbit1,orbit2,orbit3"\n'
    
    
    return sHelp