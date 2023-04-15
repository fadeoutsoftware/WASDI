/**
 * Created by p.campanella on 21/10/2016.
 */

//'use strict';
angular.module('wasdi.ConstantsService', []).
service('ConstantsService', [function () {

    /**
     * Cookie expire time in days
     * @type {number}
     */
    this.COOKIE_EXPIRE_TIME_DAYS = 1;

    /**
     * Main Server url
     * @type {string}
     */
    this.URL = environment.url;

    /**
     * Web Stomp url
     * @type {string}
     */
    this.WEBSTOMPURL = environment.webstompUrl;

    /**
     * WMS URL
     * @type {string}
     */
    this.WMSURL = environment.wmsUrl;

    /**
     * API URL
     * @type {string}
     */
    this.APIURL = this.URL + 'rest';
    this.AUTHURL = environment.authUrl;

    /**
     * BASE URL
     *
     */
    this.BASEURL = environment.baseurl;
    /**
     * Flag to ignore the workspace's ApiUrl and use the main server's Url
     * @type {boolean}
     */
     this.m_bIgnoreWorkspaceApiUrl = environment.ignoreWorkspaceApiUrl;

    /**
     * Logged User
     * @type {null}
     */
    this.m_oUser = null;

    /**
     * User's active subscriptions
     * 
     */
    this.m_aoActiveSubscriptions = []; 

    /**
     * User Projects List
     * @type {*}
     */
    this.m_aoUserProjects = []; 

    /**
     * User's active project
     * @type {*}
     */
    this.m_oActiveProject = null;

    /**
     * User Account Type
     * @type {string}
     */
    this.m_sAccountType = ""; 

    /**
     * Active Workspace
     * @type {null}
     */
    this.m_oActiveWorkspace = null;

    /**
     * Rabbit User
     * @type {string}
     */
    this.m_sRabbitUser = Secrets.RABBIT_USER;
    /**
     * Rabbit Password
     * @type {string}
     */
    this.m_sRabbitPassword = Secrets.RABBIT_PASSWORD;

    /**
     * Active Wasdi Application
     * @type {string}
     */
    this.m_sSelectedApplication = "";

    /**
     * Test if we are on a mobile device or not
     * @returns {boolean}
     */
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

    /**
     * Get Rabbit User
     * @returns {string}
     */
    this.getRabbitUser = function () {
        return this.m_sRabbitUser;
    }

    /**
     * Get Rabbit Password
     * @returns {string}
     */
    this.getRabbitPassword = function () {
        return this.m_sRabbitPassword;
    }

    /**
     * Get Main server url
     * @returns {*}
     */
    this.getURL = function() {
        return this.URL;
    }

    /**
     * Get API Url
     * @returns {*}
     */
    this.getAPIURL = function() {
        return this.APIURL;
    }

    this.getAUTHURL = function(){
        return this.AUTHURL;
    };

    /**
     * Get flag ignore workspace's Api Url
     * @returns {boolean}
     */
     this.getIgnoreWorkspaceApiUrl = function() {
        return this.m_bIgnoreWorkspaceApiUrl === true;
    }

    /**
     * Get session id (empty means no logged user)
     * @returns {string|string}
     */
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

    /**
     * Pad a number with zeroes
     * @param number
     * @param length
     * @returns {string}
     */
    this.pad = function (number, length){
        var str = "" + number;
        while (str.length < length) {
            str = '0'+str;
        }
        return str;
    }

    /**
     * Get local timezone offset
     * @returns {string}
     */
    this.getTimezoneOffset  = function () {
        var offset = new Date().getTimezoneOffset()
        offset = ((offset<0? '+':'-')+ // Note the reversed sign!
        this.pad(parseInt(Math.abs(offset/60)), 2)+
        this.pad(Math.abs(offset%60), 2));

        return offset;
    }

    /**
     * Get the google client id
     * @returns {string}
     */
    /*
    this.getClientIdGoogle=function()
    {
        return Secrets.GOOGLE_ID;
    }
    */
    /**
     * Set active user
     * @param oUser
     */
    this.setUser = function (oUser) {
        this.m_oUser = oUser;
        //set coockie
        this.setCookie("oUser",this.m_oUser,this.COOKIE_EXPIRE_TIME_DAYS);
    };

    /**
     * Get Active user
     * @returns {*}
     */
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

        //console.log("ConstantsService.getUser | this.m_oUser: ", this.m_oUser);

        return this.m_oUser;
    };

    /**
     * Get active user id or empty string
     * @returns {string|string|HomeController.login.m_sUserName|HomeController.signingUser.m_oRegistrationUser.userId}
     */
    this.getUserId = function () {
        // check if there is the user
        if(utilsIsObjectNullOrUndefined(this.m_oUser) )
        {
            return "";
        }

        return this.m_oUser.userId;
    };

    /**
     * Check if the user is logged or not
     * @returns {boolean}
     */
    this.isUserLogged = function () {

        if (angular.isUndefined(this.m_oUser) || this.m_oUser == null) return false;

        if (angular.isUndefined(this.m_oUser.userId) || this.m_oUser.userId == null) return false;

        if (this.m_oUser.userId == "") return false;

        return true;
    };
    /**
     * Return array of user's active subscriptions
     * @returns  {aoSubscriptions}
     */
    this.getActiveSubscriptions = function () {
        return this.m_aoActiveSubscriptions;
    }

    /**
     * Set the user's active subscriptions
     * @param {sActiveSubscriptions}
     */
    this.setActiveSubscriptions = function (asSubscriptions) {
        this.m_aoActiveSubscriptions = asSubscriptions; 
    }

    /**
     * Return the array of user's active projects
     * @returns {aoProjects}
     */
    this.getUserProjects = function() {
        return this.m_aoUserProjects; 
    }

    /**
     * Set the user projects array
     * @param {*} aoProjects 
     */
    this.setUserProjects = function(aoProjects) {
        this.m_aoUserProjects = aoProjects; 
    }

    /**
     * Set the user's active project
     * @param {*} oProject
     */
    this.setActiveProject = function(oProject) {
        this.m_oActiveProject = oProject
    }

    /**
     * Returns the user's active project
     * @param {*} 
     * @
     */
    this.getActiveProject = function() {
        return this.m_oActiveProject; 
    }

    /**
     * Set Active Workspace Object
     * @param oWorkspace
     */
    this.setActiveWorkspace = function (oWorkspace) {
        this.m_oActiveWorkspace = oWorkspace;
    };

    /**
     * Get Active Workpsace Object
     * @returns {null}
     */
    this.getActiveWorkspace = function () {
        return this.m_oActiveWorkspace;
    };

    /**
     * Set the name of the processor selected in the store
     * @param sProcessorName
     */
    this.setSelectedApplication = function (sProcessorName) {
        this.m_sSelectedApplication = sProcessorName;
    }

    /**
     * Get the name of the processor selected in the store
     * @returns {string}
     */
    this.getSelectedApplication = function () {
        return this.m_sSelectedApplication;
    }

    /**
     * Set the Id of the review selected in the store
     * @param sReviewId
     */
    this.setSelectedReviewId = function (sReviewId) {
        this.m_sSelectedReviewId = sReviewId;
    }

    /**
     * Get the Id of the review selected in the store
     * @returns {string}
     */
    this.getSelectedReviewId = function () {
        return this.m_sSelectedReviewId;
    }

    /**
     * Set the review selected in the store
     * @param oReview
     */
    this.setSelectedReview = function (oReview) {
        this.m_oSelectedReview = oReview;
    }

    /**
     * Get the review selected in the store
     * @returns {*}
     */
    this.getSelectedReview = function () {
        return this.m_oSelectedReview;
    }

    /**
     * Set the comment selected in the store
     * @param oComment
     */
    this.setSelectedComment = function (oComment) {
        this.m_oSelectedComment = oComment;
    }

    /**
     * Get the comment selected in the store
     * @returns {*}
     */
    this.getSelectedComment = function () {
        return this.m_oSelectedComment;
    }

    /**
     * Get the STOMP URL
     * @returns {string}
     */
    this.getStompUrl = function () {

        return this.WEBSTOMPURL;
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

    /**
     * LOG-OUT
     */
    this.logOut= function()
    {
        this.deleteCookie("oUser");
        this.m_oUser = null;
    }

    /**
     * Logout from google
     */
    /*
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
    */
    /**
     * Goggle sign out
     */
    /*
    this.googleSignOutAPI = function()
    {
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
            console.log('User signed out.');
        });
    };
    */

    /**
     * Get WASDI OGC WMS Server address
     * @returns {string}
     */
    this.getWmsUrlGeoserver = function()
    {
        return this.WMSURL;
    }

    /* MEMBERS NEED TO USE METHODS*/
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


    };
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
