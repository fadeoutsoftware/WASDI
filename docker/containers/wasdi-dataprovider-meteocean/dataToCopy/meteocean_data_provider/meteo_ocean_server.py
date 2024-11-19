import os.path

from flask import Flask, request, Response, send_file

import logging
import xarray as xr
import ast
import fsspec

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)

oServerApp = Flask(__name__)

sMeteOceanBasePath = "s3://genoatest/aloarca/wave_dataset/"
asMeteOceanModels = [
    'CCLM4-CanESM2',
    'CCLM4-MIROC5',
    'COSMO-crCLIM1-EC-EARTH',
    'COSMO-crCLIM1-HadGEM2-ES',
    'COSMO-crCLIM1-NorESM1-M',
    'HIRHAM5-CNRM-CM5',
    'HIRHAM5-EC-EARTH',
    'HIRHAM5-HadGEM2-ES',
    'HIRHAM5-IPSL-CM5A-MR',
    'HIRHAM5-MPI-ESM-LR',
    'HIRHAM5-NorESM1-M',
    'RCA4-CNRM-CM5',
    'RCA4-EC-EARTH',
    'RCA4-HadGEM2-ES',
    'RCA4-IPSL-CM5A-MR',
    'RCA4-MPI-ESM-LR',
    'RCA4-NorESM1-M'
]

@oServerApp.route('/hello', methods=['GET'])
def hello():
    return "hello from the MeteOcean flask server", 200

@oServerApp.route('/query/count', methods=['POST'])
def countResults():
    iCount = -1;
    try:
        aoInputMap = request.get_json()

        aoResults = getResults(aoInputMap)

        if aoResults is not None:
            iCount = len(aoResults)

    except Exception as oEx:
        logging.error(f"countResults. exception {oEx}")
        iCount = -1
        return str(iCount), 500

    sCount = str(iCount)

    logging.info(f"countResults. number of products matching the search {sCount}")

    return sCount, 200


@oServerApp.route('/query/list', methods=['POST'])
def executeAndRetrieve():
    aoResults = None

    try:
        aoInputMap = request.get_json()

        aoResults = getResults(aoInputMap)

    except Exception as oEx:
        logging.error(f"executeAndRetrieve. Exception {oEx}")
        return aoResults, 500

    return aoResults, 200




