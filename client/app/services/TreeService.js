
'use strict';
angular.module('wasdi.TreeService', ['wasdi.TreeService']).
service('TreeService', ['$http',  'ConstantsService', function ($http, oConstantsService) {

    this.createNewInstanceTree = function(sIdDiv,oTreeData)
    {
        if( ( utilsIsStrNullOrEmpty(sIdDiv) === true ) || ( utilsIsObjectNullOrUndefined(oTreeData) === true ) )
        {
            return false;
        }
        $(function () { $(sIdDiv).jstree(oTreeData); });

        return true;
    };

    this.openNode = function(sIdDiv,sIdNode)
    {
        return this.nodeOperation(sIdDiv,sIdNode,"open_node");
    };

    this.closeNode = function(sIdDiv,sIdNode)
    {
        return this.nodeOperation(sIdDiv,sIdNode,"close_node");
    };

    this.isOpenNode = function(sIdDiv,sIdNode){
        return this.nodeOperation(sIdDiv,sIdNode,"is_open");
    };


    this.getAllCheckedIDNode = function(sIdDiv){
        var asNodes = [];
        asNodes = $(sIdDiv).jstree(true).get_checked();
        return asNodes;
    };
    this.nodeOperation = function(sIdDiv,sIdNode, sOperation)
    {
        if( ( utilsIsStrNullOrEmpty(sIdDiv) === true ) || ( utilsIsStrNullOrEmpty(sIdNode) === true )
            || ( utilsIsStrNullOrEmpty(sOperation) === true ) )
        {
            return false;
        }
        $(sIdDiv).jstree(sOperation, sIdNode);

        return true;
    };

    this.closeAllNodes = function(sIdDiv){
        if(utilsIsStrNullOrEmpty(sIdDiv))
        {
            return false;
        }
        $(sIdDiv).jstree('close_all');
        return true;
    };

    this.openAllNodes = function(sIdDiv){
        if(utilsIsStrNullOrEmpty(sIdDiv))
        {
            return false;
        }
        $(sIdDiv).jstree('open_all');
        return true;
    };

    this.uncheckAllNodes = function(sIdDiv){
        if(utilsIsStrNullOrEmpty(sIdDiv))
        {
            return false;
        }
        $(sIdDiv).jstree("uncheck_all");
        // $(sIdDiv).jstree().deselect_all(true);

        return true;
    };

    this.getCheckNodeNameEvent = function(){
        return "check_node.jstree" ;
    };

    this.getCheckUncheckNodeNameEvent = function(){
        return this.getCheckNodeNameEvent() + " " + this.getUncheckNodeNameEvent();
    };

    this.getUncheckNodeNameEvent = function(){
        return "uncheck_node.jstree";
    };

    //click event
    this.getChangedNodeNameEvent = function(){
        return "changed.jstree";
    };


    this.onTreeEvent = function(sEvent,sIdDiv,oFunction,oController){
        if(utilsIsStrNullOrEmpty(sIdDiv) || utilsIsStrNullOrEmpty(sEvent) || utilsIsObjectNullOrUndefined(oFunction) || utilsIsObjectNullOrUndefined(oController))
        {
            return false;
        }

        $(sIdDiv).on(sEvent, function(e, data) {
            oFunction(oController,e,data);
        });

        return true;
    }

    this.getNodeById = function(sNodeId,sDivId){
        if( utilsIsStrNullOrEmpty(sNodeId) === true || utilsIsStrNullOrEmpty(sDivId) === true )
        {
            return null;
        }

        // var oNode = $(sDivId).jstree(true).find(sNodeId);
        var oNode = $(sDivId).jstree(true).get_node(sNodeId);
        return oNode;
    }

    this.loadNewTree = function(sIdDiv, oNewTree) {
        if (utilsIsStrNullOrEmpty(sIdDiv) === true || utilsIsObjectNullOrUndefined(oNewTree) === true)
        {
            return false;
        }
        $(sIdDiv).jstree(true).settings.core = oNewTree.core;
        $(sIdDiv).jstree(true).refresh(true);
        // $(sIdDiv).jstree(true).redraw(true);

        return true;
    }

    this.openAllCheckedNodes = function(sIdDiv)
    {
        var treeInst = $(sIdDiv).jstree(true);
        var aoNodes = treeInst._model.data;

        for (var iIndex in aoNodes)
        {
            if ( (utilsIsObjectNullOrUndefined(aoNodes[iIndex]) === false) &&
                 (utilsIsObjectNullOrUndefined(aoNodes[iIndex].state.checked) === false) &&
                 (aoNodes[iIndex].state.checked === true) )
            {
                $(sIdDiv).jstree("_open_to", aoNodes[iIndex].id);
            }
        }
    };

    this.numberOfCheckedNodes = function(sIdDiv)
    {
        var treeInst = $(sIdDiv).jstree(true);
        var iNumberOfCheckedNodes = 0;
        if(utilsIsObjectNullOrUndefined(treeInst._model) === true)
        {
            return iNumberOfCheckedNodes;
        }
        var aoNodes = treeInst._model.data;


        for (var iIndex in aoNodes)
        {
            if ( (utilsIsObjectNullOrUndefined(aoNodes[iIndex]) === false) &&
                 (utilsIsObjectNullOrUndefined(aoNodes[iIndex].state.checked) === false) &&
                 (aoNodes[iIndex].state.checked === true) )
            {
                iNumberOfCheckedNodes ++;
            }
        }
        return iNumberOfCheckedNodes;
    };


}]);
