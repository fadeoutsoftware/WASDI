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
    'wasdi.OrganizationService',
    'wasdi.SubscriptionService',
    'wasdi.ProjectService',
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
    'wasdi.ImagesService',

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
    'wasdi.ToggleSwitch',
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
    'wasdi.stringUtilsTime',
    'wasdi.stringUtilsFileSize',

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


wasdiApp.config(['$httpProvider', '$translateProvider', function($httpProvider, $translateProvider) {
    $httpProvider.interceptors.push('sessionInjector');

    //language configuration
    $translateProvider.useStaticFilesLoader({
        prefix: 'languages/',
        suffix: '.json'
    });

    $translateProvider.preferredLanguage('en');
    $translateProvider.useSanitizeValueStrategy('escaped');
    $translateProvider.fallbackLanguage('en');

}]);


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
    });

    //WORKSPACES
    $stateProvider.state('root.workspaces', {
        url: '/workspaces',
        views:{
            'maincontent' : { templateUrl : 'partials/workspaces.html', controller  : 'WorkspaceController'}
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

    //MARKET PLACE
    $stateProvider.state('root.marketplace',{
        url: '/marketplace',

        views:{
            'maincontent' : { templateUrl : 'partials/marketplace.html', controller  : 'MarketPlaceController'}
        }
    });

    //APP DETAILS
    $stateProvider.state('root.appdetails',{
        url: '/{processorName}/appdetails',

        views:{
            'maincontent' : { templateUrl : 'partials/wasdiapplicationdetails.html', controller  : 'WasdiApplicationDetailsController'}
        }
    });

    //APPLICATION AUTOMATIC USER INTERFACE
    $stateProvider.state('root.appui',{
        url: '/{processorName}/appui',

        views:{
            'maincontent' : { templateUrl : 'partials/wasdiapplicationui.html', controller  : 'WasdiApplicationUIController'}
        }
    });


}]);

wasdiApp.controller("HomeController", HomeController);
wasdiApp.controller("WorkspaceController", WorkspaceController);
wasdiApp.controller("AdminDashboardController", AdminDashboardController);
wasdiApp.controller("EditorController", EditorController);
wasdiApp.controller("RootController",RootController);
wasdiApp.controller("ImportController",ImportController);
wasdiApp.controller("SearchOrbitController",SearchOrbitController);
wasdiApp.controller("ValidateUserController",ValidateUserController);
wasdiApp.controller("WasdiApplicationUIController",WasdiApplicationUIController);
wasdiApp.controller("MarketPlaceController",MarketPlaceController);
wasdiApp.controller("WasdiApplicationDetailsController",WasdiApplicationDetailsController);

//dialogs
wasdiApp.controller("OrbitInfoController",OrbitInfoController);
wasdiApp.controller("ProductInfoController",ProductInfoController);
wasdiApp.controller("GetCapabilitiesController",GetCapabilitiesController);
wasdiApp.controller("ProductEditorInfoController",ProductEditorInfoController);
wasdiApp.controller("AttributesMetadataController",AttributesMetadataController);
wasdiApp.controller("SftpUploadController",SftpUploadController);
wasdiApp.controller("DeleteProcessController",DeleteProcessController);
wasdiApp.controller("WorkspaceProcessesList",WorkspaceProcessesList);
wasdiApp.controller("SnakeController",SnakeController);
wasdiApp.controller("DownloadProductInWorkspaceController",DownloadProductInWorkspaceController);
wasdiApp.controller("ImportAdvanceFiltersController",ImportAdvanceFiltersController);
wasdiApp.controller("WorkFlowManagerController",WorkFlowManagerController);
wasdiApp.controller("StyleManagerController",StyleManagerController);
wasdiApp.controller("StyleController",StyleController);
wasdiApp.controller("GetListOfWorkspacesController",GetListOfWorkspacesController);
wasdiApp.controller("ProcessorController", ProcessorController);
wasdiApp.controller("ProcessorParametersTemplateController", ProcessorParametersTemplateController);
wasdiApp.controller("WorkflowController", WorkflowController);
wasdiApp.controller("WorkspaceDetailsController", WorkspaceDetailsController);
wasdiApp.controller("WappsController", WappsController);
wasdiApp.controller("EditUserController", EditUserController);
wasdiApp.controller("FTPController", FTPController);
wasdiApp.controller("UploadController", UploadController);
wasdiApp.controller("MosaicController", MosaicController);
wasdiApp.controller("ProcessorLogsController", ProcessorLogsController);
wasdiApp.controller("ShareWorkspaceController", ShareWorkspaceController);
wasdiApp.controller("SendFeedbackController", SendFeedbackController);
wasdiApp.controller("ManualInsertBboxController", ManualInsertBboxController);
wasdiApp.controller("PayloadDialogController", PayloadDialogController);
wasdiApp.controller("ProcessParamsShareController", ProcessParamsShareController)

wasdiApp.controller("SendFeedbackController", window.SendFeedbackController);

//wasdiApp.controller("UploadFileController", window.UploadFileController);
//wasdiApp.controller("ImageEditorController", window.ImageEditorController);

wasdiApp.controller("PackageManagerController", window.PackageManagerController);
wasdiApp.controller("OrganizationUsersController", window.OrganizationUsersController);
wasdiApp.controller("SubscriptionUsersController", window.SubscriptionUsersController);
wasdiApp.controller("SubscriptionProjectsController", window.SubscriptionProjectsController);
wasdiApp.controller("OrganizationEditorController", window.OrganizationEditorController);
wasdiApp.controller("SubscriptionEditorController", window.SubscriptionEditorController);
wasdiApp.controller("ProjectEditorController", window.ProjectEditorController);
wasdiApp.run(["$rootScope", "$state", "AuthService", function($rootScope, $state, AuthService){

    $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams, options){

    })
}])
