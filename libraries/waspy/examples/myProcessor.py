import wasdi


def run():
    wasdi.wasdiLog("Hello ")
    sNome = wasdi.getParameter("NOME")
    wasdi.wasdiLog("Hello " + str(sNome))

    aoProducts = wasdi.getProductsByActiveWorkspace()

    if aoProducts is not None:
        wasdi.wasdiLog("Found " + str(len(aoProducts)))

    sOutputName = "myOutputFile.tif"
    if sOutputName not in aoProducts:
        wasdi.wasdiLog("About to execute workflow")
        sWorkFlow = "portu"
        wasdi.executeWorkflow([aoProducts[len(aoProducts) - 1]], [sOutputName], sWorkFlow)
    else:
        wasdi.wasdiLog("File exists, no need to run workflow")

    sPath = wasdi.getFullProductPath(sOutputName)

    # more code here...

    wasdi.wasdiLog("Done :-)")


if __name__ == '__main__':
    wasdi.init("./config.json")
