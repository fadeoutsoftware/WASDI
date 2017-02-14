'use strict';

// Declare app level module which depends on views, and components
var wasdiApp = angular.module('wasdi', [
    'ngRoute',
    'ui.router',//library alternative for router
    'pascalprecht.translate',
    'wasdi.ConstantsService',
    'wasdi.sessionInjector',

    'wasdi.AuthService',
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

    'wasdi.SnakeDirective',
    'wasdi.TreeDirective',

    'ui.bootstrap',
    'checklist-model',
    'moment-picker',
    'angularModalService'

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

    //ROOT abstract class
    $stateProvider.state('root', {
        url: '',
        abstract: true,
        templateUrl: 'partials/RootView.html',
        controller : 'RootController'
        //resolve: { authenticated: checkAuthentication }
    });

    //WORKSPACES
    $stateProvider.state('root.workspaces', {
        url: '/workspaces',
        views:{
            'maincontent' : { templateUrl : 'partials/workspaces.html', controller  : 'WorkspaceController'}
        },
    });

    //EDITOR
    $stateProvider.state('root.editor', {
        url: '/{workSpace}/editor',

        views:{
            'maincontent' : { templateUrl : 'partials/editor.html', controller  : 'EditorController'}
        },
    });

    //IMPORT
    $stateProvider.state('root.import',{
        url: '/{workSpace}/import',

        views:{
            'maincontent' : { templateUrl : 'partials/import.html', controller  : 'ImportController'}
        },
    });

    //SEARCH ORBIT
    $stateProvider.state('root.searchorbit',{
        url: '/{workSpace}/searchorbit',

        views:{
            'maincontent' : { templateUrl : 'partials/searchorbit.html', controller  : 'SearchOrbitController'}
        },
    });

}]);

wasdiApp.controller("HomeController", HomeController);
wasdiApp.controller("WorkspaceController", WorkspaceController);
wasdiApp.controller("EditorController", EditorController);
wasdiApp.controller("RootController",RootController);
wasdiApp.controller("ImportController",ImportController);
wasdiApp.controller("SearchOrbitController",SearchOrbitController);

//dialogs
wasdiApp.controller("OrbitInfoController",OrbitInfoController);