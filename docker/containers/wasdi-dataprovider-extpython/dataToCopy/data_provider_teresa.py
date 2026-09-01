import sys
import json
import os
import re
import logging
import time
from datetime import datetime
from pystac_client import Client
from data_provider_utils import DataProviderUtils
from pathlib import Path


s_sDataProviderName = 'TERESA_SUP'
s_sESAStacAPI = "https://eoresults.esa.int/stac/"



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

    iResultCount = 0

    # read the parameters
    aoFilters = aoInputQuery.get("filters")

    if not aoFilters:
        logging.warning(f'executeCount: filters not found')
        sys.exit(1)

    sDataset = aoFilters.get("dataset")
    sBasin = aoFilters.get("basin").replace("_", "")
    sResolution = aoFilters.get("resolution")

    sCollection = sDataset
    if sDataset not in ['TRWSI', 'SWSI']:
        sCollection = "ET_GWU_BWU"

    if not (sDataset and sBasin and sResolution):
        logging.warning(f"executeCount. Missing some parameters")
        sys.exit(1)

    oStacClient = Client.open(s_sESAStacAPI)
    oSearchResult = oStacClient.search(collections=[f'TERESA_{sCollection}'])
    sExpectedItemIdStart = f"{sDataset}-{sBasin}-{sResolution}-"
    logging.debug("executeCount: expected item id " + sExpectedItemIdStart)
    
    for oItem in oSearchResult.items():
        if oItem.id.startswith(sExpectedItemIdStart):
            iResultCount = 1

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
        
    # read the parameters
    aoFilters = aoInputQuery.get("filters")

    if not aoFilters:
        logging.warning(f'executeAndRetrieve: filters not found')
        sys.exit(1)

    sDataset = aoFilters.get("dataset")
    sBasin = aoFilters.get("basin").replace("_", "")
    sResolution = aoFilters.get("resolution")

    sCollection = sDataset
    if sDataset not in ['TRWSI', 'SWSI']:
        sCollection = "ET_GWU_BWU"

    if not (sDataset and sBasin and sResolution):
        logging.warning(f"execeuteAndRetrieve. Missing some parameters")
        sys.exit(1)

    oStacClient = Client.open(s_sESAStacAPI)
    oSearchResult = oStacClient.search(collections=[f'TERESA_{sCollection}'])
    sExpectedItemIdStart = f"{sDataset}-{sBasin}-{sResolution}-"
    logging.debug("execeuteAndRetrieve. Expected item id " + sExpectedItemIdStart)
    aoReturnList = []
    
    for oItem in oSearchResult.items():
        if oItem.id.startswith(sExpectedItemIdStart):
            sHref = oItem.assets["PRODUCT"].href
            sDownloadLink = "https://eoresults.esa.int" + sHref
            oResult = {}
            sFileSize = oItem.assets["PRODUCT"].extra_fields["file:size"]
            sDate = oItem.properties["datetime"]
            oResult["title"] = sHref.split("/")[-1]         # attenzione! Title deve essere il file name, che e' diverso dall'od. Il file name lo ricavo dal ref. TODO: cambiare
            oResult["id"] = oItem.id
            oResult["link"] = sDownloadLink
            oResult["summary"] = f'Date: {sDate}, Instrument: {sDataset}, Mode: {sBasin}, Satellite: TERESA_SUP, Size:{sFileSize}'
            oResult["provider"] = s_sDataProviderName
            oResult["platform"] = "TERESA_SUP"
            aoReturnList.append(oResult)
            break

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

    oDownloadFileViewModel = DataProviderUtils.getDownloadFileViewModel(sInputFilePath)
    
    if oDownloadFileViewModel is None:
        logging.warning(f"executeDownloadFile: Impossible to read the Download File View Model")
        sys.exit(1)
        
    sTargetFolder = aoInputParameters.get("downloadDirectory", "")
    sTargetFileName = aoInputParameters.get("downloadFileName", "")
    sUrl = oDownloadFileViewModel.url
    sDownloadedFilePath = str(Path(sTargetFolder) / sTargetFileName)

    bDownloaded = False
    if sUrl.startswith("https://"):
        bDownloaded = DataProviderUtils.downloadFile(sUrl, sDownloadedFilePath)

    if not bDownloaded:
            sDownloadedFilePath = ""

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

    sFileName = ""
    if sUrl:
        sFileName = sUrl.split("/")[-1]

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
