import logging
import sys
import os
import json

from datetime import datetime

from terracatalogueclient import Catalogue
from terracatalogueclient.config import CatalogueConfig, CatalogueEnvironment

s_sDataProviderName = 'TERRASCOPE'

# get the configuration of the terrascope client
oConfig = CatalogueConfig.from_environment(CatalogueEnvironment.CGLS)

def executeCount(sInputFilePath, sOutputFilePath):
    """
    Count the number of results matching the WASDI query specified in the input file, and writes the result
    on the output file
    :param sInputFilePath: the path of the file containing the WASDI query
    :param sOutputFilePath: the path of the file where the count will be written
    :return:
    """
    iResultsCount = -1
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)

            sCollection = aoInputQuery.get('productType')
            sStartDate = aoInputQuery.get('startFromDate')
            sEndDate = aoInputQuery.get('endToDate')
            sNorth = aoInputQuery.get('north', None)
            sWest = aoInputQuery.get('west', None)
            sSouth = aoInputQuery.get('south', None)
            sEast = aoInputQuery.get('east', None)

            if sCollection is None or sStartDate is None or sEndDate is None:
                logging.error("executeCount: collection name, start or end date not specified.")
            else:
                oStartDate = datetime.strptime(sStartDate, "%Y-%m-%dT%H:%M:%S.%fZ")
                oEndDate = datetime.strptime(sEndDate, "%Y-%m-%dT%H:%M:%S.%fZ")
                afGeometry = getBoundingBoxGeometry(sWest, sNorth, sEast, sSouth)

                iResultsCount = getProductsCount(sCollection, oStartDate, oEndDate, afGeometry)

    except Exception as oEx:
        logging.error(f'executeCount: error reading the input file: {sInputFilePath}, {oEx}')

    with open(sOutputFilePath, 'w') as oFile:
        json.dump({"count": iResultsCount}, oFile)
        return


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
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)

            sCollection = aoInputQuery.get('productType')
            sStartDate = aoInputQuery.get('startFromDate')
            sEndDate = aoInputQuery.get('endToDate')
            sNorth = aoInputQuery.get('north', None)
            sWest = aoInputQuery.get('west', None)
            sSouth = aoInputQuery.get('south', None)
            sEast = aoInputQuery.get('east', None)
            iLimit = aoInputQuery.get('limit', -1)
            iOffset = aoInputQuery.get('offset', -1) + 1 # offset in terrascope starts from 1

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

    return

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
    
        # first argument asArgs[0] is the name of the file - we are not interested in it
        sOperation = asArgs[1]
        sInputFile = asArgs[2]
        sOutputFile = asArgs[3]
        sWasdiConfigFile = asArgs[4]

        # TODO: at the end, I can probably remove those logs
        logging.debug('__main__: operation ' + sOperation)
        logging.debug('__main__: input file ' + sInputFile)
        logging.debug('__main__: output file: ' + sOutputFile)
        logging.debug('__main__: wasdi config path: ' + sWasdiConfigFile)

        if not os.path.isfile(sInputFile):
            logging.warning('__main__: input file not found. Script will exit')
            sys.exit(1)

        if not os.path.isfile(sOutputFile):
            logging.warning('__main__: output file not found. Script will exit')
            sys.exit(1)

        if sOperation == "0":
            logging.debug('__main__: chosen operation is EXECUTE AND RETRIEVE')
            executeAndRetrieve(sInputFile, sOutputFile)
        elif sOperation == "1":
            logging.debug('__main__: chosen operation is EXECUTE COUNT')
            executeCount(sInputFile, sOutputFile)
        elif sOperation == "2":
            logging.debug('__main__: chosen operation is DOWNLOAD. Not implemented here')
        else:
            logging.debug('__main__: unknown operation. Script will exit')
            sys.exit(1)

    except Exception as oEx:
        logging.error('__main__: Exception ' + str(oEx))

    sys.exit(0)
