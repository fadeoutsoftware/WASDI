'use strict';

// Declare app level module which depends on views, and components
var wasdiApp = angular.module('wasdi', [
    'ngRoute',
    'pascalprecht.translate',
    'wasdi.ConstantsService',
    'wasdi.sessionInjector',
    'wasdi.AuthService',
    'wasdi.MapService',
    'wasdi.WorkspaceService',
    'wasdi.FileBufferService',
    'wasdi.ProductService',
    'wasdi.SnakeDirective'
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


wasdiApp.config(['$locationProvider', '$routeProvider', function($locationProvider, $routeProvider) {
    $locationProvider.hashPrefix('!');
    $routeProvider.when('/home', {templateUrl: 'partials/home.html', controller: 'HomeController'} );
    $routeProvider.when('/workspaces', {templateUrl: 'partials/workspaces.html', controller: 'WorkspaceController'} );
    $routeProvider.when('/editor', {templateUrl: 'partials/editor.html', controller: 'EditorController'} );

    $routeProvider.otherwise({redirectTo: '/home'});

}]);

wasdiApp.controller("HomeController", HomeController);
wasdiApp.controller("WorkspaceController", WorkspaceController);
wasdiApp.controller("EditorController", EditorController)