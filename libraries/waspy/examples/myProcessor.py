import wasdi


def run():
    wasdi.setVerbose(True)
    bVerbosity = wasdi.getVerbose()
    
    wasdi.hello()

    wasdi.wasdiLog(wasdi.getUser())
    
    wasdi.setSubPid("3e3253fc-52e7-47f6-ad6f-a5d9439691ff", 11)

    # params
    aoParams = wasdi.getParametersDict()
    aoParams['hello'] = 'darling'
    wasdi.setParametersDict(aoParams)
    wasdi.addParameter('next', 'please')
    asKeys = ['hello', 'no']
    for sKey in asKeys:
        sValue = wasdi.getParameter(sKey, oDefault='escape')

    wasdi.wasdiLog("Welcome to your first WASPY processor :-)")
    sNome = wasdi.getParameter("name")
    wasdi.wasdiLog("Hello " + str(sNome))

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
