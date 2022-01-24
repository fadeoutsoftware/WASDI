import fetch from 'node-fetch';
'use strict';
class Wasdi {
    constructor(){}

    helloWasdiWorld(){

        var requestOptions = {
            method: 'GET',
            redirect: 'follow'
        };

        fetch("http://www.wasdi.net/wasdiwebserver/rest/wasdi/hello", requestOptions)
            .then(response => response.text())
            .then(result => console.log(result))
            .catch(error => console.log('error', error));
    }
}

var wasdiInstance = new Wasdi();
wasdiInstance.helloWasdiWorld();
wasdiInstance.helloWasdiWorld();

