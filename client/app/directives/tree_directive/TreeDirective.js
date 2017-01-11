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


            /*
            *  NOTE: $SCOPE = EDITOR SCOPE
            * */
            $scope.$watch('m_oController.m_oTree', function (newValue, oldValue, scope)
            {
                if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_oTree))
                {

                    //load tree
                    $('#jstree').jstree($scope.m_oController.m_oTree);

                    //bind event (it triggered when click a node tree)
                    $('#jstree').on("changed.jstree", function (e, data) {

                        data.event.preventDefault();//TODO CHECK IF IT'S USEFUL


                        // if there isn't running processes AND the node it's a band do $scope.m_oController.openBandImage()
                        /*PUBLIC BAND*/
                        if($scope.m_oController.isEmptyListOfRunningProcesses() == true && data.node.children.length == 0 && !utilsIsObjectNullOrUndefined(data.node.original.band))
                        {
                            //TODO CHECK IF THERE IS THE BAND

                            if(data.node.icon == 'assets/icons/check.png')
                            {
                                $('#jstree').jstree(true).set_icon(data.node.id, 'assets/icons/uncheck.png');
                                $scope.m_oController.removeBandImage(data.node.original.band);
                            }
                            else
                            {
                                $('#jstree').jstree(true).set_icon(data.node.id, 'assets/icons/check.png');
                                $scope.m_oController.openBandImage(data.node.original.band,data.node.id);
                            }

                        }

                    });

                    /* BIND EVENT DOUBLE CLICK */
                    $("#jstree").delegate("a","dblclick", function(e) {
                        var instance = $.jstree.reference(this);
                        var node = instance.get_node(this);
                        //TODO WHEN I DBLCLICK ON PRODUCT I PUBLISH IT
                    });

                    //bind event (event = after tree is loaded do checkTreeNode())
                    /*
                    * */
                    $('#jstree').on("loaded.jstree", function (e, data)
                    {
                        //if the page was reload it method check all nodes in tree,
                        //this nodes are processes running in server
                        $scope.m_oController.checkNodesInTree();
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