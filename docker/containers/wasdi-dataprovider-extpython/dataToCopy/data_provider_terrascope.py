import sys
import json
import os
import re
import logging
import time
from datetime import datetime

from data_provider_utils import DataProviderUtils

from datetime import datetime

from terracatalogueclient import Catalogue, ProductFile
from terracatalogueclient.config import CatalogueConfig, CatalogueEnvironment

s_sDataProviderName = 'TERRASCOPE'

oConfig = CatalogueConfig.from_environment(CatalogueEnvironment.CGLS)

def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""

def getBoundingBoxGeometry(sWest, sNorth, sEast, sSouth):
    """
    Format the bounding box according to the terrascope API format
    :param sWest: string representing the west
    :param sNorth: string representing the north
    :param sEast: string representing the east
    :param sSouth: string representing the south
    :return: a list of floats representing the bounding box
    """
    if sWest is None or sNorth is None or sEast is None or sSouth is None:
        logging.debug("getBoundingBoxGeometry: no bounding box specified")
        return None
    return [float(sWest), float(sSouth), float(sEast), float(sNorth)]


def getProductsCount(sCollection, oStartDate, oEndDate, afBBox):
    """
    Get from the data provider the count of products matching the search parameters
    :param sCollection: name of the collection
    :param oStartDate: the datetime object representing the start date
    :param oEndDate: the datetime object representing the end date
    :param afBBox: the list of floats representing the bounding box
    :return: the number of products matching the search parameters
    """
    oParameters = {}

    if oStartDate is not None and oEndDate is not None:
        oParameters['start'] = oStartDate
        oParameters['end'] = oEndDate

    if afBBox is not None:
        oParameters['bbox'] = afBBox

    try:
        oCatalogue = Catalogue(oConfig)
        return oCatalogue.get_product_count(sCollection, **oParameters)

    except Exception as oEx:
        logging.warning(f"getProductsCount: exception using terrascope APIs {oEx}")
    return None

