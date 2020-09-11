/**
 * Created by p.campanella on 21/10/2016.
 */

//'use strict';
angular.module('wasdi.ConstantsService', []).
service('ConstantsService', [function () {

    this.COOKIE_EXPIRE_TIME_DAYS = 1;//days

    this.URL = environment.url;
    this.WEBSTOMPURL = environment.webstompUrl;
    this.WMSURL = environment.wmsUrl;
    this.WPSPROXY = environment.wpsProxy;

    this.APIURL = this.URL + 'rest';
    this.AUTHURL = environment.authUrl;

    this.TEST = false; //test global var

    // Logged User
    this.m_oUser = null;

    // Active Workspace
    this.m_oActiveWorkspace = null;

    // Rabbit User
    this.m_sRabbitUser = Secrets.RABBIT_USER;
    this.m_sRabbitPassword = Secrets.RABBIT_PASSWORD;

    this.testMode = function()
    {
        return this.TEST;
    }

    this.isMobile = function() {

        if (navigator.userAgent.match((/Android/i)) ||
            navigator.userAgent.match(/BlackBerry/i) ||
            navigator.userAgent.match(/iPhone|iPad|iPod/i) ||
            navigator.userAgent.match(/Opera Mini/i) ||
            navigator.userAgent.match(/IEMobile/i)
        )
            return true;

        return false;
    }

    this.getRabbitUser = function () {
        return this.m_sRabbitUser;
    }

    this.getRabbitPassword = function () {
        return this.m_sRabbitPassword;
    }

    this.getWPSPROXY = function()
    {
       return  this.WPSPROXY;
    }

    this.getURL = function() {
        return this.URL;
    }

    this.getAPIURL = function() {
        return this.APIURL;
    }

    this.getAUTHURL = function(){
        return this.AUTHURL;
    };

    this.getSessionId = function() {

        if (this.m_oUser != null)
        {
            //get the token
            // var oAccessToken = window.localStorage.access_token;
            // try {
            //     var oDecoded = jwt.verify(oAccessToken);
            // } catch (err){
            //     //the token is no longer valid
            //     if(err[""]=="TokenExpiredError") {
            //         //TODO refresh the token, update both tokens and return the access token
            //     }
            // }
            //decode the token
            //var oDecodedToken = jwt_decode(oAccessToken);

            //TODO  then return the access token
            if (angular.isDefined(this.m_oUser.sessionId))
                return this.m_oUser.sessionId;
        }

        return "";
    };

    this.pad = function (number, length){
        var str = "" + number;
        while (str.length < length) {
            str = '0'+str;
        }
        return str;
    }

    this.getTimezoneOffset  = function () {
        var offset = new Date().getTimezoneOffset()
        offset = ((offset<0? '+':'-')+ // Note the reversed sign!
        this.pad(parseInt(Math.abs(offset/60)), 2)+
        this.pad(Math.abs(offset%60), 2));

        return offset;
    }

    this.getClientIdGoogle=function()
    {
        return Secrets.GOOGLE_ID;
    }

    //-------------------- SET USER---------------------
    this.setUser = function (oUser) {
        this.m_oUser = oUser;
        //set coockie
        this.setCookie("oUser",this.m_oUser,this.COOKIE_EXPIRE_TIME_DAYS);
    }

    //-------------------- GET USER ---------------------
    this.getUser = function () {
        // check if there is the user
        if(utilsIsObjectNullOrUndefined(this.m_oUser) )
        {
            var oUser = this.getCookie("oUser")
            // check if the user was save in cookie
            if( !(utilsIsObjectNullOrUndefined(oUser)) )
                this.m_oUser = oUser;
            else this.m_oUser=null;
        }

        return this.m_oUser;
    }

    this.getUserId = function () {
        // check if there is the user
        if(utilsIsObjectNullOrUndefined(this.m_oUser) )
        {
            return "";
        }

        return this.m_oUser.userId;
    }

    this.isUserLogged = function () {

        if (angular.isUndefined(this.m_oUser) || this.m_oUser == null) return false;

        if (angular.isUndefined(this.m_oUser.userId) || this.m_oUser.userId == null) return false;

        if (this.m_oUser.userId == "") return false;

        return true;
    }

    this.setActiveWorkspace = function (oWorkspace) {
        this.m_oActiveWorkspace = oWorkspace;
    }

    this.getActiveWorkspace = function () {
        return this.m_oActiveWorkspace;
    }

    this.getStompUrl = function () {

        return this.WEBSTOMPURL;
    }

    /**
     * getWasdiGeoserver
     * @returns {string}
     */
    this.getWasdiGeoserverWPS = function()
    {
        return this.WASDIGEOSERVERWPS;
    }
    /*------------- COOKIES --------------*/

    //set by w3school.com
    this.setCookie=function (cname, cvalue, exdays) {
        var d = new Date();
        d.setTime(d.getTime() + (exdays*24*60*60*1000));
        var expires = "expires="+ d.toUTCString();
        //FOR OBJECT ELEMENT I ADD cvalue=JSON.stringify(cvalue);
        cvalue=JSON.stringify(cvalue);
        document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
    }

    //get by w3school.com
    this.getCookie=function (cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for(var i = 0; i <ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0)==' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return JSON.parse(c.substring(name.length,c.length));//FOR OBJECT ELEMENT I ADD JSON.parse()
            }
        }
        return "";
    }

    //delete
    this.deleteCookie= function(cname)
    {
        this.setCookie(cname, "", -1000);
    }
    /**************************************************/
    //LOG-OUT
    this.logOut= function()
    {

        // if(!gapi.auth2){
        //     gapi.load('auth2', function() {
        //         gapi.auth2.init();
        //         gapi.auth2.signOut();
        //     });
        // }
        this.deleteCookie("oUser");
        this.logOutGoogle();

        this.m_oUser = null;
    }

    this.logOutGoogle = function ()
    {
        try
        {
            if (_.isNil(gapi) == false)
            {
                var oController = this;
                if (_.isNil(gapi.auth2) === true)
                {
                    gapi.load('auth2', function () {
                        gapi.auth2.init();
                        oController.googleSignOutAPI();
                    });
                }
                else
                {
                    this.googleSignOutAPI();
                }
            }
            else
            {
                throw "Google API null or undefined, cannot perform logout";
            }
        }catch (e)
        {
            console.error("logOutGoogle(): ", e);
        }
    }

    this.googleSignOutAPI = function()
    {
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
            console.log('User signed out.');
        });
    };


    //GET GEOSERVER
    this.getWmsUrlGeoserver = function()
    {
        return this.WMSURL;
    }
    /* MEMBERS NEED TO USE METHODS*/

    // if m_oUser == null try to load user in cookie, if fail m_oUser == null
    this.m_oUser = this.getUser();


    /***************** LOCAL STORAGE *******************/
    this.checkIfBrowserSupportLocalStorage = function()
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
    this.setItemLocalStorage = function(sName,sValue)
    {
        if(utilsIsStrNullOrEmpty(sName))
            return false
        localStorage.setItem(sName,sValue);
        return true
    }




    /* Get local value
     * */
    this.getItemInLocalStorage = function(sName)
    {
        if(utilsIsStrNullOrEmpty(sName))
            return false;
        //retrieve
        return localStorage.getItem(sName);
    }

    /* Remove Item
    * */
    this.removeLocalStorageItem = function(sName)
    {
        if(utilsIsStrNullOrEmpty(sName))
            return false;
        localStorage.removeItem(sName);
    }
    /**************************************************/

}]);
