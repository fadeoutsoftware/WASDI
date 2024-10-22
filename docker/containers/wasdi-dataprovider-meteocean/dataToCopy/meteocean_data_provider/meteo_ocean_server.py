from flask import Flask, request, jsonify
import logging
import xarray as xr
import numpy as np

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)

oServerApp = Flask(__name__)

@oServerApp.route('/hello', methods=['GET'])
def hello():
    return "hello from the flask server", 200

@oServerApp.route('/query/count', methods=['POST'])
def countResults():
    """
    oData = request.get_json()

    if oData is None:
        return jsonify({"error":"No JSON data fount"}), 400

    if not isinstance(oData, dict):
        return jsonify({"error": "Expected a JSON object (map)"}), 400

    # Now 'data' is a Python dictionary
    print(oData)
    return jsonify({"message": "ok"}), 200
    """
    iCount = -1

    aoInputMap = request.get_json()

    # preliminary controls. Check the inputs for validation
    if not isInputValid(aoInputMap):
        logging.warning("countResults. some of the input parameters are not valid")
        return iCount

    # if the inputs are valid, we can proceed to check if there are data in the bounding box specified in the query

    # so far we get the name of the local dataset, but it will need to be replaced with the access to the S3 bucket
    # TODO: replace with the access to the bucket

    sDatasetFolderPath = "C:/Users/valentina.leone/Desktop/WORK/Return/104435/wave_dataset"

    # get the name of the file to access
    # TODO: we try with HINDCAST, then we extend to other models

    sFileName = ""
    sDataset = aoInputMap.get('productType')
    sVariable = aoInputMap.get('productLevel')
    sCase = aoInputMap.get('sensorMode')
    dNorth = aoInputMap.get('north')
    dSouth = aoInputMap.get('south')
    dWest = aoInputMap.get('west')
    dEast = aoInputMap.get('east')

    if sDataset.lower() == 'hindcast':
        logging.info("countResults. Selected dataset: hindcast")
        sFileName += 'hindcast_' + sVariable + '_1979_2005__' + sCase + ".nc"
        sProductFilePath = sDatasetFolderPath + '/' + sFileName
        logging.info("countResults. product path: " + sProductFilePath)

        oHindcastDataset = xr.open_dataset(sProductFilePath, engine='h5netcdf')
        oDatasetVariableData = oHindcastDataset[sVariable]

        bIsInBoundingBox = (oDatasetVariableData['longitude'] >= dWest) & (oDatasetVariableData['longitude'] <= dEast) \
                           & (oDatasetVariableData['latitude'] >= dSouth) & (oDatasetVariableData['latitude'] >= dNorth)

        oValuesInBoundingBox = oDatasetVariableData.where(bIsInBoundingBox, drop=True)

        if np.any(~np.isnan(oValuesInBoundingBox)):
            logging.info("countResults. some values in the selected bounding box")
            return "1", 200
        else:
            logging.info("countResults. no data in the selected bounding box")
            return "0", 200

        """
        oNewDataset = oValuesInBoundingBox.to_dataset()

        oNewDataset.to_netcdf("C:/Users/valentina.leone/Desktop/WORK/Return/subsample.nc", engine="h5netcdf")
        """

@oServerApp.route('/query/list', methods=['POST'])
def executeAndRetrieve():
    aoResults = None

    aoInputMap = request.get_json()

    # preliminary controls. Check the inputs for validation
    if not isInputValid(aoInputMap):
        logging.warning("executeAndRetrieve. some of the input parameters are not valid")
        return None

    # if the inputs are valid, we can proceed to check if there are data in the bounding box specified in the query

    # so far we get the name of the local dataset, but it will need to be replaced with the access to the S3 bucket
    # TODO: replace with the access to the bucket
    sDatasetFolderPath = "C:/Users/valentina.leone/Desktop/WORK/Return/104435/wave_dataset"

    # get the name of the file to access
    # TODO: we try with HINDCAST, then we extend to other models

    sFileName = ""
    sDataset = aoInputMap.get('productType')
    sVariable = aoInputMap.get('productLevel')
    sCase = aoInputMap.get('sensorMode')
    dNorth = aoInputMap.get('north')
    dSouth = aoInputMap.get('south')
    dWest = aoInputMap.get('west')
    dEast = aoInputMap.get('east')

    if sDataset.lower() == 'hindcast':
        logging.info("countResults. Selected dataset: hindcast")
        sFileName += 'hindcast_' + sVariable + '_1979_2005__' + sCase + ".nc"
        sProductFilePath = sDatasetFolderPath + '/' + sFileName
        logging.info("countResults. product path: " + sProductFilePath)

        oHindcastDataset = xr.open_dataset(sProductFilePath, engine='h5netcdf')
        oDatasetVariableData = oHindcastDataset[sVariable]

        bIsInBoundingBox = (oDatasetVariableData['longitude'] >= dWest) \
                           & (oDatasetVariableData['longitude'] <= dEast) \
                           & (oDatasetVariableData['latitude'] >= dSouth) \
                           & (oDatasetVariableData['latitude'] <= dNorth)

        oValuesInBoundingBox = oDatasetVariableData.where(bIsInBoundingBox, drop=True)

        if np.any(~np.isnan(oValuesInBoundingBox)):
            logging.info("executeAndRetrieve. some values in the selected bounding box")
            dSouth = oValuesInBoundingBox['latitude'].min().values
            dNorth = oValuesInBoundingBox['latitude'].max().values
            dWest = oValuesInBoundingBox['longitude'].min().values
            dEast = oValuesInBoundingBox['longitude'].max().values
            oResultViewModel = createQueryResultViewModel(sDataset, sFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast)
            aoResults = list()
            aoResults.append(oResultViewModel)
        else:
            logging.info("executeAndRetrieve. no data in the selected bounding box")
            aoResults = list()
    return aoResults

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


def isBoudningBoxValid(dNorth, dSouth, dWest, dEast):
    # an empty bounding box is valid
    if dNorth is None and dSouth is None and dWest is None and dEast is None:
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


def createQueryResultViewModel(sDataset, sOriginalFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast):
    sTitle = f"{sDataset}_{sVariable}_{sCase}_{formatDecimal(dWest)}W_{formatDecimal(dNorth)}N_{formatDecimal(dEast)}E_{formatDecimal(dSouth)}S.nc"
    oResultVM = dict()
    oResultVM['preview'] = None
    oResultVM['title'] = sTitle
    oResultVM['summary'] = None
    oResultVM['id'] = sTitle
    oResultVM['link'] = f"{sOriginalFileName},{sVariable},{sCase},{dNorth},{dSouth},{dWest},{dEast}"
    oResultVM['footprint'] = f"POLYGON (({dWest} {dSouth}, {dWest} {dNorth}, {dEast} {dNorth}, {dEast} {dSouth}, {dWest} {dSouth}))"
    oResultVM['provider'] = "MeteOcean"
    oResultVM['volumeName'] = None
    oResultVM['volumePath'] = None
    oResultVM['properties'] = None

    return oResultVM

def formatDecimal(dValue):
    sSign = 'P' if dValue >= 0 else 'N'
    dAbsValue = abs(dValue)
    sIntegerPart, sDecimalPart = str(dAbsValue).split('.')
    sIntegerPart = sIntegerPart.zfill(3)
    sDecimalPart = sDecimalPart.ljust(4, '0')
    formatted_value = f"{sSign}{sIntegerPart}.{sDecimalPart}"

    return formatted_value

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
    s
    return None


if __name__ == '__main__':
    oServerApp.run(debug=True)
