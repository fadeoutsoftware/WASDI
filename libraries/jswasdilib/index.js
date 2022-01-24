import fetch from 'node-fetch';

'use strict';

class Wasdi {
    constructor() {
    }

    /**
     * Test method to check wasdi instance, with a tiny bit of developer's traditions
     */
    helloWasdiWorld() {

        var requestOptions = {
            method: 'GET',
            redirect: 'follow'
        };

        fetch("http://www.wasdi.net/wasdiwebserver/rest/wasdi/hello", requestOptions)
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
}

var wasdiInstance = new Wasdi();
wasdiInstance.helloWasdiWorld();
wasdiInstance.helloWasdiWorld();
// Template - removed private credentials

// wasdiInstance.login(username, password);



