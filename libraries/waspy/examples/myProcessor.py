import wasdi


def run(parameters, processId):
    wasdi.wasdiLog('Here\'s the list of your workspaces:')
    aoWorkspaces = wasdi.getWorkspaces()
    wasdi.wasdiLog(aoWorkspaces)
    wasdi.wasdiLog('The ID of currently selected workspace is:')
    sActiveWorkspace = wasdi.getActiveWorkspaceId()
    wasdi.wasdiLog(sActiveWorkspace)

    wasdi.wasdiLog('Let\'s search some images...')
    aoImages = wasdi.searchEOImages("S1", "2018-09-01", "2018-09-02", 44, 11, 43, 12, sProductType='GRD')
    wasdi.wasdiLog('Found ' + str(len(aoImages)) + ' images')

    wasdi.wasdiLog('Download the first one passing the dictionary...')
    sImportWithDict = wasdi.importProduct(None, None, aoImages[0])
    wasdi.wasdiLog('Import with dict returned: ' + sImportWithDict)

    wasdi.wasdiLog('Now, these are the products in your workspace: ')
    asProducts = wasdi.getProductsByActiveWorkspace()
    wasdi.wasdiLog(asProducts)

    wasdi.wasdiLog('Let\'s run a workflow on the first image to rectify its georeference...')
    sStatus = wasdi.executeWorkflow([asProducts[0]], ['lovelyOutput'], 'LISTSinglePreproc')
    if sStatus == 'DONE':
        wasdi.wasdiLog('The product is now in your workspace, look at it on the website')

    wasdi.wasdiLog('It\'s over!')

def WasdiHelp():
    sHelp = "Wasdi Tutorial"
    return sHelp