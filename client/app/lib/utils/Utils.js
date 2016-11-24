/**
 * Created by a.corrado on 17/11/2016.
 */

function utilsIsStrNullOrEmpty(sString)
{
    if( sString && typeof sString != 'string')
    {
        throw "[Utils.isStrNullOrEmpty] The value is NOT a string";
        //return true;
    }

    if( sString && sString.length > 0)
        return false; // string has content
    else
        return true; // string is empty or null
}

function utilsIsObjectNullOrUndefined(oObject)
{
    if(oObject == null || oObject == undefined )
        return true;
    return false;

}

function utilsIsEmail(sValue)
{
    if(utilsIsStrNullOrEmpty( sValue) )
        return false;

    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(sValue);
}