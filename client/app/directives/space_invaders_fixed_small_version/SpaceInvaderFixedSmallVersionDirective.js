/**
 * Created by a.corrado on 08/05/2017.
 */

angular.module('wasdi.SpaceInvaderFixedSmallVersionDirective', [])
    .directive('smallinvaderfixed', function () {
        "use strict";
        return{
            restrict:"E",
            templateUrl:"directives/space_invaders_fixed_small_version/SpaceInvadersFixedSmallVersionView.html"
        };
    });