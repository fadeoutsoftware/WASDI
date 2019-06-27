import wasdi


def run(parameters, processId):
    wasdi.wasdiLog('Let\'s search some images')
    aoImages = wasdi.searchEOImages("S1", "2018-09-01", "2018-09-02", 44, 11, 43, 12)
    wasdi.wasdiLog('Found ' + str(len(aoImages)))


def WasdiHelp():
    sHelp = "Wasdi Big Dive Test"
    return sHelp