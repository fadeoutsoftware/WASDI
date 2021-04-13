/**
 * Created by a.corrado on 31/03/2017.
 */


var EditPanelController = (function() {

    function EditPanelController($scope, oClose,oExtras,oSnapOperationService,$timeout,oWorkspaceService,oFilterService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oTime = $timeout;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oFilterService = oFilterService;
        this.m_oClose = oClose;

        this.m_oExtras = oExtras;
        this.m_oBand = oExtras.maskManager.band;
        this.m_oProduct = oExtras.maskManager.product;
        this.m_sWorkspaceId = oExtras.maskManager.workspaceId;
        this.m_sWorkspaceId =  oExtras.filterBand.workspaceId;
        this.m_oSelectedBand = oExtras.filterBand.selectedBand;
        this.m_iPanScalingValue = oExtras.colorManipulation.panScalingValue;

        this.m_sSelectedFilter = "";
        this.m_oSelectedTab = "MaskManager" // mask manager,filter band

        //mask selected
        this.m_oMasksSaved = oExtras.maskManager.masksSaved;
        // this.m_oBody = oExtras.body;
        this.m_sMaskColor = {
            rangeMaskColor:"rgb(0,143,255)",
            logicalMaskColor:"rgb(0,143,255)"
        };
        this.m_oWindowsOpened = {
            rangeMask:false,
            logicalMask:false
        };
        this.m_sFilterMaskTable = "";
        this.m_bAreProductMasksLoading = false;
        this.m_bIsLoadingColourManipulation = false;
        //MASK LIST
        this.m_aoMasks=[];
        //IF THERE AREN'T DIFFERENT
        if(utilsIsObjectNullOrUndefined(this.m_oMasksSaved ) === true)
        {
            this.getProductMasks(this.m_oProduct.fileName,this.m_oBand.name, this.m_sWorkspaceId);
        }
        else
        {
            this.m_aoMasks = this.m_oMasksSaved;
        }
        this.m_sRangeMinValue = 0.0;
        this.m_sRangeMaxValue = 1.0;
        this.m_sRangeSelectedRaster = "";
        this.m_sRangeListOfRasters=[];
        this.m_sTextAreaLogicalMask = "";
        this.m_bAreSelectedAllMasks = false;
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
        ];
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
            tiePoint:false
        };

        this.m_asDataSources = [];

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
        // TODO BUGGED METHOD
        // this.getProductColorManipulation(this.m_oProduct.fileName,this.m_oBand.name,true,this.m_sWorkspaceId);

        var oController = this;
        $scope.close = function(oReturnValue) {
            oClose(oReturnValue, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.saveMaskManager = function() {
            var result = {
                body:oController.getBodyImage(),
                listOfMasks:oController.m_aoMasks
            };
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        $scope.applyFilterOnClose = function(result) {
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
            oClose(oReturnValue, 300); // close, but give 500ms for bootstrap to animate

        };
    };

    /************************************* MASK MANAGER ***********************************************/
    /**
     *
     * @param sButtonClicked
     */
    EditPanelController.prototype.changeWindowsOpened = function(sButtonClicked)
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

    EditPanelController.prototype.cleanRangeMaskWindow = function()
    {
        this.m_sRangeMinValue = 0.0;
        this.m_sRangeMaxValue = 1.0;
        this.m_sRangeSelectedRaster = this.m_sRangeListOfRasters[0];
        this.m_sMaskColor.rangeMaskColor =  "rgb(0,143,255)" ;
    };

    EditPanelController.prototype.cleanLogicalMaskWindow = function()
    {
        this.m_sTextAreaLogicalMask = "";
        this.m_sMaskColor.logicalMaskColor = "rgb(0,143,255)";
    };

    EditPanelController.prototype.closeAllWindows = function(){
        this.m_oWindowsOpened.rangeMask = false;
        this.m_oWindowsOpened.logicalMask = false;
        this.cleanRangeMaskWindow();
        this.cleanLogicalMaskWindow();


    };

    EditPanelController.prototype.saveRangeMaskAndClose = function()
    {
        if( utilsIsObjectNullOrUndefined(this.m_aoMasks) )
        {
            this.closeAllWindows();
            return false;
        }

        var sDescription = this.m_sRangeMinValue + "<=" + this.m_sRangeSelectedRaster + "<=" + this.m_sRangeMaxValue ;
        var oReturnValue = {
            name:this.newUniqueNameGenerator(),
            type:"Range",
            colour:this.m_sMaskColor.rangeMaskColor,
            transparency: 0.5,
            description:sDescription,
            min:this.m_sRangeMinValue,
            max:this.m_sRangeMaxValue,
            isUserGeneratedMask: true,
            selected:true
        };
        utilsConvertRGBAInObjectColor(this.m_sMaskColor.rangeMaskColor);
        //add as firs element of array
        this.m_aoMasks.unshift(oReturnValue);
        this.closeAllWindows();
        return true;
    };

    EditPanelController.prototype.addOperationOrConstantOrFunctionLogicalMask = function(sValue,sIdTextArea)
    {
        if(utilsIsStrNullOrEmpty(sIdTextArea)) return false;

        this.insertAtCaret(sIdTextArea,sValue);

        return true;
    };

    /*
    *   https://stackoverflow.com/questions/1064089/inserting-a-text-where-cursor-is-using-javascript-jquery
    *   HOW TO INSERT TEXT IN CURSOR POSITION
    * */
    EditPanelController.prototype.insertAtCaret = function(areaId, text) {
        var txtarea = document.getElementById(areaId);
        if (!txtarea) { return; }

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

    };

    EditPanelController.prototype.saveLogicalMask = function(){
        if( utilsIsObjectNullOrUndefined(this.m_aoMasks) )
        {
            this.closeAllWindows();
            return false;
        }
        var sDescription = this.m_sTextAreaLogicalMask;
        var oReturnValue = {
            name:this.newUniqueNameGenerator(),
            type:"Maths",
            colour:this.m_sMaskColor.logicalMaskColor,
            transparency: 0.5,
            description:sDescription,
            isUserGeneratedMask: true,
            selected:true
        };
        //add as first element of array
        this.m_aoMasks.unshift(oReturnValue);
        this.closeAllWindows();
        return true;
    };

    EditPanelController.prototype.newUniqueNameGenerator = function()
    {

        var sName = "Mask";
        var iNumberOfMasks = this.m_aoMasks.length;
        var iNameCounter = 0;
        for(var iIndexMask = 0 ; iIndexMask < iNumberOfMasks; iIndexMask++ )
        {
            if( this.m_aoMasks[iIndexMask].name === sName )
            {
                iIndexMask = -1;
                sName = "Mask" + iNameCounter;
                iNameCounter = iNameCounter + 1;
                // continue;
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
    EditPanelController.prototype.checkedDataSource = function(sCheck,bBoolean)
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

    EditPanelController.prototype.deleteMaskByIndex = function(iIndex)
    {
        this.m_aoMasks.splice(iIndex,1);
    };

    /**
     *
     * @param sText
     * @param sIdTextArea
     * @returns {boolean}
     */
    EditPanelController.prototype.addDataSourceLogicalMask = function(sText,sIdTextArea)
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
    EditPanelController.prototype.getProductMasks = function(sFile,sBand, sWorkspaceId)
    {
        if( (utilsIsStrNullOrEmpty(sFile) === true) || (utilsIsStrNullOrEmpty(sBand) === true) || (utilsIsStrNullOrEmpty(sWorkspaceId)))
        {
            return false;
        }

        var oController = this;
        this.m_bAreProductMasksLoading = true;
        this.m_oSnapOperationService.getListOfProductMask(sFile,sBand,sWorkspaceId).then(function (data) {
            oController.m_bAreProductMasksLoading = false;
            if(utilsIsObjectNullOrUndefined(data.data) === false)
            {
                var aoProductMasks = data.data;

                if (utilsIsObjectNullOrUndefined(aoProductMasks)) return;

                for (var i=0; i<aoProductMasks.length; i++) {
                    var oWasdiMask = {};
                    oWasdiMask.type = aoProductMasks[i].maskType;
                    oWasdiMask.description = aoProductMasks[i].description;
                    oWasdiMask.name = aoProductMasks[i].name;
                    oWasdiMask.colour = "rgba("+aoProductMasks[i].colorRed+","+aoProductMasks[i].colorGreen+","+aoProductMasks[i].colorBlue+","+aoProductMasks[i].transparency+")";
                    oWasdiMask.originalMaskObject = aoProductMasks[i];
                    oWasdiMask.selected = false;
                    oController.m_aoMasks.push(oWasdiMask);
                }
            }

        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION GET PRODUCT MASK DOESN'T WORK");
            oController.m_bAreProductMasksLoading = false;
        });

        return true;
    };

    /**
     *
     * @param oProduct
     * @returns {*}
     */
    EditPanelController.prototype.getBandList = function (oProduct)
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
    EditPanelController.prototype.getBodyImage = function()
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
            if (!this.m_aoMasks[iIndexMask].selected) continue;

            var oColor = utilsConvertRGBAInObjectColor(this.m_aoMasks[iIndexMask].colour);
            if(utilsIsObjectNullOrUndefined(oColor) === true) break;

            var iRedRGB = oColor.red;
            var iGreenRGB = oColor.green;
            var iBlueRGB= oColor.blue;
            var fTransparencyRGB= 1.0-oColor.transparency;

            if(this.m_aoMasks[iIndexMask].type === 'Range' && !utilsIsObjectNullOrUndefined( this.m_aoMasks[iIndexMask].isUserGeneratedMask))
            {
                var iMin = this.m_aoMasks[iIndexMask].min;
                var iMax = this.m_aoMasks[iIndexMask].max;
                var oRangeMask = this.getRangeMaskBody(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,iMin,iMax);
                oBodyImage.rangeMasks.push(oRangeMask);
            }
            else if(this.m_aoMasks[iIndexMask].type === 'Maths' && !utilsIsObjectNullOrUndefined( this.m_aoMasks[iIndexMask].isUserGeneratedMask))
            {
                var sExpression = this.m_aoMasks[iIndexMask].description;
                var oMathMask = this.getMathMask(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,sExpression);
                oBodyImage.mathMasks.push(oMathMask);
            }
            else
            {
                var sName = this.m_aoMasks[iIndexMask].name;
                var oProductMask = this.getProductMask(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,sName);

                oBodyImage.productMasks.push(oProductMask);
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
    EditPanelController.prototype.getRangeMaskBody = function(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,iMin,iMax)
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
    EditPanelController.prototype.getMathMask = function(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,sExpression)
    {
        var oBodyMask = {
            "colorBlue": iBlueRGB,
            "colorGreen": iGreenRGB,
            "colorRed": iRedRGB,
            "transparency": fTransparencyRGB,
            "expression": sExpression
        };
        return oBodyMask;
    };

    EditPanelController.prototype.getProductMask = function(iRedRGB,iGreenRGB,iBlueRGB,fTransparencyRGB,sName)
    {
        var oBodyMask = {
            "colorBlue": iBlueRGB,
            "colorGreen": iGreenRGB,
            "colorRed": iRedRGB,
            "transparency": fTransparencyRGB,
            "name": sName
        };
        return oBodyMask;
    };

    EditPanelController.prototype.initRangeListOfRasters = function()
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
        this.m_sRangeSelectedRaster = this.m_sRangeListOfRasters[0];

    };
    EditPanelController.prototype.clickedSelectAllMasks = function(){
        var bNewStatusOfMask;
        if(this.m_bAreSelectedAllMasks === true)
        {
            bNewStatusOfMask = true;
        }
        else
        {
            bNewStatusOfMask = false;
        }

        var iNumberOfMasks = this.m_aoMasks.length;
        for(var iIndexOfMasks = 0; iIndexOfMasks < iNumberOfMasks ; iIndexOfMasks++)
        {
            this.m_aoMasks[iIndexOfMasks].selected = bNewStatusOfMask;
        }
    };

    EditPanelController.prototype.maskSavedSelected = function(){
        if(utilsIsObjectNullOrUndefined(this.m_oMasksSaved) === true)
        {
            return false;
        }

        var iNumberOfSavedMasks = this.m_oMasksSaved.length;
        for(var iIndexSavedMask = 0; iIndexSavedMask < iNumberOfSavedMasks; iIndexSavedMask++)
        {
            if( this.m_oMasksSaved[iIndexSavedMask].type === "Range" || this.m_oMasksSaved[iIndexSavedMask].type === "Maths" )
            {
                //if the saved mask is Range or Math
                this.m_aoMasks.unshift(this.m_oMasksSaved[iIndexSavedMask]);
            }
            else
            {
                //if the saved mask is a product mask
                var iNumberOfMasks = this.m_aoMasks.length;
                for(var iIndexMask = 0; iIndexMask < iNumberOfMasks; iIndexMask++)
                {
                    //find the mask saved previously in list of masks just loaded
                    if(this.m_aoMasks[iIndexMask].name === this.m_oMasksSaved[iIndexSavedMask].name)
                    {
                        this.m_aoMasks[iIndexMask].selected = this.m_oMasksSaved[iIndexSavedMask].selected;
                    }

                }
            }
        }


    };

    /************************************* FILTER BAND ***********************************************/

    EditPanelController.prototype.closeAndDonwloadProduct= function(result){

        this.m_oClose(result, 500); // close, but give 500ms for bootstrap to animate

    };

    EditPanelController.prototype.addUserFilterOptions = function()
    {

        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){}//this.value = oThat.m_iSelectedValue;
        };
        var aaoEmptyMatrix = this.makeEmptyMatrix(4,4,oDefaultValue);
        this.m_aoUserFilterOptions[0].options.push( {name:"NewFilter", matrix:aaoEmptyMatrix});
    };


    EditPanelController.prototype.makeEmptyMatrix = function(iNumberOfCollumns,iNumberOfRows,iDefaultValue){
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

    EditPanelController.prototype.getFilters = function()
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

    EditPanelController.prototype.numberOfRows = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix))
            return 0;
        return aaoMatrix.length;
    };

    EditPanelController.prototype.numberOfCollumns = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || aaoMatrix.length <= 0)
            return 0;
        return aaoMatrix[0].length;
    };


    EditPanelController.prototype.addRowInMatrix = function(aaoMatrix,oElementRow)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || utilsIsObjectNullOrUndefined(oElementRow)  )
            return false;
        var iNumberOfRowValues;
        if(aaoMatrix.length === 0)
            iNumberOfRowValues = 5;//default value
        else
            iNumberOfRowValues = aaoMatrix[0].length;

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

    EditPanelController.prototype.addDefaultRowInMatrix = function(aaoMatrix)
    {

        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){}//this.value = oThat.m_iSelectedValue;
        };
        return this.addRowInMatrix(aaoMatrix,oDefaultValue)
    };

    EditPanelController.prototype.removeRowInMatrix = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || aaoMatrix.length <= 0)
            return false;
        aaoMatrix.pop();
        return true;
    };

    EditPanelController.prototype.addColumnInMatrix = function(aaoMatrix,oDefaultValue)
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
    EditPanelController.prototype.addDefaultColumnInMatrix = function(aaoMatrix)
    {

        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){}//this.value = oThat.m_iSelectedValue;
        };
        return this.addColumnInMatrix(aaoMatrix,oDefaultValue)
    };
    EditPanelController.prototype.removeColumnInMatrix = function(aaoMatrix)
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

    EditPanelController.prototype.generateFiltersListFromServer = function(oData)
    {
        if(utilsIsObjectNullOrUndefined(oData)) return false;

        var asProperties = utilsGetPropertiesObject(oData);

        if(asProperties === []) return false;

        for(var iIndexProperty = 0; iIndexProperty < asProperties.length; iIndexProperty++)
        {
            for(var iIndexOptions = 0 ; iIndexOptions < oData[asProperties[iIndexProperty]].length ; iIndexOptions++)
            {
                var iNumberOfRows = oData[asProperties[iIndexProperty]][iIndexOptions].kernelHeight;
                var iNumberOfColumns = oData[asProperties[iIndexProperty]][iIndexOptions].kernelWidth;

                var oDefaultValue = {
                    color:"white" ,
                    fontcolor:"black",
                    value:"0",
                    // click:function(){this.value = oThat.m_iSelectedValue;}
                    click:function(){}
                };
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

    EditPanelController.prototype.generateArrayOptionsValues = function(aArrayValues)
    {
        if(utilsIsObjectNullOrUndefined(aArrayValues) === true)
            return [];


        var aReturnArray = [];
        var iNumberOfValues =  aArrayValues.length;
        for(var iIndexArrayValues = 0 ; iIndexArrayValues < iNumberOfValues; iIndexArrayValues++ )
        {
            var oDefaultValue = {
                color:"white" ,
                fontcolor:"black",
                value:"0",
                click:function(){}//this.value = oThat.m_iSelectedValue;
            };
            var iValue = aArrayValues[iIndexArrayValues];
            oDefaultValue.value = iValue;
            aReturnArray.push(oDefaultValue);
        }

        return aReturnArray;
    };

    EditPanelController.prototype.initMatrix = function(aaoMatrix,aoValues)
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


    EditPanelController.prototype.getSelectedOptionsObject = function(sName)
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

    EditPanelController.prototype.collapsePanels = function()
    {
        jQuery('.collapse').collapse('hide');
    };
    /************************************** COLOR MANIPULATION *****************************************/

    EditPanelController.prototype.getProductColorManipulation = function(sFile,sBand,bAccurate,sWorkspaceId)
    {
        if( utilsIsStrNullOrEmpty(sFile) === true || utilsIsStrNullOrEmpty(sBand) === true || utilsIsStrNullOrEmpty(sWorkspaceId) === true || utilsIsObjectNullOrUndefined(bAccurate) === true )
        {
            return false;
        }
        var oController = this;
        this.m_bIsLoadingColourManipulation = true;
        this.m_oSnapOperationService.getProductColorManipulation(sFile,sBand,bAccurate,sWorkspaceId).then(function (data, status) {
            if (data.data != null)
            {
                if (data.data != undefined)
                {
                     //oController.m_oColorManipulation = data.data;
                     if (utilsIsObjectNullOrUndefined(oController.m_oBand) === false)
                     {
                         oController.m_oBand.colorManipulation = data.data;
                         oController.drawColourManipulationHistogram("colourManipulationContainer",data.data.histogramBins);

                   }
                }
            }
            oController.m_bIsLoadingColourManipulation = false;
        },function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>PRODUCT COLOR MANIPULATION ');
            oController.m_bIsLoadingColourManipulation = false;
        });

        return true;
    };

    EditPanelController.prototype.drawColourManipulationHistogram = function(sNameDiv,afValues)
    {

        if(utilsIsStrNullOrEmpty(sNameDiv) === true)
        {
            return false;
        }
        var oHTMLElement = angular.element(document).find(sNameDiv);
        if(utilsIsObjectNullOrUndefined(oHTMLElement) === true || oHTMLElement.length === 0 )
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(afValues) === true)
        {
            return false;
        }
        //todo test
        var x = utilsGenerateArrayWithFirstNIntValue(0,afValues.length);
        if(utilsIsObjectNullOrUndefined(x) === true)
        {
            return false;
        }

        var trace = {
            x: x ,
            y: afValues,
            // type: 'histogram'
            type: 'bar',
            marker: {
                color: '#43516A',
            }
        };
        var data = [trace];
        var oHistogramSize = this.calculateHistogramDimension(0.7,0.7);

        var layout = {
            // title: "Colour Manipolation",
            showlegend: false,
            height:oHistogramSize.height,
            //height:200,
            //width:500,
            width:oHistogramSize.width,
            xaxis: {
                showgrid: true,
                zeroline: true,
            },
            paper_bgcolor:"#000000",
            // paper_bgcolor:"#EF4343",
            margin: {
                l: 5,
                r: 5,
                b: 5,
                t: 5,
                pad: 4
            },

        };

        Plotly.newPlot(sNameDiv, data,layout,{staticPlot: true});//,layout,{staticPlot: true}

        return true;
    };

    /**
     * processingProductColorManipulation
     */
    EditPanelController.prototype.processingProductColorManipulation = function()
    {
        // if (utilsIsObjectNullOrUndefined(this.m_oActiveBand) === true) return;

        var sWorkspaceId,oBand;
        oBand = this.m_oBand;
        sWorkspaceId = this.m_sWorkspaceId;

        //get map size
        var oMapContainerSize = this.getMapContainerSize(this.m_iPanScalingValue);
        var heightMapContainer = oMapContainerSize.height;
        var widthMapContainer = oMapContainerSize.width;

        var sFileName = this.m_oProduct.fileName;
        //get body
        var oBodyMapContainer = this.createBodyForProcessingBandImage(sFileName,oBand.name,oBand.actualFilter,0,0,
            oBand.width, oBand.height,widthMapContainer, heightMapContainer, oBand.colorManipulation);
        //processing image with color manipulation
        var oReturnValue = {
            bodyMapContainer:oBodyMapContainer,
            workspaceId:sWorkspaceId
        };

        // this.m_oClose(oReturnValue, 500);
        this.m_oClose(oReturnValue, 1000);
        // this.processingGetBandImage(oBodyMapContainer, sWorkspaceId);
    };

    EditPanelController.prototype.getMapContainerSize = function( iScalingValue){
        var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
        //var elementMapContainer = angular.element(document.querySelector('#editPanelBody'));
        var heightMapContainer = elementMapContainer[0].offsetHeight * iScalingValue;
        var widthMapContainer = elementMapContainer[0].offsetWidth * iScalingValue;

        return {
            height:heightMapContainer,
            width:widthMapContainer
        };

    };

    EditPanelController.prototype.getModalBodySize = function( ){
        var elementModalBody = angular.element(document.querySelector('#editPanelBody'));
        //var elementMapContainer = angular.element(document.querySelector('#editPanelBody'));
        if(utilsIsObjectNullOrUndefined(elementModalBody) === true || utilsIsObjectNullOrUndefined(elementModalBody[0]) === true ||
            elementModalBody[0].hasOwnProperty("offsetHeight") === false || elementModalBody[0].hasOwnProperty("offsetWidth") === false )
        {
            return {
                height:"",
                width:""
            };
        }

        var heightMapContainer = elementModalBody[0].offsetHeight ;
        var widthMapContainer = elementModalBody[0].offsetWidth ;

        return {
            height:heightMapContainer,
            width:widthMapContainer
        };

    };

    EditPanelController.prototype.calculateHistogramDimension = function(fHeightScaleValue,fWidthScaleValue)
    {
        var oWindowSize = this.getModalBodySize();
        if(utilsIsObjectNullOrUndefined(oWindowSize))
        {
            return null;
        }
        if( (utilsIsANumber(oWindowSize.height) === false) || (utilsIsANumber(oWindowSize.width) === false))
        {
            return null;
        }
        oWindowSize.height = oWindowSize.height * fHeightScaleValue;
        oWindowSize.width = oWindowSize.width * fWidthScaleValue;
        return oWindowSize
    };


    EditPanelController.prototype.createBodyForProcessingBandImage = function(sFileName, sBandName, sFilters, iRectangleX, iRectangleY,
                                                                           iRectangleWidth, iRectangleHeight, iOutputWidth, iOutputHeight,oColorManipulation){

        var oBandImageBody = {
            "productFileName": sFileName,
            "bandName": sBandName,
            "filterVM": sFilters,
            "vp_x": iRectangleX,
            "vp_y": iRectangleY,
            "vp_w": iRectangleWidth,
            "vp_h": iRectangleHeight,
            "img_w": iOutputWidth,
            "img_h": iOutputHeight,
            "colorManiputalion":oColorManipulation
        };

        return oBandImageBody;
    };

    EditPanelController.prototype.getDefaultProductColorManipulation = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oBand.colorManipulation ) === false)
        {
            delete this.m_oBand.colorManipulation;
            //without the property body.colorManipulation the server return default colorManipulation
        }
        //get default value of color manipolation
        this.processingProductColorManipulation();
    };


    /**
     *
     * @param oColorManipulation
     * @returns {boolean}
     */
    EditPanelController.prototype.adjust95percentageColourManipulation = function(oColorManipulation)
    {
        if(utilsIsObjectNullOrUndefined(oColorManipulation) === true)
        {
            return false;
        }
        //min value
        oColorManipulation.colors[0].value = oColorManipulation.histogramMin;
        //average value
        oColorManipulation.colors[1].value = (oColorManipulation.histogramMax / 2 );
        //max value
        oColorManipulation.colors[2].value = oColorManipulation.histogramMax;

        this.processingProductColorManipulation();

        return true;
    };

    EditPanelController.prototype.generateColor = function(oColors)
    {
        if( utilsIsObjectNullOrUndefined(oColors) === true )
        {
            return "";
        }

        return "rgb(" + oColors.colorBlue + "," + oColors.colorGreen + "," + oColors.colorRed +")";
    };

    EditPanelController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        '$timeout',
        'WorkspaceService',
        'FilterService'
    ];
    return EditPanelController;
})();
window.EditPanelController = EditPanelController;
