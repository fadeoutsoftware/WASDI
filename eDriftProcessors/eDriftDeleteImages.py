import wasdi

def run(parameters, processId):
    
    wasdi.wasdiLog('eDrift Delete Tool start')
    
    #Per Date
    sEndDate = parameters.get('enddate', "0")
    #Purge all the date
    sPurge = parameters.get('purgedate', "0")
    #Sentinel Images
    sSentinel = parameters.get('sentinel', "0")
    #Masks
    sMasks = parameters.get('masks', "0")
    
    wasdi.wasdiLog('END DATE ' + sEndDate)
    wasdi.wasdiLog('PURGE DATE ' + sPurge)
    wasdi.wasdiLog('SENTINEL ' + sSentinel)
    wasdi.wasdiLog('MASKS ' + sMasks)
    
    wasdi.wasdiLog('ACTIVE WORKSPACE = ' + wasdi.getActiveWorkspaceId())
    
    #wasdi.wasdiLog('FLOODTILES ' + sMasks)
    asProducts = wasdi.getProductsByActiveWorkspace()
    
    iLen = len(asProducts)
    
    wasdi.wasdiLog('Products in the WORKSPACE = ' + str(iLen))
    
    for sProduct in asProducts:
        
        # Delete Masks?
        if (sMasks == "1"):
            if (sProduct.endswith('_mask.tif')):
                wasdi.wasdiLog('DELETE ' + sProduct)
                wasdi.deleteProduct(sProduct)
        
        # Per Day                
        if (sEndDate != "0"):
            
            #Only tiles, or all the day?
            sTiles = "_"
            
            if (sPurge == "1"):
                sTiles = ""
                        
            if (sEndDate + sTiles in sProduct):
                wasdi.wasdiLog('DELETE ' + sProduct)
                wasdi.deleteProduct(sProduct)
        
        #Sentinel Images
        if (sSentinel == "1"):
            if (sProduct.startswith("S1A_")):
                wasdi.wasdiLog('DELETE ' + sProduct)
                wasdi.deleteProduct(sProduct)
                
    wasdi.wasdiLog('eDRIFT Delete Tools done')
                
def WasdiHelp():
    sHelp = "eDRIFT Delete Images Utility\n"
    sHelp += "Tool to clean the Automatic Flood Chain\n"
    sHelp += "Parameters are:\n"
    sHelp += "enddate: YYYY-MM-DD of the day to clean. Default 0 => do not delete for date\n"
    sHelp += "purgedate: 0 or 1. If 0 clean only the tiles of the date. If 1 clean all the products of the day\n"
    sHelp += "masks: 0 or 1. Clean all the masks\n"
    sHelp += "sentinel: 0 or 1. Clean all the Sentinel Products and Preprocessed Products\n"
    
    
    return sHelp