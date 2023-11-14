/**
 * Created by a.corrado on 21/04/2017.
 */


/*
    this method take in input a JSON with this format:
 [
 {
 "alias": "",
 "condition": "",
 "defaultValue": "Sentinel Precise (Auto Download)",
 "description": "",
 "field": "orbitType",
 "format": "",
 "interval": "",
 "itemAlias": "",
 "label": "Orbit State Vectors",
 "notEmpty": false,
 "notNull": false,
 "pattern": "",
 "unit": "",
 "valueSet": [
 "Sentinel Precise (Auto Download)",
 "Sentinel Restituted (Auto Download)",
 "DORIS Preliminary POR (ENVISAT)",
 "DORIS Precise VOR (ENVISAT) (Auto Download)",
 "DELFT Precise (ENVISAT, ERS1&2) (Auto Download)",
 "PRARE Precise (ERS1&2) (Auto Download)",
 "Kompsat5 Precise"
 ]
 },
 {
 "alias": "",
 "condition": "",
 "defaultValue": "3",
 "description": "",
 "field": "polyDegree",
 "format": "",
 "interval": "",
 "itemAlias": "",
 "label": "Polynomial Degree",
 "notEmpty": false,
 "notNull": false,
 "pattern": "",
 "unit": "",
 "valueSet": []
 },
 {
 "alias": "",
 "condition": "",
 "defaultValue": "false",
 "description": "",
 "field": "continueOnFail",
 "format": "",
 "interval": "",
 "itemAlias": "",
 "label": "Do not fail if new orbit file is not found",
 "notEmpty": false,
 "notNull": false,
 "pattern": "",
 "unit": "",
 "valueSet": []
 }
 ]

 i must convert it in this format
    {
    orbitType:"Sentinel Precise (Auto Download)"
    polyDegree:3,
    continueOnFail:false,
    }
 */
function utilsProjectConvertJSONFromServerInOptions(oJSONInput)
{
    if(utilsIsObjectNullOrUndefined(oJSONInput) == true)
        return null;
    var iNumberOfParameters = oJSONInput.length;
    if( iNumberOfParameters == 0)
        return null;

    var oNewObjectOutput = {};
    for(var iIndexParameter = 0; iIndexParameter < iNumberOfParameters; iIndexParameter++ )
    {
        if( ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter]) == true ) || ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter].field) == true ) ||
            ( utilsIsStrNullOrEmpty(oJSONInput[iIndexParameter].field) == true ) || ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter].defaultValue) == true ) )
        {
            continue;
        }
        else
        {
            switch(oJSONInput[iIndexParameter].defaultValue)
            {
                case "true":oNewObjectOutput[oJSONInput[iIndexParameter].field] = true;//convert string in boolean
                            break;
                case "false":oNewObjectOutput[oJSONInput[iIndexParameter].field] = false;//convert string in boolean
                            break;
                default: oNewObjectOutput[oJSONInput[iIndexParameter].field] = oJSONInput[iIndexParameter].defaultValue;
            }


        }
    }
    return oNewObjectOutput;
}

function utilsProjectGetArrayOfValuesForParameterInOperation(oJSONInput,sProperty)
{
    if( utilsIsObjectNullOrUndefined(oJSONInput) === true ) {
        return null;
    }
    var iNumberOfParameters = oJSONInput.length;
    if( iNumberOfParameters === 0)
        return null;
    if(utilsIsObjectNullOrUndefined(sProperty) === true)
        return null;


    for(var iIndexParameter = 0; iIndexParameter < iNumberOfParameters; iIndexParameter++ )
    {
        // if field === sProperty
        if( ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter]) === false )&&(utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter].field) === false)
                                                                                 &&(oJSONInput[iIndexParameter].field === sProperty) )
        {

            if(oJSONInput[iIndexParameter].valueSet.length !== 0)
            {
                // if valueSet isn't empty
                return oJSONInput[iIndexParameter].valueSet;
            }
        }
    }
}