def getResults(aoInputMap):
    # preliminary controls. Check the inputs for validation
    if not isInputValid(aoInputMap):
        logging.warning("getResults. some of the input parameters are not valid")
        return None

    try:
        sDatasetName = aoInputMap.get('productType')
        sVariable = aoInputMap.get('productLevel')
        sCase = aoInputMap.get('sensorMode')
        sModel = aoInputMap.get('polarisation')
        bBiasAdjustment = aoInputMap.get('timeliness')
        dNorth = aoInputMap.get('north')
        dSouth = aoInputMap.get('south')
        dWest = aoInputMap.get('west')
        dEast = aoInputMap.get('east')
        sQuantiles = aoInputMap.get('platformSerialIdentifier')
        sMonth = aoInputMap.get('startFromDate')
        sSeason = aoInputMap.get('endFromDate')

        iLimit = aoInputMap.get('limit')
        iOffset = aoInputMap.get('offset')

        # get file name from one of the files
        sReferenceFileForBoundingBox = os.path.join(sMeteOceanBasePath, "hindcast_hs_1979_2005__monthlymax.nc")

        oS3FileSystem = getS3FileSystem()
        sS3Product = oS3FileSystem.open(sReferenceFileForBoundingBox)
        oDataset = xr.open_dataset(sS3Product, engine='h5netcdf')
        aoResults = list()

        dDatasetWest, dDatasetNorth, dDatasetEast, dDatasetSouth = getBoundingBoxFromDataset(oDataset)
        if not isBoundBoxEmpty(dNorth, dSouth, dWest, dEast):
            if not doesBoundingBoxIntersect(dDatasetWest, dDatasetNorth, dDatasetEast, dDatasetSouth, dNorth, dSouth, dEast,
                                            dWest):
                return aoResults

        asFileNamesList = getFileNamesList(sDatasetName, sVariable, sCase, bBiasAdjustment, sModel)

        for sFileName in asFileNamesList:
            sActualModel = sModel
            if isStringNullOrEmpty(sActualModel):
                sActualModel = extractModelFromFilename(sFileName)

            if isBoundBoxEmpty(dNorth, dSouth, dWest, dEast):
                aoResultViewModelList = getQueryResultViewModelList(sDatasetName, sActualModel, bBiasAdjustment, sFileName,
                                                                    sVariable, sCase, dDatasetNorth, dDatasetSouth,
                                                                    dDatasetWest, dDatasetEast, sQuantiles, sMonth, sSeason)
                aoResults.extend(aoResultViewModelList)
                continue

            if isSinglePoint(dNorth, dSouth, dWest, dEast):
                logging.info(
                    f"getResults. bounding box is a single point with latitude {dNorth} and longitude {dWest}")
                logging.info("getResults. found a value close to the point")
                aoResultViewModelList = getQueryResultViewModelList(sDatasetName, sActualModel, bBiasAdjustment, sFileName,
                                                                    sVariable, sCase, dNorth,
                                                                    dSouth, dWest, dEast, sQuantiles, sMonth, sSeason)
                aoResults.extend(aoResultViewModelList)
            else:
                aoResultViewModelList = getQueryResultViewModelList(sDatasetName, sActualModel, bBiasAdjustment, sFileName,
                                                                sVariable, sCase, dNorth,
                                                                dSouth, dWest, dEast, sQuantiles, sMonth, sSeason)
                aoResults.extend(aoResultViewModelList)

        if len(aoResults) > iLimit \
                and iLimit is not None \
                and iOffset is not None \
                and iLimit > 0 \
                and iOffset >= 0:
            iStart = iOffset
            iEnd = iStart + iLimit
            aoResults = aoResults[iStart:iEnd]

    except Exception as oEx:
        logging.error(f"getResults. Exception {oEx}")
        return None

    return aoResults


