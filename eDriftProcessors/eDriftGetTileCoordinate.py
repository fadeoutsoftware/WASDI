import wasdi

def run(parameters, processId):
    wasdi.wasdiLog('eDrift Check Images tool start')
    
    sBBox = parameters.get('bbox', "29.0,92.0,10.0,100.0")
    sGridStep = parameters.get('gridstep',"1,1")
    sTileIndex = parameters.get('tileindex',"0,0")
    sReverse = parameters.get('reverse',"0")
    
    adBBOX = sBBox.split(",")
        
    if (len(adBBOX)!=4):
        wasdi.wasdiLog("Boundig Box Must have 4 values: UpLeft Lat, UpLeft Lon, LowRight Lat, Low Right Lon")
        wasdi.updateProcessStatus(processId, "ERROR", 100)
        return
    
    adGridStep = sGridStep.split(",")
    
    if (len(adGridStep) != 2):
        wasdi.wasdiLog("Grid Step Must have 2 values: LatStep,LonStep")
        wasdi.updateProcessStatus(processId, "ERROR", 100)
        return
    
    aiTileIndex = sTileIndex.split(",")
    
    if (len(aiTileIndex) != 2):
        wasdi.wasdiLog("Tile Index Must have 2 values: x,y")
        wasdi.updateProcessStatus(processId, "ERROR", 100)
        return
    
    sOutputJson = '{}'
    
    
    if (sReverse == "0"):
        wasdi.wasdiLog('Searching Geo Coordinates for tile index (x,y) [' + str(aiTileIndex[0]) + ',' + aiTileIndex[1]+']')
        
        dOutLat = float(adBBOX[2]) +float(aiTileIndex[0])*float(adGridStep[0]) + float(adGridStep[0]);
        
        dOutLon = float(adBBOX[1]) +float(aiTileIndex[1])*float(adGridStep[1]);
        
        wasdi.wasdiLog('Tile Upper Left Lat ' + str(dOutLat))
        wasdi.wasdiLog('Tile Upper Left Lon ' + str(dOutLon))
        
        sOutputJson = '{"lat":'+ str(dOutLat) + ',\n"lon":'+ str(dOutLon)+',\n"x":' + str(int(aiTileIndex[0]))+',\n"y":' + str(int(aiTileIndex[1]))+'\n}'
    else:
        wasdi.wasdiLog('Searching tile index for Geo Coordinates (lat,lon) [' + str(aiTileIndex[0]) + ',' + aiTileIndex[1]+']')
        iOutXIndex = ((float(aiTileIndex[0])-float(adBBOX[2]))/float(adGridStep[0]))-1
        iOutYIndex = ((float(aiTileIndex[1])-float(adBBOX[1]))/float(adGridStep[1]))
        wasdi.wasdiLog('Tile Upper Left Lat ' + str(iOutXIndex))
        wasdi.wasdiLog('Tile Upper Left Lon ' + str(iOutYIndex))
        
        sOutputJson = '{"lat":'+ str(aiTileIndex[0]) + ',\n"lon":'+ str(aiTileIndex[1])+',\n"x":' + str(int(iOutXIndex))+',\n"y":' + str(int(iOutYIndex))+'\n}'
        
    wasdi.wasdiLog('Save Payload: ' + sOutputJson)
    wasdi.setProcessPayload(processId, sOutputJson)
    wasdi.updateProcessStatus(processId, "DONE", 100)
    
    

            
    
def WasdiHelp():
    sHelp = "eDRIFT Get Tile Coordinate Utility\n"
    sHelp += "Tool to get lat lon coordinates of a tile from the tile matrix index\n"
    sHelp += "If reverse = 1 find the tile index starting from upper left lat,lon\n"
    sHelp += "Parameters are:\n"
    sHelp += 'bbox: bbox in lat lon string:"ULLat,ULLon,LRLat,LRLon'
    sHelp += "gridstep: xstep,ystep\n"
    sHelp += "tileindex: x,y (or lat,lon if reverse = 1\n"
    sHelp += 'reverse: "0" => from index to lat lon. "1" => from lat lon to index\n'
    
    
    return sHelp