/**
 * Created by a.corrado on 14/04/2017.
 */
angular.module('wasdi.SpaceInvaderFixedDirective', [])
    .directive('invaderfixed', function () {
        "use strict";
        return{
            restrict:"E",
            templateUrl:"directives/space_invaders/SpaceInvadersView.html"
        };
    });