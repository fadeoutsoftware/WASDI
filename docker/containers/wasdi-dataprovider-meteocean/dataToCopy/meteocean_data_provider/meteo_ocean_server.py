import io
import os.path
import fsspec
import ast

from flask import Flask, request, jsonify, Response, send_file
import logging
import xarray as xr
import numpy as np
import ast

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)

oServerApp = Flask(__name__)

@oServerApp.route('/hello', methods=['GET'])
def hello():
    return "hello from the flask server", 200

@oServerApp.route('/query/count', methods=['POST'])
def countResults():
    try:
        aoInputMap = request.get_json()

        # preliminary controls. Check the inputs for validation
        if not isInputValid(aoInputMap):
            logging.warning("countResults. some of the input parameters are not valid")
            return "-1", 400

        # TODO: replace with the access to the bucket
        # sDatasetFolderPath = "C:/Users/valentina.leone/Desktop/WORK/Return/104435/wave_dataset"

        sDatasetName = aoInputMap.get('productType')
        sVariable = aoInputMap.get('productLevel')
        sCase = aoInputMap.get('sensorMode')
        sModel = aoInputMap.get('polarisation')
        bBiasAdjustment = aoInputMap.get('timeliness') # TODO: check if it is a boolean or a string
        dNorth = aoInputMap.get('north')
        dSouth = aoInputMap.get('south')
        dWest = aoInputMap.get('west')
        dEast = aoInputMap.get('east')

        sFileName = getFileName(sDatasetName, sVariable, sCase, bBiasAdjustment, sModel)

        logging.info("countResults. Selected dataset: hindcast")
        # sProductFilePath = sDatasetFolderPath + '/' + sFileName

        sS3Product = readFileFromS3(sFileName)
        oDataset = xr.open_dataset(sS3Product, engine='h5netcdf')
        oDatasetVariableData = oDataset[sVariable]

        if oDatasetVariableData is None:
            return "-1", 500

        if isBoundBoxEmpty(dNorth, dSouth, dWest, dEast):
            return "1", 200

        if isSinglePoint(dNorth, dSouth, dWest, dEast):
            logging.info(f"countResults. bounding box is a single point with latitude {dNorth} and longitude {dWest}")
            oDataPoint = oDatasetVariableData.sel(latitude=dNorth, longitude=dWest, method="nearest")
            if np.any(~np.isnan(oDataPoint)):
                logging.info("countResults. found a value close to the point")
                return "1", 200
            else:
                logging.info("executeAndRetrieve. no data in the selected bounding box")
                return "0", 200
        else:
            oValuesInBoundingBox = selectValuesInBoundingBox(oDatasetVariableData, dNorth, dSouth, dWest, dEast)

            if np.any(~np.isnan(oValuesInBoundingBox)):
                logging.info("countResults. some values in the selected bounding box")
                return "1", 200

            else:
                logging.info("countResults. no data in the selected bounding box")
                return "0", 200

    except Exception as oEx:
        logging.error(f"countResults. exception {oEx}")

    return "-1", 500


