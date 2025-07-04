import urllib.parse
import copernicusmarine
import sys
import json
import os
import re
import logging
import time
from datetime import datetime

s_sDataProviderName = 'COPERNICUSMARINE'
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

    oResult = searchOnCopernicusMarine(aoInputQuery)

    aoReturnObject = {}
    if oResult is None:
        aoReturnObject["count"] = -1
    else:
        aoReturnObject["count"] = 1

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

    oResult = searchOnCopernicusMarine(aoInputQuery)
    logging.debug(f'executeAndRetrieve: result {oResult}')

    aoReturnList = [oResult]

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
    asCMDataProviderConfig = None

    aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders', [])
    for oProvider in aoWasdiDataProviders:
        if oProvider.get('name', "") == s_sDataProviderName:
            asCMDataProviderConfig = oProvider
            break

    if asCMDataProviderConfig is None:
        logging.warning(f"executeDownloadFile: no configuration found for {s_sDataProviderName}. Impossible to continue")
        sys.exit(1)

    sUsername = asCMDataProviderConfig.get('user', None)
    sPassword = asCMDataProviderConfig.get('password', None)

    sDownloadedFilePath = executeDownloadFromCopernicusMarine(aoInputParameters, sUsername, sPassword)

    logging.debug(f"executeDownloadFile: path to the downloaded file {sDownloadedFilePath}")

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


def searchOnCopernicusMarine(oJsonQuery):
    """
    Implements the search for products on copernicus marine
    :param oJsonQuery: the object representing the WASDI query
    :return: the object representing the WASDI result view model
    """

    sDatasetId = oJsonQuery['productName']
    sProductId = oJsonQuery['productType']
    sVariables = oJsonQuery['sensorMode']

    # it looks like the toolbox does not need the "-TDS" suffix anymore, so we remove it
    if sProductId.endswith("-TDS"):
        sProductId = sProductId[0:len(sProductId) - 4]

    asCMMetadataFilters = []
    asCMMetadataFilters.append(sProductId)
    asCMMetadataFilters.append(sDatasetId)
    if sVariables is not None:
        asVariables = sVariables.split()
        if len(asVariables) > 0:
            asCMMetadataFilters.extend(asVariables)

    logging.debug(f"searchOnCopernicusMarine: Sending query with text filters {asCMMetadataFilters}")

    aoCatalogue = None
    try:
        aoCatalogue = copernicusmarine.describe(
            contains=asCMMetadataFilters,
            product_id=sProductId,
            dataset_id=sDatasetId,
            disable_progress_bar=True)
    except Exception as oEx:
        logging.error(f"searchOnCopernicusMarine: exception  retrieving the metadata from the provider: {oEx}")
        sys.exit(1)

    if aoCatalogue is None:
        logging.warning(f"searchOnCopernicusMarine: no catalogue found")
        sys.exit(1)

    if aoCatalogue.products is None or len(aoCatalogue.products) == 0:
        logging.warning(f"searchOnCopernicusMarine: the catalogue doesn't contain products")
        sys.exit(1)

    logging.warning(f"searchOnCopernicusMarine: found {len(aoCatalogue.products)} products")
    oResult = findFirstMatchingResults(oJsonQuery, aoCatalogue.products)
    logging.debug(f"searchOnCopernicusMarine: first matching result {oResult}")
    return oResult