@oServerApp.route('/download', methods=['GET'])
def download():

    # https://hindcast_hs_1979_2005__monthlymax.nc,hs,monthlymax,46.11,29.99999999999947,-5.899999999999977,36.75671199999988
    if not (request.headers.get("x-api-key") == 'super-secret-WASDI-key'):
        return Response(f"Invalid API key", status=401)

    logging.info("download. API key is valid")

    try:
        sFileNameFromRequest = request.args.get('fileName') # f"{sOriginalFileName},{sVariable},{sCase},{dNorth},{dSouth},{dWest},{dEast},downloadFileName;MeteOcean"

        asDownloadInformation = sFileNameFromRequest.split(",")

        sBaseFile = asDownloadInformation[0].replace("https://", "")
        sVariable = asDownloadInformation[1]
        sNorth = asDownloadInformation[3]
        sSouth = asDownloadInformation[4]
        sWest = asDownloadInformation[5]
        sEast = asDownloadInformation[6]
        sDownloadFileName = asDownloadInformation[7]
        sQuantile = asDownloadInformation[8]
        sMonth = asDownloadInformation[9]
        sSeason = asDownloadInformation[10]

        logging.info(f"download. base file on S3: {sBaseFile}, download file name: {sDownloadFileName}, coordinates: {sWest}W, {sNorth}N, {sEast}E, {sSouth}S")

        if isStringNullOrEmpty(sBaseFile):
            return Response("No base file specified", status=400)

        if isStringNullOrEmpty(sDownloadFileName):
            return Response("No download file name specified", status=400)

        sS3Product = readFileFromS3(sBaseFile)

        if ast.literal_eval(sNorth) is None \
                or ast.literal_eval(sSouth) is None \
                or ast.literal_eval(sWest) is None \
                or ast.literal_eval(sEast) is None:
            # if the bounding box is empty, then we download all the file
            logging.info("download. No bounding box specified. Sending the whole file")
            oResponse = send_file(sS3Product, as_attachment=True, download_name=sBaseFile)
            oResponse.headers["Content-Disposition"] = f"attachment; filename={sDownloadFileName}"
            return oResponse

        else:
            logging.info("download. Bounding box specified. Sending a subset of the file")
            oDataset = xr.open_dataset(sS3Product, engine='h5netcdf')

            sRelevantVariable = recoverRelevantVariable(oDataset, sVariable)

            oDatasetVariableData = oDataset[sRelevantVariable]
            oResultDataset = selectValuesInBoundingBox(oDatasetVariableData, float(sNorth), float(sSouth), float(sWest), float(sEast))
            print(oResultDataset)
            if not isStringNullOrEmpty(sQuantile):
               oResultDataset = oResultDataset.sel(quantile=float(sQuantile))
            if not isStringNullOrEmpty(sMonth):
                oResultDataset = oResultDataset.sel(month=int(sMonth))
            if not isStringNullOrEmpty(sSeason):
                oResultDataset = oResultDataset.sel(season=sSeason)

            oNewDataset = oResultDataset.to_dataset()
            oNetcdfBytes = oNewDataset.to_netcdf()

            oResponse = Response(oNetcdfBytes,
                                 mimetype="application/x-netcdf",
                                 headers={
                                    "Content-Disposition":f"attachment; filename={sDownloadFileName}",
                                    "Content-Length": str(len(oNetcdfBytes)),
                                })
            return oResponse, 200
    except Exception as oEx:
        logging.error(f"download. Exception {oEx}")
        return Response(f"Error occurred: {str(oEx)}", status=500)


def recoverRelevantVariable(oDataset, sTentativeVariable):
    sActualVariable = sTentativeVariable
    asDatasetVariables = list(oDataset.variables)

    if sActualVariable in asDatasetVariables:
        logging.info(f"recoverRelevantVariable. Variable {sTentativeVariable} found in the dataset")
        return sActualVariable

    asExcludedVariables = ["longitude", "latitude", "quantile", "season", "surface", "month"]

    asActualVariables = [sVariable for sVariable in asDatasetVariables if sVariable not in asExcludedVariables]

    if len(asActualVariables) > 0:
        sActualVariable = asActualVariables[0]
        logging.info(f"recoverRelevantVariable. Variable {sTentativeVariable} NOT found in the dataset, using instead {sActualVariable}")

    return sActualVariable


def getBoundingBoxFromDataset(oDataset):
    dSouth = oDataset['latitude'].min().values
    dNorth = oDataset['latitude'].max().values
    dWest = oDataset['longitude'].min().values
    dEast = oDataset['longitude'].max().values

    return dWest, dNorth, dEast, dSouth


