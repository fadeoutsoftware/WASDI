/**
 * Created by a.corrado on 05/04/2017.
 */
angular.module('wasdi.SpaceInvaderDirective', [])
    .directive('invader', function () {
        "use strict";
        return{
            restrict:"E",
            templateUrl:"directives/space_invaders/SpaceInvadersView.html"
        };
    });