def findFirstMatchingResults(oWasdiJsonQuery, aoCatalogue):
    f"""
    Starting from the list of results returned by the "describe" method of the CM toolbox, it looks for the first result
    that matches the WASDI search parameters
    :param oWasdiJsonQuery: the map representing the WASDI search parameters
    :param aoCatalogue: the array of objects representing the available products in the CM marine catalogue
    :return: the first result in {aoCatalogue} that matches the WASDI search parameters
    """
    # check the name of the product
    sWasdiProductId = oWasdiJsonQuery['productType']

    for aoProductMap in aoCatalogue:
        # it looks like the toolbox does not need the "-TDS" suffix anymore, so we remove it
        if sWasdiProductId.endswith("-TDS"):
            sWasdiProductId = sWasdiProductId[0:len(sWasdiProductId) - 4]

        sCMProductId = aoProductMap.product_id
        if sCMProductId != sWasdiProductId:
            continue

        # check the datasets
        aoDatasetsLists = aoProductMap.datasets

        for aoDatasetMap in aoDatasetsLists:
            sWasdiDatasetId = oWasdiJsonQuery['productName']
            sCMDatasetId = aoDatasetMap.dataset_id

            if sWasdiDatasetId != sCMDatasetId:
                continue

            logging.debug(
                f'findFirstMatchingResults: found dataset {sWasdiDatasetId}')
            aoVersionsList = aoDatasetMap.versions
            for oVersion in aoVersionsList:
                # TODO: should I return some default value if the coordinates or the depth is not specified?
                oMatchingServiceMap = getServiceMatchingVariables(oVersion, oWasdiJsonQuery)

                if oMatchingServiceMap is not None:
                    # if we find a matching service, we can then use the information in the wasdi query VM and in the
                    # matching service map to populate the result VM
                    return createResultViewModel(sCMProductId, sCMDatasetId, oWasdiJsonQuery, oMatchingServiceMap)


def getServiceMatchingVariables(oVersion, oWasdiJsonQuery):
                                # , asWasdiVariables, sNorth, sWest, sSouth, sEast, sMinDepth, sMaxDepth, sStartDate, sEndDate):
    """
    Get the first service storing the dataset and matching the search parameters in the WASDI query, i.e. the variables
    and the bounding box
    :param oVersion: the map representing one version of the dataset
    :param oWasdiJsonQuery:  the WASDI query map
    :return: the map representing the first service that was found matching all the search parameters in the WASDI query
    """
    oFirstMatchingService = None

    for oPart in oVersion.parts:
        for oServiceMap in oPart.services:
            sServiceTypeName = oServiceMap.service_name

            aoCMVariablesList = oServiceMap.variables

            if aoCMVariablesList is None:
                logging.debug(f"getServiceMatchingVariables: service '{sServiceTypeName}' has no variables")
                continue

            # let's check if the dataset available in that service contains all the variables
            # selected in the wasdi query
            bServiceContainsVariables = checkVariables(aoCMVariablesList, oWasdiJsonQuery)

            if bServiceContainsVariables:
                # the dataset provided by this service contains all the information selected in the WASDI query
                logging.debug(f"getServiceMatchingVariables: dataset in '{sServiceTypeName}' has all variables needed")
                oFirstMatchingService = oServiceMap
                break

    return oFirstMatchingService


def checkVariables(aoCMVariablesList, oWasdiJsonQuery):
    """
    Check if all the variables that were selected in the WASDI are present in the dataset
    :param aoCMVariablesList: list of CM objects representing the variables
    the bounding box selected in WASDI, False otherwise
    """
    sVariablesFromSensorMode = oWasdiJsonQuery['sensorMode']
    asWasdiVariables = sVariablesFromSensorMode.split(" ")

    logging.debug(f'checkVariables: variables received from wasdi {asWasdiVariables}')
    # array of booleans used to track the presence, in the dataset, of the variable selected in WASDI .
    # For a given element 'i', bIsVariableInCM[i] = True if the variable is present in the dataset

    sNorth = oWasdiJsonQuery.get('north', None)
    sWest = oWasdiJsonQuery.get('west', None)
    sSouth = oWasdiJsonQuery.get('south', None)
    sEast = oWasdiJsonQuery.get('east', None)

    if sNorth is None or sWest is None or sSouth is None or sEast is None:
        logging.warning(f'one of more points of the bounding box are not specified')
        return False

    # depth could miss from the query: that's ok
    sMinDepth = oWasdiJsonQuery.get('cloudCoverageFrom', None)
    sMaxDepth = oWasdiJsonQuery.get('cloudCoverageTo', None)

    sStartDate = oWasdiJsonQuery.get('startFromDate', None)
    sEndDate = oWasdiJsonQuery.get('endToDate', None)

    if sStartDate is None or sEndDate is None:
        logging.warning(f'temporal filters are not well specified, one or more is null')
        return False

    bIsVariableInCM = [False] * len(asWasdiVariables)

    for oVariableMap in aoCMVariablesList:
        sVariableShortName = oVariableMap.short_name
        try:
            # if the index is not in the asWasdiVariables, the method .index() will throw an exception
            iIndex = asWasdiVariables.index(sVariableShortName)
            bIsWasdiQueryCompliant = isWasdiQueryCompliantWithCMCoordinate(oVariableMap, sNorth, sWest, sSouth, sEast,
                                                                           sMinDepth, sMaxDepth, sStartDate, sEndDate)
            if bIsWasdiQueryCompliant:
                bIsVariableInCM[iIndex] = True
        except ValueError:
            logging.error(f'CM variable {sVariableShortName} not among the one in the WASDI query. Continue to search')
            continue

    # piece of code just for debug
    i = 0
    while i < len(bIsVariableInCM):
        if bIsVariableInCM[i] is False:
            logging.warning(f"checkVariables: variable {asWasdiVariables[i]} not found")
        i += 1

    return all(bIsVariableInCM)

