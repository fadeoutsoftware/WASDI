/**
 * Created by a.corrado on 31/03/2017.
 */


var MaskManagerController = (function() {

    function MaskManagerController($scope, oClose, oExtras,$timeout) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWindowsOpened = {
            rangeMask:false,
            logicalMask:false
        };
        this.m_aoMasks=[];
        this.m_sRangeMinValue = 0.0;
        this.m_sRangeMaxValue = 1.0;
        this.m_sRangeSelectedRaster = "Defaultvalue";
        this.m_sRangeListOfRasters=[""];
        this.m_oTime = $timeout;
        this.m_sTextAreaLogicalMask = "";
        this.m_asColors = [
            "rgb(0,0,0)",
            "rgb(64,64,64)",
            "rgb(128,128,128)",
            "rgb(192,192,192)",
            "rgb(255,255,255)",
            "rgb(0,255,255)",
            "rgb(0,0,255)",
            "rgb(255,0,255)",
            "rgb(255,255,0)",
            "rgb(255,200,0)",
            "rgb(255,0,0)",
            "rgb(255,175,175)",
            "rgb(0,255,0)",
        ];
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
        this.m_asBands = [
            "Band Test1",
            "Band Test2",
            "Band Test3",
            "Band Test4",
            "Band Test5",
            "Band Test6",
            "Band Test7",
            "Band Test8",
            "Band Test9",
            "Band Test10",
        ];
        this.m_asShow_Tie_pointGrids = [
            "latitude",
            "longitude",
            "incident_angle",
            "elevation_angle",
            "slant_range_time"
        ];
        this.m_oCheckboxs = {
            bands:false,
            masks:false,
            tiePoint:false,
        };

        this.m_asDataSources = [];
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };


    };

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

    MaskManagerController.prototype.closeAllWindows = function(){
        this.m_oWindowsOpened.rangeMask = false;
        this.m_oWindowsOpened.logicalMask = false;
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
            colour:"rgb(255,0,0)",
            transparency: 0.5,
            description:sDescription
        };

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
            colour:"rgb(255,0,0)",
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

        MaskManagerController.$inject = [
            '$scope',
            'close',
            'extras',
            '$timeout'
            // 'GetParametersOperationService'
        ];
        return MaskManagerController;
    })();
