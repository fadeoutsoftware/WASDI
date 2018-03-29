/**
 * Created by a.corrado on 31/03/2017.
 */


var MaskManagerController = (function() {

    function MaskManagerController($scope, oClose, oExtras,$timeout,oSnapOperationService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        // this.m_oFilterService = oFilterService;
        this.m_oExtras = oExtras;
        this.m_oBand = oExtras.band;
        this.m_oProduct = oExtras.product;
        this.m_sWorkspaceId = oExtras.workspaceId;
        // this.m_oBody = oExtras.body;
        this.m_sMaskColor = {
            rangeMaskColor:"rgb(0,143,255)",
            logicalMaskColor:"rgb(0,143,255)"
        };
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oWindowsOpened = {
            rangeMask:false,
            logicalMask:false
        };

        this.getProductMasks(this.m_oProduct.fileName,this.m_oBand.name, this.m_sWorkspaceId);
        // this.getProductMasks(this.m_aoProducts[1].fileName,this.m_aoProducts[1].bandsGroups.bands[0].name);
        // this.getProductMasks(this.m_aoProducts[2].fileName,this.m_aoProducts[2].bandsGroups.bands[0].name);
        this.m_aoMasks=[];
        this.m_sRangeMinValue = 0.0;
        this.m_sRangeMaxValue = 1.0;
        this.m_sRangeSelectedRaster = "Defaultvalue";
        this.m_sRangeListOfRasters=[""];
        this.m_oTime = $timeout;
        this.m_sTextAreaLogicalMask = "";
        // this.m_asColors = [
        //     "rgb(0,0,0)",
        //     "rgb(64,64,64)",
        //     "rgb(128,128,128)",
        //     "rgb(192,192,192)",
        //     "rgb(255,255,255)",
        //     "rgb(0,255,255)",
        //     "rgb(0,0,255)",
        //     "rgb(255,0,255)",
        //     "rgb(255,255,0)",
        //     "rgb(255,200,0)",
        //     "rgb(255,0,0)",
        //     "rgb(255,175,175)",
        //     "rgb(0,255,0)",
        // ];
        this.m_asContants = [
            "PI",
            "E",
            "NaN",
            "true",
            "false",
            "x",
            "y",
            "LAT",
            "LONG",
            "TIME",
            "0.5",
            "0.0",
            "1.0",
            "2.0",
            "0",
            "1",
            "2",
            "273.15"
        ];
        this.m_asOperators = [
            "@ ? @ : @",
            "if @ then @ else @",
            "@ || @",
            "@ or @",
            "@ && @",
            "@ and @",
            "@ < @",
            "@ <= @",
            "@ > @",
            "@ >= @",
            "@ == @",
            "@ | @",
            "@ ^ @",
            "@ & @",
            "@ + @",
            "@ - @",
            "@ * @",
            "@ / @",
            "@ % @",
            "+ @",
            "- @",
            "~ @",
            "! @",
            "not @"
        ];
        this.m_asFunction = [
            "abs(@)",
            "acos(@)",
            "ampl(@,@)",
            "asin(@)",
            "atan(@)",
            "atan2(@)",
            "avg()",
            "ceil(@)",
            "coef_var()",
            "cos(@)",
            "cosech(@)",
            "cosh(@)",
            "deg(@)",
            "distance()",
            "distance_deriv()",
            "distance_integ()",
            "exp(@)",
            "exp10(@)",
            "feq(@,@)",
            "feq(@,@,@)",
            "floor(@)",
            "fneq(@,@)",
            "inf(@)",
            "inrange()",
            "inrange_deriv()",
            "inrange_integ()",
            "log(@)",
            "log10(@)",
            "max(@,@)",
            "min(@,@)",
            "nan(@)",
            "phase(@,@)",
            "pow(@,@)",
            "rad(@)",
            "random_gaussian()",
            "random_uniform()",
            "rint(@)",
            "round(@)",
            "sech(@)",
            "sign(@)",
            "sin(@)",
            "sinh(@)",
            "sq(@)",
            "sqrt(@)",
            "stdev()",
            "tan(@)",
            "tanh(@)"
        ]
        // this.m_oProduct = this.m_oExtras.product;
        // this.m_oProductChecked={};
        // // this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        // this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_asBands = this.getBandList(this.m_oProduct);

        this.m_asShow_Tie_pointGrids = [
            "latitude",
            "longitude",
            "incident_angle",
            "elevation_angle",
            "slant_range_time"
        ];
        this.initRangeListOfRasters();

        this.m_oCheckboxs = {
            bands:false,
            masks:false,
            tiePoint:false,
        };

        this.m_asDataSources = [];
        var oController = this;
        $scope.close = function() {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.save = function() {
            var result = oController.getBodyImage();
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };
    /**
     *
     * @param sButtonClicked
     */
    MaskManagerController.prototype.changeWindowsOpened = function(sButtonClicked)
    {
        switch(sButtonClicked)
        {
            case "rangeMask":
                this.closeAllWindows();
                this.m_oWindowsOpened.rangeMask = !this.m_oWindowsOpened.rangeMask;

                break;
            case "logicalMask":
                this.closeAllWindows();
                this.m_oWindowsOpened.logicalMask = !this.m_oWindowsOpened.logicalMask;

                break;
            default:

        }
    };

    MaskManagerController.prototype.cleanRangeMaskWindow = function()
    {
        this.m_sRangeMinValue = 0.0;
        this.m_sRangeMaxValue = 1.0;
        this.m_sRangeSelectedRaster = "Defaultvalue";
        this.m_sMaskColor.rangeMaskColor =  "rgb(0,143,255)" ;
    };

    MaskManagerController.prototype.cleanLogicalMaskWindow = function()
    {
        this.m_sTextAreaLogicalMask = "";
        this.m_sMaskColor.logicalMaskColor = "rgb(0,143,255)";
    };

    MaskManagerController.prototype.closeAllWindows = function(){
        this.m_oWindowsOpened.rangeMask = false;
        this.m_oWindowsOpened.logicalMask = false;
        this.cleanRangeMaskWindow();
        this.cleanLogicalMaskWindow();
        //this.m_oScope.$apply();
        //this.m_oTime(null,1000);

    };

    MaskManagerController.prototype.saveRangeMaskAndClose = function()
    {
        if( utilsIsObjectNullOrUndefined(this.m_aoMasks) )
            return false;
        var sDescription = this.m_sRangeMinValue + "<=" + this.m_sRangeSelectedRaster + "<=" + this.m_sRangeMaxValue ;
        var oReturnValue = {
            name:this.newUniqueNameGenerator(),
            type:"Range",
            colour:this.m_sMaskColor.rangeMaskColor,
            transparency: 0.5,
            description:sDescription,
            min:this.m_sRangeMinValue,
            max:this.m_sRangeMaxValue
        };
        utilsConvertRGBAInObjectColor(this.m_sMaskColor.rangeMaskColor);
        this.m_aoMasks.push(oReturnValue);
        this.closeAllWindows();
        return true;
    };

    MaskManagerController.prototype.addOperationOrConstantOrFunctionLogicalMask = function(sValue,sIdTextArea)
    {
        if(utilsIsStrNullOrEmpty(sIdTextArea))
            return false;

        this.insertAtCaret(sIdTextArea,sValue);

        return true;
    };

    /*
    *   https://stackoverflow.com/questions/1064089/inserting-a-text-where-cursor-is-using-javascript-jquery
    *   HOW TO INSERT TEXT IN CURSOR POSITION
    * */
    MaskManagerController.prototype.insertAtCaret = function(areaId, text) {
        var txtarea = document.getElementById(areaId);
        if (!txtarea) { return; }

        var scrollPos = txtarea.scrollTop;
        var strPos = 0;
        var br = ((txtarea.selectionStart || txtarea.selectionStart == '0') ?
            "ff" : (document.selection ? "ie" : false ) );
        if (br == "ie") {
            txtarea.focus();
            var range = document.selection.createRange();
            range.moveStart ('character', -txtarea.value.length);
            strPos = range.text.length;
        } else if (br == "ff") {
            strPos = txtarea.selectionStart;
        }
        this.m_sTextAreaLogicalMask = utilsInsertSubstringIntoString(this.m_sTextAreaLogicalMask,strPos,0,text);

        // var front = (txtarea.value).substring(0, strPos);
        // var back = (txtarea.value).substring(strPos, txtarea.value.length);
        // txtarea.value = front + text + back;
        //strPos = strPos + text.length;

        // if (br == "ie") {
        //     txtarea.focus();
        //     var ieRange = document.selection.createRange();
        //     ieRange.moveStart ('character', -txtarea.value.length);
        //     ieRange.moveStart ('character', strPos);
        //     ieRange.moveEnd ('character', 0);
        //     ieRange.select();
        // } else if (br == "ff") {
        //     txtarea.selectionStart = strPos;
        //     txtarea.selectionEnd = strPos;
        //     txtarea.focus();
        // }
        //  txtarea.scrollTop = scrollPos;
        // var oController = this;

        // setTimeout(function () {
        //     oController.m_oScope.$apply();
        // },100);

    }

    MaskManagerController.prototype.saveLogicalMask = function(){
        if( utilsIsObjectNullOrUndefined(this.m_aoMasks) )
            return false;
        var sDescription = this.m_sTextAreaLogicalMask;
        var oReturnValue = {
            name:this.newUniqueNameGenerator(),
            type:"Maths",
            colour:this.m_sMaskColor.logicalMaskColor,
            transparency: 0.5,
            description:sDescription
        };

        this.m_aoMasks.push(oReturnValue);
        this.closeAllWindows();
        return true;
    };

    MaskManagerController.prototype.newUniqueNameGenerator = function()
    {

        var sName = "Mask";
        var iNumberOfMasks = this.m_aoMasks.length;
        var iNameCounter = 0;
        for(var iIndexMask = 0 ; iIndexMask < iNumberOfMasks; iIndexMask++ )
        {
            if( this.m_aoMasks[iIndexMask].name === sName )
            {
                iIndexMask = 0;
                sName = "Mask" + iNameCounter;
                iNameCounter = iNameCounter ++;
            }
        }
        return sName;

    };
    /**
     *
     * @param sCheck
     * @param bBoolean
     * @returns {boolean}
     */
    MaskManagerController.prototype.checkedDataSource = function(sCheck,bBoolean)
    {
        if(utilsIsObjectNullOrUndefined(this.m_asDataSources))
            return false;

        if(sCheck === "band")
        {
            if(bBoolean)
            {
                this.m_asDataSources = this.m_asDataSources.concat(this.m_asBands);
            }
            else
            {
                var iNumberOfDataSources = this.m_asDataSources.length;
                for(var iIndexSource = 0 ; iIndexSource < iNumberOfDataSources ; iIndexSource++)
                {
                    var iNumberOfBands = this.m_asBands.length;
                    for(var iIndexBand = 0 ; iIndexBand < iNumberOfBands ; iIndexBand++)
                    {
                        if(this.m_asDataSources[iIndexSource] === this.m_asBands[iIndexBand])
                        {
                            this.m_asDataSources.splice(iIndexSource,1);
                            iNumberOfDataSources--;
                            iIndexSource--;
                        }
                    }
                }
            }
        }
        if(sCheck === "masks")
        {
            if(bBoolean)
            {
                var iNumberOfMasks = this.m_aoMasks.length;
                var asMasksName = [];
                for(var iIndexMask = 0; iIndexMask < iNumberOfMasks; iIndexMask++)
                {
                    asMasksName.push(this.m_aoMasks[iIndexMask].name);
                }
                this.m_asDataSources = this.m_asDataSources.concat(asMasksName);
            }
            else
            {
                var iNumberOfDataSources = this.m_asDataSources.length;
                for(var iIndexSource = 0 ; iIndexSource < iNumberOfDataSources ; iIndexSource++)
                {
                    var iNumberOfMasks = this.m_aoMasks.length;
                    for(var iIndexMask = 0 ; iIndexMask < iNumberOfMasks ; iIndexMask++)
                    {
                        if(this.m_asDataSources[iIndexSource] === this.m_aoMasks[iIndexMask].name)
                        {
                            this.m_asDataSources.splice(iIndexSource,1);
                            iNumberOfDataSources--;
                            iIndexSource--;
                        }
                    }
                }
            }
        }
        if(sCheck === "tie-point")
        {
            if(bBoolean)
            {
                this.m_asDataSources = this.m_asDataSources.concat(this.m_asShow_Tie_pointGrids);
            }
            else
            {
                var iNumberOfDataSources = this.m_asDataSources.length;
                for(var iIndexSource = 0 ; iIndexSource < iNumberOfDataSources ; iIndexSource++)
                {
                    var iNumberOfShow_Tie_pointGrids = this.m_asShow_Tie_pointGrids.length;
                    for(var iIndexMask = 0 ; iIndexMask < iNumberOfShow_Tie_pointGrids ; iIndexMask++)
                    {
                        if(this.m_asDataSources[iIndexSource] === this.m_asShow_Tie_pointGrids[iIndexMask])
                        {
                            this.m_asDataSources.splice(iIndexSource,1);
                            iNumberOfDataSources--;
                            iIndexSource--;
                        }
                    }
                }
            }
        }

        return true;
    };

    MaskManagerController.prototype.deleteMaskByIndex = function(iIndex)
    {
        this.m_aoMasks.splice(iIndex,1);
    };

    /**
     *
     * @param sText
     * @param sIdTextArea
     * @returns {boolean}
     */
    MaskManagerController.prototype.addDataSourceLogicalMask = function(sText,sIdTextArea)
    {
        if(utilsIsStrNullOrEmpty(sIdTextArea))
            return false;
        var sString =  this.m_sTextAreaLogicalMask.replace('@',sText);
        if(sString === "")
            this.insertAtCaret(sIdTextArea,sText);
        else
            this.m_sTextAreaLogicalMask = sString;
        return true;
    };
    /**
     *
     * @param sFile
     * @param sBand
     * @returns {boolean}
     */
    MaskManagerController.prototype.getProductMasks = function(sFile,sBand, sWorkspaceId)
    {
        if( (utilsIsStrNullOrEmpty(sFile) === true) || (utilsIsStrNullOrEmpty(sBand) === true) || (utilsIsStrNullOrEmpty(sWorkspaceId)))
        {
            return false;
        }

        this.m_oSnapOperationService.getListOfProductMask(sFile,sBand,sWorkspaceId).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) === false)
            {
                //TODO ADD TO m_aoMasks
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION GET PRODUCT MASK DOESN'T WORK");
        });

        return true;
    };
    /**
     *
     * @param oProduct
     * @returns {*}
     */
    MaskManagerController.prototype.getBandList = function (oProduct)
    {
        if(utilsIsObjectNullOrUndefined(oProduct) === true)
        {
            return null;
        }
        if(utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands) === true)
        {
            return null;
        }
        var iNumberOfBands = oProduct.bandsGroups.bands.length;
        var asReturnArray = [];
        for(var iIndexProductBand = 0 ; iIndexProductBand < iNumberOfBands; iIndexProductBand++)
        {
            var sBandName = oProduct.bandsGroups.bands[iIndexProductBand].name;
            if(utilsIsStrNullOrEmpty(sBandName) === false)
            {
                asReturnArray.push(sBandName);
            }
        }

        return asReturnArray;

    };

    /**
     *
     * @returns {*}
     */
    MaskManagerController.prototype.getBodyImage = function()
    {
        var sFileName = this.m_oProduct.fileName;
        var sBandName = this.m_oBand.name;
        var sFilters = null;
        var iRectangleX = 0;
        var iRectangleY = 0;
        var iRectangleWidth = this.m_oBand.width;
        var iRectangleHeight= this.m_oBand.height;
        //get canvas map container
        var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
        var heightMapContainer = elementMapContainer[0].offsetHeight;
        var widthMapContainer = elementMapContainer[0].offsetWidth;

        //create body
        var oBodyImage = utilsProjectCreateBodyForProcessingBandImage(sFileName, sBandName, sFilters, iRectangleX, iRectangleY, iRectangleWidth, iRectangleHeight, widthMapContainer, heightMapContainer);
        oBodyImage.productMasks = [];
        oBodyImage.rangeMasks = [];
        oBodyImage.mathMasks = [];
        var iNumberOfMasks = this.m_aoMasks.length;
        // add masks operation into body
        for( var iIndexMask = 0 ; iIndexMask < iNumberOfMasks; iIndexMask++ )
        {

            var oColor = utilsConvertRGBAInObjectColor(this.m_aoMasks[iIndexMask].colour);
            if(utilsIsObjectNullOrUndefined(oColor) === true)
                break;
            var iRedRGB = oColor.red;
            var iGreenRGB = oColor.green;
            var iBlueRGB= oColor.blue;
            var fTransparencyRGB= oColor.transparency;
            if(this.m_aoMasks[iIndexMask].type === 'Range')
            {
                var iMin = this.m_aoMasks[iIndexMask].min;
                var iMax = this.m_aoMasks[iIndexMask].max;
                var oRangeMask = this.getRangeMaskBody(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,iMin,iMax);
                oBodyImage.rangeMasks.push(oRangeMask);
            }
            if(this.m_aoMasks[iIndexMask].type === 'Maths')
            {
                var sExpression = this.m_aoMasks[iIndexMask].description;
                var oMathMask = this.getMathMask(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,sExpression);
                oBodyImage.mathMasks.push(oMathMask);
            }
        }
        return oBodyImage;
    };
    /**
     *
     * @param iRedRGB
     * @param iGreenRGB
     * @param iBlueRGB
     * @param fTransparencyRGB
     * @param iMin
     * @param iMax
     * @returns {{colorBlue: *, colorGreen: *, colorRed: *, transparency: *, min: *, max: *}}
     */
    MaskManagerController.prototype.getRangeMaskBody = function(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,iMin,iMax)
    {

        var oBodyMask =   {
            "colorBlue": iBlueRGB,
            "colorGreen": iGreenRGB,
            "colorRed": iRedRGB,
            "transparency": fTransparencyRGB,
            "min": iMin,
            "max": iMax
        };
        return oBodyMask;
    };
    /**
     *
     * @param iRedRGB
     * @param iGreenRGB
     * @param iBlueRGB
     * @param fTransparencyRGB
     * @param sExpression
     * @returns {{colorBlue: *, colorGreen: *, colorRed: *, transparency: *, expression: *}}
     */
    MaskManagerController.prototype.getMathMask = function(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,sExpression)
    {
        var oBodyMask =   {
            "colorBlue": iBlueRGB,
            "colorGreen": iGreenRGB,
            "colorRed": iRedRGB,
            "transparency": fTransparencyRGB,
            "expression": sExpression
        };
        return oBodyMask;
    }

    MaskManagerController.prototype.initRangeListOfRasters = function()
    {
        // this.m_sRangeListOfRasters
        var iNumberOfBands = this.m_asBands.length;
        for(var iIndexBand = 0 ; iIndexBand < iNumberOfBands; iIndexBand++ )
        {
            this.m_sRangeListOfRasters.push(this.m_asBands[iIndexBand]);
        }
        var iNumberofTiePointGrids = this.m_asShow_Tie_pointGrids.length;
        for(var iIndex = 0; iIndex < iNumberofTiePointGrids; iIndex++)
        {
            this.m_sRangeListOfRasters.push(this.m_asShow_Tie_pointGrids[iIndex]);
        }


    }

    MaskManagerController.$inject = [
        '$scope',
        'close',
        'extras',
        '$timeout',
        'SnapOperationService',
        // 'FilterService'
        // 'GetParametersOperationService'
    ];
    return MaskManagerController;
})();