def isWasdiQueryCompliantWithCMCoordinate(oVariableMap, sNorth, sWest, sSouth, sEast, sMinDepth, sMaxDepth, sStartDate, sEndDate):
    """
    Each variable in copernicus marine is a map containing a set of 'coordinates' (e.g. time, depth,
    latitude, longitude) describing the dimensions of the data. This method checks that the filters in the WASDI
    query (temporal filter, bounding box and depth filters) are included in the values associated to the coordinates
    of the variable.
    :param oVariableMap: the map structure describing a CM variable
    :param sNorth: north coordinate provided in the WASDI query
    :param sWest: west coordinate provided in the WASDI query
    :param sSouth: south coordinate provided in the WASDI query
    :param sEast: east coordinate provided in the WASDI query
    :param sMinDepth: min depth specified in the WASDI query
    :param sMaxDepth: max depth specified in the WASDI query
    :param sStartDate: start date specified in the WASDI query
    :param sEndDate: end date specified in the WASDI query
    :return: True if the filters selected in wasdi query are consistent with the coordinates of the variable
    """

    for oCoordinateMap in oVariableMap.coordinates:
        sCoordinateName = oCoordinateMap.coordinate_id

        if sCoordinateName == 'latitude':
            if not isGeoReferenceValid(oCoordinateMap, sSouth, sNorth):
                logging.warning(f"checkCoordinates: latitude not included in CM values")
                return False
            else:
                logging.debug(f"checkCoordinates: latitude included in CM values")

        if sCoordinateName == 'longitude':
            if not isGeoReferenceValid(oCoordinateMap, sWest, sEast):
                logging.warning(f"checkCoordinates: longitude not included in CM values")
                return False
            else:
                logging.debug(f"checkCoordinates: longitude included in CM values")

        if sCoordinateName == 'depth':
            afDepthValues = oCoordinateMap.values
            # if a depth was specified, then we need to put it to its negative value, as it is specified in CM
            if sMinDepth is not None and float(-sMinDepth) not in afDepthValues:
                logging.warning(f"checkCoordinates: min depth not available in CM")
                return False
            else:
                logging.debug(f"checkCoordinates: min depth included in CM values")
            if sMaxDepth is not None and float(-sMaxDepth) not in afDepthValues:
                logging.warning(f"checkCoordinates: max depth not available in CM")
                return False
            else:
                logging.debug(f"checkCoordinates: max depth included in CM values")

        if sCoordinateName == 'time':
            if not isTimeRangeValid(oCoordinateMap, sStartDate, sEndDate):
                logging.warning(f"checkCoordinates: time range not available in CM")
                return False
            else:
                logging.debug(f"checkCoordinates: time range included in CM values")
    return True


