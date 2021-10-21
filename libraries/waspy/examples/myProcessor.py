import wasdi


def run():
    wasdi.wasdiLog("Welcome to your first WASPY processor :-)")
    wasdi.executeSen2Cor("S2A_MSIL1C_20211011T145731_N0301_R039_T19NFD_20211011T182720")
    # sNome = wasdi.getParameter("name")
    # wasdi.wasdiLog("Hello " + str(sNome))
    #
    # bIsThere = wasdi._fileOnNode("medium_map.tif")
    # wasdi.wasdiLog("Result " + str(bIsThere))
    #
    # aoProducts = wasdi.getProductsByActiveWorkspace()
    #
    # if aoProducts is not None:
    #     wasdi.wasdiLog("Found " + str(len(aoProducts)))
    #
    # sOutputName = "myOutputFile.tif"
    # if sOutputName not in aoProducts:
    #     wasdi.wasdiLog("About to execute SNAP workflow")
    #     sWorkFlow = "snap_workflow_name"
    #     wasdi.executeWorkflow([aoProducts[len(aoProducts) - 1]], [sOutputName], sWorkFlow)
    # else:
    #     wasdi.wasdiLog("File exists, no need to run workflow")
    #
    # sPath = wasdi.getFullProductPath(sOutputName)
    #
    # # more code here...




    wasdi.wasdiLog("Done :-)")


if __name__ == '__main__':
    wasdi.init("./config.json")
    run()
