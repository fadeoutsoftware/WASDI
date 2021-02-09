/**
 * Created by m.menapace on 9/02/2021.
 */


var WorkspaceDetailsController = (function () {

    function WorkspaceDetailsController($scope, oExtras) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to this controller
         */
        this.m_oScope.m_oController = this;
        /**
         * import the extras
         */
        this.m_oExtras = oExtras;
        /**
         * workspace id
         */
        this.m_workspaceId = this.m_oExtras.WorkSpaceId;
        // the workspace id passed through extras to the modal
        // then, if ok, call the other methods from angular starting from here
        // get WS viewmodel, date and co [...]


    } // end constructor

    /*WorkspaceDetailsController.prototype.printSomething = function (){
        console.log("helo ! ");
        console.log(this.m_workspaceId);
    }*/

    WorkspaceDetailsController.$inject = [
        '$scope',
        'extras'
    ];
    return WorkspaceDetailsController;
})();