def isTimeRangeValid(oTimeMap, sStartDate, sEndDate):
    """
    Checks if the time range specified in WASDI query is included in the time range of the dataset
    :param oTimeMap: the map describing the temporal range in CM
    :param sStartDate: starting date time as specified in WASDI
    :param sEndDate: ending date time as specified in WASDI
    :return: True if the date-time range specified in WASDI is consistent with the date-time range of the CM dataset
    """

    try:
        sWasdiDateFormat = '%Y-%m-%dT%H:%M:%S.%fZ'
        oWasdiStartDate = datetime.strptime(sStartDate, sWasdiDateFormat)
        oWasdiEndDate = datetime.strptime(sEndDate, sWasdiDateFormat)

        if oWasdiStartDate > oWasdiEndDate:
            logging.warning(f"isTimeRangeValid: invalid time interval. start date {sStartDate} comes after {sEndDate}")

        sCMDateFormat = '%Y-%m-%dT%H:%M:%SZ'
        oCMEndDate = datetime.strptime(oTimeMap.maximum_value, sCMDateFormat)
        oCMStartDate = datetime.strptime(oTimeMap.minimum_value, sCMDateFormat)

        return oWasdiStartDate >= oCMStartDate \
            and oWasdiStartDate <= oCMEndDate \
            and oWasdiEndDate >= oCMStartDate \
            and oWasdiEndDate <= oCMEndDate

    except ValueError as oEx:
        logging.error(f"isTimeRangeValid: exception {oEx}")

    return False

def isGeoReferenceValid(oGeoReferenceMap, sWasdiMinGeoRef, sWasdiMaxGeoRef):
    """
    Checks if the spatial range (either latitude or longitude) specified in WASDI query is included
    in the spatial range of the dataset
    :param oGeoReferenceMap: the map describing the spatial range (either latitude or longitude) in CM
    :param sWasdiMinGeoRef: minimum latitude or longitude as specified in WASDI
    :param sWasdiMaxGeoRef: maximum latitude or longitude as specified in WASDI
    :return: True if the spatial range specified in WASDI is consistent with the spatial range of the CM dataset
    """

    fWasdiMax = float(sWasdiMaxGeoRef)
    fWasdiMin = float(sWasdiMinGeoRef)

    fMax = oGeoReferenceMap.maximum_value
    fMin = oGeoReferenceMap.minimum_value

    if fWasdiMax <= fMax and fWasdiMax >= fMin and fWasdiMin <= fMax and fWasdiMin >= fMin:
        logging.debug(f"isGeoReferenceValid: latitude values available")
        return True

    return False


def createResultViewModel(sCMProductId, sCMDatasetId, oWasdiJsonQuery, oMatchingServiceMap):
    # get the coordinates
    sNorth = oWasdiJsonQuery['north']
    sWest = oWasdiJsonQuery['west']
    sSouth = oWasdiJsonQuery['south']
    sEast = oWasdiJsonQuery['east']
    sWasdiCoordinatesFormat = \
        f"{sWest} {sSouth}, {sWest} {sNorth}, {sEast} {sNorth}, {sEast} {sSouth}, {sWest} {sSouth}"

    # get start and end date
    sStartDateTime = oWasdiJsonQuery['startFromDate']
    sEndDateTime = oWasdiJsonQuery['endToDate']

    # get the depth, if present, otherwise get None
    sMinDepth = oWasdiJsonQuery.get('cloudCoverageFrom', None)
    sMaxDepth = oWasdiJsonQuery.get('cloudCoverageTo', None)

    # create the link
    sVariables = oWasdiJsonQuery['sensorMode']

    oLinkMap = createLink(sCMDatasetId,
                          sVariables, sStartDateTime, sEndDateTime, sNorth, sSouth, sWest, sEast, sMinDepth, sMaxDepth)
    sJsonLink = json.dumps(oLinkMap)
    sJsonLink = "https://payload=" + sJsonLink
    # get summary
    sSummary = createSummary(sStartDateTime, sEndDateTime, sCMProductId)

    oProperties = {
        'variables': sVariables,
        'protocol': 'SUBS',
        'sizeInBytes': 0,
        'size': '0 B',
        'startDate': sStartDateTime,
        'endDate': sEndDateTime,
        'format': 'nc',
        'link': sJsonLink,
        'dataset': sCMDatasetId,
        'productType': sCMProductId,
        'beginPosition': f"{sStartDateTime[0:10]} - {sEndDateTime[0:10]}",
        'platformname': 'CM'
    }

    oResultViewModel = {
        'id': sCMProductId,
        'title': sCMDatasetId + ".nc",
        'footprint': sWasdiCoordinatesFormat,
        'link': sJsonLink,
        'summary': sSummary,
        'properties': oProperties
    }

    return oResultViewModel


