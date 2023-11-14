/**
 * Created by a.corrado on 22/11/2016.
 */
angular.module('wasdi.TreeDirective', [])
    .directive('tree', ['ProductService','ModalService','$http',function (oProductService,oModalService,$http) {
        "use strict";

        function linkFunction($scope, element, attr){

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
            this.generateMetadataTree = function(oElement,oNewTree,iIndexNewTreeAttribute)
            {


                if (typeof oElement != "undefined" && oElement != null)
                {
                    /* i generate new object
                     {
                     *       'text':'name'
                     *       'Children':[]
                     * }
                     * */
                    if(utilsIsSubstring(oElement.name,"node.") === true)
                    {
                        this.replaceNodeNameWithOperationName(oElement);
                    }
                    var oNode = new Object();
                    oNode.text = oElement.name;
                    oNode.attributes = oElement.attributes;
                    oNode.children = [];
                    if(!oNode.attributes)
                        oNode.icon = "assets/icons/folder_20x20.png";
                    else
                        oNode.icon = "fa fa-info-circle fa-lg";
                    oNewTree.push(oNode);

                    if(oElement.elements != null)// if is a leaf
                    {
                        // i call the algorithm for all child
                        for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
                        {
                            this.generateMetadataTree(oElement.elements[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
                        }
                    }
                }

            };

            this.replaceNodeNameWithOperationName = function(oElement)
            {
                if(utilsIsObjectNullOrUndefined(oElement) === true)
                {
                    return false;
                }
                var iNumberOfElementAttributes = oElement.attributes.length;
                for(var iIndexElementAttributes = 0 ; iIndexElementAttributes < iNumberOfElementAttributes; iIndexElementAttributes++)
                {
                    if(oElement.attributes[iIndexElementAttributes].description === "operator")
                    {
                        oElement.name = oElement.attributes[iIndexElementAttributes].data;
                        break;
                    }
                }
                return true;
            };

            this.myDisableNode = function(sIdInput)
            {
                if(utilsIsObjectNullOrUndefined(sIdInput) === true)
                    return false;
                $("#jstree").jstree().disable_node(sIdInput);
                $('#jstree').jstree(true).set_icon(sIdInput, 'fa fa-spinner fa-spin');
                return true;
            };

            this.myEnableNode = function(sIdInput,sIcon)
            {
                if(utilsIsObjectNullOrUndefined(sIdInput) === true)
                    return false;

                if(utilsIsObjectNullOrUndefined(sIcon)=== true || utilsIsStrNullOrEmpty(sIcon) === true)
                    sIcon='fa fa-spinner fa-spin';

                $("#jstree").jstree().enable_node(sIdInput);
                $('#jstree').jstree(true).set_icon(sIdInput,sIcon );
                return true;
            };

            // this.openCloseNode = function(sId)
            // {
            //     //open node
            //     var isOpen = $("#jstree").jstree("is_open", sId);
            //     if(!isOpen)
            //     {
            //         $("#jstree").jstree("open_node", sId);
            //     }
            //     else
            //     {
            //         $("#jstree").jstree("close_node", sId);
            //     }
            // };

            var oController = this;
            /*
            *  NOTE: $SCOPE = EDITOR SCOPE
            * */

            /* WHEN THE TREE WAS CHANGED */
            $scope.$watch('m_oController.m_oTree', function (newValue, oldValue, scope)
            {
                if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_oTree))
                {
                    if(!utilsIsObjectNullOrUndefined(oldValue))
                    {
                        /******************** RELOADED TREE CASE ****************************/
                        //if the tree is reloaded need  $('#jstree').jstree(true).refresh();
                        $('#jstree').jstree(true).settings.core.data = newValue.core.data;
                        $('#jstree').jstree(true).refresh();
                        $scope.m_oController.selectNodeByFileNameInTree($scope.m_oController.m_oLastDownloadedProduct);
                    }
                    else
                    {
                        /****************** INIT JS TREE  *************************/
                        //load tree
                        $('#jstree').jstree($scope.m_oController.m_oTree);

                        //Bind Click node event
                        $('#jstree').on("changed.jstree", function (e, data) {

                            /*CLICK ON PUBLIC BAND*/
                            // if there aren't running publish band processes AND the node it's a band do $scope.m_oController.openBandImage()
                            //change icons
                            //data.event.type !="contextmenu" => discard right click of mouse (plugin)
                            if (angular.isUndefined(data.event))  return;

                            //exclude click event context menu
                            if(!utilsIsObjectNullOrUndefined(data.node) && data.event.type !="contextmenu")
                            {
                                //  oController.openCloseNode(data.node.id);

                                //is it a band?
                                if( data.node.children.length == 0 && !utilsIsObjectNullOrUndefined(data.node.original.band))
                                {

                                    //if(data.node.icon == 'assets/icons/check.png')
                                    if(data.node.original.band.bVisibleNow == true)
                                    {
                                        data.node.original.band.bVisibleNow = false;
                                        $('#jstree').jstree(true).set_icon(data.node.id, 'assets/icons/uncheck_20x20.png');
                                        $scope.m_oController.removeBandImage(data.node.original.band);
                                    }
                                    else
                                    {
                                        oController.myDisableNode(data.node.id);
                                        //the tree icon is change when it receive the "publishband" message by rabbit or
                                        //when the band was pubblished (http request)
                                        // method: receivedPublishBandMessage()
                                        $scope.m_oController.openBandImage(data.node.original.band);
                                    }

                                }
                            }
                            /* CLICK ON METADATA */
                            if( (utilsIsObjectNullOrUndefined(data.node)=== false) && (data.node.text==="Metadata") && (data.node.original.clicked === false) )
                            {

                                if( (utilsIsObjectNullOrUndefined(data.node.original.url)=== false) && (utilsIsStrNullOrEmpty(data.node.original.url) === false) && (data.node.children.length === 0))
                                {
                                    data.node.original.clicked = true;//lock click on metadata  semaphore
                                    // var test=$("#tree").jstree().get_node(  data.node.id);
                                    $("#jstree").jstree().disable_node( data.node.id);
                                    $('#jstree').jstree(true).set_icon( data.node.id, 'fa fa-spinner fa-spin');

                                    //if url != 0 AND (children IS empty == true)
                                    $http.get(data.node.original.url).then(function (result_data) {
                                            //reload product list
                                            if(result_data.data !== "")
                                            {
                                                var temp=[];
                                                oController.generateMetadataTree(result_data.data,temp,0);

                                                var iLengthChildren = temp[0].children.length;

                                                /* Draw new nodes */
                                                for( var iIndexChildren = 0; iIndexChildren < iLengthChildren; iIndexChildren++)
                                                {
                                                    $('#jstree').jstree().create_node(data.node.id, temp[0].children[iIndexChildren]);
                                                }

                                            }
                                            data.node.original.clicked = false; //release semaphore
                                            $("#jstree").jstree().enable_node(data.node);
                                            $('#jstree').jstree(true).set_icon( data.node.id, 'assets/icons/metadata-24.png');

                                        },function (error) {
                                            console.log("Error in: " + data.node.original.url + " the request doesn't work");
                                            data.node.original.clicked = false; //release semaphore
                                            $("#jstree").jstree().enable_node(data.node);
                                            $('#jstree').jstree(true).set_icon( data.node.id, 'assets/icons/metadata-24.png');
                                    });
                                }
                            }
                        });

                        ///* BIND EVENT DOUBLE CLICK */
                        $("#jstree").delegate("a","dblclick", function(e) {
                           var instance = $.jstree.reference(this);
                           var node = instance.get_node(this);
                            if( (utilsIsObjectNullOrUndefined(node.original.attributes) === false) && (node.original.attributes.length > 0))
                            {
                                oModalService.showModal({
                                    templateUrl: "dialogs/attributes_metadata_info/AttributesMetadataDialog.html",
                                    controller: "AttributesMetadataController",
                                    inputs: {
                                        extras: {
                                            metadataAttributes:node.original.attributes,
                                            nameNode:node.text
                                        }
                                    }
                                }).then(function (modal) {
                                    modal.element.modal();
                                    modal.close.then(function (result) {
                                        if(utilsIsObjectNullOrUndefined(result)===true) return false;
                                        //oController.m_oScope.Result = result;
                                    });
                                });

                                return true;
                            }
                        });
                    }
                }

            });


        }

        return{
            restrict:"E",

            template:'<div id="jstree"class="jstree" ></div>',
            link: linkFunction
        };
}]);
