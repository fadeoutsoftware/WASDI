import wasdi
import os

def run(parameters, processId):
    
    wasdi.wasdiLog('eDrift Delete Images start')
    sEndDate = parameters.get('enddate', "")
    sPurge = parameters.get('purge', "0")
    
    wasdi.wasdiLog('END DATE ' + sEndDate)
    asProducts = wasdi.getProductsByActiveWorkspace()
    
    for sProduct in asProducts:
        #if (sProduct.endswith('_mask.tif')):
        #    wasdi.wasdiLog('DELETE ' + sProduct)
        #    wasdi.deleteProduct(sProduct)
        #if (sProduct.endswith('_preproc.tif')):
        #    wasdi.wasdiLog('DELETE ' + sProduct)
        #    wasdi.deleteProduct(sProduct)
        
        if (sEndDate == "0"):
            return;
        if (sEndDate + "_" in sProduct):
            wasdi.wasdiLog('DELETE ' + sProduct)
            wasdi.deleteProduct(sProduct)
                
def WasdiHelp():
    sHelp = "eDRIFT Delete Images Utility"
    
    return sHelp