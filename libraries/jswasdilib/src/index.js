fetch = require('node-fetch');


'use strict';

class Wasdi {
    _m_sUser;
    _m_sPassword;

    _m_sActiveWorkspace;
    _m_sWorkspaceOwner;
    _m_sWorkspaceBaseUrl;

    _m_sParametersFilePath;
    _m_sSessionId;
    _m_bValidSession;
    _m_sBasePath;

    _m_bDownloadActive;
    _m_bUploadActive;
    _m_bVerbose;
    _m_aoParamsDictionary = {};

    _m_sMyProcId;
    _m_sBaseUrl = 'https://www.wasdi.net/wasdiwebserver/rest';
    _m_bIsOnServer;
    _m_iRequestsTimeout = 2 * 60;
    // handled through field instead of local variables
    _m_sWorkspaceName = '';
    _m_sWorkspaceId = '';

    constructor() {
        this._m_sUser = undefined;
        this._m_sPassword = undefined;

        this._m_sActiveWorkspace = undefined;
        this._m_sWorkspaceOwner = '';
        this._m_sWorkspaceBaseUrl = '';

        this._m_sParametersFilePath = undefined;
        this._m_sSessionId = '';
        this._m_bValidSession = false;
        this._m_sBasePath = undefined;

        this._m_bDownloadActive = true;
        this._m_bUploadActive = true;
        this._m_bVerbose = true;
        this._m_aoParamsDictionary = {};

        this._m_sMyProcId = '';
        this._m_sBaseUrl = 'https://www.wasdi.net/wasdiwebserver/rest';
        this._m_bIsOnServer = false;
        this._m_iRequestsTimeout = 2 * 60;

        this._m_sWorkspaceName = '';
        this._m_sWorkspaceId = '';
    }

    /**
     * Print status utility
     */
    printStatus() {
        console.log('');
        console.log('[INFO] jswasdilib.printStatus: user: ' + this.User);
        console.log('[INFO] jswasdilib.printStatus: password: ***********');
        console.log('[INFO] jswasdilib.printStatus: session id: ' + this.SessionId);
        console.log('[INFO] jswasdilib.printStatus: active workspace: ' + this.ActiveWorkspaceId);
        console.log('[INFO] jswasdilib.printStatus: workspace owner: ' + this.m_sWorkspaceOwner);
        console.log('[INFO] jswasdilib.printStatus: parameters file path: ' + this.ParametersFilePath);
        console.log('[INFO] jswasdilib.printStatus: base path: ' + this.BasePath);
        console.log('[INFO] jswasdilib.printStatus: download active: ' + this.DownloadActive);
        console.log('[INFO] jswasdilib.printStatus: upload active: ' + this.UploadActive);
        console.log('[INFO] jswasdilib.printStatus: verbose: ' + this.Verbose);
        console.log('[INFO] jswasdilib.printStatus: param dict: ' + this.ParametersDict);
        console.log('[INFO] jswasdilib.printStatus: proc id: ' + this.ProcId);
        console.log('[INFO] jswasdilib.printStatus: base url: ' + this.BaseUrl);
        console.log('[INFO] jswasdilib.printStatus: is on server: ' + this.IsOnServer);
        console.log('[INFO] jswasdilib.printStatus: workspace base url: ' + this.WorkspaceBaseUrl);

        if (this.ValidSession) {
            console.log('[INFO] jswasdilib.printStatus: session is valid :-)');
        } else {
            console.log('[ERROR] jswasdilib.printStatus: session is not valid :-(' +
                '  ******************************************************************************');
        }

    }

    /**
     * Test method to check wasdi instance, with a tiny bit of developer's traditions
     */
    helloWasdiWorld() {
        let response;
        var requestOptions = {
            method: 'GET',
            redirect: 'follow'
        };

        fetch("https://www.wasdi.net/wasdiwebserver/rest/wasdi/hello", requestOptions)
            .then(response => response.text())
            .then(result => console.log(result))
            .catch(error => console.log('error', error));
    }

    /**
     * Api call for the login to Wasdi services
     * @param sUserName The username, corresponding to the e-mail used during registration
     * @param sPassword The selected password
     */
    login(sUserName, sPassword) {
        /*var myHeaders = new fetch.Headers();
        myHeaders.append("Content-Type", "application/x-www-form-urlencoded");*/

        var urlencoded = new URLSearchParams();
        urlencoded.append("client_id", "wasdi_client");
        urlencoded.append("grant_type", "password");
        urlencoded.append("username", sUserName);
        urlencoded.append("password", sPassword);

        var requestOptions = {
            method: 'POST',
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: urlencoded,
            redirect: 'follow'
        };

        fetch("https://www.wasdi.net/auth/realms/wasdi/protocol/openid-connect/token", requestOptions)
            .then(response => response.text())
            .then(result => console.log(result))
            .catch(error => console.log('error', error));
    }

