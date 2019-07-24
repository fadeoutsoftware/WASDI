import wasdi
import gdal
import numpy
import json
import gdalconst


def run(parameters, processId):
    wasdi.wasdiLog("Convert Old Flood Maps")
        
    sFile = ""
    floodMapDS = gdal.Open(sFile)
    floodMapData = floodMapDS.ReadAsArray()
    numpy.nan_to_num(floodMapData, copy=False)
    floodMapData[floodMapData==1] = 3
    floodMapData[floodMapData==0] = 1
    
    
def WasdiHelp():