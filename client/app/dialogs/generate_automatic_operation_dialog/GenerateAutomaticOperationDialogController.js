/**
 * Created by a.corrado on 23/05/2017.
 */
var GenerateAutomaticOperationDialogController = (function() {

    function GenerateAutomaticOperationDialogController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oGetParameters = this.m_oExtras.getParameters;
        this.m_oParameters=[];
        /*metadataAttributes:node.original.attributes*/
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        this.getParameters()
    }

    /**
     * getParameters
     */
    GenerateAutomaticOperationDialogController.prototype.getParameters = function()
    {
        var oController = this;
        this.m_oGetParameters.success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                // oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                oController.m_oParameters = data;
                // oController.m_oReturnValue.options = oController.m_oOptions;
                // oController.m_oScope.$apply();
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS, THERE AREN'T DATA");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        });

    }

    GenerateAutomaticOperationDialogController.prototype.getTypeOfParameter = function(oParameter)
    {
        if(utilsIsObjectNullOrUndefined(oParameter) === true)
        {
            return null;
        }
        //ATTENTION don't change the order of if

        if( (oParameter.defaultValue === true) || (oParameter.defaultValue === false))
        {
            //checkbox case
            return "checkbox";
        }

        if(oParameter.valueSet.length > 0)
        {
            // drop-down list case
            return "dropdown";
        }
        else
        {
            //input text case
            return "text";
        }

        return null;
    }

    GenerateAutomaticOperationDialogController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return GenerateAutomaticOperationDialogController;
})();
