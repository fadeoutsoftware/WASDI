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

//sString1 contains sString2
function utilsIsSubstring(sString1,sString2)
{
    if(utilsIsStrNullOrEmpty(sString1) || utilsIsStrNullOrEmpty(sString2))
    {
        console.log("Error, Invalid String ")
    }

    if(sString1.indexOf(sString2) == -1)
        return false;
    return true;
}


function utilsFindBootstrapEnvironment()
{
    var envs = ["xs","sm","md","lg"];//extra small,small,medium,large screen
    var $el = $('#screen-size-helper');
    for (var i = envs.length - 1; i >= 0; i--) {
        var env = envs[i];

        $el.addClass('hidden-'+env);
        var bIsElemHidden = $el.is(':hidden');
        $el.removeClass('hidden-'+env);

        if (bIsElemHidden == true) {
            return env;
        }
    }
}

function utilsStrContainsCaseInsensitive(sSource, sStrToSearch)
{
    if (sSource == null)
        return false;

    var s1 = sSource.toLowerCase();
    var s2 = sStrToSearch.toLowerCase();
    return (s1.indexOf(s2) > -1);
}