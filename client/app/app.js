'use strict';

// Declare app level module which depends on views, and components
var wasdiApp = angular.module('wasdi', [
    'ngRoute',
    'ngAnimate',
    'ui.router',//library alternative for router
    'pascalprecht.translate',
    'JSONedit',
    'directive.g+signin',

    //US SERVICE
    'wasdi.ConstantsService',
    'wasdi.sessionInjector',
    'wasdi.AuthService',
    'wasdi.AuthServiceFacebook',
    'wasdi.MapService',
    'wasdi.GlobeService',
    'wasdi.WorkspaceService',
    'wasdi.FileBufferService',
    'wasdi.ProductService',
    'wasdi.ConfigurationService',
    'wasdi.OpenSearchService',
    'wasdi.ProcessesLaunchedService',
    'wasdi.RabbitStompService',
    'wasdi.SearchOrbitService',
    'wasdi.ResultsOfSearchService',
    'wasdi.SnapOperationService',
    'wasdi.GetParametersOperationService',
    'wasdi.SatelliteService',
    'wasdi.CatalogService',
    'wasdi.PagesService',
    'wasdi.FilterService',
    'wasdi.ProcessorService',
    'wasdi.AuthServiceGoogle',
    'wasdi.TreeService',
    'wasdi.LightSearchService',

    //DIRECTIVE
    'wasdi.SnakeDirective',
    'wasdi.TreeDirective',
    'wasdi.SpaceInvaderDirective',
    'wasdi.SpaceInvaderFixedDirective',
    'wasdi.MultiselectDirective',
    'wasdi.SpaceInvaderFixedSmallVersionDirective',
    'wasdi.MultiselectDropdownMenuDirective',
    'wasdi.SquaresDirective',
    'wasdi.MultiRadioButtonDropdownMenuDirective',
    'wasdi.ImagePreviewDirective',
    'wasdi.ToggleSwitch',
    'wasdi.ImageEditorDirective',
    'wasdi.DropdownMenuDirective',
    'wasdi.ToDoListDirective',
    'wasdi.InputTextDirective',
    'wasdi.Map2DDirective',
    'wasdi.DateDirective',
    'wasdi.TableOfProductsDirective',
    'wasdi.LightSearchProductDirective',
    'wasdi.ChipsListDirective',
    
    //FILTERS
    'wasdi.stringUtils',

    //EXTERNAL LIB
    'ui.bootstrap',
    'checklist-model',
    'moment-picker',//time picker
    'angularModalService',//modal
    'ngFileUpload',//upload
    'colorpicker',
    'rzSlider'
    // 'btorfs.multiselect'//multi select

]);


wasdiApp.config(['$httpProvider', '$translateProvider', function($httpProvider, $translateProvider) {
    $httpProvider.interceptors.push('sessionInjector');

    //language configuration
    $translateProvider.useStaticFilesLoader({
        prefix: 'languages/',
        suffix: '.json'
    });

    $translateProvider.preferredLanguage('it');
    $translateProvider.useSanitizeValueStrategy('escaped');

}]);


//wasdiApp.config(['$locationProvider', '$routeProvider', function($locationProvider, $routeProvider) {
//    $locationProvider.hashPrefix('!');
//    $routeProvider.when('/home', {templateUrl: 'partials/home.html', controller: 'HomeController'} );
//    $routeProvider.when('/workspaces', {templateUrl: 'partials/workspaces.html', controller: 'WorkspaceController'} );
//    $routeProvider.when('/editor', {templateUrl: 'partials/editor.html', controller: 'EditorController'} );
//
//    $routeProvider.otherwise({redirectTo: '/home'});
//
//}]);
//ROUTER
wasdiApp.config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {


    //set default page
    $urlRouterProvider.otherwise("/home");

    //LOGIN PAGE
    $stateProvider.state('home', {
        url: '/home',
        templateUrl: 'partials/home.html',
        controller: 'HomeController'
    });

    //VALIDATE REGISTRATION
    $stateProvider.state('validatenewuser', {
        url: '/validatenewuser',
        templateUrl: 'partials/validatenewuser.html',
        controller: 'ValidateUserController'
    });

    //ROOT abstract class
    $stateProvider.state('root', {
        url: '',
        abstract: true,
        templateUrl: 'partials/RootView.html',
        controller : 'RootController',
        // resolve: { authenticated: function () {
        //         //decheckAuthentication
        //         debugger;
        //
        //         return AuthService.checkSession();
        //     } }
    });

    //WORKSPACES
    $stateProvider.state('root.workspaces', {
        url: '/workspaces',
        views:{
            'maincontent' : { templateUrl : 'partials/workspaces.html', controller  : 'WorkspaceController'}
        }
    });

    //EDITOR
    $stateProvider.state('root.editor', {
        url: '/{workSpace}/editor',
        views:{
            'maincontent' : { templateUrl : 'partials/editor.html', controller  : 'EditorController'}
        }
    });

    //IMPORT
    $stateProvider.state('root.import',{
        url: '/import', // /{workSpace}

        views:{
            'maincontent' : { templateUrl : 'partials/import.html', controller  : 'ImportController'}
        }
    });

    //SEARCH ORBIT
    $stateProvider.state('root.searchorbit',{
        url: '/searchorbit',// /{workSpace}

        views:{
            'maincontent' : { templateUrl : 'partials/searchorbit.html', controller  : 'SearchOrbitController'}
        }
    });
    //CATALOG
    $stateProvider.state('root.catalog',{
        url: '/catalog',// /{workSpace}

        views:{
            'maincontent' : { templateUrl : 'partials/catalog.html', controller  : 'CatalogController'}
        }
    });
    //APP STORE
    $stateProvider.state('root.appstore',{
        url: '/appstore',// /{workSpace}

        views:{
            'maincontent' : { templateUrl : 'partials/appstore.html', controller  : 'AppStoreController'}
        }
    });


}]);

