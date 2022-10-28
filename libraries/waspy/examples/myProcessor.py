import wasdi


def run():
    wasdi.setVerbose(True)
        
    wasdi.hello()
    
    aoImage = wasdi.searchEOImages("S1", sFileName="S1A_IW_SLC__1SDV_20221021T054200_20221021T054228_045536_057197_35F8")
    wasdi.wasdiLog(str(aoImage))

    wasdi.wasdiLog("Welcome to your first WASPY processor :-)")
    sNome = wasdi.getParameter("name")
    wasdi.wasdiLog("Hello " + str(sNome))
    
    bIsThere = wasdi._fileOnNode("medium_map.tif")
    wasdi.wasdiLog("Result " + str(bIsThere))

    aoProducts = wasdi.getProductsByActiveWorkspace()

    if aoProducts is not None:
        wasdi.wasdiLog("Found " + str(len(aoProducts)))

    sOutputName = "myOutputFile.tif"
    if sOutputName not in aoProducts:
        wasdi.wasdiLog("About to execute SNAP workflow")
        sWorkFlow = "snap_workflow_name"
        wasdi.executeWorkflow([aoProducts[len(aoProducts) - 1]], [sOutputName], sWorkFlow)
    else:
        wasdi.wasdiLog("File exists, no need to run workflow")

    sPath = wasdi.getFullProductPath(sOutputName)

    # more code here...

    wasdi.wasdiLog("Done :-)")


if __name__ == '__main__':
    #wasdi.init("./config.json")
    wasdi.init()
    run()