function utilsProjectShowRabbitMessageUserFeedBack(oMessage, oTranslate) {

    var sMessageCode = oMessage.messageCode;
    var sUserMessage = "";
    // Get extra operations
    switch(sMessageCode)
    {
        case "DOWNLOAD":
            sUserMessage = oTranslate.instant("MSG_DOWNLOAD");
            break;
        case "PUBLISHBAND":
            sUserMessage = oTranslate.instant("MSG_PUBLISHBAND_1") + oMessage.payload.bandName + oTranslate.instant("MSG_PUBLISHBAND_2") + oMessage.payload.productName + oTranslate.instant("MSG_MSG_PUBLISHBAND_3");
            break;
        case "UPDATEPROCESSES":
            console.log("UPDATE PROCESSES"+" " +utilsGetTimeStamp());
            break;
        case "MOSAIC":
            sUserMessage = oTranslate.instant("MSG_MOSAIC");
            break;
        case "SUBSET":
            sUserMessage = oTranslate.instant("MSG_SUBSET");
            break;
        case "MULTISUBSET":
            sUserMessage = oTranslate.instant("MSG_MULTISUBSET");
            break;
        case "GRAPH":
            sUserMessage = oTranslate.instant("MSG_GRAPH");
            break;
        case "RUNPROCESSOR":
            sUserMessage = oTranslate.instant("MSG_RUNPROCESSOR");
            break;
        case "RUNIDL":
            sUserMessage = oTranslate.instant("MSG_RUNPROCESSOR");
            break;
        case "RUNMATLAB":
            sUserMessage = oTranslate.instant("MSG_RUNPROCESSOR");
            break;
        case "FTPUPLOAD":
            sUserMessage = oTranslate.instant("MSG_FTPUPLOAD");
            break;
        case "RASTERGEOMETRICRESAMPLE":
            sUserMessage = oTranslate.instant("MSG_RASTERGEOMETRICRESAMPLE");
            break;
        case "FILTER":
            sUserMessage = oTranslate.instant("MSG_FILTER");
            break;
        case "REGRID":
            sUserMessage = oTranslate.instant("MSG_REGRID");
            break;
        case "DEPLOYPROCESSOR":
            sUserMessage = oTranslate.instant("MSG_DEPLOYPROCESSOR");
            break;
        case "DELETEPROCESSOR":
            sUserMessage = oTranslate.instant("MSG_DELETEPROCESSOR");
            break;
        case "INFO":
        case "ENVIRONMENTUPDATE":
        case "DELETEPROCESSOR":
            sUserMessage =  oMessage.payload;
            break;
        case "REDEPLOYPROCESSOR":
            sUserMessage = oTranslate.instant("MSG_REDEPLOYPROCESSOR");
            break;
        case "LIBRARYUPDATE":
            sUserMessage = oTranslate.instant("MSG_LIBRARYUPDATE");
            break;
        case "KILLPROCESSTREE":
            sUserMessage = oTranslate.instant("MSG_KILLPROCESSTREE");
            break;
        case "READMETADATA":
            sUserMessage = oTranslate.instant("MSG_READMETADATA");
            break;
        case "TERMINATEJUPYTERNOTEBOOK":
        case "LAUNCHJUPYTERNOTEBOOK":
            sUserMessage = "";
        default:
            console.log("ERROR: GOT EMPTY MESSAGE<br>READY");
    }

    // Is there a feedback for the user?
    if (!utilsIsStrNullOrEmpty(sUserMessage)) {
        var oAudio = new Audio('assets/audio/message.wav');
        oAudio.play();

        // Give the short message
        var oDialog = utilsVexDialogAlertBottomRightCorner(sUserMessage);
        utilsVexCloseDialogAfter(6000,oDialog);
    }

}

/**
 * Updates the label of a node of a tree
 * @param {*} sIdNodeInput 
 * @param {*} sNewLabelNodeInput 
 */
function utilsJstreeUpdateLabelNode (sIdNodeInput, sNewLabelNodeInput)
{
    if(utilsIsObjectNullOrUndefined(sIdNodeInput) === true)return false;

    if(utilsIsObjectNullOrUndefined(sNewLabelNodeInput) === true)return false;

    var oNode = null;
    oNode = $('#jstree').jstree(true).get_node(sIdNodeInput);
    $('#jstree').jstree(true).rename_node(oNode,sNewLabelNodeInput);

    return true;
}

function utilsProjectConvertPositionsSatelliteFromServerInCesiumArray(aaArrayInput)
{
    if(utilsIsObjectNullOrUndefined(aaArrayInput) === true)
        return [];
    if(aaArrayInput.length === 0 )
        return [];

    var iLengthArray = aaArrayInput.length;
    var aReturnArray = [];
    var aTemp = [];
    for( var iIndexArray = 0; iIndexArray < iLengthArray; iIndexArray++)
    {
        aTemp = aaArrayInput[iIndexArray].split(";");

        // skip if aTemp is wrong
        if(utilsIsObjectNullOrUndefined(aTemp)=== true ||  aTemp.length !== 4)
            continue;

        aReturnArray.push(aTemp[1]);//push log
        aReturnArray.push(aTemp[0]);//push lat
        aReturnArray.push(aTemp[2]);//push alt
    }

    return aReturnArray;
}

function utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(sInput)
{
    if(utilsIsStrNullOrEmpty(sInput) === true)
        return [];

    var aSplitedInput = sInput.split(";")
    var aReturnValue = [];
    aReturnValue.push(aSplitedInput[1]);
    aReturnValue.push(aSplitedInput[0]);
    aReturnValue.push(aSplitedInput[2]);
    return aReturnValue;
}

function utilsProjectCheckInDialogIfProductNameIsInUsed(sProductName, aoListOfProducts)
{
    if(aoListOfProducts === null || sProductName === null )
        return false;
    var iNumberOfProducts = aoListOfProducts.length;
    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
    {
        if(aoListOfProducts[iIndexProduct].name === sProductName)
            return true;
    }
    return false;
}

function utilsProjectGetSelectedBandsByProductName(sProductName, asSelectedBands)
{
    if(utilsIsObjectNullOrUndefined(asSelectedBands) === true)
        return null;

    var iNumberOfSelectedBands = asSelectedBands.length;
    var asReturnBandsName = [];
    for( var iIndexSelectedBand = 0 ; iIndexSelectedBand < iNumberOfSelectedBands; iIndexSelectedBand++ )
    {
        //check if the asSelectedBands[iIndexSelectedBand] is a sProductName band
        if(utilsIsSubstring(asSelectedBands[iIndexSelectedBand] , sProductName))
        {
            var sBandName=  asSelectedBands[iIndexSelectedBand].replace(sProductName + "_","");
            asReturnBandsName.push(sBandName);
        }
    }

    return asReturnBandsName;
}


function utilsProjectGetProductByName(sName,aoProducts){
    if(utilsIsStrNullOrEmpty(sName) === true)
        return null;
    var iNumberOfProducts = aoProducts.length;

    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
    {
        if( aoProducts[iIndexProduct].name === sName)
        {
            return aoProducts[iIndexProduct];
        }
    }
    return null;
}

function utilsProjectGetBandsFromSelectedProducts(asSelectedProducts,aoProducts)
{
    if( utilsIsObjectNullOrUndefined(asSelectedProducts) === true)
        return null;
    var iNumberOfSelectedProducts = asSelectedProducts.length;
    var asProductsBands=[];
    for(var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfSelectedProducts; iIndexSelectedProduct++)
    {
        var oProduct = utilsProjectGetProductByName(asSelectedProducts[iIndexSelectedProduct],aoProducts);
        var iNumberOfBands;

        if(utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands) === true)
            iNumberOfBands = 0;
        else
            iNumberOfBands = oProduct.bandsGroups.bands.length;

        for(var iIndexBand = 0; iIndexBand < iNumberOfBands; iIndexBand++)
        {
            if( utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands[iIndexBand]) === false )
            {
                asProductsBands.push(oProduct.name + "_" + oProduct.bandsGroups.bands[iIndexBand].name);
            }
        }
    }
    return asProductsBands;
    // return ["test","secondo"];
}


function utilsProjectGetProductsName (aoProducts)
{
    if(utilsIsObjectNullOrUndefined(aoProducts) === true)
    {
        return []
    }
    var iNumberOfProducts = aoProducts.length;
    var asReturnValue = [];
    for( var iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts; iIndexProduct ++ )
    {
        asReturnValue.push(aoProducts[iIndexProduct].name);
    }
    return asReturnValue;
}
/****************************************************** MODALS ******************************************************/

/**
 *
 * @param oCallback
 * @param oOptions
 * @param sTemplateUrl
 * @param sControllerName
 * @returns {boolean}
 */
function utilsProjectOpenDialog(oCallback,oOptions,sTemplateUrl,sControllerName,oModalService)
{
    /*
    Example of options
    {
        products:oController.m_aoProducts,
        selectedProduct:oSelectedProduct
    }
    * */
    if(utilsIsStrNullOrEmpty(sTemplateUrl) === true || utilsIsStrNullOrEmpty(sControllerName) || utilsIsObjectNullOrUndefined(oModalService))
    {
        return false;
    }

    oModalService.showModal({
        templateUrl: sTemplateUrl,
        controller: sControllerName,
        inputs: {
            extras: oOptions
        }
    }).then(function(modal){
        modal.element.modal();
        modal.close.then(oCallback)
    });
    return true;
}

/**
 *
 * @param oCallback
 * @param oOptions
 * @returns {boolean}
 */
function utilsProjectOpenGetListOfWorkspacesSelectedModal (oCallback,oOptions,oModalService)
{
    if(utilsIsObjectNullOrUndefined(oCallback) === true || utilsIsObjectNullOrUndefined(oModalService))
    {
        return false;
    }

    utilsProjectOpenDialog(oCallback,oOptions,"dialogs/get_list_of_workspace_selected/GetListOfWorkspacesSelectedView.html","GetListOfWorkspacesController",oModalService)

    return true;
}

