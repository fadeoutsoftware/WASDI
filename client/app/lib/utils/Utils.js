/**
 * Created by a.corrado on 17/11/2016.
 */
/*
 V 1.0
 V 1.1 (Added utilsGetTimeStamp & utilsBrowserSupportWebGl)
List of methods:

 BOOTSTRAP
 utilsFindBootstrapEnvironment

 COOKIE
 utilsSetCookie
 utilsGetCookie

 LOCAL STORAGE
 utilsCheckIfBrowserSupportLocalStorage
 utilsSetItemLocalStorage
 utilsGetItemInLocalStorage
 utilsRemoveLocalStorageItem

 NUMBER
 utilsIsANumber
 utilsIsInteger

 OBJECT
 utilsIsObjectNullOrUndefined
 utilsFindObjectInArray

 STRING
 utilsIsSubstring
 utilsIsStrNullOrEmpty
 utilsStrContainsCaseInsensitive
 utilsIsString
 utilsIsEmail

 TIMESTAMP
 utilsGetTimeStamp

 WEBGL
 utilsBrowserSupportWebGl
* */

/**
 *
 * @param sString
 * @returns {boolean}
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

/**
 *
 * @param oObject
 * @returns {boolean}
 */
function utilsIsObjectNullOrUndefined(oObject)
{
    if(oObject == null || oObject == undefined )
        return true;
    return false;

}
/**
 *
 * @param sValue
 * @returns {boolean}
 */
function utilsIsEmail(sValue)
{
    if(utilsIsStrNullOrEmpty( sValue) )
        return false;

    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(sValue);
}
/**
 *
 * @param sString1
 * @param sString2
 * @returns {boolean}
 */
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

/**
 *
 * @returns {string}
 */
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
/**
 *
 * @param sSource
 * @param sStrToSearch
 * @returns {boolean}
 */
function utilsStrContainsCaseInsensitive(sSource, sStrToSearch)
{
    if (sSource == null)
        return false;

    var s1 = sSource.toLowerCase();
    var s2 = sStrToSearch.toLowerCase();
    return (s1.indexOf(s2) > -1);
}
/**
 *
 * @param oValue
 * @returns {boolean}
 */
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
/**
 *
 * @param oArray
 * @param oObject
 * @returns {number}
 */
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
/**
 *
 * @param sString
 * @returns {boolean}
 */
function utilsIsString(sString)
{
    if( sString && typeof sString == 'string')
    {
        return true;
    }
    return false;

}

/**
 * isInteger
 * @param oInput
 * @returns {boolean}
 */

function utilsIsInteger(oInput)
{
    if(utilsIsANumber(oInput) == false)
        return false;
    return oInput % 1 === 0;
}
/*************** LOCAL STORAGE UTILS ********************/
//TODO TEST LOCAL STORAGE FUNCTIONS
/**
 *
 * @returns {boolean}
 */
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
/**
 *
 * @param sName
 * @param sValue
 * @returns {boolean}
 */
function utilsSetItemLocalStorage (sName,sValue)
{
    if(utilsIsStrNullOrEmpty(sName))
        return false
    localStorage.setItem(sName,sValue);
    return true
}


/* Get local value
 * */
/**
 *
 * @param sName
 * @returns {boolean}
 */
function utilsGetItemInLocalStorage (sName)
{
    if(utilsIsStrNullOrEmpty(sName))
        return false;
    //retrieve
    return localStorage.getItem(sName);
}

/* Remove Item
 * */
/**
 *
 * @param sName
 * @returns {boolean}
 */
function utilsRemoveLocalStorageItem (sName)
{
    if(utilsIsStrNullOrEmpty(sName))
        return false;
    localStorage.removeItem(sName);
}


/************************* COOKIES ***************************/

//TODO TEST COOKIES functions !!
//set by w3school.com
/**
 *
 * @param cname
 * @param cvalue
 * @param exdays
 */
function utilsSetCookie(cname, cvalue, exdays) {
    var d = new Date();
    d.setTime(d.getTime() + (exdays*24*60*60*1000));
    var expires = "expires="+ d.toUTCString();
    ////FOR OBJECT ELEMENT I ADD cvalue=JSON.stringify(cvalue);
    //cvalue=JSON.stringify(cvalue);
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

//get by w3school.com
/**
 *
 * @param cname
 * @returns {*}Whoops! Lost connection to undefined
 */
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
/************************ TIMESTAMP ************************/
function utilsGetTimeStamp()
{
    // return (new Date().getTime());
    var currentdate = new Date();
    var datetime = "Last Sync: " + currentdate.getDate() + "/"
        + (currentdate.getMonth()+1)  + "/"
        + currentdate.getFullYear() + " @ "
        + currentdate.getHours() + ":"
        + currentdate.getMinutes() + ":"
        + currentdate.getSeconds();
    return datetime;
}

/********************* WEBGL *********************/
function utilsBrowserSupportWebGl(){
    if (window.WebGLRenderingContext)//check if browser supports WebGL
    {
        return true;
    }
    else
    {
        return false;
    }
}