def createSummary(sStartDate, sEndDate, sProductId):
    asSummaryElements = []

    if sStartDate[0:10] == sEndDate[0:10]:
        sDate = sStartDate[0:10]
    else:
        sDate = f"{sStartDate[0:10]} - {sEndDate[0:10]}"
    asSummaryElements.append('Date: ' + sDate)

    oPattern = re.compile(r"(\d\d\d_)+(\d\d\d)")
    oMatches = oPattern.search(sProductId)
    if oMatches:
        sMode = oMatches.group(0)
        asSummaryElements.append('Mode: ' + sMode)

    iProdStringLength = len(sProductId)
    if iProdStringLength > 20:
        sInstrument = sProductId[iProdStringLength - 20:iProdStringLength]
    elif iProdStringLength > 10:
        sInstrument = sProductId[iProdStringLength - 20:iProdStringLength]
    else:
        sInstrument = sProductId
    asSummaryElements.append('Instrument: ' + sInstrument)

    # size not available in CM
    asSummaryElements.append('Size: 0 B')

    return ', '.join(asSummaryElements)


def createLink(sDatasetId, sVariables, sStartDateTime, sEndDateTime, sNorth, sSouth, sWest, sEast, sMinDepth, sMaxDepth):
    oDownloadJsonParameters = {
        'datasetId': sDatasetId,
        'variables': sVariables,
        'startDateTime': sStartDateTime,
        'endDateTime': sEndDateTime,
        'north': sNorth,
        'south': sSouth,
        'west': sWest,
        'east': sEast
    }

    if sMinDepth is not None:
        oDownloadJsonParameters['minDepth'] = float(sMinDepth)

    if sMaxDepth is not None:
        oDownloadJsonParameters['maxDepth'] = float(sMaxDepth)

    return oDownloadJsonParameters

def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""