    /**
     * Asyncronous call to load and initialize base parameters of the library
     * @param filename
     * @returns {Promise<void>}
     */
    async loadConfig(filename) {

        let promise = fetch(filename)
            .then(response => {
                return response.json();
            })
            .then(jsondata => {
                // async, so must init here

                this._m_sUser = jsondata.USER;
                this._m_sPassword = jsondata.PASSWORD;
                this._m_sWorkspaceName = jsondata.WORKSPACE;
                    // suppose that, at least, user and password are set
                    return(this._m_sUser != undefined && this._m_sPassword != undefined);
            });
        let result = await promise;
        return result;
        /*
            global m_sUser
   global m_sPassword
   global m_sParametersFilePath
   global m_sSessionId
   global m_sBasePath

   global m_bDownloadActive
   global m_bUploadActive
   global m_bVerbose

         */


    }


    get User() {
        return this._m_sUser;
    }

    get Password() {
        return this._m_sPassword;
    }

    get ActiveWorkspace() {
        return this._m_sActiveWorkspace;
    }

    get WorkspaceOwner() {
        return this._m_sWorkspaceOwner;
    }

    get WorkspaceBaseUrl() {
        return this._m_sWorkspaceBaseUrl;
    }

    get ParametersFilePath() {
        return this._m_sParametersFilePath;
    }

    get SessionId() {
        return this._m_sSessionId;
    }

    get ValidSession() {
        return this._m_bValidSession;
    }

    get BasePath() {
        return this._m_sBasePath;
    }

    get DownloadActive() {
        return this._m_bDownloadActive;
    }

    get UploadActive() {
        return this._m_bUploadActive;
    }

    get Verbose() {
        return this._m_bVerbose;
    }

    get ParamsDictionary() {
        return this._m_aoParamsDictionary;
    }

    get MyProcId() {
        return this._m_sMyProcId;
    }

    get BaseUrl() {
        return this._m_sBaseUrl;
    }

    get IsOnServer() {
        return this._m_bIsOnServer;
    }

    get RequestsTimeout() {
        return this._m_iRequestsTimeout;
    }

    get WorkspaceId() {
        return this._m_sWorkspaceId;
    }

    get WorkspaceName() {
        return this._m_sWorkspaceName;
    }

    set m_sUser(value) {
        this._m_sUser = value;
    }

    set m_sPassword(value) {
        this._m_sPassword = value;
    }

    set m_sActiveWorkspace(value) {
        this._m_sActiveWorkspace = value;
    }

    set m_sWorkspaceOwner(value) {
        this._m_sWorkspaceOwner = value;
    }

    set m_sWorkspaceBaseUrl(value) {
        this._m_sWorkspaceBaseUrl = value;
    }

    set m_sParametersFilePath(value) {
        this._m_sParametersFilePath = value;
    }

    set m_sSessionId(value) {
        this._m_sSessionId = value;
    }

    set m_bValidSession(value) {
        this._m_bValidSession = value;
    }

    set m_sBasePath(value) {
        this._m_sBasePath = value;
    }

    set m_bDownloadActive(value) {
        this._m_bDownloadActive = value;
    }

    set m_bUploadActive(value) {
        this._m_bUploadActive = value;
    }

    set m_bVerbose(value) {
        this._m_bVerbose = value;
    }

    set m_aoParamsDictionary(value) {
        this._m_aoParamsDictionary = value;
    }

    set m_sMyProcId(value) {
        this._m_sMyProcId = value;
    }

    set m_sBaseUrl(value) {
        this._m_sBaseUrl = value;
    }

    set m_bIsOnServer(value) {
        this._m_bIsOnServer = value;
    }

    set m_iRequestsTimeout(value) {
        this._m_iRequestsTimeout = value;
    }

    set WorkspaceName(value) {
        this._m_sWorkspaceName = value;
    }

    set WorkspaceId(value) {
        this._m_sWorkspaceId = value;
    }

    async run(){
        var wasdiInstance = new Wasdi();
        this.helloWasdiWorld();

        var boolReturn = await this.loadConfig("./config.json");


        this.printStatus();
    }
}

var wasdiInstance = new Wasdi();
wasdiInstance.run();