@oServerApp.route('/query/list', methods=['POST'])
def executeAndRetrieve():
    aoResults = None

    try:
        aoInputMap = request.get_json()

        # preliminary controls. Check the inputs for validation
        if not isInputValid(aoInputMap):
            logging.warning("executeAndRetrieve. some of the input parameters are not valid")
            return None

        sDatasetName = aoInputMap.get('productType')
        sVariable = aoInputMap.get('productLevel')
        sCase = aoInputMap.get('sensorMode')
        sModel = aoInputMap.get('polarisation')
        bBiasAdjustment = aoInputMap.get('timeliness')  # TODO: check if it is a boolean or a string
        dNorth = aoInputMap.get('north')
        dSouth = aoInputMap.get('south')
        dWest = aoInputMap.get('west')
        dEast = aoInputMap.get('east')

        sFileName = getFileName(sDatasetName, sVariable, sCase, bBiasAdjustment, sModel)

        sS3Product = readFileFromS3(sFileName)
        oDataset = xr.open_dataset(sS3Product, engine='h5netcdf')
        oDatasetVariableData = oDataset[sVariable]

        if oDatasetVariableData is None:
            return None, 500   # TODO: what should I return here?

        if isBoundBoxEmpty(dNorth, dSouth, dWest, dEast):
            dWest, dNorth, dEast, dSouth = getBoundingBoxFromDataset(oDatasetVariableData)
            oResultViewModel = createQueryResultViewModel(sDatasetName, sFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast)
            aoResults = list()
            aoResults.append(oResultViewModel)
        elif isSinglePoint(dNorth, dSouth, dWest, dEast):
            logging.info(f"executeAndRetrieve. bounding box is a single point with latitude {dNorth} and longitude {dWest}")
            oDataPoint = oDatasetVariableData.sel(latitude=dNorth, longitude=dWest, method="nearest")
            if np.any(~np.isnan(oDataPoint)):
                logging.info("executeAndRetrieve. found a value close to the point")
                oResultViewModel = createQueryResultViewModel(sDatasetName, sFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast)
                aoResults = list()
                aoResults.append(oResultViewModel)
            else:
                logging.info("executeAndRetrieve. no data in the selected bounding box")
                aoResults = list()
        else:
            oValuesInBoundingBox = selectValuesInBoundingBox(oDatasetVariableData, dNorth, dSouth, dWest, dEast)

            if np.any(~np.isnan(oValuesInBoundingBox)):
                logging.info("executeAndRetrieve. some values in the selected bounding box")
                dWest, dNorth, dEast, dSouth = getBoundingBoxFromDataset(oValuesInBoundingBox)
                oResultViewModel = createQueryResultViewModel(sDatasetName, sFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast)
                aoResults = list()
                aoResults.append(oResultViewModel)
            else:
                logging.info("executeAndRetrieve. no data in the selected bounding box")
                aoResults = list()
    except Exception as oEx:
        logging.error(f"executeAndRetrieve. Exception {oEx}")
    return aoResults


@oServerApp.route('/download', methods=['GET'])
def download():

    # formato in cui arriva la richiesta:
    # https://hindcast_hs_1979_2005__monthlymax.nc,hs,monthlymax,46.11,29.99999999999947,-5.899999999999977,36.75671199999988
    if not (request.headers.get("x-api-key") == 'super-secret-WASDI-key'):
        return Response(f"Invalid API key", status=401)

    try:
        sFileNameFromRequest = request.args.get('fileName') # f"{sOriginalFileName},{sVariable},{sCase},{dNorth},{dSouth},{dWest},{dEast},downloadFileName;MeteOcean"
        # let's split the file name to get all the information
        asDownloadInformation = sFileNameFromRequest.split(",")

        # nome di base da cui scaricare il file
        sBaseFile = asDownloadInformation[0].replace("https://", "")
        sVariable = asDownloadInformation[1]
        sNorth = asDownloadInformation[3]
        sSouth = asDownloadInformation[4]
        sWest = asDownloadInformation[5]
        sEast = asDownloadInformation[6]
        sDownloadFileName = asDownloadInformation[7]

        if isStringNullOrEmpty(sBaseFile):
            return Response("No base file specified", status=400)

        if isStringNullOrEmpty(sDownloadFileName):
            return Response("No download file name specified", status=400)

        sS3Product = readFileFromS3(sBaseFile)

        if ast.literal_eval(sNorth) is None \
                or ast.literal_eval(sSouth) is None \
                or ast.literal_eval(sWest) is None \
                or ast.literal_eval(sEast) is None:
            # se la bounding box e' vuota, allora prendiamo tutto il file
            oResponse = send_file(sS3Product, as_attachment=True, download_name=sBaseFile)
            oResponse.headers["Content-Disposition"] = f"attachment; filename={sDownloadFileName}"
            return oResponse

        else:
            oDataset = xr.open_dataset(sS3Product, engine='h5netcdf')
            oDatasetVariableData = oDataset[sVariable]
            oValuesInBoundingBox = selectValuesInBoundingBox(oDatasetVariableData, float(sNorth), float(sSouth), float(sWest), float(sEast))
            oNewDataset = oValuesInBoundingBox.to_dataset()

            oBuffer = io.BytesIO()
            oNewDataset.to_netcdf(oBuffer)
            oBuffer = io.BytesIO(oBuffer.getvalue())

            oResponse = send_file(oBuffer, as_attachment=True, download_name=sDownloadFileName)
            oResponse.headers["Content-Disposition"] = f"attachment; filename={sDownloadFileName}"
            return oResponse

    except Exception as oEx:
        logging.error(f"download. Exception {oEx}")
        return Response(f"Error occurred: {str(oEx)}", status=500)

    return Response("Could not find dataset", status=404)


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

    if sDataset in ['rcp85_mid', 'rcp85_end']:
        sModel = aoInputMap.get('polarisation')
        logging.info(f"isInputValid: model {sModel}")
        if isStringNullOrEmpty(sModel):
            logging.warning("isInputValid. model is null or empty")
            return False

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

