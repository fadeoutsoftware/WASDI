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
            oNewObjectOutput[oJSONInput[iIndexParameter].field] = oJSONInput[iIndexParameter].defaultValue;//create
        }
    }
    return oNewObjectOutput;
}