def executeCount(sInputFilePath, sOutputFilePath):
    """
    Count the number of results matching the WASDI query specified in the input file, and writes the result
    on the output file
    :param sInputFilePath: the path of the file containing the WASDI query
    :param sOutputFilePath: the path of the file where the count will be written
    :return:
    """
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeCount: input file not found')
        sys.exit(1)

    oQueryViewModel = DataProviderUtils.getQueryViewModel(sInputFilePath)

    if oQueryViewModel is None:
        logging.warning(f'executeCount: input file {sInputFilePath} is None')
        sys.exit(1)

    iResultCount = -1

    try:
        sCollection = oQueryViewModel.productType
        sStartDate = oQueryViewModel.startFromDate
        sEndDate = oQueryViewModel.endToDate
        sNorth = oQueryViewModel.north
        sWest = oQueryViewModel.west
        sSouth = oQueryViewModel.south
        sEast = oQueryViewModel.east

        if sNorth is None or sSouth is None or sEast is None or sWest is None:
            logging.debug("executeCount: some of the coordinates are None, using standard coordinates instead")
            sNorth = 90
            sWest = -180
            sSouth = -90
            sEast = 180

        if sCollection is None or sStartDate is None or sEndDate is None:
            logging.error("executeCount: collection name, start or end date not specified.")
        else:
            oStartDate = datetime.strptime(sStartDate, "%Y-%m-%dT%H:%M:%S.%fZ")
            oEndDate = datetime.strptime(sEndDate, "%Y-%m-%dT%H:%M:%S.%fZ")
            afGeometry = getBoundingBoxGeometry(sWest, sNorth, sEast, sSouth)

            iResultCount = getProductsCount(sCollection, oStartDate, oEndDate, afGeometry)
    except Exception as oEx:
        logging.error(f'executeCount: error reading the input file: {sInputFilePath}, {oEx}')

    aoReturnObject = {"count": iResultCount}

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnObject, oFile)
    except Exception as oEx:
        logging.warning(f'executeCount: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)

def getProducts(sCollection, oStartDate, oEndDate, afBBox, iLimit, iOffset):
    """
    Get from the data provider the list of products matching the search parameters
    :param sCollection: name of the collection
    :param oStartDate: the datetime object representing the start date
    :param oEndDate: the datetime object representing the end date
    :param afBBox: the list of floats representing the bounding box
    :return: the list of products matching the search parameters
    """
    oParameters = {}

    if oStartDate is not None and oEndDate is not None:
        oParameters['start'] = oStartDate
        oParameters['end'] = oEndDate

    if afBBox is not None:
        oParameters['bbox'] = afBBox

    if iLimit > 0 and iOffset > 0:
        oParameters['limit'] = iLimit
        oParameters['startIndex'] = iOffset

    try:
        oCatalogue = Catalogue(oConfig)
        return oCatalogue.get_products(sCollection, **oParameters)

    except Exception as oEx:
        logging.warning(f"getProductsCount: exception using terrascope APIs {oEx}")

    return None

def executeAndRetrieve(sInputFilePath, sOutputFilePath):
    """
    Search for the products matching the WASDI query specified in the input file, and writes the list of results
    on the output file
    :param sInputFilePath: the path of the file containing the WASDI query
    :param sOutputFilePath: the path of the file where the list of results will be written
    :return:
    """

    if not os.path.isfile(sInputFilePath):
        logging.warning('executeAndRetrieve: input file not found')
        sys.exit(1)

    aoInputQuery = None

    try:
        oQueryViewModel = DataProviderUtils.getQueryViewModel(sInputFilePath)

        sCollection = oQueryViewModel.productType
        sStartDate = oQueryViewModel.startFromDate
        sEndDate = oQueryViewModel.endToDate
        sNorth = oQueryViewModel.north
        sWest = oQueryViewModel.west
        sSouth = oQueryViewModel.south
        sEast = oQueryViewModel.east
        iLimit = oQueryViewModel.limit
        iOffset = oQueryViewModel.offset + 1  # offset in terrascope starts from 1

        if sNorth is None or sSouth is None or sEast is None or sWest is None:
            logging.debug("executeCount: some of the coordinates are None, using standard coordinates instead")
            sNorth = 90
            sWest = -180
            sSouth = -90
            sEast = 180

        if sCollection is None or sStartDate is None or sEndDate is None:
            logging.error("executeAndRetrieve: collection name, start or end date not specified.")
        else:
            oStartDate = datetime.strptime(sStartDate, "%Y-%m-%dT%H:%M:%S.%fZ")
            oEndDate = datetime.strptime(sEndDate, "%Y-%m-%dT%H:%M:%S.%fZ")
            afGeometry = getBoundingBoxGeometry(sWest, sNorth, sEast, sSouth)

            aoProducts = getProducts(sCollection, oStartDate, oEndDate, afGeometry, iLimit, iOffset)
            aoWasdiSearchResults = []
            if aoProducts is not None:
                for oProduct in aoProducts:
                    aoWasdiSearchResults.append(createResultViewModel(oProduct))

            with open(sOutputFilePath, 'w') as oFile:
                json.dump(aoWasdiSearchResults, oFile)
                return

    except Exception as oEx:
        logging.error(f'executeAndRetrieve: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(1)

def createResultViewModel(oProduct):

    sWasdiCoordinatesFormat = ""
    if oProduct.bbox is not None and len(oProduct.bbox) == 4:
        fWest = oProduct.bbox[0]
        fSouth = oProduct.bbox[1]
        fEast = oProduct.bbox[2]
        fNorth = oProduct.bbox[3]

        sWasdiCoordinatesFormat = \
            f"POLYGON(({fWest} {fSouth}, {fWest} {fNorth}, {fEast} {fNorth}, {fEast} {fSouth}, {fWest} {fSouth}))"

    oWasdiProperties = {}
    oProductProperties = oProduct.properties
    if oProductProperties is not None:
        oWasdiProperties = flattenDict(oProductProperties)

    sTitle = ""
    sLink = ""
    if 'links.data[0].href' in oWasdiProperties:
        sLink = oWasdiProperties.get('links.data[0].href', "")
        sTitle = sLink.split('/')[-1]
    else:
        sTitle = oProduct.id


    asSummaryParts = []
    if 'acquisitionInformation[0].acquisitionParameters.beginningDateTime' in oWasdiProperties:
        asSummaryParts.append("Date: " + oWasdiProperties.get('acquisitionInformation[0].acquisitionParameters.beginningDateTime'))

    asSummaryParts.append("Mode: " + oWasdiProperties.get('title'))
    asSummaryParts.append("Instrument: " + oWasdiProperties.get('title'))

    if 'links.data[0].length' in oWasdiProperties:
        asSummaryParts.append("Size: " + str(oWasdiProperties.get('links.data[0].length')) + "B")

    return {
        'id': oProduct.id,
        'title': sTitle,
        'footprint': sWasdiCoordinatesFormat,
        'link': sLink,
        'summary': ", ".join(asSummaryParts),
        'properties': oWasdiProperties
    }

def flattenDict(oDictionary, sParentKey = '', sSeparator = '.'):
    """
    Given the dictionary of properties of a product, as returned by the search on the data provider, flatten
    the struction of that dictionary, whose values could be strings, lists, or other dictionaries
    :param oDictionary: the dictionary of properties of a product
    :return: a dictionary with a flat structure, where each value is a primitive type
    """
    oFlatDictionary = {}
    for sKey, oValue in oDictionary.items():
        sNewKey = f"{sParentKey}{sSeparator}{sKey}" if sParentKey else sKey
        if isinstance(oValue, dict):
            oFlatDictionary.update(flattenDict(oValue, sNewKey, sSeparator=sSeparator))
        elif isinstance(oValue, list):
            for i, oItem in enumerate(oValue):
                if isinstance(oItem, dict):
                    oFlatDictionary.update(flattenDict(oItem, f"{sNewKey}[{i}]", sSeparator=sSeparator))
                else:
                    oFlatDictionary[f"{sNewKey}[{i}]"] = oItem
        else:
            oFlatDictionary[sNewKey] = oValue
    return oFlatDictionary

def executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    """
    Execute the download of a FCOVER product, given its link
    :param sInputFilePath: path to the input file containing the info for the download
    :param sOutputFilePath: path to the output file where to write the result of the opeation
    :param sWasdiConfigFilePath: path to the WASDI config fi;e
    :return:
    """
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeDownloadFile: input file not found')
        return sys.exit(1)

    oDownloadFileViewModel = DataProviderUtils.getDownloadFileViewModel(sInputFilePath)

    if oDownloadFileViewModel is None:
        logging.warning(f"executeDownloadFile: Impossible to read the Download File View Model")
        sys.exit(1)

    sTargetFolder = oDownloadFileViewModel.downloadDirectory
    sTargetFileName = oDownloadFileViewModel.downloadFileName
    iMaxRetry = oDownloadFileViewModel.maxRetry
    sUrl = oDownloadFileViewModel.url

    sResPath = ""
    sFullProductPath = sTargetFolder
    if not sFullProductPath.endswith("/"):
        sFullProductPath += "/"

    if stringIsNullOrEmpty(sUrl):
        logging.warning(f"executeDownloadFile: resource URL is null or empty. Impossible to proceed")
        sys.exit(1)

    if stringIsNullOrEmpty(sTargetFolder):
        logging.warning(f"executeDownloadFile: target folder path for download is null or empty")
        sys.exit(1)

    if stringIsNullOrEmpty(sTargetFileName):
        logging.warning(f"executeDownloadFile: file name not specified")
        sys.exit(1)

    try:
        oCatalogue = Catalogue(oConfig)
        oProduct = ProductFile(sUrl, None)
        oCatalogue.download_file(oProduct, sTargetFolder)

        if os.path.exists(sFullProductPath):
            sResPath = sFullProductPath

    except Exception as oE:
        logging.error(f"executeDownloadFile: something went wrong while downloading the file")
        # if somehow the file was downloaded, even partially, just delete the file
        if os.path.exists(sFullProductPath):
            os.remove(sFullProductPath)
        sys.exit(1)

    oRes = {
        'outputFile': sResPath
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

    sUrl = aoInputQuery.get("url", "")

    sFileName = ""

    try:
        sFileName = os.path.basename(sUrl)
    except Exception as oEx:
        logging.warning(f"getFileName: error getting file name from url, {oEx}")

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
