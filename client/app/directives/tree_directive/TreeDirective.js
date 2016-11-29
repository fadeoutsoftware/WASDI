/**
 * Created by a.corrado on 22/11/2016.
 */
angular.module('wasdi.TreeDirective', [])
    .directive('tree', function () {
        "use strict";

        function linkFunction($scope, element, attr){

            var generateWellFormedTree;// method

            /* the oTreeAttribute is a object
             * as in the following:
             * {
             *    'text' : 'Name',
             *    'Children':[] <----- {
             *                              'text':'name'
             *                               'Children':[]
             *                         }
             * }
             * */

            //this.generateWellFormedTree=function(oElement,oNewTree,iIndexNewTreeAttribute)
            //{
            //
            //
            //    if (typeof oElement != "undefined" && oElement != null)
            //    {
            //        /* i generate new object
            //         {
            //         *       'text':'name'
            //         *       'Children':[]
            //         * }
            //         * */
            //
            //        var oNode = new Object();
            //        oNode.text=oElement.name;
            //        oNode.children= [];
            //        oNewTree.push(oNode);
            //
            //        if(oElement.elements != null)// if is a leaf
            //        {
            //            // i call the algorithm for all child
            //            for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
            //            {
            //                this.generateWellFormedTree(oElement.elements[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
            //            }
            //        }
            //
            //        /*
            //         if(oElement.bands != null)// if is a leaf
            //         {
            //         // i call the algorithm for all child
            //         for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
            //         {
            //         this.generateWellFormedTree(oElement.bands[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
            //         }
            //         }*/
            //    }
            //
            //}
            var prova;
            $scope.$watch('m_oController.m_oTree', function (newValue, oldValue, scope)
            {
                if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_oTree))
                {
                    //load tree
                    $('#jstree').jstree($scope.m_oController.m_oTree);

                    //bind to events triggered on the tree
                    $('#jstree').on("changed.jstree", function (e, data) {

                        data.event.preventDefault();

                        // if the node it's a band do $scope.m_oController.openBandImage()
                        if(data.node.children.length == 0 && $scope.m_oController.m_bStatusPublishing == false)
                        {
                            //TODO CHECK IF THERE IS A BAND
                            $scope.m_oController.openBandImage(data.node.original.band)
                            //console.log(data.selected);
                            //console.log(data.node.text);
                            //console.log(data.node.id);
                            //console.log(data.node.children);
                        }

                    });
                }
            });

        }

        return{
            restrict:"E",

            template:'<div id="jstree"class="panel-body jstree" ></div>',
            link: linkFunction
        };
});