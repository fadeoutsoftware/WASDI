import cdsapi
import sys
import json
import os
import re
import logging

s_sDataProviderName = 'CDS'


def executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    if not os.path.isfile(sInputFilePath) or not os.path.isfile(sOutputFilePath):
        logging.warning('executeDownloadFile: input or output file not found')
        sys.exit(1)
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputParameters = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(1)

    if aoInputParameters is None:
        logging.warning(f'executeDownloadFile: there was an error reading the input file: {sWasdiConfigFilePath}')
        sys.exit(1)

    if stringIsNullOrEmpty(sWasdiConfigFilePath):
        logging.warning(f'executeDownloadFile: data provider configuration is None or empty string: {sWasdiConfigFilePath}')
        sys.exit(1)

    try:
        with open(sWasdiConfigFilePath) as oWasdiConfigJsonFile:
            aoDataProviderConfig = json.load(oWasdiConfigJsonFile)
    except Exception as oEx:
        logging.warning(f'executeDownloadFile: error reading the wasdiConfig file: {sWasdiConfigFilePath}, {oEx}')
        sys.exit(1)

    if aoDataProviderConfig is None:
        logging.warning(f'executeDownloadFile: wasdiConfig file is None: {sWasdiConfigFilePath}')
        sys.exit(1)

    # find the configuration for the data provider
    asCDSDataProviderConfig = None

    aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders', [])
    for oProvider in aoWasdiDataProviders:
        if oProvider['name'] == s_sDataProviderName:
            asCDSDataProviderConfig = oProvider
            break

    if asCDSDataProviderConfig is None:
        logging.warning(f"executeDownloadFile: no configuration found for {s_sDataProviderName}. Impossible to continue")
        sys.exit(1)

    sAdapterConfigFilePath = asCDSDataProviderConfig.get('adapterConfig', None)

    if stringIsNullOrEmpty(sAdapterConfigFilePath):
        logging.warning('executeDownloadFile: path to the adapter config file is null or empty')
        sys.exit(1)

    if not os.path.isfile(sAdapterConfigFilePath):
        logging.warning(f"executeDownloadFile: adapter config file not found at path {sAdapterConfigFilePath}")
        sys.exit(1)

    logging.debug(f"executeDownloadFile: adapter config file found at {sAdapterConfigFilePath}")

    try:
        with open(sAdapterConfigFilePath) as oJsonFile:
            aoCDSConfig = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error reading the adapter config file file: {sAdapterConfigFilePath}, {oEx}')
        return sys.exit(1)

    if aoCDSConfig is None:
        logging.error(f'executeDownloadFile: adapter config file file is null. Impossible to read the parameters')
        return sys.exit(1)

    sAPIUrl = aoCDSConfig.get('apiUrl', None)
    sAPIKey = aoCDSConfig.get('key', None)

    if sAPIUrl is None or sAPIKey is None:
        logging.error(f'executeDownloadFile: API url or API key not present in the adapter config file')
        return sys.exit(1)

    logging.debug(f'executeDownloadFile:api url and key correctly read from the adapter config file')

    sDownloadedFilePath = executeDownloadFromCDS(aoInputParameters, sAPIUrl, sAPIKey)

    oRes = {
        'outputFile': sDownloadedFilePath
    }

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
            logging.debug(f"executeDownloadFile: path to the downloaded file written in the output file")
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""

def executeDownloadFromCDS(aoInputParameters, sAPIUrl, sAPIKey):
    sDownloadedFilePath = ""

    if aoInputParameters is None or sAPIUrl is None or sAPIKey is None:
        logging.debug(f"executeDownloadFromCDS: input parameters, API url or API kes not specified")
        return sDownloadedFilePath

    sDownloadDirectory = aoInputParameters.get('downloadDirectory', None)
    sDownloadFileName = aoInputParameters.get('downloadFileName')

    if stringIsNullOrEmpty(sDownloadDirectory) or stringIsNullOrEmpty(sDownloadFileName):
        logging.warning("executeDownloadFromCDS: output directory or file not specified.")
        return sDownloadedFilePath

    if not os.path.exists(sDownloadDirectory):
        logging.warning("executeDownloadFromCDS: output directory does not exist")
        return sDownloadedFilePath

    aoCDSPayload = aoInputParameters.get('cds_payload', None)

    if aoCDSPayload is None:
        logging.error(f"executeDownloadFromCDS: cds_payload parameter not found in the input file")
        return sDownloadedFilePath

    sDataset = aoInputParameters.get('dataset', None)

    if sDataset is None:
        logging.error(f"executeDownloadFromCDS: dataset parameter not found in the input file")
        return sDownloadedFilePath

    logging.debug(f"executeDownloadFromCDS: sending the request to the CDS client")

    try:
        logging.debug(f"executeDownloadFromCDS: sending the request to the CDS client")
        oCDSClient = cdsapi.Client(url=sAPIUrl, key=sAPIKey)
        sDownloadedFilePath = oCDSClient.retrieve(sDataset, aoCDSPayload, os.path.join(sDownloadDirectory, sDownloadFileName))
        sDownloadedFilePath = os.path.normpath(sDownloadedFilePath)

    except Exception as oEx:
        logging.error(f"executeDownloadFromCDS: exception calling the CDS client {oEx}")

    logging.debug(f"executeDownloadFromCDS: file downloaded at {sDownloadedFilePath}")
    return sDownloadedFilePath


if __name__ == '__main__':
    logging.basicConfig(encoding='utf-8', format='[%(levelname)s] %(message)s', level=logging.DEBUG)

    sOperation = None
    sInputFile = None
    sOutputFile = None

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
        logging.debug('__main__: operation EXECUTE AND RETRIEVE not implemented. Script will exit')
        sys.exit(1)
    elif sOperation == "1":
        logging.debug('__main__: operation is EXECUTE COUNT not implemented. Script will exit')
        sys.exit(1)
    elif sOperation == "2":
        logging.debug('__main__: chosen operation is DOWNLOAD')
        executeDownloadFile(sInputFile, sOutputFile, sWasdiConfigFile)
    else:
        logging.debug('__main__: unknown operation. Script will exit')
        sys.exit(1)

    sys.exit(0)
