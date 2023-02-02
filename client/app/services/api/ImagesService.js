/**
 *
 */

'use strict';
angular.module('wasdi.ImagesService', ['wasdi.ImagesService']).
service('ImagesService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_sResource = "/images";



    /**
     * Upload or Update Processor logo
     * @param sWorkspaceId
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.uploadProcessorLogo = function (sProcessorId, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/processors/logo/upload?processorId=' + encodeURI(sProcessorId), oBody ,oOptions);
    };

    /**
     * Upload Processor Image
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.uploadProcessorImage = function (sProcessorId, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/processors/gallery/upload?processorId=' + encodeURI(sProcessorId), oBody ,oOptions);
    };

    /**
     * Removes one of the images of the processor
     * @param sProcessorId
     * @param sImage
     * @returns {*}
     */
    this.removeProcessorImage = function (sProcessorName, sImage) {
        return this.m_oHttp.delete(this.APIURL + this.m_sResource + '/delete?collection=' + encodeURI(sProcessorName) + "&folder=processors&name=" + sImage);
    };

    /**
     * Create an updated link to access a WASDI Image.
     * To have the image a session token is needed. Since the image is fetched directly by the browser
     * there is the need to put the token as additional query param.
     * This fuction check if the url does not have the sessionId yet and, if confirmed, adds the token Query Param
     * @param {*} sImageBaseUrl Url to access the image
     * @returns Url with the token parameter added
     */
    this.getImageLink = function(sImageBaseUrl) { 
        if (utilsIsStrNullOrEmpty(sImageBaseUrl)) return sImageBaseUrl;

        if (this.m_oConstantsService!=null) {
            var sSessionId = this.m_oConstantsService.getSessionId();
            var sTokenParam = "&token=" + sSessionId;
            if (!sImageBaseUrl.includes(sTokenParam)) {
                sImageBaseUrl = sImageBaseUrl + sTokenParam;
            }
        }

        return sImageBaseUrl;
    }

    /**
     * Updates the link of logo and images in a processor.
     * The logo can be a placeholder or an uploaded image: this method check if there is a logo
     * link available and, in case, generate the correct link (calling getImageLink) and replaces
     * the imgLink attribute of the processor.
     * 
     * If images are available, for each, the getImageLink method is called to add the sessionId to the link
     * 
     * @param {*} oProcessor 
     * @returns 
     */
    this.updateProcessorLogoImageUrl = function(oProcessor) {
        if (oProcessor == null) return "";

        if (!utilsIsStrNullOrEmpty(oProcessor.logo)) {
            var sUrl = this.getImageLink(oProcessor.logo);
            oProcessor.imgLink = sUrl;
        }

        if (oProcessor.images != null) {
            for (var iImage = 0; iImage<oProcessor.images.length; iImage++) {
                oProcessor.images[iImage] = this.getImageLink(oProcessor.images[iImage]);
            }    
        }
    }

    this.getImageNameFromUrl = function (sUrl) {

        try {
            let asSplit = sUrl.split('&')
            
            for (var iLinkParts = 0; iLinkParts<asSplit.length; iLinkParts++) {
                if (asSplit[iLinkParts].includes("name=")) {
                    let asResplit = asSplit[iLinkParts].split("=");
                    return asResplit[1];
                }
            }
        }
        catch (error) {
            return  sImageName;
        }
        return "";
    }
}]);
