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