def isInputValid(aoInputMap):
    # - check that the data provider is valid
    sPlatformName = aoInputMap.get('platformName')
    logging.info("isInputValid. platform name: " + sPlatformName)
    if isStringNullOrEmpty(sPlatformName):
        logging.warning("isInputValid. platform name is null or empty")
        return False

    # - check that the dataset (productType) is valid
    sDataset = aoInputMap.get('productType')
    logging.info("isInputValid. dataset " + sDataset)
    if isStringNullOrEmpty(sDataset):
        logging.warning("isInputValid. dataset is null or empty")
        return False

    # - check that the variable (productLevel) is valid
    sVariable = aoInputMap.get('productLevel')
    logging.info(f"isInputValid. product level {sVariable}")
    if isStringNullOrEmpty(sVariable):
        logging.warning("isInputValid. variable is null or empty")
        return False

    # - check that the case (sensorMode) is valid
    sCase = aoInputMap.get('sensorMode')
    logging.info(f"isInputValid. case {sCase}")
    if isStringNullOrEmpty(sCase):
        logging.warning("isInputValid. case is null or empty")
        return False

    # - check that the frequency (instrument) is valid
    sInstrument = aoInputMap.get('instrument')
    logging.info(f"isInputValid: frequency {sInstrument}")
    if isStringNullOrEmpty(sInstrument):
        logging.warning("isInputValid. frequency is null or empty")
        return False

    # - check the value of the bias adjustment
    if sVariable == 'hs':
        sBiasAdj = aoInputMap.get('timeliness')
        logging.info(f"isInputValid: bias-adjusted {sBiasAdj}")
        if isStringNullOrEmpty(sBiasAdj):
            logging.warning("isInputValid. bias-adjustment is null or empty")
            return False

    '''
    if sDataset in ['rcp85_mid', 'rcp85_end']:
        sModel = aoInputMap.get('polarisation')
        logging.info(f"isInputValid: model {sModel}")
        if isStringNullOrEmpty(sModel):
            logging.warning("isInputValid. model is null or empty")
            return False
    '''

    # - check that the coordinates are valid
    dNorth = aoInputMap.get('north')
    dSouth = aoInputMap.get('south')
    dWest = aoInputMap.get('west')
    dEast = aoInputMap.get('east')

    if not isBoudningBoxValid(dNorth, dSouth, dWest, dEast):
        logging.warning("isInputValid. bounding box not valid")
        return False

    return True

def isStringNullOrEmpty(sString):
    sString = None if sString == 'None' else sString
    return sString is None or sString == ''


def isBoundBoxEmpty(dNorth, dSouth, dWest, dEast):
    if dNorth is None and dSouth is None and dWest is None and dEast is None:
        return True

    return False


def isBoudningBoxValid(dNorth, dSouth, dWest, dEast):
    # an empty bounding box is valid
    if isBoundBoxEmpty(dNorth, dSouth, dWest, dEast):
        return True

    return isLatitudeValid(dNorth, dSouth) and isLongitudeValid(dEast, dWest)


def isLatitudeValid(dNorth, sSouth):
    if dNorth is None or sSouth is None:
        return False

    if not -90 <= dNorth <= 90:
        return False

    if not -90 <= sSouth <= 90:
        return False

    return sSouth <= dNorth


def isLongitudeValid(dEast, dWest):
    if dEast is None or dWest is None:
        return False

    if not -180 < dEast < 180:
        return False

    if not -180 < dWest < 180:
        return False

    return dWest <= dEast


def isSinglePoint(dNorth, dSouth, dWest, dEast):
    return not isBoundBoxEmpty(dNorth, dSouth, dWest, dEast) and dNorth == dSouth and dWest == dEast


def selectValuesInBoundingBox(oDataset, dNorth, dSouth, dWest, dEast):
    bIsInBoundingBox = (oDataset['longitude'] >= dWest) & (oDataset['longitude'] <= dEast) \
                       & (oDataset['latitude'] >= dSouth) & (oDataset['latitude'] <= dNorth)

    # TODO: should I apply the same extraction also to the other variables???
    oValuesInBoundingBox = oDataset.where(bIsInBoundingBox, drop=True)

    return oValuesInBoundingBox

def selectValuesForQuantile(oDataset, sQuantile):
    return oDataset.sel(quantile=sQuantile)


def getQueryResultViewModelList(sDataset, sModel, bBiasAdjustment, sOriginalFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast, sQuantile, sMonth, sSeason):
    asQuantilesList = ["0.1", "0.5", "0.9", "0.95", "0.99"]
    asResults = []

    if "quantiles" in sCase:
        if isStringNullOrEmpty(sQuantile):
            for sQuantile in asQuantilesList:
                oViewModel = createQueryResultViewModel(sDataset, sModel, bBiasAdjustment, sOriginalFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast, sQuantile, sMonth, sSeason)
                asResults.append(oViewModel)
        else:
            if sQuantile in asQuantilesList:
                oViewModel = createQueryResultViewModel(sDataset, sModel, bBiasAdjustment, sOriginalFileName, sVariable, sCase,
                                                        dNorth, dSouth, dWest, dEast, sQuantile, sMonth, sSeason)
                asResults.append(oViewModel)
    else:
        oViewModel = createQueryResultViewModel(sDataset, sModel, bBiasAdjustment, sOriginalFileName, sVariable, sCase,
                                                dNorth, dSouth, dWest, dEast, sQuantile, sMonth, sSeason)
        asResults.append(oViewModel)

    return asResults


