/**
 * Created by a.corrado on 24/05/2017.
 */


var FilterBandController = (function() {

    function FilterBandController($scope, oClose,oExtras,oWorkspaceService,oFilterService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_aoWorkspaceList = [];
        this.m_oClose = oClose;
        this.m_iActiveProvidersTab = 0;
        this.m_iSelectedValue = 0 ;
        this.m_oFilterService = oFilterService;
        this.m_sWorkspaceId =  this.m_oExtras.workspaceId;
        this.m_oSelectedBand = this.m_oExtras.selectedBand;
        this.m_sSelectedFilter = "";
        this.m_aoFilterProperties = [
            {name:"Operation:",value:""},
            {name:"Name:",value:""},
            {name:"Shorthand:",value:""},
            {name:"Tags:",value:""},
            {name:"Kernel quotient:",value:""},
            {name:"Kernel offset X:",value:""},
            {name:"Kernel offset Y:",value:""},
            {name:"Kernel width:",value:""},
            {name:"Kernel height:",value:""}
        ];
        this.m_aiFillOptions=[-2,-1,0,1,2,3,4,5];
        this.testMatrix=[
            [
                {color:"blue" ,fontcolor:"black", value:"1", click:function(){console.log('hello')}},
                {color:"red" ,fontcolor:"black", value:"0" , click:function(){console.log('hello')}},
                {color:"yellow" ,fontcolor:"black", value:"0" , click:function(){console.log('hello')}},
                {color:"yellow" ,fontcolor:"black", value:"0"}
            ],
            [
                {color:"white",fontcolor:"black",value:"0" , click:function(){console.log('hello')}},
                {color:"red" ,fontcolor:"black", value:"1"},
                {color:"black" , fontcolor:"white",value:"0"},
                {color:"yellow" ,fontcolor:"black", value:"0"}
            ],
            [{color:"blue" ,fontcolor:"black", value:"1", click:function(){console.log('hello')}},{color:"red" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"}],
            [{color:"blue" ,fontcolor:"black", value:"1", click:function(){console.log('hello')}},{color:"red" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"}],

        ];

        this.m_aoSystemFilterOptions = [];
        this.m_aoUserFilterOptions = [
            {name:"User" ,options:[]}
        ];

        this.getFilters();

        var oController = this;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        $scope.applyFilterOnClose = function(result) {
            // var oOption = oController.getSelectedOptionsObject('Arithmetic Mean 3x3');
            var oReturnValue,oOption;
            if( (utilsIsObjectNullOrUndefined(oController.m_sSelectedFilter) === true) || (oController.m_sSelectedFilter === "") )
            {
                oReturnValue = null;
            }
            else
            {
                oOption = oController.getSelectedOptionsObject(oController.m_sSelectedFilter);
                delete oOption.matrix;
                oReturnValue = {
                    band: oController.m_oSelectedBand,
                    filter: oOption
                };
            }
            // oOption = oController.getSelectedOptionsObject(oController.m_sSelectedFilter);
            // delete oOption.matrix;
            //
            // oReturnValue = {
            //     band: oController.m_oSelectedBand,
            //     filter: oOption
            // };
            oClose(oReturnValue, 300); // close, but give 500ms for bootstrap to animate

        };

    }

    FilterBandController.prototype.closeAndDonwloadProduct= function(result){

        this.m_oClose(result, 500); // close, but give 500ms for bootstrap to animate

    };

    FilterBandController.prototype.addUserFilterOptions = function()
    {
        var oThat = this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}//
        };
        var aaoEmptyMatrix = this.makeEmptyMatrix(4,4,oDefaultValue);
        this.m_aoUserFilterOptions[0].options.push( {name:"NewFilter", matrix:aaoEmptyMatrix});
    };

    /*TODO REMOVE*/
    FilterBandController.prototype.removeUserFilterOptions = function(){
    };

    FilterBandController.prototype.makeEmptyMatrix = function(iNumberOfCollumns,iNumberOfRows,iDefaultValue){
        if( utilsIsObjectNullOrUndefined(iNumberOfCollumns) || utilsIsObjectNullOrUndefined(iNumberOfRows)|| utilsIsObjectNullOrUndefined(iDefaultValue) )
            return null;
        var aaMatrix = [];
        for(var iIndexRows = 0; iIndexRows <  iNumberOfRows; iIndexRows++)
        {

            var aoRow = [];
            for(var iIndexCollumns = 0; iIndexCollumns <  iNumberOfCollumns; iIndexCollumns++)
            {
               //aoRow.push(iDefaultValue);
               aoRow[iIndexCollumns] =  {color: iDefaultValue.color ,fontcolor:iDefaultValue.fontcolor, value:iDefaultValue.value, click:iDefaultValue.click};
            }

            aaMatrix[iIndexRows] = aoRow;

        }

        return aaMatrix;
    };

    FilterBandController.prototype.getFilters = function()
    {
        var oController = this;
        this.m_oFilterService.getFilters().then(function (data, status) {
            if (data.data != null)
            {
                if (data.data != undefined)
                {
                    oController.generateFiltersListFromServer(data.data);
                }
            }
        },function (data,status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN GET FILTERS');
        });
    };

    FilterBandController.prototype.numberOfRows = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix))
            return 0;
        return aaoMatrix.length;
    };

    FilterBandController.prototype.numberOfCollumns = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || aaoMatrix.length <= 0)
            return 0;
        return aaoMatrix[0].length;
    };


    FilterBandController.prototype.addRowInMatrix = function(aaoMatrix,oElementRow)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || utilsIsObjectNullOrUndefined(oElementRow)  )
            return false;

        if(aaoMatrix.length === 0)
            var iNumberOfRowValues = 5;//default value
        else
            var iNumberOfRowValues = aaoMatrix[0].length;

        var aoRow = [];
        for(var iIndexRow = 0; iIndexRow < iNumberOfRowValues; iIndexRow++)
        {
            aoRow[iIndexRow]=({
                color:oElementRow.color ,
                fontcolor:oElementRow.fontcolor,
                value:oElementRow.value,
                click:oElementRow.click
            });

        }

        aaoMatrix.push(aoRow);
        return true;
    };

    FilterBandController.prototype.addDefaultRowInMatrix = function(aaoMatrix)
    {
        var oThat=this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}
        };
        return this.addRowInMatrix(aaoMatrix,oDefaultValue)
    };

    FilterBandController.prototype.removeRowInMatrix = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || aaoMatrix.length <= 0)
            return false;
        aaoMatrix.pop();
        return true;
    };

    FilterBandController.prototype.addColumnInMatrix = function(aaoMatrix,oDefaultValue)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || utilsIsObjectNullOrUndefined(oDefaultValue) )
            return false;

        var iNumberOfRows = aaoMatrix.length;

        for(var iIndexRows=0; iIndexRows < iNumberOfRows; iIndexRows++)
        {
            //var iIndexNumberOf = aaoMatrix[iIndexRows].length;
            aaoMatrix[iIndexRows].push({color: oDefaultValue.color ,fontcolor:oDefaultValue.fontcolor, value:oDefaultValue.value, click:oDefaultValue.click});
        }
        return true;
    };
    FilterBandController.prototype.addDefaultColumnInMatrix = function(aaoMatrix)
    {
        var oThat=this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}
        };
        return this.addColumnInMatrix(aaoMatrix,oDefaultValue)
    };
    FilterBandController.prototype.removeColumnInMatrix = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix))
            return false;
        var iNumberOfRows = aaoMatrix.length;
        for(var iIndexRows=0; iIndexRows < iNumberOfRows; iIndexRows++)
        {
            //var iIndexNumberOf = aaoMatrix[iIndexRows].length;
            aaoMatrix[iIndexRows].pop();
        }
        return true;
    };

    FilterBandController.prototype.generateFiltersListFromServer = function(oData)
    {
        if(utilsIsObjectNullOrUndefined(oData)) return false;

        var asProperties = utilsGetPropertiesObject(oData);

        if(asProperties === []) return false;

        var oThat = this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}
        };

        for(var iIndexProperty = 0; iIndexProperty < asProperties.length; iIndexProperty++)
        {
            for(var iIndexOptions = 0 ; iIndexOptions < oData[asProperties[iIndexProperty]].length ; iIndexOptions++)
            {
                var iNumberOfRows = oData[asProperties[iIndexProperty]][iIndexOptions].kernelHeight;
                var iNumberOfColumns = oData[asProperties[iIndexProperty]][iIndexOptions].kernelWidth;

                oData[asProperties[iIndexProperty]][iIndexOptions].matrix =  this.makeEmptyMatrix(iNumberOfColumns,iNumberOfRows,oDefaultValue);
                var aiArray = this.generateArrayOptionsValues(oData[asProperties[iIndexProperty]][iIndexOptions].kernelElements);
                this.initMatrix(oData[asProperties[iIndexProperty]][iIndexOptions].matrix,aiArray );
            }

            var oObject = {
                name:asProperties[iIndexProperty],
                options:oData[asProperties[iIndexProperty]],

            };
            this.m_aoSystemFilterOptions.push(oObject);
        }
        return true;
    };

    FilterBandController.prototype.generateArrayOptionsValues = function(aArrayValues)
    {
        if(utilsIsObjectNullOrUndefined(aArrayValues) === true)
            return [];
        var oThat = this;

        var aReturnArray = [];
        var iNumberOfValues =  aArrayValues.length;
        for(var iIndexArrayValues = 0 ; iIndexArrayValues < iNumberOfValues; iIndexArrayValues++ )
        {
            var oDefaultValue = {
                color:"white" ,
                fontcolor:"black",
                value:"0",
                click:function(){this.value = oThat.m_iSelectedValue;}//
            };
            var iValue = aArrayValues[iIndexArrayValues];
            oDefaultValue.value = iValue;
            aReturnArray.push(oDefaultValue);
        }

        return aReturnArray;
    };

    FilterBandController.prototype.initMatrix = function(aaoMatrix,aoValues)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) === true || utilsIsObjectNullOrUndefined(aoValues) === true )
            return false;
        var iNumberOfRows = aaoMatrix.length;
        var iNumberOfValues = aoValues.length;
        var iIndexValue = 0;

        for(var iIndexRow = 0; iIndexRow < iNumberOfRows ; iIndexRow++)
        {
            if(iIndexValue >= iNumberOfValues)
                return false;

            var iNumberOfColumns = aaoMatrix[iIndexRow].length;
            for(var iIndexColumn = 0; iIndexColumn < iNumberOfColumns ; iIndexColumn++)
            {
                aaoMatrix[iIndexRow][iIndexColumn] = aoValues[iIndexValue];
                iIndexValue++;
            }
        }
        return true;
    };

    // FilterBandController.prototype.applyFilter = function()
    // {
    //     if( utilsIsStrNullOrEmpty(this.m_sWorkspaceId) === true)
    //         return false;
    //     var oBand = this.m_oSelectedBand;
    //     // if( utilsIsObjectNullOrUndefined(oBody) === true)
    //     //     return false;
    //     var oController = this;
    //     var oBody = {
    //         "productFileName": sFileName,
    //         "bandName": oBand.name,
    //         // "filterVM": "",
    //         "vp_x": 0,
    //         "vp_y": 0,
    //         "vp_w": oBand.width,
    //         "vp_h": oBand.height,
    //         "vp_w_original": oBand.width,
    //         "vp_h_original": oBand.height,
    //         "img_w": widthMapContainer,
    //         "img_h": heightMapContainer
    //     };
    //     this.m_oFilterService.getProductBand(oBody,this.m_sWorkspaceId).success(function (data, status) {
    //         if (data != null)
    //         {
    //             if (data != undefined)
    //             {
    //                 //TODO IT
    //                 // oController.generateFiltersListFromServer(data);
    //             }
    //         }
    //     }).error(function (data,status) {
    //         utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN GET APPLY FILTER');
    //     });
    //     return true;
    // };

    FilterBandController.prototype.getSelectedOptionsObject = function(sName)
    {
        if(utilsIsStrNullOrEmpty(sName)=== true) return null;
        //TODO IT
        var iNumberOfSystemFilters = this.m_aoSystemFilterOptions.length;
        for(var iIndexSystemFilter = 0; iIndexSystemFilter < iNumberOfSystemFilters; iIndexSystemFilter++)
        {

            // var asSystemProperties = utilsGetPropertiesObject(this.m_aoSystemFilterOptions);
            // var iNumberOfOptions = asSystemProperties.length;

            var aoOptions = this.m_aoSystemFilterOptions[iIndexSystemFilter].options;
            var iNumberOfOptions = aoOptions.length;

            for(var iIndexOptions = 0; iIndexOptions < iNumberOfOptions; iIndexOptions++)
            {
                var oOption = this.m_aoSystemFilterOptions[iIndexSystemFilter];
                var sOptionName = oOption.options[iIndexOptions].name;
                if( sOptionName === sName)
                {
                    return oOption.options[iIndexOptions];
                }

            }
        }
        return null;
    }

    FilterBandController.prototype.collapsePanels = function()
    {
        jQuery('.collapse').collapse('hide');
    };
    FilterBandController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'FilterService'
    ];
    return FilterBandController;
})();
window.FilterBandController=FilterBandController;
