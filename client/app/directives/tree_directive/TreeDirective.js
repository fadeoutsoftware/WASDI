/**
 * Created by a.corrado on 22/11/2016.
 */
angular.module('wasdi.TreeDirective', [])
    .directive('tree', function () {
        "use strict";

        function linkFunction(scope, element, attr){

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
            var oTree=
            {
                'core' :
                {
                    'data' :
                    [
                        'Simple root node',
                        {
                            'text' : 'Root node 2',
                            'state' :
                            {
                                'opened' : true,
                                'selected' : true
                            },
                            'children' :
                            [
                                { 'text' : 'Child 1' },
                                'Child 2'
                            ]
                        }
                    ]
                    ,"check_callback" : true
                },
                "plugins" : [ "contextmenu" ],  // all plugin i use
                "contextmenu" : // my right click menu
                {
                    "items" : function ($node)
                    {
                        return {
                            "prova1" : {
                                "label" : "func1",
                                "action" : function (obj) {  }
                            },
                            "prova2" : {
                                "label" : "func2",
                                "action" : function (obj) {  }
                            },
                            "prova3" : {
                                "label" : "func3",
                                "action" : function (obj) {  }
                            }
                        };
                    }
                }
            }

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


            //load tree
            $('#jstree').jstree(oTree);

        }

        return{
            restrict:"E",
            template:'<div id="jstree"class="panel-body jstree" ></div>',
            link: linkFunction
        };
});