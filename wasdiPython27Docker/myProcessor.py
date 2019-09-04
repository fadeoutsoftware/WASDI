import wasdi


def run(): 
    wasdi.wasdiLog('Start myProcessor Python 2.7')
    sName = wasdi.getParameter("NAME")
    wasdi.wasdiLog('Hello ' + sName)

    
def WasdiHelp():
    sHelp = "Help myProcessor Python 2.7"
    return sHelp


if __name__ == '__main__':
    wasdi.init('./config.json')
    run()
