import wasdi

def run():
    wasdi.wasdiLog('Start myProcesssor Python 3.8')
    sName = wasdi.getParameter('NAME')
    #wasdi.importProduct(sFileUrl, sBoundingBox, asProduct)
    wasdi.wasdiLog('Hello ' + sName)

def WasdiHelp():
    sHelp = 'Help myProcesssor Python 3.8'
    return sHelp

if __name__ == '__main__':
    wasdi.init('./config.json')
    run()
