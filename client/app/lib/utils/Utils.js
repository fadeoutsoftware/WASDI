/**
 * Created by a.corrado on 17/11/2016.
 */

function utilsIsStrNullOrEmpty(sString)
{
    if( sString && typeof sString != 'string')
    {
        throw "[Utils.isStrNullOrEmpty] The value is NOT a string";
        return true;
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

function utilsIsANumber(oValue)
{

    //isNaN(123) //false
    //isNaN(-1.23) //false
    //isNaN(5-2) //false
    //isNaN(0) //false
    //isNaN('123') //false
    //isNaN('Hello') //true
    //isNaN('2005/12/12') //true
    //isNaN('') //false
    //isNaN(true) //false
    //isNaN(undefined) //true
    //isNaN('NaN') //true
    //isNaN(NaN) //true
    //isNaN(0 / 0) //true
    if(isNaN(oValue) == false)
        return true;
    return false;
}


/*
* return index of object in array
* return -1 if there are some error or the object isn't inside array
* */
function utilsFindObjectInArray(oArray,oObject)
{
    //ERROR PARAMS == NULL OR UNDEFINED
    if(utilsIsObjectNullOrUndefined(oArray) || utilsIsObjectNullOrUndefined(oObject))
        return -1;
    // 0 ELEMENTS IN ARRAY
    if(oArray.length == 0)
        return -1;

    var iArrayLength = oArray.length;
    for(var iIndex = 0; iIndex < iArrayLength; iIndex ++)
    {
        if(oArray[iIndex] == oObject)
            return iIndex;
    }
    /* the object isn't inside array */
    return -1;
}

function utilsIsString(sString)
{
    if( !sString && typeof sString == 'string')
    {
        return true;
    }
    return false;

}


/*************** LOCAL STORAGE UTILS ********************/
//TODO TEST LOCAL STORAGE FUNCTIONS
function utilsCheckIfBrowserSupportLocalStorage()
{
    if (typeof(Storage) !== "undefined")
    {
        // Code for localStorage/sessionStorage.
        return true;
    }
    else
    {
        // Sorry! No Web Storage support..
        //TODO Error with dialog
        console.log("Error no web storage support");
        return false;
    }

    return false;
}

/*
 * Set local storage, if sName is empty or null return false
 * */
function utilsSetItemLocalStorage (sName,sValue)
{
    if(utilsIsStrNullOrEmpty(sName))
        return false
    localStorage.setItem(sName,sValue);
    return true
}


/* Get local value
 * */
function getItemInLocalStorage (sName)
{
    if(utilsIsStrNullOrEmpty(sName))
        return false;
    //retrieve
    return localStorage.getItem(sName);
}

/* Remove Item
 * */
function removeLocalStorageItem (sName)
{
    if(utilsIsStrNullOrEmpty(sName))
        return false;
    localStorage.removeItem(sName);
}


/************************* COOKIES ***************************/

//TODO TEST COOKIES functions !!
//set by w3school.com
function utilsSetCookie(cname, cvalue, exdays) {
    var d = new Date();
    d.setTime(d.getTime() + (exdays*24*60*60*1000));
    var expires = "expires="+ d.toUTCString();
    ////FOR OBJECT ELEMENT I ADD cvalue=JSON.stringify(cvalue);
    //cvalue=JSON.stringify(cvalue);
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

//get by w3school.com
function utilsGetCookie(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i = 0; i <ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length,c.length);
            //return JSON.parse(c.substring(name.length,c.length));//FOR OBJECT ELEMENT I ADD JSON.parse()
        }
    }
    return "";
}