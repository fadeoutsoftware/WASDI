import sys
import json
import os
import re
import logging
import time
from datetime import datetime

s_sDataProviderName = ''

def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""

def executeCount(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeCount: input file not found')

    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeCount: error reading the input file: {sInputFilePath}, {oEx}')
        sys.exit(1)

    if aoInputQuery is None:
        logging.warning(f'executeCount: input file {sInputFilePath} is None')
        sys.exit(1)
    
    aoReturnObject = {}
    # TODO: Make the query and return the total count
    iResultCount = 0

    aoReturnObject["count"] = iResultCount

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnObject, oFile)
    except Exception as oEx:
        logging.warning(f'executeCount: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)

def executeAndRetrieve(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeAndRetrieve: input file not found')

    aoInputQuery = None
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeAndRetrieve: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(-1)

    if aoInputQuery is None:
        logging.warning(f'executeAndRetrieve: input file: {sInputFilePath} is None')
        sys.exit(1)
        
    # TODO: Make the query, get the result and convert to QueryResultViewModels
    aoReturnList = []

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnList, oFile)
    except Exception as oEx:
        logging.warning(f'executeAndRetrieve: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


def executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeDownloadFile: input file not found')

    aoInputParameters = None
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputParameters = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(-1)

    if aoInputParameters is None:
        logging.warning(f'executeDownloadFile: there was an error reading the input file: {sWasdiConfigFilePath}')
        sys.exit(1)

    if stringIsNullOrEmpty(sWasdiConfigFilePath):
        logging.warning(f'executeDownloadFile: data provider configuration is None or empty string: {sWasdiConfigFilePath}')
        sys.exit(1)

    aoDataProviderConfig = None
    try:
        with open(sWasdiConfigFilePath) as oWasdiConfigJsonFile:
            aoDataProviderConfig = json.load(oWasdiConfigJsonFile)
    except Exception as oEx:
        logging.warning(f'executeDownloadFile: error reading the wasdiConfig file: {sWasdiConfigFilePath}, {oEx}')
        sys.exit(1)

    if aoDataProviderConfig is None:
        logging.warning(f'executeDownloadFile:  wasdiConfig file is None: {sWasdiConfigFilePath}')
        
    sTargetFolder = aoInputParameters.get("downloadDirectory", "")
    sTargetFileName = aoInputParameters.get("downloadFileName", "")
    iMaxRetry = aoInputParameters.get("maxRetry", 1)

    # find the configuration for the data provider
    oDataProviderConfig = None

    aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders', [])
    for oProvider in aoWasdiDataProviders:
        if oProvider.get('name', "") == s_sDataProviderName:
            oDataProviderConfig = oProvider
            break

    if oDataProviderConfig is None:
        logging.warning(f"executeDownloadFile: no configuration found for {s_sDataProviderName}. Impossible to continue")
        sys.exit(1)


    # TODO: Download the file in sTargetFolder + sTargetFileName
    sDownloadedFilePath = sTargetFolder + sTargetFileName

    oRes = {
        'outputFile': sDownloadedFilePath
    }

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
            logging.debug(f"path to the downloaded file written in the output file")
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


def getFileName(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('getFileName: input file not found')

    aoInputQuery = None
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'getFileName: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(-1)

    if aoInputQuery is None:
        logging.warning(f'getFileName: input file: {sInputFilePath} is None')
        sys.exit(1)
        
    sUrl = aoInputQuery.get("url","")
    # TODO: Extract the file name from the Url
    sFileName = ""
    
    oRes = {
        'fileName': sFileName
    }    

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
    except Exception as oEx:
        logging.warning(f'getFileName: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


if __name__ == '__main__':
    logging.basicConfig(encoding='utf-8', format='[%(levelname)s] %(message)s', level=logging.DEBUG)

    sOperation = None
    sInputFile = None
    sOutputFile = None

    # let's read the arguments
    asArgs = sys.argv

    try:
        if asArgs is None or len(asArgs) < 5:
            logging.error("__main__: no arguments passed to the data provider")
            sys.exit(1)

        sOperation = asArgs[1]
        sInputFile = asArgs[2]
        sOutputFile = asArgs[3]
        sWasdiConfigFile = asArgs[4]

        # first argument asArgs[0] is the name of the file - we are not interested in it
        logging.debug('__main__: operation ' + sOperation)
        logging.debug('__main__: input file ' + sInputFile)
        logging.debug('__main__: output file: ' + sOutputFile)
        logging.debug('__main__: wasdi config path: ' + sWasdiConfigFile)

    except Exception as oE:
        logging.error('__main__: Exception ' + str(oE))
        sys.exit(1)
    
    if sOperation == "0":
        logging.debug('__main__: chosen operation is EXECUTE AND RETRIEVE')
        executeAndRetrieve(sInputFile, sOutputFile)
    elif sOperation == "1":
        logging.debug('__main__: chosen operation is EXECUTE COUNT')
        executeCount(sInputFile, sOutputFile)
    elif sOperation == "2":
        logging.debug('__main__: chosen operation is DOWNLOAD')
        executeDownloadFile(sInputFile, sOutputFile, sWasdiConfigFile)
    elif sOperation == "3":
        logging.debug('__main__: chosen operation is GET FILE NAME')
        getFileName(sInputFile, sOutputFile)
    else:
        logging.debug('__main__: unknown operation. Script will exit')
        sys.exit(1)

    sys.exit(0)