def createQueryResultViewModel(sDataset, sModel, bBiasAdjustment, sOriginalFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast, sQuantiles, sMonth, sSeason):
    # sTitle = f"MeteOcean_{sDataset}_{sVariable}_{sCase}_{formatDecimal(dWest)}W_{formatDecimal(dNorth)}N_{formatDecimal(dEast)}E_{formatDecimal(dSouth)}S.nc"
    sTitle = getWasdiProductName(sDataset, sModel, bBiasAdjustment, sVariable, sCase, dNorth, dSouth, dWest, dEast, sQuantiles, sMonth, sSeason)
    oResultVM = dict()
    oResultVM['preview'] = None
    oResultVM['title'] = sTitle
    oResultVM['summary'] = ""
    oResultVM['id'] = sTitle
    oResultVM['link'] = f"https://{sOriginalFileName},{sVariable},{sCase},{dNorth},{dSouth},{dWest},{dEast},{sTitle},{sQuantiles},{sMonth},{sSeason};MeteOcean"
    oResultVM['footprint'] = f"POLYGON (({dWest} {dSouth}, {dWest} {dNorth}, {dEast} {dNorth}, {dEast} {dSouth}, {dWest} {dSouth}))"
    oResultVM['provider'] = "MeteOcean"
    oResultVM['volumeName'] = None
    oResultVM['volumePath'] = None

    sDate = str()
    if sDataset == "hindcast" or sDataset == 'historical':
        sDate = "1979-01-01T00:00:00.00Z"
    elif sDataset == 'rcp85_mid':
        sDate = "2034-01-01T00:00:00.00Z"
    elif sDataset == 'rcp85_end':
        sDate = '2074-01-01T00:00:00.00Z'

    oProperties = dict()
    oProperties['date'] = sDate
    oProperties["productType"] = sDataset
    oProperties["fileName"] = sTitle

    oResultVM['properties'] = oProperties

    return oResultVM

def getWasdiProductName(sDataset, sModel, bBiasAdjustment, sVariable, sCase, dNorth, dSouth, dWest, dEast, sQuantile, sMonth, sSeason):
    sExtension = ".nc"
    sWasdiProductName = ""

    if sDataset == 'hindcast':
        sWasdiProductName = f"MeteOcean_{sDataset}_{sVariable}_{sCase}_{formatDecimal(dWest)}W_{formatDecimal(dNorth)}N_{formatDecimal(dEast)}E_{formatDecimal(dSouth)}S"
    else:
        sDataBias = None
        if sVariable == 'hs' and bBiasAdjustment == 'true':
            sDataBias = 'ba_eqm_month'
        else:
            sDataBias = 'raw'

        sWasdiProductName = f"MeteOcean_{sDataset}_{sModel}_{sVariable}_{sCase}_{sDataBias}_{formatDecimal(dWest)}W_{formatDecimal(dNorth)}N_{formatDecimal(dEast)}E_{formatDecimal(dSouth)}S"

    if not isStringNullOrEmpty(sQuantile):
        sWasdiProductName += "_" + sQuantile.replace(".", "")

    if not isStringNullOrEmpty(sMonth):
        sWasdiProductName += "_" + sMonth
    elif not isStringNullOrEmpty(sSeason):
        sWasdiProductName += "_" + sSeason
    sWasdiProductName += sExtension

    return sWasdiProductName