def executeDownloadFromCopernicusMarine(aoInputParameters, sUsername, sPassword):
    sDatasetId = aoInputParameters.get('datasetId', None)
    sVariables = aoInputParameters.get('variables', None)
    sStartDateTime = aoInputParameters.get('startDateTime', None)
    sEndDateTime = aoInputParameters.get('endDateTime', None)
    sNorth = aoInputParameters.get('north', None)
    sEast = aoInputParameters.get('east', None)
    sSouth = aoInputParameters.get('south', None)
    sWest = aoInputParameters.get('west', None)
    sDownloadDirectory = aoInputParameters.get('downloadDirectory', None)
    sDownloadFileName = aoInputParameters.get('downloadFileName', None)
    sMinDepth = aoInputParameters.get('minDepth', None)
    sMaxDepth = aoInputParameters.get('maxDepth', None)

    if stringIsNullOrEmpty(sDatasetId) or stringIsNullOrEmpty(sVariables):
        logging.warning("executeDownloadFromCopernicusMarine: datasetId or variables missing")
        sys.exit(1)

    if stringIsNullOrEmpty(sStartDateTime) or stringIsNullOrEmpty(sEndDateTime):
        logging.warning("executeDownloadFromCopernicusMarine: start date or end date missing")
        sys.exit(1)

    if stringIsNullOrEmpty(sNorth) or stringIsNullOrEmpty(sSouth) \
            or stringIsNullOrEmpty(sWest) or stringIsNullOrEmpty(sEast):
        logging.warning("executeDownloadFromCopernicusMarine: some coordinates of the bounding box are missing")
        sys.exit(1)

    if stringIsNullOrEmpty(sDownloadDirectory) or stringIsNullOrEmpty(sDownloadFileName):
        logging.warning("executeDownloadFromCopernicusMarine: output directory or file name not specified")
        sys.exit(1)

    if stringIsNullOrEmpty(sUsername) or stringIsNullOrEmpty(sPassword):
        logging.warning("executeDownloadFromCopernicusMarine: username or password not specified")
        sys.exit(1)

    logging.debug(f"executeDownloadFromCopernicusMarine: dataset and variables to download {sDatasetId}, {sVariables}")
    logging.debug(f"executeDownloadFromCopernicusMarine: selected time frame {sStartDateTime}, {sEndDateTime}")
    logging.debug(f"executeDownloadFromCopernicusMarine: selected bounding box N:{sNorth}, E:{sEast}, S:{sSouth}, W:{sWest}")
    logging.debug(f"executeDownloadFromCopernicusMarine: download directory {sDownloadDirectory}")
    logging.debug(f"executeDownloadFromCopernicusMarine: download file name {sDownloadFileName}")
    logging.debug(f"executeDownloadFromCopernicusMarine: min depth {sMinDepth}")
    logging.debug(f"executeDownloadFromCopernicusMarine: max depth {sMaxDepth}")
    asVariables = sVariables.split()

    sCMStartDateTime = sStartDateTime
    sCMEndDateTime = sEndDateTime
    try:
        oWasdiStartDateTime = datetime.strptime(sStartDateTime, "%Y-%m-%dT%H:%M:%S.%fZ")
        oWasdiEndDateTime = datetime.strptime(sEndDateTime, "%Y-%m-%dT%H:%M:%S.%fZ")

        sCMStartDateTime = oWasdiStartDateTime.strftime("%Y-%m-%dT%H:%M:%S")
        sCMEndDateTime = oWasdiEndDateTime.strftime("%Y-%m-%dT%H:%M:%S")
    except Exception as oEx:
        logging.warning(f"executeDownloadFromCopernicusMarine: exception while converting time filter to CM format {oEx}")

    sDownloadedFilePath = ""
    try:
        sDownloadedFilePath = copernicusmarine.subset(
            dataset_id=sDatasetId,
            variables=asVariables,
            start_datetime=sCMStartDateTime,
            end_datetime=sCMEndDateTime,
            minimum_longitude=sWest,
            maximum_longitude=sEast,
            minimum_latitude=sSouth,
            maximum_latitude=sNorth,
            minimum_depth=sMinDepth,
            maximum_depth=sMaxDepth,
            output_filename=sDownloadFileName,
            output_directory=sDownloadDirectory,
            force_download=True, # this parameter avoids the display of a prompt asking for confirmation before the download
            username=sUsername,
            password=sPassword
        )
    except Exception as oEx:
        logging.error(f"executeDownloadFromCopernicusMarine: exception from CM toolbox call {oEx}")

    sDownloadedFilePath = os.path.normpath(sDownloadedFilePath)
    return sDownloadedFilePath


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

    sFileName = "dataset.nc"

    try:
        aoPayloadMap = fromWasdiPayloadToMap(sUrl)

        if aoPayloadMap is None:
            logging.warning(f'getFileName: error trying to decode url {sUrl}')
            sys.exit(1)

        sDatasetId = aoPayloadMap.get("datasetId", "")

        if sDatasetId is not None and sDatasetId != "":
            sFileName = sDatasetId + "_" + str(datetime.now().timestamp()) + ".nc"

    except Exception as oEx:
        logging.warning(f'getFileName: exception retrieving file name from url {sUrl}, {oEx}')
        sys.exit(1)

    oRes = {
        'fileName': sFileName
    }

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
    except Exception as oEx:
        logging.warning(f'getFileName: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


def fromWasdiPayloadToMap(sUrl):
    sDecodedUrl = decodeUrl(sUrl)

    if sDecodedUrl is None or sDecodedUrl == '':
        logging.warning(f"fromWasdiPayloadToStringMap. Decoded url is null or empty {sUrl}")
        return None

    asTokens = sDecodedUrl.split("payload=")
    if len(asTokens) == 2:
        sPayload = asTokens[1]
        logging.debug(f"fromWasdiPayloadToStringMap. json string: {sPayload}")
        try:
            return json.loads(sPayload)
        except json.JSONDecodeError:
            logging.warning(f"fromWasdiPayloadToStringMap. Failed to parse JSON: {sPayload}")
            return None

    logging.warning(f"fromWasdiPayloadToStringMap. Payload not found in url {sUrl}")

    return None


def decodeUrl(sEncodedURL):
    try:
        return urllib.parse.unquote(sEncodedURL, encoding="utf-8")
    except Exception as oEx:
        logging.warning(f'decodeUrl: impossible to decode url {sEncodedURL}, {oEx}')
        return None


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
