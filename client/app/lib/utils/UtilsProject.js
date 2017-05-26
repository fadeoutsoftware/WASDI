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
            next;//return null?
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


                //create
            // if(oJSONInput[iIndexParameter].valueSet.length !== 0)// if it's an array of values, set the default value as first one
            // {
            //     var aMyValueSet = [];
            //  }
            // else
            // {
                // no array
                // oNewObjectOutput[oJSONInput[iIndexParameter].field] = oJSONInput[iIndexParameter].defaultValue;//create
            // }
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

    var oReturnArray = [];
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
//test
String.prototype.distance = function (char) {
    var index = this.indexOf(char);

    if (index === -1) {
        alert(char + " does not appear in " + this);
    } else {
        alert(char + " is " + (this.length - index) + " characters from the end of the string!");
    }
}




function utilsProjectShowRabbitMessageUserFeedBack(oMessage) {

    var sMessageCode = oMessage.messageCode;
    var sUserMessage = "";
    // Get extra operations
    switch(sMessageCode)
    {
        case "DOWNLOAD":
            sUserMessage = "File now available on WASDI Server";
            break;
        case "PUBLISH":
            sUserMessage = "Publish done";
            break;
        case "PUBLISHBAND":
            sUserMessage = "Band published. Product: " + oMessage.payload.productName;
            break;
        case "UPDATEPROCESSES":
            console.log("UPDATE PROCESSES"+" " +utilsGetTimeStamp());
            break;
        case "APPLYORBIT":
            sUserMessage = "Apply orbit Completed";
            break;
        case "CALIBRATE":
            sUserMessage = "Radiometric Calibrate Completed";
            break;
        case "MULTILOOKING":
            sUserMessage = "Multilooking Completed";
            break;
        case "NDVI":
            sUserMessage = "NDVI Completed";
            break;
        case "TERRAIN":
            sUserMessage = "Range doppler terrain correction Completed";
            break;

        default:
            console.log("RABBIT ERROR: got empty message ");
    }

    // Is there a feedback for the user?
    if (!utilsIsStrNullOrEmpty(sUserMessage)) {
        // Give the short message
        var oDialog = utilsVexDialogAlertBottomRightCorner(sUserMessage);
        utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
    }

}
