'use strict';

// Declare app level module which depends on views, and components
var wasdiApp = angular.module('wasdi', [
    'ngRoute',
    'ngAnimate',
    'ui.router',//library alternative for router
    'pascalprecht.translate',
    'JSONedit',
//    'directive.g+signin',

    //WASDI SERVICES
    'wasdi.ConstantsService',
    'wasdi.sessionInjector',
    'wasdi.AuthService',
    'wasdi.MapService',
    'wasdi.GlobeService',
    'wasdi.WorkspaceService',
    'wasdi.ProcessWorkspaceService',
    'wasdi.FileBufferService',
    'wasdi.ProductService',
    'wasdi.ConfigurationService',
    'wasdi.OpenSearchService',
    'wasdi.RabbitStompService',
    'wasdi.ResultsOfSearchService',
    'wasdi.CatalogService',
    'wasdi.PagesService',
    'wasdi.ProcessorService', 
    'wasdi.ConsoleService', 
    'wasdi.WorkflowService', 
    'wasdi.StyleService', 
    'wasdi.FeedbackService', 
    'wasdi.ProcessorParametersTemplateService',
    'wasdi.OpportunitySearchService', 
    'wasdi.TreeService',
    'wasdi.LightSearchService',
    'wasdi.ProcessorMediaService',
    'wasdi.NodeService',
    'wasdi.AdminDashboardService',
    'wasdi.PackageManagerService',

    //DIRECTIVES
    'wasdi.SnakeDirective',
    'wasdi.TreeDirective',
    'wasdi.SpaceInvaderDirective',
    'wasdi.SpaceInvaderFixedDirective',
    'wasdi.MultiselectDirective',
    'wasdi.SpaceInvaderFixedSmallVersionDirective',
    'wasdi.MultiselectDropdownMenuDirective',
    'wasdi.SquaresDirective',
    'wasdi.MultiRadioButtonDropdownMenuDirective',
    //'wasdi.ImagePreviewDirective',
    'wasdi.ToggleSwitch',
    //'wasdi.ImageEditorDirective',
    'wasdi.DropdownMenuDirective',
    'wasdi.wapTextBox',
    'wasdi.wapNumericBox',
    'wasdi.wapSelectArea',
    'wasdi.wapDateTimePicker',
    'wasdi.wapProductList',
    'wasdi.wapDropDown',
    'wasdi.wapProductsCombo',
    'wasdi.wapSearchEOImage',
    'wasdi.wapCheckBox',
    'wasdi.wapSlider',
    'wasdi.wapListBox',
    'wasdi.angularLightSlider',
    'wasdi.insertableTextArea',
    /*'wasdi.ChipsListDirective',*/

    //FILTERS
    'wasdi.stringUtils',

    //EXTERNAL LIBS
    'ui.bootstrap',
    'checklist-model',
    'moment-picker',//time picker
    'angularModalService',//modal
    'ngFileUpload',//upload
    'colorpicker',
    'rzSlider'
    // 'btorfs.multiselect'//multi select

]);


wasdiApp.config(['$httpProvider', '$translateProvider', function ($httpProvider, $translateProvider) {
    $httpProvider.interceptors.push('sessionInjector');

    //language configuration
    $translateProvider.useStaticFilesLoader({
        prefix: 'languages/',
        suffix: '.json'
    });

    $translateProvider.preferredLanguage('en');
    $translateProvider.useSanitizeValueStrategy('escaped');

}]);


//ROUTER
wasdiApp.config(['$stateProvider', '$urlRouterProvider', function ($stateProvider, $urlRouterProvider) {


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
        controller: 'RootController',
    });

    //WORKSPACES
    $stateProvider.state('root.workspaces', {
        url: '/workspaces',
        views: {
            'maincontent': {templateUrl: 'partials/workspaces.html', controller: 'WorkspaceController'}
        }
    });

    //AdminDashboard
    $stateProvider.state('root.adminDashboard', {
        url: '/adminDashboard',
        views: {
            'maincontent': {templateUrl: 'partials/adminDashboard.html', controller: 'AdminDashboardController'}
        }
    });

    //EDITOR
    $stateProvider.state('root.editor', {
        url: '/{workSpace}/editor',
        views: {
            'maincontent': {templateUrl: 'partials/editor.html', controller: 'EditorController'}
        }
    });

    //IMPORT
    $stateProvider.state('root.import', {
        url: '/import', // /{workSpace}

        views: {
            'maincontent': {templateUrl: 'partials/import.html', controller: 'ImportController'}
        }
    });

    //SEARCH ORBIT
    $stateProvider.state('root.searchorbit', {
        url: '/searchorbit',// /{workSpace}

        views: {
            'maincontent': {templateUrl: 'partials/searchorbit.html', controller: 'SearchOrbitController'}
        }
    });

    //CATALOG
    $stateProvider.state('root.catalog', {
        url: '/catalog',

        views: {
            'maincontent': {templateUrl: 'partials/catalog.html', controller: 'CatalogController'}
        }
    });

    //MARKET PLACE
    $stateProvider.state('root.marketplace', {
        url: '/marketplace',

        views: {
            'maincontent': {templateUrl: 'partials/marketplace.html', controller: 'MarketPlaceController'}
        }
    });

    //APP DETAILS
    $stateProvider.state('root.appdetails', {
        url: '/{processorName}/appdetails',

        views: {
            'maincontent': {
                templateUrl: 'partials/wasdiapplicationdetails.html',
                controller: 'WasdiApplicationDetailsController'
            }
        }
    });

    //APPLICATION AUTOMATIC USER INTERFACE
    $stateProvider.state('root.appui', {
        url: '/{processorName}/appui',

        views: {
            'maincontent': {templateUrl: 'partials/wasdiapplicationui.html', controller: 'WasdiApplicationUIController'}
        }
    });


}]);