def formatDecimal(dValue):
    sSign = 'P' if dValue >= 0 else 'N'
    dAbsValue = abs(dValue)
    # Separate integer and decimal parts
    iIntpart = int(dAbsValue)
    decimal_part = dAbsValue - iIntpart

    # Format the integer part to be  3 digits, padded with zeroes
    iIntStr = f'{iIntpart:03}'

    # Format the decimal part to have exactly 4 digits, truncate if needed
    dDecimalStr = f'{decimal_part:.4f}'.split('.')[1]  # Get the decimal part as string

    return f'{sSign}{iIntStr}{dDecimalStr}'


def getFileNamesList(sDataset, sVariable, sCase, bBiasAdjustment, sModel):
    oResults = list()

    if sDataset == 'hindcast':
        oResults.append(f"{sDataset}_{sVariable}_1979_2005__{sCase}.nc")
        return oResults

    sDataBias = None
    if sVariable == 'hs' and bBiasAdjustment == 'true':
        sDataBias = 'ba_eqm_month'
    else:
        sDataBias = 'raw'

    if sDataset == "historical":
        if not isStringNullOrEmpty(sModel):
            oResults.append(f"{sModel}_{sDataBias}_historical_{sVariable}_1979_2005__{sCase}.nc")
        else:
            for sMeteOceanModel in asMeteOceanModels:
                oResults.append(f"{sMeteOceanModel}_{sDataBias}_historical_{sVariable}__1979_2005__{sCase}.nc")
    elif sDataset == 'rcp85_mid':
        if not isStringNullOrEmpty(sModel):
            oResults.append(f"{sModel}_{sDataBias}_rcp85_mid_{sVariable}_2034_2060__{sCase}.nc")
        else:
            for sMeteOceanModel in asMeteOceanModels:
                oResults.append(f"{sMeteOceanModel}_{sDataBias}_rcp85_mid_{sVariable}_2034_2060__{sCase}.nc")
    elif sDataset == 'rcp85_end':
        if not isStringNullOrEmpty(sModel):
            oResults.append(f"{sModel}_{sDataBias}_rcp85_end_{sVariable}_2074_2100__{sCase}.nc")
        else:
            for sMeteOceanModel in asMeteOceanModels:
                oResults.append(f"{sMeteOceanModel}_{sDataBias}_rcp85_end_{sVariable}_2074_2100__{sCase}.nc")

    return oResults


def getS3FileSystem():
    oFileSystemRead = fsspec.filesystem('s3',
                                        anon=True,
                                        skip_instance_cache=True,
                                        client_kwargs={'endpoint_url': 'https://usgs.osn.mghpcc.org'})
    return oFileSystemRead


def readFileFromS3(sFileName):
    oFileSystemRead = fsspec.filesystem('s3',
                                        anon=True,
                                        skip_instance_cache=True,
                                        client_kwargs={'endpoint_url': 'https://usgs.osn.mghpcc.org'})

    sBasePath = "s3://genoatest/aloarca/wave_dataset/"
    sFullPath = sBasePath + sFileName

    return oFileSystemRead.open(sFullPath)


def extractModelFromFilename(filename):
    for sModel in asMeteOceanModels:
        if sModel in filename:
            return sModel
    return None


def doesBoundingBoxIntersect(dDatasetWest, dDatasetNorth, dDatasetEast, dDatasetSouth, dQueryNorth, dQuerySouth, dQueryEast, dQueryWest):

    # Check if there is any intersection between the two bounding boxes
    oLatitudeOverlap = not (dQuerySouth > dDatasetNorth or dQueryNorth < dDatasetSouth)
    oLongitudeOverlap = not (dQueryWest > dDatasetEast or dQueryEast < dDatasetWest)

    # Return True if both latitude and longitude overlap, indicating intersection
    return oLatitudeOverlap and oLongitudeOverlap


if __name__ == '__main__':
    oServerApp.run(debug=True, host='0.0.0.0')
