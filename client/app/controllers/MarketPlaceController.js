/**
 * Marketplace
 * Created by p.campanella on 09/07/2020.
 */

var MarketPlaceController = (function() {

    /**
     * Class constructor
     * @param $scope
     * @param oConstantsService
     * @param oAuthService
     * @param oProcessorService
     * @param oProcessorMediaService
     * @constructor
     */
    function MarketPlaceController($scope, $state, oConstantsService, oAuthService, oProcessorService, oProcessorMediaService) {
        /**
         * Angular Scope
         */
        this.m_oScope = $scope;

        /**
         * Appication State
         */
        this.m_oState=$state;
        /**
         * Reference to the controller
         * @type {MarketPlaceController}
         */
        this.m_oScope.m_oController = this;
        /**
         * Constant Service
         */
        this.m_oConstantsService = oConstantsService;
        /**
         * Auth Service
         */
        this.m_oAuthService = oAuthService;
        /**
         * Processors Service
         */
        this.m_oProcessorService = oProcessorService;

        /**
         * Processors Media Service
         */
        this.m_oProcessorMediaService = oProcessorMediaService;

        /**
         * Name Filter
         * @type {string}
         */
        this.m_sNameFilter = "";

        /**
         * Flag to know if load more is enabled or not
         * @type {boolean}
         */
        this.m_bLoadMoreEnabled = true;

        /**
         * List of applications
         * @type {*[]}
         */
        this.m_aoApplicationList = []

        /**
         * List of Categories
         * @type {*[]}
         */
        this.m_aoCategories = []

        /**
         * List of Publishers
         * @type {*[]}
         */
        this.m_aoPublishers = []

        /**
         * Filters
         * @type {{score: number, itemsPerPage: number, minPrice: number, name: string, publishers: [], orderBy: string, orderDirection: number, categories: [], maxPrice: number, page: number}}
         */
        this.m_oAppFilter = {
            categories: [],
            publishers: [],
            name: "",
            score: 0,
            minPrice: -1,
            maxPrice: -1,
            itemsPerPage: 12,
            page: 0,
            orderBy: "name",
            orderDirection: 1
        }

        /**
         * Flag to know if the page is waiting for applications data to come
         * @type {boolean}
         */
        this.m_bWaiting = true;

        /**
         * Local reference to the controller
         * @type {MarketPlaceController}
         */
        let oController = this;


        /**
         * Price Slider
         * @type {{options: {stepsArray: ({legend: string, value: number}|{value: number})[], ticksTooltip: MarketPlaceController.m_oSlider.options.ticksTooltip, ceil: number, floor: number, showTicksValues: boolean}, value: number}}
         */
        this.m_oSlider = {
            value: 10000,
            options: {
                showSelectionBar: true,
                floor: 0,
                ceil: 10000,
                showTicksValues: false,
                onEnd: function (sliderId, modelValue, highValue, pointerType) {
                    oController.m_oAppFilter.maxPrice=modelValue;
                    oController.m_oAppFilter.page = 0;
                    oController.refreshAppList();
                }
            }
        };


        // Ask the list of Applications to the WASDI server
        this.m_oProcessorService.getMarketplaceList(this.m_oAppFilter).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoApplicationList = oController.setDefaultImagesAndVotes(data.data);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            }
            oController.m_bWaiting = false;
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            oController.m_bWaiting = false;
        })) ;

        // Get the list of categories
        this.m_oProcessorMediaService.getCategories().then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoCategories = data.data;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING CATEGORIES");
            }
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING CATEGORIES");
        }));

        // Get the list of publishers
        this.m_oProcessorMediaService.getPublishersFilterList().then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoPublishers = data.data;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING PUBLISHERS");
            }
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING PUBLISHERS");
        }));
    }


    /**
     * Refresh the application list
     */
    MarketPlaceController.prototype.refreshAppList = function() {

        let oController = this;

        if (this.m_sNameFilter !== this.m_oAppFilter.name) {
            this.m_oAppFilter.page = 0;
            this.m_oAppFilter.name = this.m_sNameFilter;
        }

        oController.m_bWaiting = false;

        this.m_oProcessorService.getMarketplaceList(this.m_oAppFilter).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                if (oController.m_oAppFilter.page == 0) {
                    oController.m_aoApplicationList = oController.setDefaultImagesAndVotes(data.data);
                }
                else {
                    if (data.data.length>0) {
                        oController.m_aoApplicationList = oController.m_aoApplicationList.concat(oController.setDefaultImagesAndVotes(data.data));
                    }
                }

                // If there is no data, we do not need Load More Button
                if (data.data.length > 0) {
                    oController.m_bLoadMoreEnabled = true;
                }
                else {
                    oController.m_bLoadMoreEnabled = false;
                }
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            }

            oController.m_bWaiting = false;
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST")
            oController.m_bWaiting = false;

        }));

    }

    /**
     * Handle load more button
     */
    MarketPlaceController.prototype.loadMore = function () {
        this.m_oAppFilter.page = this.m_oAppFilter.page+1;
        this.refreshAppList();
    }

    /**
     * Handle a click on a order by menu
     * @param sColumn
     */
    MarketPlaceController.prototype.orderByClicked = function (sColumn) {

        if (this.m_oAppFilter.orderBy === sColumn) {
            if (this.m_oAppFilter.orderDirection === 1) this.m_oAppFilter.orderDirection = -1;
            else if (this.m_oAppFilter.orderDirection === -1) this.m_oAppFilter.orderDirection = 1;
            else this.m_oAppFilter.orderDirection = 1;
        }

        this.m_oAppFilter.orderBy = sColumn;
        this.m_oAppFilter.page = 0;

        this.refreshAppList();
    }

    /**
     * Handle a click on a category
     * @param sCategoryId
     */
    MarketPlaceController.prototype.categoryClicked = function (sCategoryId) {
        if (this.m_oAppFilter.categories.includes(sCategoryId)) {
            this.m_oAppFilter.categories = this.m_oAppFilter.categories.filter(function(e) { return e !== sCategoryId })
        }
        else {
            this.m_oAppFilter.categories.push(sCategoryId);
        }
        this.m_oAppFilter.page = 0;

        this.refreshAppList();
    }

    /**
     * Handle a click on a publisher
     * @param sPublisher
     */
    MarketPlaceController.prototype.publisherClicked = function (sPublisher) {
        if (this.m_oAppFilter.publishers.includes(sPublisher)) {
            this.m_oAppFilter.publishers = this.m_oAppFilter.publishers.filter(function(e) { return e !== sPublisher })
        }
        else {
            this.m_oAppFilter.publishers.push(sPublisher);
        }

        this.m_oAppFilter.page = 0;

        this.refreshAppList();
    }

    /**
     * Handle a click on a score filter
     * @param iRanking
     */
    MarketPlaceController.prototype.rankingClicked = function (iRanking) {
        this.m_oAppFilter.score = iRanking;
        this.m_oAppFilter.page = 0;

        this.refreshAppList();
    }

    /**
     * Open Application Page
     * @returns {*[]} Array of strings, names of the tabs
     */
    MarketPlaceController.prototype.openApplicationPage = function(sApplicationName) {

        this.m_oConstantsService.setSelectedApplication(sApplicationName);
        this.m_oState.go("root.appdetails", { processorName : sApplicationName });
        //this.m_oState.go("root.appdetails");
    }

    /**
     * Initialize application default logo
     * @param aoProcessorList
     * @returns {*}
     */
    MarketPlaceController.prototype.setDefaultImagesAndVotes = function(aoProcessorList)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessorList) === true)
        {
            return aoProcessorList;
        }
        var sDefaultImage = "assets/wasdi/miniLogoWasdi.png";
        var iNumberOfProcessors = aoProcessorList.length;
        for(var iIndexProcessor = 0; iIndexProcessor < iNumberOfProcessors; iIndexProcessor++)
        {
            if(utilsIsObjectNullOrUndefined(aoProcessorList[iIndexProcessor].imgLink))
            {
                aoProcessorList[iIndexProcessor].imgLink = sDefaultImage;
            }

            if(utilsIsObjectNullOrUndefined(aoProcessorList[iIndexProcessor].votes)) {
                if (aoProcessorList[iIndexProcessor].score>0) {
                    aoProcessorList[iIndexProcessor].votes = 1;
                }
                else {
                    aoProcessorList[iIndexProcessor].votes = 0;
                }
            }
        }
        return aoProcessorList;
    };

    /**
     * Function to decide if the application is of the actual user or not
     * @param oProcessor
     * @returns {boolean}
     */
    MarketPlaceController.prototype.isMine = function(oProcessor) {

        if (!oProcessor.isMine) return false;
        else {
            if (oProcessor.publisher === this.m_oConstantsService.getUserId()) {
                return true;
            }

            return  false;
        }
    }

    MarketPlaceController.prototype.getVotesText = function(oProcessor) {
        let sText = "";

        if (oProcessor.votes>0) {

        }
        else {

        }
    }

    MarketPlaceController.$inject = [
        '$scope',
        '$state',
        'ConstantsService',
        'AuthService',
        'ProcessorService',
        'ProcessorMediaService'
    ];

    return MarketPlaceController;
}) ();
window.MarketPlaceController = MarketPlaceController;