wasdiApp.controller("HomeController", window.HomeController);
wasdiApp.controller("MarketPlaceController", window.MarketPlaceController);
//wasdiApp.controller("CatalogController", window.CatalogController);
wasdiApp.controller("WorkspaceController", window.WorkspaceController);
wasdiApp.controller("AdminDashboardController", window.AdminDashboardController);
wasdiApp.controller("EditorController", window.EditorController);
wasdiApp.controller("RootController", window.RootController);
wasdiApp.controller("ImportController", window.ImportController);
wasdiApp.controller("SearchOrbitController", window.SearchOrbitController);
wasdiApp.controller("ValidateUserController", window.ValidateUserController);
wasdiApp.controller("WasdiApplicationUIController", window.WasdiApplicationUIController);
wasdiApp.controller("WasdiApplicationDetailsController", window.WasdiApplicationDetailsController);

//dialogs
wasdiApp.controller("OrbitInfoController", window.OrbitInfoController);
wasdiApp.controller("ProductInfoController", window.ProductInfoController);
wasdiApp.controller("GetCapabilitiesController", window.GetCapabilitiesController);
//wasdiApp.controller("MergeProductsController", window.MergeProductsController);
wasdiApp.controller("ProductEditorInfoController", window.ProductEditorInfoController);
wasdiApp.controller("AttributesMetadataController", window.AttributesMetadataController);
wasdiApp.controller("SftpUploadController", window.SftpUploadController);
wasdiApp.controller("DeleteProcessController", window.DeleteProcessController);
wasdiApp.controller("WorkspaceProcessesList", window.WorkspaceProcessesList);
wasdiApp.controller("SnakeController", window.SnakeController);
//wasdiApp.controller("GetInfoProductCatalogController", window.GetInfoProductCatalogController);
wasdiApp.controller("DownloadProductInWorkspaceController", window.DownloadProductInWorkspaceController);
//wasdiApp.controller("FilterBandController", window.FilterBandController);
//wasdiApp.controller("MaskManagerController", window.MaskManagerController);
wasdiApp.controller("ImportAdvanceFiltersController", window.ImportAdvanceFiltersController);
wasdiApp.controller("WorkFlowManagerController", window.WorkFlowManagerController);
wasdiApp.controller("StyleManagerController", window.StyleManagerController);
wasdiApp.controller("StyleController", window.StyleController);
wasdiApp.controller("GetListOfWorkspacesController", window.GetListOfWorkspacesController);
wasdiApp.controller("ProcessorController", window.ProcessorController);
wasdiApp.controller("WorkflowController", window.WorkflowController);
wasdiApp.controller("WorkspaceDetailsController", window.WorkspaceDetailsController);
//wasdiApp.controller("WpsController", window.WpsController);
wasdiApp.controller("WappsController", window.WappsController);
wasdiApp.controller("EditUserController", window.EditUserController);
wasdiApp.controller("FTPController", window.FTPController);
wasdiApp.controller("UploadController", window.UploadController);
wasdiApp.controller("MosaicController", window.MosaicController);
//wasdiApp.controller("EditPanelController", window.EditPanelController);
wasdiApp.controller("ProcessorLogsController", window.ProcessorLogsController);
wasdiApp.controller("ShareWorkspaceController", window.ShareWorkspaceController);
wasdiApp.controller("SendFeedbackController", window.SendFeedbackController);
wasdiApp.controller("ManualInsertBboxController", window.ManualInsertBboxController);
wasdiApp.controller("PayloadDialogController", window.PayloadDialogController);

wasdiApp.controller("ProcessorParametersTemplateController", window.ProcessorParametersTemplateController);
wasdiApp.controller("ProcessParamsShareController", window.ProcessParamsShareController);

wasdiApp.controller("SendFeedbackController", window.SendFeedbackController);

//wasdiApp.controller("UploadFileController", window.UploadFileController);
//wasdiApp.controller("ImageEditorController", window.ImageEditorController);

wasdiApp.controller("PackageManagerController", window.PackageManagerController); 


wasdiApp.run(["$rootScope", "$state", "AuthService", function ($rootScope, $state, AuthService) {

    $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams, options) {

    })
}])
