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
}







