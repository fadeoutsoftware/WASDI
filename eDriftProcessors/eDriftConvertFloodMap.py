import wasdi
import os

def run(parameters, processId):
    
    wasdi.wasdiLog('eDrift Convert flood Map Images start')
    sFloodMap = parameters.get('floodMap', "")
    
    wasdi.wasdiLog('MAP ' + sFloodMap)
    asProducts = wasdi.getProductsByActiveWorkspace()
    
    for sProduct in asProducts:
        if (sProduct.endswith('_mask.tif')):
            wasdi.wasdiLog('DELETE ' + sProduct)
            wasdi.deleteProduct(sProduct)
        #if (sProduct.endswith('_preproc.tif')):
        #    wasdi.wasdiLog('DELETE ' + sProduct)
        #    wasdi.deleteProduct(sProduct)
    
def WasdiHelp():
    sHelp = "eDRIFT Delete Images Utility"
    
    return sHelp