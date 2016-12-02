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
        this.m_oMapService.initMap('wasdiMapImport');
        //if user is logged
        //if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser()))
        //    this.m_oUser = this.m_oConstantsService.getUser();
        //else
        //    this.m_oState.go("login");
        this.m_oUser = this.m_oConstantsService.getUser();

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
    ImportController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'MapService'
    ];

    return ImportController;
}) ();