def createQueryResultViewModel(sDataset, sOriginalFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast):
    sTitle = f"MeteOcean_{sDataset}_{sVariable}_{sCase}_{formatDecimal(dWest)}W_{formatDecimal(dNorth)}N_{formatDecimal(dEast)}E_{formatDecimal(dSouth)}S.nc"
    oResultVM = dict()
    oResultVM['preview'] = None
    oResultVM['title'] = sTitle
    oResultVM['summary'] = ""
    oResultVM['id'] = sTitle
    oResultVM['link'] = f"https://{sOriginalFileName},{sVariable},{sCase},{dNorth},{dSouth},{dWest},{dEast},{sTitle};MeteOcean"
    oResultVM['footprint'] = f"POLYGON (({dWest} {dSouth}, {dWest} {dNorth}, {dEast} {dNorth}, {dEast} {dSouth}, {dWest} {dSouth}))"
    oResultVM['provider'] = "MeteOcean"
    oResultVM['volumeName'] = None
    oResultVM['volumePath'] = None

    sDate = str()
    if sDataset == "hindcast":
        sDate = "1979-01-01T00:00:00.00Z"
    elif sDataset == 'rcp85_mid':
        sDate = "2034-01-01T00:00:00.00Z"
    elif sDataset == 'rcp85_end':
        sDate = '2074-01-01T00:00:00.00Z'

    oProperties = dict()
    oProperties['date'] = sDate
    oProperties["productType"] = sDataset

    oResultVM['properties'] = oProperties


    return oResultVM

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

def getFileName(sDataset, sVariable, sCase, bBiasAdjustment, sModel):
    if sDataset == 'hindcast':
        return f"{sDataset}_{sVariable}_1979_2005__{sCase}.nc"

    sDataBias = None
    if sVariable == 'hs' and bBiasAdjustment:
        sDataBias = 'ba_eqm_month'
    else:
        sDataBias = 'raw'

    if sDataset == 'rcp85_mid':
        return f"{sModel}_{sDataBias}_2034_{sVariable}_2060_{sCase}.nc"

    if sDataset == 'rcp85_end':
        return f"{sModel}_{sDataBias}_2074_{sVariable}_2100_{sCase}.nc"

    return None

def readFileFromS3(sFileName):
    oFileSystemRead = fsspec.filesystem('s3',
                                        anon=True,
                                        skip_instance_cache=True,
                                        client_kwargs={'endpoint_url': 'https://usgs.osn.mghpcc.org'})

    sBasePath = "s3://genoatest/aloarca/wave_dataset/"
    sFullPath = sBasePath + sFileName

    return oFileSystemRead.open(sFullPath)


if __name__ == '__main__':
    oServerApp.run(debug=True)


    """
    oNewDataset = oValuesInBoundingBox.to_dataset()

    oNewDataset.to_netcdf("C:/Users/valentina.leone/Desktop/WORK/Return/subsample.nc", engine="h5netcdf")
    """

