/**
 * Created by a.corrado on 30/11/2016.
 */

var ImportController = (function() {

    function ImportController($scope, oConstantsService, oAuthService,$state,oMapService ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oScope.m_oController=this;
        this.m_oMapService=oMapService;
        this.m_bIsOpen=true;
        this.m_bIsVisibleListOfLayers = true; //TODO SET FALSE AFTER MERGE
        this.m_oMapService.initMapWithDrawSearch('wasdiMapImport');
        this.m_aoLayersList= [];

        //if user is logged
        //if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser()))
        //    this.m_oUser = this.m_oConstantsService.getUser();
        //else
        //    this.m_oState.go("login");
        this.m_oUser = this.m_oConstantsService.getUser();

        this.generateLayersList();

        /* Wait message by rectangle in map
        *  change css on layer table
        * */
        $scope.$on('on-mouse-over-rectangle', function(event, args) {
            var iIndex = $scope.m_oController.indexOfRectangleInLayersList(args.rectangle);
            if(iIndex != -1)
            {
                var sId = "#layer"+iIndex+" td";
                $(sId).css({'color':'#009036', 'border-top': '1px solid #009036', 'border-bottom': '1px solid #009036'});
            }
        });

        /* Wait message by rectangle in map
        *  change css on layer table
         * */
        $scope.$on('on-mouse-leave-rectangle', function(event, args) {
            var iIndex = $scope.m_oController.indexOfRectangleInLayersList(args.rectangle);
            if(iIndex != -1)
            {
                var sId = "#layer"+iIndex+" td";
                $(sId).css({'color':'', 'border-top': '', 'border-bottom': ''});
            }
        });
    }
    /***************** METHODS ***************/
    //OPEN LEFT NAV-BAR
    ImportController.prototype.openNav= function() {
        document.getElementById("mySidenav").style.width = "30%";
    }
    //CLOSE LEFT NAV-BAR
    ImportController.prototype.closeNav=function() {
        document.getElementById("mySidenav").style.width = "0";
    }

    //ALTERNATIVE METHODS
    ImportController.prototype.openCloseNav=function()
    {
        var oController=this;
        if(oController.m_bIsOpen == true)
        {
            oController.closeNav();
            oController.m_bIsOpen = false;
        }
        else{
            if(oController.m_bIsOpen == false)
            {
                oController.openNav();
                oController.m_bIsOpen=true;
            }
        }
    }

    /*
    * Generate layers list
    * */

    ImportController.prototype.generateLayersList=function()
    {
        var oController = this;
        //TODO TEST EXAMPLE
        var aaBounds=[
                        [[57.559322, -8.767822], [59.1210604, -6.021240]],
                        [[54.559322, -5.767822], [56.1210604, -3.021240]],
                        [[56.559322, -7.767822], [58.1210604, -5.021240]],
                        [[55.559322, -6.767822], [57.1210604, -4.021240]]
                        ];
        if(utilsIsObjectNullOrUndefined(aaBounds))
            var iLength = 0;
        else
            var iLength = aaBounds.length;

        for(var iIndexLayers = 0; iIndexLayers < iLength; iIndexLayers++)
        {
            var oRectangle = oController.m_oMapService.addRectangleOnMap(aaBounds[iIndexLayers][0],aaBounds[iIndexLayers][1],null,
                                function (event) {console.log("Test: "+iIndexLayers)});

            oController.m_aoLayersList.push({layerBounds:aaBounds[iIndexLayers], rectangle:oRectangle});
        }

    }
    /*********************** CHANGE CSS RECTANGLE (LEAFLET MAP) ****************************/
    /*
    *   Change style of rectangle when the mouse is over the layer (TABLE CASE)
    * */
    ImportController.prototype.changeStyleRectangleMouseOver=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            console.log("Error: rectangle is undefined ");
            return false;
        }
        oRectangle.setStyle({weight:3,fillOpacity:0.7});
    }
    /*
     *   Change style of rectangle when the mouse is leave the layer (TABLE CASE)
     * */
    ImportController.prototype.changeStyleRectangleMouseLeave=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            console.log("Error: rectangle is undefined ");
            return false;
        }
        oRectangle.setStyle({weight:1,fillOpacity:0.2});
    }
    /************************************************************/

    /* the method take in input a rectangle, return the layer index Bound with rectangle
    * */
    ImportController.prototype.indexOfRectangleInLayersList=function(oRectangle) {
        var oController = this;
        if (utilsIsObjectNullOrUndefined(oController.m_aoLayersList)) {
            console.log("Error LayerList is empty");
            return false;
        }
        var iLengthLayersList = oController.m_aoLayersList.length;

        for (var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
        {
            if(oController.m_aoLayersList[iIndex].rectangle == oRectangle)
                return iIndex; // I FIND IT !!
        }
        return -1;//the method doesn't find the Rectangle in LayersList.
    }

    ImportController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'MapService'
    ];

    return ImportController;
}) ();
