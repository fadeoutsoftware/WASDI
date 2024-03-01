import copernicusmarine
import sys
import json
import os
import re
import logging


def executeCount(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeCount: input file not found')

    with open(sInputFilePath) as oJsonFile:
        aoInputQuery = json.load(oJsonFile)

    if aoInputQuery is None:
        logging.warning(f'executeCount: there was an error reading the input file: {sInputFilePath}')
        sys.exit(1)

    oResult = 1 #TODO put this back: searchOnCopernicusMarine(aoInputQuery)

    aoReturnObject = {}
    if oResult is None:
        aoReturnObject["count"] = -1
    else:
        aoReturnObject["count"] = 1

    with open(sOutputFilePath, 'w') as oFile:
        json.dump(aoReturnObject, oFile)


def executeAndRetrieve(sInputFilePath, sOutputFilePath):
    '''
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeAndRetrieve: input file not found')

    aoInputQuery = None
    with open(sInputFilePath) as oJsonFile:
        aoInputQuery = json.load(oJsonFile)

    if aoInputQuery is None:
        logging.warning(f'executeAndRetrieve: there was an error reading the input file: {sInputFilePath}')
        sys.exit(1)

    oResult = searchOnCopernicusMarine(aoInputQuery)
    logging.debug(f'executeAndRetrieve: result {oResult}')

    aoReturnList = [oResult]
    '''

    aoReturnList = []

    oResult1 = {}
    oResult1["title"] = "myfile1.tif"
    oResult1["summary"] = "My nice result"
    oResult1["id"] = "myfile1"
    oResult1["link"] = "http://thisisalink.com"
    oResult1["footprint"] = "POLYGON"
    oResult1["provider"] = "CM"

    aoReturnList.append(oResult1)

    with open(sOutputFilePath, 'w') as oFile:
        json.dump(aoReturnList, oFile)


def executeDownloadFile(sInputFilePath, sOutputFilePath, sJsonDataProviderConfig):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeDownloadFile: input file not found')

    aoInputParameters = None
    with open(sInputFilePath) as oJsonFile:
        aoInputParameters = json.load(oJsonFile)

    if aoInputParameters is None:
        logging.warning(f'executeDownloadFile: there was an error reading the input file: {sInputFilePath}')
        sys.exit(1)

    if sJsonDataProviderConfig is None or sJsonDataProviderConfig == "":
        logging.warning(f'executeDownloadFile: data provider configuration is None or empty string: {sJsonDataProviderConfig}')
        sys.exit(1)

    aoDataProviderConfig = json.loads(sJsonDataProviderConfig)

    sUsername = aoDataProviderConfig.get('user', None)
    sPassword = aoDataProviderConfig.get('password', None)

    sDownloadedFilePath = executeDownloadFromCopernicusMarine(aoInputParameters, sUsername, sPassword)

    logging.debug(f"executeDownloadFile: path to the downloaded file {sDownloadedFilePath}")

    oRes = {
        'outputFile': sDownloadedFilePath
    }

    with open(sOutputFilePath, 'w') as oFile:
        json.dump(oRes, oFile)
        logging.debug(f"path to the downloaded file written in the output file")


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
        aoCatalogue = copernicusmarine.describe(contains=asCMMetadataFilters, include_datasets=True)
    except Exception as oEx:
        logging.error(f"searchOnCopernicusMarine: exception  retrieving the metadata from the provider: {oEx}")
        sys.exit(1)

    if aoCatalogue is None:
        logging.warning(f"searchOnCopernicusMarine: no catalogue found")
        sys.exit(1)

    if 'products' not in aoCatalogue and len(aoCatalogue['products'] == 0):
        logging.warning(f"searchOnCopernicusMarine: the catalogue doesn't contain products")
        sys.exit(1)

    oResult = findFirstMatchingResults(oJsonQuery, aoCatalogue['products'])
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

    sVariablesFromSensorMode = oWasdiJsonQuery['sensorMode']
    asWasdiVariables = sVariablesFromSensorMode.split(" ")

    for aoProductMap in aoCatalogue:
        # it looks like the toolbox does not need the "-TDS" suffix anymore, so we remove it
        if sWasdiProductId.endswith("-TDS"):
            sWasdiProductId = sWasdiProductId[0:len(sWasdiProductId) - 4]

        sCMProductId = aoProductMap.get('product_id', '')
        if sCMProductId != sWasdiProductId:
            logging.debug(f'findFirstMatchingResults: '
                  f'product id {sWasdiProductId} from WASDI doesnt match product id {sCMProductId} from CM')
            continue

        # check the datasets
        aoDatasetsLists = aoProductMap.get('datasets', [])

        for aoDatasetMap in aoDatasetsLists:
            sWasdiDatasetId = oWasdiJsonQuery['productName']
            sCMDatasetId = aoDatasetMap.get('dataset_id', '')

            if sWasdiDatasetId != sCMDatasetId:
                logging.debug(
                    f'findFirstMatchingResults: '
                    f'dataset id {sWasdiDatasetId} from WASDI does not match dataset id {sCMProductId} from CM')
                continue

            aoVersionsList = aoDatasetMap.get('versions', [])
            for oVersion in aoVersionsList:
                # TODO: should I return some default value if the coordinates or the depth is not specified?
                oMatchingServiceMap = getServiceMatchingVariables(oVersion, asWasdiVariables, oWasdiJsonQuery['north'],
                                            oWasdiJsonQuery['west'], oWasdiJsonQuery['south'], oWasdiJsonQuery['east'],
                                            oWasdiJsonQuery['cloudCoverageFrom'], oWasdiJsonQuery['cloudCoverageTo'])

                if oMatchingServiceMap is not None:
                    # if we find a matching service, we can then use the information in the wasdi query VM and in the
                    # matching service map to populate the result VM
                    return createResultViewModel(sCMProductId, sCMDatasetId, oWasdiJsonQuery, oMatchingServiceMap)


def getServiceMatchingVariables(oVersion, asWasdiVariables, sNorth, sWest, sSouth, sEast, sMinDepth, sMaxDepth):
    """
    Get the first service storing the dataset and matching the search parameters in the WASDI query, i.e. the variables
    and the bounding box
    :param oVersion: the map representing one version of the dataset
    :param asWasdiVariables: list of strings representing the names of the variables coming from the WASDI query
    param sNorth: north coordinate provided in the WASDI query
    :param sWest: west coordinate provided in the WASDI query
    :param sSouth: south coordinate provided in the WASDI query
    :param sEast: east coordinate provided in the WASDI query
    :return: the map representing the first service that was found matching all the search parameters in the WASDI query
    """
    oFirstMatchingService = None

    for oPart in oVersion['parts']:
        for oServiceMap in oPart['services']:

            oServiceTypeMap = oServiceMap.get('service_type', None)
            sServiceTypeName = None
            if oServiceTypeMap is not None:
                sServiceTypeName = oServiceTypeMap.get('service_name', None)

            aoCMVariablesList = oServiceMap.get('variables', None)

            if aoCMVariablesList is None:
                logging.debug(f"getServiceMatchingVariables: service '{sServiceTypeName}' has no variables")
                continue

            # let's check if the dataset available in that service contains all the variables
            # selected in the wasdi query
            bServiceContainsVariables = checkVariablesAndCoordinates(aoCMVariablesList, asWasdiVariables,
                                                                     sNorth, sWest, sSouth, sEast, sMinDepth, sMaxDepth)

            if bServiceContainsVariables:
                # the dataset provided by this service contains all the information selected in the WASDI query
                logging.debug(f"getServiceMatchingVariables: dataset in '{sServiceTypeName}' has all variables needed")
                oFirstMatchingService = oServiceMap
                break

    return oFirstMatchingService


def checkVariablesAndCoordinates(aoCMVariablesList, asWasdiVariables, sNorth, sWest, sSouth, sEast, sMinDepth, sMaxDepth):
    """
    Check if all the variables that were selected in the WASDI are present in the dataset
    :param aoCMVariablesList: list of CM objects representing the variables
    :param asWasdiVariables: list of strings representing the names of the variables coming from the WASDI query
    :param sNorth: north coordinate provided in the WASDI query
    :param sWest: west coordinate provided in the WASDI query
    :param sSouth: south coordinate provided in the WASDI query
    :param sEast: east coordinate provided in the WASDI query
    :return: True if all the variables selected in the WASDI query are present in the dataset and have values in
    the bounding box selected in WASDI, False otherwise
    """
    logging.debug(f'checkVariablesAndCoordinates: variables received from wasdi {asWasdiVariables}')
    # array of booleans used to track the presence, in the dataset, of the variable selected in WASDI .
    # For a given element 'i', bIsVariableInCM[i] = True if the variable is present in the dataset
    bIsVariableInCM = [False] * len(asWasdiVariables)

    for oVariableMap in aoCMVariablesList:
        sVariableShortName = oVariableMap['short_name']
        try:
            # if the index is not in the asWasdiVariables, the method .index() will throw an exception
            iIndex = asWasdiVariables.index(sVariableShortName)
            logging.debug(f'checkVariablesAndCoordinates: checking variable {sVariableShortName}:')
            bIsBBoxMatching = isWasdiBBoxIncludedInCMCoordinates(oVariableMap, sNorth, sWest, sSouth, sEast)
            if bIsBBoxMatching:
                bIsVariableInCM[iIndex] = True
        except ValueError:
            logging.error(f'CM variable {sVariableShortName} not among the one in the WASDI query. Continue to search')
            continue

    # piece of code just for debug
    i = 0
    while i < len(bIsVariableInCM):
        if bIsVariableInCM[i] is False:
            logging.warning(f"checkVariablesAndCoordinates: variable {asWasdiVariables[i]} not found")
        i += 1

    return all(bIsVariableInCM)


def isWasdiBBoxIncludedInCMCoordinates(oVariableMap, sNorth, sWest, sSouth, sEast):
    """
    Check if the values of a variable are available with the bounding box provided by the WASDI query
    :param oVariableMap: the map containing the metadata about a dataset's variable
    :param sNorth: north coordinate provided in the WASDI query
    :param sWest: west coordinate provided in the WASDI query
    :param sSouth: south coordinate provided in the WASDI query
    :param sEast: east coordinate provided in the WASDI query
    :return: True if the variable's coordinates are included in the bounding box provided by the WASDI query
    """
    bLatitudeMatch = False
    bLongitudeMatch = False
    if 'coordinates' in oVariableMap:

        for oCoordinateMap in oVariableMap['coordinates']:

            if 'minimum_value' not in oCoordinateMap or 'maximum_value' not in oCoordinateMap:
                logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: coordinates miss the min or the max value")
                continue

            sGeoReference = oCoordinateMap['coordinates_id']

            if sGeoReference == 'latitude':
                fMax = oCoordinateMap['maximum_value']
                fMin = oCoordinateMap['minimum_value']
                logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: CM North {fMax}")
                logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: CM South {fMin}")
                if sNorth <= fMax and sNorth >= fMin and sSouth <= fMax and sSouth >= fMin:
                    logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: latitude values available")
                    bLatitudeMatch = True

            if sGeoReference == 'longitude':
                fMax = oCoordinateMap['maximum_value']
                fMin = oCoordinateMap['minimum_value']
                logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: CM East {fMax}")
                logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: CM West {fMin}")
                if sWest <= fMax and sWest >= fMin and sEast <= fMax and sEast >= fMin:
                    logging.debug(f"isWasdiBBoxIncludedInCMCoordinates: longitude values available")
                    bLongitudeMatch = True

    return bLatitudeMatch and bLongitudeMatch


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

    # create the link
    sVariables = oWasdiJsonQuery['sensorMode']
    oLinkMap = createLink(sCMDatasetId, sVariables, sStartDateTime, sEndDateTime, sNorth, sSouth, sWest, sEast)
    sJsonLink = json.dumps(oLinkMap)
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
        'title': sCMDatasetId,
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


def createLink(sDatasetId, sVariables, sStartDateTime, sEndDateTime, sNorth, sSouth, sWest, sEast):
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

    asVariables = sVariables.split()  # TODO: this needs to be verified (are we splitting according to blank spaces)?

    '''
    # TODO: this I need to check
    minimum_depth = 0,
    maximum_depth = 5,
    '''
    sDownloadedFilePath = ""
    try:
        sDownloadedFilePath = copernicusmarine.subset(
            dataset_id=sDatasetId,
            variables=asVariables,
            start_datetime=sStartDateTime,
            end_datetime=sEndDateTime,
            minimum_longitude=sWest,
            maximum_longitude=sEast,
            minimum_latitude=sSouth,
            maximum_latitude=sNorth,
            output_filename=sDownloadFileName,
            output_directory=sDownloadDirectory,
            force_download=True,     # forcing the download avoids a prompt which is asking for confirmation before the download
            username=sUsername,
            password=sPassword
        )
    except Exception as oEx:
        logging.error(f"executeDownloadFromCopernicusMarine: exception from CM toolbox call {oEx}")

    sDownloadedFilePath = os.path.normpath(sDownloadedFilePath)
    return sDownloadedFilePath



if __name__ == '__main__':
    logging.basicConfig(encoding='utf-8', format='[%(levelname)s] %(message)s', level=logging.DEBUG)

    logging.debug('__main__: this is the very start of the data provider')
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
        sJsonDataProviderConfig = asArgs[4]

        # TODO: we can remove all those logs
        logging.debug('__main__: first argument is the name of the file - we are not interested:' + asArgs[0])
        logging.debug('__main__: operation ' + sOperation)
        logging.debug('__main__: input file ' + sInputFile)
        logging.debug('__main__: output file: ' + sOutputFile)
        logging.debug('__main__: data provider config ' + sJsonDataProviderConfig)

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
        executeDownloadFile(sInputFile, sOutputFile, sJsonDataProviderConfig)
    else:
        logging.debug('__main__: unknown operation. Script will exit')
        sys.exit(1)


    # print(copernicusmarine.__version__)  # version 1.0.1

    # try to log-in executing the following command:
    # copernicusmarine.login(username='user_name', passoword='****')
    # output of the command: Credentials file stored in C:/Users/valentina.leone/.copernicusmarine/.copernicusmarine-credentials.
    # this command must be executed only one time. After that, the credentials will be taken from the credential file


    '''
    print("Number of products found: " + str(len(catalogue['products'])))
    product = catalogue['products'][0]
    print("Product title: " + product['title'])
    print("Product id: " + product['product_id'])
    print("Number of datasets: " + str(len(product['datasets'])))
    dataset = product['datasets'][0]
    print("Dataset id: " + dataset['dataset_id'])
    print("Number of versions: " + str(len(dataset['versions'])))
    version = dataset['versions'][0]
    print("Number of parts: " + str(len(version['parts'])))
    part = version['parts'][0]
    print("Number of services: " + str(len(part['services'])))
    services = part['services']
    for service in services:
        print("*** Service: " + service['service_type']['service_name'])
        variables = service['variables']
        for variable in variables:
            print("\t" + variable['short_name'] + ", " + variable['standard_name'])
            print("\t\tbbox: " + str(variable['bbox']))
            coordinates = variable["coordinates"]
            for coordinate in coordinates:
                print("\t\tcoordinate: " + coordinate["coordinates_id"])
                if coordinate['coordinates_id'] == "time":
                    print("\t\t\tmin value" + str(coordinate['minimum_value']))
                    print("\t\t\tmax value" + str(coordinate['maximum_value']))
                    print("\t\t\tnum values" + str(len(coordinate['values'])))
    print("the end")
    '''
    sys.exit(0)
