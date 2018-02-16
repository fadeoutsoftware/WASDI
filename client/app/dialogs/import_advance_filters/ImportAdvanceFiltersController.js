

var ImportAdvanceFiltersController = (function() {

    function ImportAdvanceFiltersController($scope, oClose,oExtras,oWorkspaceService,oProductService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProduct = this.m_oExtras.product;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oProductService = oProductService;
        this.m_sFileName = "";

        this.m_oAdvanceFilter = {
            filterActive:"Seasons",//Seasons,Range,Months
            savedData:[],
            selectedSeasonYears:[],
            selectedSeason:"",
            listOfYears:[],
            listOfMonths:[],
            // listOfDays:[],
            selectedYears:[],
            selectedDayFrom:"",
            selectedDayTo:"",
            selectedMonthFrom:"",
            selectedMonthTo:"",
            selectedYearsSearchForMonths:[],
            selectedMonthsSearchForMonths:[]
        };

        this.initDefaultYears();
        this.initDefaultMonths();

        var oController = this;
        $scope.close = function(result) {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function(result) {
            var aoSavedData = oController.m_oAdvanceFilter.savedData;
            oClose(aoSavedData, 300); // close, but give 500ms for bootstrap to animate
        };

    };

    ImportAdvanceFiltersController.prototype.getPeriod = function(iMonthFrom,iDayFrom,iMonthTo,iDayTo){
        if( (utilsIsObjectNullOrUndefined(iMonthFrom) === true) || (utilsIsObjectNullOrUndefined(iDayFrom) === true) ||
            (utilsIsObjectNullOrUndefined(iMonthTo) === true) || (utilsIsObjectNullOrUndefined(iDayTo) === true) )
            return null;

        var dateSensingPeriodFrom = new Date();
        var dateSensingPeriodTo = new Date();
        dateSensingPeriodFrom.setMonth(iMonthFrom);
        dateSensingPeriodFrom.setDate(iDayFrom);
        dateSensingPeriodTo.setMonth(iMonthTo);
        dateSensingPeriodTo.setDate(iDayTo);
        return{
            dateSensingPeriodFrom:dateSensingPeriodFrom,
            dateSensingPeriodTo:dateSensingPeriodTo
        }
    };

    ImportAdvanceFiltersController.prototype.getPeriodSpring = function()
    {
        return this.getPeriod(02,21,05,20)
    };
    ImportAdvanceFiltersController.prototype.getPeriodSummer = function()
    {
        return this.getPeriod(05,21,08,22)
    };
    ImportAdvanceFiltersController.prototype.getPeriodWinter = function()
    {
        return this.getPeriod(11,21,02,20)
    };
    ImportAdvanceFiltersController.prototype.getPeriodAutumn = function()
    {
        return this.getPeriod(08,23,11,20)
    };

    ImportAdvanceFiltersController.prototype.addSeason = function()
    {
        var sSeason = this.m_oAdvanceFilter.selectedSeason;

        switch(sSeason) {
            case "spring":
                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodSpring();
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Spring";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }

                }

                break;
            case "summer":

                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodSummer();
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Summer";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }
                }
                break;
            case "autumn":
                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodAutumn();
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Autumn";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }
                }
                break;
            case "winter":
                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodWinter();
                        // P.Campanella 10/02/2018: the winter start in yyyy and ends in yyyy+1. Or viceversa yyyy-1 to yyyyy
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]-1);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Winter";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }
                }
                break;
        }
    };

    ImportAdvanceFiltersController.prototype.saveDataInAdvanceFilter = function(sName,oData)
    {
        if(utilsIsObjectNullOrUndefined(oData) === true || utilsIsStrNullOrEmpty(sName) === true)
            return false;

        var oSaveData = {
            name:sName,
            data:oData
        };

        var iNumberOfSaveData = this.m_oAdvanceFilter.savedData.length;
        for(var iIndexSaveData = 0; iIndexSaveData < iNumberOfSaveData; iIndexSaveData++)
        {
            if(this.m_oAdvanceFilter.savedData[iIndexSaveData].name === oSaveData.name)
            {
                return false;
            }
        }
        this.m_oAdvanceFilter.savedData.push(oSaveData);

        return true;
    };

    ImportAdvanceFiltersController.prototype.removeSaveDataChips = function(oData)
    {
        if(utilsIsObjectNullOrUndefined(oData) === true)
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.savedData) === true)
            return false;
        var iNumberOfSaveData = this.m_oAdvanceFilter.savedData.length;

        for(var iIndexNumberOfSaveData = 0; iIndexNumberOfSaveData < iNumberOfSaveData; iIndexNumberOfSaveData++)
        {
            if(this.m_oAdvanceFilter.savedData[iIndexNumberOfSaveData] === oData)
            {
                this.m_oAdvanceFilter.savedData.splice(iIndexNumberOfSaveData, 1);
                break;
            }
        }

        return true;
    };

    ImportAdvanceFiltersController.prototype.initDefaultYears = function()
    {
        var oActualDate = new Date();
        var iYears = oActualDate.getFullYear();
        for(var iIndex = 0 ; iIndex < 20; iIndex++)
        {
            this.m_oAdvanceFilter.listOfYears.push(iYears.toString());
            iYears--;
        }

    };
    // ImportController.prototype.initDefaultDays = function()
    // {
    //
    //     for(var iIndex = 0 ; iIndex < 31; iIndex++)
    //     {
    //         var sIndex = (iIndex + 1).toString();
    //         this.m_oAdvanceFilter.listOfDays.push(sIndex);
    //     }
    //
    // };
    ImportAdvanceFiltersController.prototype.initDefaultMonths = function()
    {
        /*
            January - 31 days
            February - 28 days in a common year and 29 days in leap years
            March - 31 days
            April - 30 days
            May - 31 days
            June - 30 days
            July - 31 days
            August - 31 days
            September - 30 days
            October - 31 days
            November - 30 days
            December - 31 days
        * */
        var asMonths = ["January","February","March","April","May","June","July","August","September","October","November","December"];
        for(var iIndex = 0 ; iIndex < asMonths.length; iIndex++)
        {
            this.m_oAdvanceFilter.listOfMonths.push(asMonths[iIndex]);
        }

    };

    ImportAdvanceFiltersController.prototype.getMonthDays = function(sMonth, sYear)
    {
        var sMonthLowerCase = sMonth.toLocaleLowerCase();
        switch(sMonthLowerCase) {
            case "january":
                return 31;
                break;
            case "february":

                if(utilsLeapYear(sYear))
                {
                    return 29
                }
                else
                {
                    return 28;
                }
                break;
            case "march":
                return 31;
                break;
            case "april":
                return 30;
                break;
            case "may":
                return 31;
                break;
            case "june":
                return 30;
                break;
            case "july":
                return 31;
                break;
            case "august":
                return 31;
                break;
            case "september":
                return 30;
                break;
            case "october":
                return 31;
                break;
            case "november":
                return 30;
                break;
            case "december":
                return 31;
                break;

        }
    };

    ImportAdvanceFiltersController.prototype.getMonthDaysFromRangeOfMonths = function(sMonth, asYears)
    {
        if(utilsIsObjectNullOrUndefined(asYears) === true)
            return [];
        if(asYears.length < 1)
            return [];
        var iNumberOfYears = asYears.length;
        var sMonthLowerCase = sMonth.toLocaleLowerCase();
        var iReturnValue = 0;
        if( (sMonthLowerCase === "february") )
        {
            for(var iIndexYear = 0 ; iIndexYear < iNumberOfYears; iIndexYear++)
            {
                var iTemp = this.getMonthDays(sMonth,asYears[iIndexYear])
                //if true it takes the new value, in the case of leap years it takes 29 days
                if(iTemp > iReturnValue)
                    iReturnValue = iTemp;
            }
        }
        else
        {
            iReturnValue = this.getMonthDays(sMonth,asYears[0]);
        }
        return utilsGenerateArrayWithFirstNIntValue(iReturnValue);
        //check leap year
    };
    ImportAdvanceFiltersController.prototype.getMonthDaysFrom = function(){
        return this.getMonthDaysFromRangeOfMonths(this.m_oAdvanceFilter.selectedMonthFrom, this.m_oAdvanceFilter.selectedYears)
    };
    ImportAdvanceFiltersController.prototype.getMonthDaysTo = function(){
        return this.getMonthDaysFromRangeOfMonths(this.m_oAdvanceFilter.selectedMonthTo, this.m_oAdvanceFilter.selectedYears)
    };

    ImportAdvanceFiltersController.prototype.areSelectedMonthAndYearFrom = function(){
        return (this.m_oAdvanceFilter.selectedYears.length !== 0 && this.m_oAdvanceFilter.selectedMonthFrom !== "");
    };
    ImportAdvanceFiltersController.prototype.areSelectedMonthAndYearTo = function(){
        return (this.m_oAdvanceFilter.selectedYears.length !== 0 && this.m_oAdvanceFilter.selectedMonthTo !== "");
    };

    ImportAdvanceFiltersController.prototype.addFilterDataFromTo = function(){
        if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedYears) === true ) return false;
        var iNumberOfSelectedYears = this.m_oAdvanceFilter.selectedYears.length;
        for(var iIndexYear = 0; iIndexYear < iNumberOfSelectedYears; iIndexYear++ )
        {
            var sName="";
            sName +=   this.m_oAdvanceFilter.selectedDayFrom.toString() + "/" + this.m_oAdvanceFilter.selectedMonthFrom.toString();
            sName +=  " - " +this.m_oAdvanceFilter.selectedDayTo.toString() + "/" + this.m_oAdvanceFilter.selectedMonthTo.toString();
            sName += " " + this.m_oAdvanceFilter.selectedYears[iIndexYear].toString();

            var dateSensingPeriodFrom = new Date();
            var dateSensingPeriodTo = new Date();
            dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedYears[iIndexYear]);
            dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthFrom));
            // TODO CHECK LEAP YEAS (29 DAYS FEBRUARY)
            dateSensingPeriodFrom.setDate( this.m_oAdvanceFilter.selectedDayFrom);
            dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedYears[iIndexYear]);
            dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthTo));
            dateSensingPeriodTo.setDate(this.m_oAdvanceFilter.selectedDayTo);
            var oData={
                dateSensingPeriodFrom:dateSensingPeriodFrom,
                dateSensingPeriodTo:dateSensingPeriodTo
            };
            this.saveDataInAdvanceFilter(sName,oData);
        }

    };

    ImportAdvanceFiltersController.prototype.convertNameMonthInNumber = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return -1;

        var sMonthLowerCase = sName.toLocaleLowerCase();
        switch(sMonthLowerCase) {
            case "january":
                return 0;
                break;
            case "february":
                return 1;
                break;
            case "march":
                return 2;
                break;
            case "april":
                return 3;
                break;
            case "may":
                return 4;
                break;
            case "june":
                return 5;
                break;
            case "july":
                return 6;
                break;
            case "august":
                return 7;
                break;
            case "september":
                return 8;
                break;
            case "october":
                return 9;
                break;
            case "november":
                return 10;
                break;
            case "december":
                return 11;
                break;

        }
        return -1;
    };

    /**
     *
     * @returns {boolean}
     */
    ImportAdvanceFiltersController.prototype.addFilterMonths = function()
    {
        if( (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedYearsSearchForMonths) === true) || (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedMonthsSearchForMonths) === true) )
        {
            return false;
        }
        var iNumberOfSelectedYears = this.m_oAdvanceFilter.selectedYearsSearchForMonths.length;
        var iNumberOfSelectedMonths = this.m_oAdvanceFilter.selectedMonthsSearchForMonths.length;

        for(var iIndexYear = 0; iIndexYear < iNumberOfSelectedYears; iIndexYear++)
        {
            for(var iIndexMonth = 0; iIndexMonth < iNumberOfSelectedMonths; iIndexMonth++)
            {
                var sName = this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear].toString() +" "+  this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth];
                var dateSensingPeriodFrom = new Date();
                var dateSensingPeriodTo = new Date();
                dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear]);
                dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth]));
                dateSensingPeriodFrom.setDate(1);
                dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear]);
                dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth]));
                dateSensingPeriodTo.setDate(this.getMonthDays(this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth],this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear]));
                var oData={
                    dateSensingPeriodFrom:dateSensingPeriodFrom,
                    dateSensingPeriodTo:dateSensingPeriodTo
                };
                this.saveDataInAdvanceFilter(sName,oData);
            }
        }
        return true;
    };
    ImportAdvanceFiltersController.prototype.cleanAdvanceFilters = function()
    {

        this.m_oAdvanceFilter.selectedSeasonYears = [];
        this.m_oAdvanceFilter.selectedYears = [];
        this.m_oAdvanceFilter.selectedDayFrom = "";
        this.m_oAdvanceFilter.selectedDayTo = "";
        this.m_oAdvanceFilter.selectedMonthFrom = "";
        this.m_oAdvanceFilter.selectedMonthTo = "";
        this.m_oAdvanceFilter.selectedYearsSearchForMonths = [];
        this.m_oAdvanceFilter.selectedMonthsSearchForMonths = [];

    };

    ImportAdvanceFiltersController.prototype.removeAllAdvanceSavedFilters = function()
    {
        this.m_oAdvanceFilter.savedData = [];
    };

    ImportAdvanceFiltersController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService'

    ];
    return ImportAdvanceFiltersController;
})();