wasdiApp.controller("HomeController", HomeController);
wasdiApp.controller("WorkspaceController", WorkspaceController);
wasdiApp.controller("EditorController", EditorController);
wasdiApp.controller("RootController",RootController);
wasdiApp.controller("ImportController",ImportController);
wasdiApp.controller("SearchOrbitController",SearchOrbitController);
wasdiApp.controller("ValidateUserController",ValidateUserController);
wasdiApp.controller("AppStoreController",AppStoreController);

//dialogs
wasdiApp.controller("OrbitInfoController",OrbitInfoController);
wasdiApp.controller("ProductInfoController",ProductInfoController);
wasdiApp.controller("GetCapabilitiesController",GetCapabilitiesController);
wasdiApp.controller("MergeProductsController",MergeProductsController);
wasdiApp.controller("ApplyOrbitController",ApplyOrbitController);
wasdiApp.controller("RadiometricCalibrationController",RadiometricCalibrationController);
wasdiApp.controller("MultilookingController",MultilookingController);
wasdiApp.controller("NDVIController",NDVIController);
wasdiApp.controller("ProductEditorInfoController",ProductEditorInfoController);
wasdiApp.controller("RangeDopplerTerrainCorrectionController",RangeDopplerTerrainCorrectionController);
wasdiApp.controller("AttributesMetadataController",AttributesMetadataController);
wasdiApp.controller("SftpUploadController",SftpUploadController);
wasdiApp.controller("DeleteProcessController",DeleteProcessController);
wasdiApp.controller("ProcessesLogsController",ProcessesLogsController);
wasdiApp.controller("WorkFlowController",WorkFlowController);
wasdiApp.controller("SnakeController",SnakeController);
wasdiApp.controller("CatalogController",CatalogController);
wasdiApp.controller("AddProductInCatalogController",AddProductInCatalogController);
wasdiApp.controller("GetInfoProductCatalogController",GetInfoProductCatalogController);
wasdiApp.controller("DownloadProductInWorkspaceController",DownloadProductInWorkspaceController);
wasdiApp.controller("FilterBandController",FilterBandController);
wasdiApp.controller("MaskManagerController",MaskManagerController);
wasdiApp.controller("ImportAdvanceFiltersController",ImportAdvanceFiltersController);
wasdiApp.controller("WorkFlowManagerController",WorkFlowManagerController);
wasdiApp.controller("GetListOfWorkspacesController",GetListOfWorkspacesController);
wasdiApp.controller("ProcessorController", ProcessorController);
wasdiApp.controller("WpsController", WpsController);
wasdiApp.controller("GenerateAutomaticOperationDialogController", GenerateAutomaticOperationDialogController);
wasdiApp.controller("WappsController", WappsController);
wasdiApp.controller("RasorWappController", RasorWappController);
wasdiApp.controller("OperaWappController", OperaWappController);
wasdiApp.controller("EditUserController", EditUserController);
wasdiApp.controller("FTPController", FTPController);
wasdiApp.controller("ListFloodAreaDetectionController", ListFloodAreaDetectionController);
wasdiApp.controller("JRCWorkflowController", JRCWorkflowController);
wasdiApp.controller("UploadController", UploadController);
wasdiApp.controller("JRCS2Controller", JRCS2Controller);
wasdiApp.controller("MosaicController", MosaicController);
wasdiApp.controller("EditPanelController", EditPanelController);
wasdiApp.controller("EDriftFloodAutomaticChainController", EDriftFloodAutomaticChainController);
wasdiApp.controller("ProcessErrorLogsDialogController", ProcessErrorLogsDialogController);
wasdiApp.controller("EdriftCheckImagesTool", EdriftCheckImagesTool);
wasdiApp.controller("ShareWorkspaceController", ShareWorkspaceController);
wasdiApp.controller("eDriftFloodEventDialogController", eDriftFloodEventDialogController);

wasdiApp.controller(UploadFileController.REG_NAME, UploadFileController);
wasdiApp.controller(JRCClassificationController.REG_NAME, JRCClassificationController);
wasdiApp.controller(ImageEditorController.REG_NAME, ImageEditorController);


wasdiApp.run(["$rootScope", "$state", "AuthService", function($rootScope, $state, AuthService){

    $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams, options){
        // if(toState.name.indexOf("root") >= 0)
        // {
        //     AuthService.checkSession()
        //         .then(function(response){
        //             debugger;
        //         })
        //     event.preventDefault();
        // }

    })

    // $rootScope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState, fromParams) {
    //     // if (!Authorization.authorized && _.has(toState, 'data.authorization') && _.has(toState, 'data.redirectTo')) {
    //     //     $state.go(toState.data.redirectTo);
    //     // }
    // });
}])