/**
 *
 * @param sFileName
 * @param sBandName
 * @param sFilters
 * @param iRectangleX
 * @param iRectangleY
 * @param iRectangleWidth
 * @param iRectangleHeight
 * @param iOutputWidth
 * @param iOutputHeight
 * @returns {{productFileName: *, bandName: *, filterVM: *, vp_x: *, vp_y: *, vp_w: *, vp_h: *, img_w: *, img_h: *}}
 */
function utilsProjectCreateBodyForProcessingBandImage(sFileName, sBandName, sFilters, iRectangleX, iRectangleY, iRectangleWidth, iRectangleHeight, iOutputWidth, iOutputHeight){

    var oBandImageBody = {
        "productFileName": sFileName,
        "bandName": sBandName,
        "filterVM": sFilters,
        "vp_x": iRectangleX,
        "vp_y": iRectangleY,
        "vp_w": iRectangleWidth,
        "vp_h": iRectangleHeight,
        "img_w": iOutputWidth,
        "img_h": iOutputHeight
    };

    return oBandImageBody;
};

/**
 * getMapContainerSize
 * @returns {{height: number, width: number}}
 */
function utilsProjectGetMapContainerSize()
{
    return utilsProjectGetContainerSize(('#mapcontainer'));
}

/**
 *
 * @returns {{height, width}}
 */
function utilsProjectGetPreviewContainerSize()
{

    return utilsProjectGetContainerSize(('#panelBodyMapPreviewEditor'));
}

/**
 * utilsProjectGetContainerSize
 * @param sIdContainer
 * @returns {*}
 */
function utilsProjectGetContainerSize(sIdContainer){
    if(utilsIsStrNullOrEmpty(sIdContainer) === true)
    {
        return null;
    }

    try {
        var elementMapContainer = angular.element(document.querySelector(sIdContainer));
        var heightMapContainer = elementMapContainer[0].offsetHeight;
        var widthMapContainer = elementMapContainer[0].offsetWidth;
    
        return {
            height:heightMapContainer,
            width:widthMapContainer
        };
    }
    catch (error) {
        return null;
    }

}

function utilsProjectGetPolygonArray(sPolygonString){
    if(utilsIsStrNullOrEmpty(sPolygonString) === true)
    {
        return null;
    }

    var sTemp = sPolygonString;
    sTemp = sTemp.replace("POLYGON","");
    sTemp = sTemp.replace("((","");
    sTemp = sTemp.replace("))","");
    sTemp = sTemp.split(",");

    return sTemp;
}

function utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
{
    if(utilsIsObjectNullOrUndefined(aoProduct) === true)
    {
        return [];
    }
    var iNumberOfProducts = aoProduct.length;
    var aoReturnValue=[];
    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
    {

        var oValue = {
            name:aoProduct[iIndexProduct].name,
            id:aoProduct[iIndexProduct].fileName
        };
        aoReturnValue.push(oValue);
    }

    return aoReturnValue;
}

function utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct){
    if(utilsIsObjectNullOrUndefined(aoProduct) === true)
    {
        return [];
    }
    var iNumberOfProducts = aoProduct.length;
    var oReturnValue={};
    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
    {
        if( oSelectedProduct.name === aoProduct[iIndexProduct].name )
        {
            oReturnValue = aoProduct[iIndexProduct];
            break;
        }

    }
    return oReturnValue;
}

/**
 * Converts the WASDI Operation Code in a more user friendly description
 * @param {Object} oOperation 
 */
function utilsConvertOperationToDescription(oOperation) {
    var sOperation = oOperation.operationType;
    var sDescription = oOperation.operationType;

    var sSubType = "";
    
    if (utilsIsStrNullOrEmpty(oOperation.operationSubType)==false) {
        sSubType = " - " + oOperation.operationSubType;
    }

    if (sOperation == "RUNPROCESSOR") {
        sDescription = "APP"
    }
    else if (sOperation == "RUNIDL") {
        sDescription = "APP"
    }
    else if (sOperation == "RUNMATLAB") {
        sDescription = "APP"
    }
    else if (sOperation == "INGEST") {
        sDescription = "INGEST"
    }
    else if (sOperation == "DOWNLOAD") {
        sDescription = "FETCH"
    }
    else if (sOperation == "PUBLISHBAND") {
        sDescription = "PUBLISH"
    }
    else if (sOperation == "GRAPH") {
        sDescription = "WORKFLOW"
    }
    else if (sOperation == "DEPLOYPROCESSOR") {
        sDescription = "DEPLOY"
    }

    sDescription = sDescription + sSubType;

    return sDescription;
}

