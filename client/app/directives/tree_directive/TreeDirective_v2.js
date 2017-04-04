/**
 * Created by a.corrado on 22/11/2016.
 */
angular.module('wasdi.TreeDirectiveV2', [])
    .directive('tree', function () {
        "use strict";

        function linkFunction($scope, element, attr){


        }

        return{
            restrict:"E",

            template:'<div id="jstree"class="jstree" ></div>',
            scope: {
                customerInfo: '=oTree'
            },
            link: linkFunction
        };
    });