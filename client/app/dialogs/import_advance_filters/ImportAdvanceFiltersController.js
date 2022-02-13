

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

        //TODO REMOVE IT
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
        this.test=[{
            description:'miao',
            selected:false,
            link:''
        }];
        this.m_oAdvanceFilterOptions = {
            listOfYears:this.getLastNYears(20),
            listOfMonths:this.getListOfMonths(),
            listOfDays:this.getListOfDays(31),
            listOfSeasons:this.getSeasonsList(),
            selectedYears:[],
            selectedMonths:[],
            selectedDays:[],
            selectedSeasons:[],
            savedData:[]
        }
        // this.getListOfYearsObjects(20);
        // this.getListOfMonthsObject();
        this.initDefaultYears();//TODO REMOVE IT
        this.initDefaultMonths();//TODO REMOVE IT

        var oController = this;
        $scope.close = function(result) {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function(result) {
            var aoSavedData = oController.m_oAdvanceFilterOptions.savedData;
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
        return this.getPeriod(2,21,5,20)
    };
    ImportAdvanceFiltersController.prototype.getPeriodSummer = function()
    {
        return this.getPeriod(5,21,8,22)
    };
    ImportAdvanceFiltersController.prototype.getPeriodWinter = function()
    {
        return this.getPeriod(11,21,2,20)
    };
    ImportAdvanceFiltersController.prototype.getPeriodAutumn = function()
    {
        return this.getPeriod(8,23,11,20)
    };



    ImportAdvanceFiltersController.prototype.saveDataInAdvanceFilter = function(sName,oData,aoSaveData)
    {
        if(utilsIsObjectNullOrUndefined(oData) === true || utilsIsStrNullOrEmpty(sName) === true || utilsIsObjectNullOrUndefined(aoSaveData))
            return false;

        var oSaveData = {
            name:sName,
            data:oData
        };

        var iNumberOfSaveData = aoSaveData.length;
        for(var iIndexSaveData = 0; iIndexSaveData < iNumberOfSaveData; iIndexSaveData++)
        {
            if(aoSaveData[iIndexSaveData].name === oSaveData.name)
            {
                return false;
            }
        }
        aoSaveData.push(oSaveData);

        return true;
    };

    ImportAdvanceFiltersController.prototype.removeSaveDataChips = function(oData)
    {
        if(utilsIsObjectNullOrUndefined(oData) === true)
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions.savedData) === true)
            return false;
        var iNumberOfSaveData = this.m_oAdvanceFilterOptions.savedData.length;

        for(var iIndexNumberOfSaveData = 0; iIndexNumberOfSaveData < iNumberOfSaveData; iIndexNumberOfSaveData++)
        {
            if(this.m_oAdvanceFilterOptions.savedData[iIndexNumberOfSaveData] === oData)
            {
                this.m_oAdvanceFilterOptions.savedData.splice(iIndexNumberOfSaveData, 1);
                break;
            }
        }

        return true;
    };

    ImportAdvanceFiltersController.prototype.initDefaultYears = function()
    {
        // var oActualDate = new Date();
        // var iYears = oActualDate.getFullYear();
        // for(var iIndex = 0 ; iIndex < 20; iIndex++)
        // {
        //     this.m_oAdvanceFilter.listOfYears.push(iYears.toString());
        //     iYears--;
        // }
        this.m_oAdvanceFilter.listOfYears = this.getLastNYears(20);
    };

    ImportAdvanceFiltersController.prototype.getLastNYears = function(iN){
        if(utilsIsInteger(iN) === false)
        {
            return null;
        }
        var aiReturnListOfYeras = [];
        var oActualDate = new Date();
        var iYears = oActualDate.getFullYear();
        for( var iIndexYear = 0; iIndexYear < iN; iIndexYear++){
            aiReturnListOfYeras.push(iYears.toString());
            iYears--;
        }
        return aiReturnListOfYeras;
    }

    // ImportAdvanceFiltersController.prototype.getListOfYearsObjects = function(iN)
    // {
    //     if(utilsIsInteger(iN) === false)
    //     {
    //         return null;
    //     }
    //     var oActualDate = new Date();
    //     var iYears = oActualDate.getFullYear();
    //     for( var iIndexYear = 0; iIndexYear < iN; iIndexYear++){
    //         var oYear = {
    //             description:iYears.toString(),
    //             selected:false,
    //             link:""
    //         };
    //         //var oyear
    //         this.m_oAdvanceFilterOptions.listOfYears.push(oYear);
    //         iYears--;
    //     }
    //     // this
    // }
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
    ImportAdvanceFiltersController.prototype.getListOfMonths = function()
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
        var asReturnValue = [];
        for(var iIndex = 0 ; iIndex < asMonths.length; iIndex++)
        {
            asReturnValue.push(asMonths[iIndex]);
        }
        return asReturnValue;

    };
    // ImportAdvanceFiltersController.prototype.getListOfMonthsObject = function()
    // {
    //     /*
    //         January - 31 days
    //         February - 28 days in a common year and 29 days in leap years
    //         March - 31 days
    //         April - 30 days
    //         May - 31 days
    //         June - 30 days
    //         July - 31 days
    //         August - 31 days
    //         September - 30 days
    //         October - 31 days
    //         November - 30 days
    //         December - 31 days
    //     * */
    //     var asMonths = ["January","February","March","April","May","June","July","August","September","October","November","December"];
    //     for(var iIndex = 0 ; iIndex < asMonths.length; iIndex++)
    //     {
    //         var oMonth = {
    //             description:asMonths[iIndex],
    //             selected:false,
    //             link:""
    //         }
    //         this.m_oAdvanceFilterOptions.listOfMonths.push(oMonth);
    //     }
    //
    // };
    /**
     *
     * @param sMonth
     * @param sYear
     * @returns {number}
     */
    ImportAdvanceFiltersController.prototype.getMonthDays = function(sMonth, sYear)
    {
        var sMonthLowerCase = sMonth.toLocaleLowerCase();
        switch(sMonthLowerCase) {
            case "january":
                return 31;
            case "february":

                if(utilsLeapYear(sYear))
                {
                    return 29
                }
                else
                {
                    return 28;
                }
            case "march":
                return 31;
            case "april":
                return 30;
            case "may":
                return 31;
            case "june":
                return 30;
            case "july":
                return 31;
            case "august":
                return 31;
            case "september":
                return 30;
            case "october":
                return 31;
            case "november":
                return 30;
            case "december":
                return 31;

        }
    };
    /**
     *
     * @param iNumberOfDayes
     * @returns {Array}
     */
    ImportAdvanceFiltersController.prototype.getListOfDays = function(iNumberOfDayes){
        if(utilsIsInteger(iNumberOfDayes) === false)
        {
            return [];
        }
        var asReturnValue = [];
        for(var iIndex = 0 ; iIndex < iNumberOfDayes; iIndex++ )
        {
            asReturnValue.push((iIndex+1).toString());
        }
        return asReturnValue;
    };

    /**
     *
     * @param sMonth
     * @param asYears
     * @returns {*}
     */
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
        return utilsGenerateArrayWithFirstNIntValue(1,iReturnValue);
        //check leap year
    };
    // ImportAdvanceFiltersController.prototype.getMonthDaysFrom = function(){
    //     return this.getMonthDaysFromRangeOfMonths(this.m_oAdvanceFilter.selectedMonthFrom, this.m_oAdvanceFilter.selectedYears)
    // };
    // ImportAdvanceFiltersController.prototype.getMonthDaysTo = function(){
    //     return this.getMonthDaysFromRangeOfMonths(this.m_oAdvanceFilter.selectedMonthTo, this.m_oAdvanceFilter.selectedYears)
    // };
    //
    // ImportAdvanceFiltersController.prototype.areSelectedMonthAndYearFrom = function(){
    //     return (this.m_oAdvanceFilter.selectedYears.length !== 0 && this.m_oAdvanceFilter.selectedMonthFrom !== "");
    // };
    // ImportAdvanceFiltersController.prototype.areSelectedMonthAndYearTo = function(){
    //     return (this.m_oAdvanceFilter.selectedYears.length !== 0 && this.m_oAdvanceFilter.selectedMonthTo !== "");
    // };
    //
    // ImportAdvanceFiltersController.prototype.addFilterDataFromTo = function(){
    //     if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedYears) === true ) return false;
    //     var iNumberOfSelectedYears = this.m_oAdvanceFilter.selectedYears.length;
    //     for(var iIndexYear = 0; iIndexYear < iNumberOfSelectedYears; iIndexYear++ )
    //     {
    //         var sName="";
    //         sName +=   this.m_oAdvanceFilter.selectedDayFrom.toString() + "/" + this.m_oAdvanceFilter.selectedMonthFrom.toString();
    //         sName +=  " - " +this.m_oAdvanceFilter.selectedDayTo.toString() + "/" + this.m_oAdvanceFilter.selectedMonthTo.toString();
    //         sName += " " + this.m_oAdvanceFilter.selectedYears[iIndexYear].toString();
    //
    //         var dateSensingPeriodFrom = new Date();
    //         var dateSensingPeriodTo = new Date();
    //         dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedYears[iIndexYear]);
    //         dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthFrom));
    //         // TODO CHECK LEAP YEAS (29 DAYS FEBRUARY)
    //         dateSensingPeriodFrom.setDate( this.m_oAdvanceFilter.selectedDayFrom);
    //         dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedYears[iIndexYear]);
    //         dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthTo));
    //         dateSensingPeriodTo.setDate(this.m_oAdvanceFilter.selectedDayTo);
    //         var oData={
    //             dateSensingPeriodFrom:dateSensingPeriodFrom,
    //             dateSensingPeriodTo:dateSensingPeriodTo
    //         };
    //         this.saveDataInAdvanceFilter(sName,oData);
    //     }
    //
    // };

    ImportAdvanceFiltersController.prototype.convertNameMonthInNumber = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return -1;

        var sMonthLowerCase = sName.toLocaleLowerCase();
        switch(sMonthLowerCase) {
            case "january":
                return 0;
            case "february":
                return 1;
            case "march":
                return 2;
            case "april":
                return 3;
            case "may":
                return 4;
            case "june":
                return 5;
            case "july":
                return 6;
            case "august":
                return 7;
            case "september":
                return 8;
            case "october":
                return 9;
            case "november":
                return 10;
            case "december":
                return 11;

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
    /**
     *
     * @returns {[string,string,string,string]}
     */
    ImportAdvanceFiltersController.prototype.getSeasonsList = function()
    {
        return ["Spring","Summer","Autumn","Winter"];
    };
    ImportAdvanceFiltersController.prototype.removeSavedData = function()
    {
        this.m_oAdvanceFilterOptions.savedData = [];
    };

    ImportAdvanceFiltersController.prototype.addFiltersData = function()
    {
        var iNumberOfSelectedYears = this.m_oAdvanceFilterOptions.selectedYears.length;
        for(var iIndexYear = 0 ; iIndexYear < iNumberOfSelectedYears ; iIndexYear++)
        {
            if( (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions.selectedSeasons) === false) &&
                (this.m_oAdvanceFilterOptions.selectedSeasons.length > 0) )
            {
                this.addSeason(this.m_oAdvanceFilterOptions.selectedSeasons,this.m_oAdvanceFilterOptions.selectedYears[iIndexYear],this.m_oAdvanceFilterOptions.savedData);
            }
            if( (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions.selectedMonths) === false) &&
                (this.m_oAdvanceFilterOptions.selectedMonths.length > 0) )
            {
                this.addMonths(this.m_oAdvanceFilterOptions.selectedMonths,this.m_oAdvanceFilterOptions.selectedYears[iIndexYear],this.m_oAdvanceFilterOptions.savedData);
            }

            if( (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions.selectedMonths) === false) &&
                (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions.selectedDays) === false ) &&
                (this.m_oAdvanceFilterOptions.selectedDays.length > 0 ) )
            {
                this.addFilterPeriods(this.m_oAdvanceFilterOptions.selectedMonths,this.m_oAdvanceFilterOptions.selectedDays,
                                      this.m_oAdvanceFilterOptions.selectedYears[iIndexYear],this.m_oAdvanceFilterOptions.savedData);
            }

        }
    };
    /**
     *
     * @param asSelectedMonths
     * @param iYear
     * @param aoSaveData
     * @returns {boolean}
     */
    ImportAdvanceFiltersController.prototype.addMonths = function(asSelectedMonths,iYear,aoSaveData)
    {
        if( utilsIsObjectNullOrUndefined(asSelectedMonths) || utilsIsObjectNullOrUndefined(iYear) || utilsIsObjectNullOrUndefined(aoSaveData) )
        {
            return false;
        }

        var iNumberOfSelectedMonths = asSelectedMonths.length;
        for(var iIndexMonth = 0 ; iIndexMonth < iNumberOfSelectedMonths; iIndexMonth++)
        {
            var sName = iYear.toString() +" "+  asSelectedMonths[iIndexMonth];
            var dateSensingPeriodFrom = new Date();
            var dateSensingPeriodTo = new Date();
            dateSensingPeriodFrom.setYear(iYear);
            dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(asSelectedMonths[iIndexMonth]));
            dateSensingPeriodFrom.setDate(1);

            dateSensingPeriodTo.setYear(iYear);
            dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(asSelectedMonths[iIndexMonth]));
            dateSensingPeriodTo.setDate(this.getMonthDays(asSelectedMonths[iIndexMonth],iYear));
            var oData={
                dateSensingPeriodFrom:dateSensingPeriodFrom,
                dateSensingPeriodTo:dateSensingPeriodTo
            };
            this.saveDataInAdvanceFilter(sName,oData,aoSaveData);
        }
    };
    /**
     *
     * @param asSelectedMonths
     * @param asSelectedDays
     * @param iYear
     * @param aoSaveData
     * @returns {boolean}
     */
    ImportAdvanceFiltersController.prototype.addFilterPeriods = function(asSelectedMonths,asSelectedDays,iYear,aoSaveData)
    {
        if( utilsIsObjectNullOrUndefined(asSelectedMonths) || utilsIsObjectNullOrUndefined(asSelectedDays) ||
            utilsIsObjectNullOrUndefined(iYear) || utilsIsObjectNullOrUndefined(aoSaveData))
        {
            return false
        }
        var iNumberOfSelectedMonths = asSelectedMonths.length;
        // var iNumberOfSelectedDays = asSelectedDays.length;
        for(var iIndexSelectedMonth = 0; iIndexSelectedMonth < iNumberOfSelectedMonths; iIndexSelectedMonth++ )
        {
            var aiSelectedDays = this.convertArrayOfStringInArrayOfInteger(asSelectedDays);
            var aaiPeriodsOfTimes = this.getPeriodsOfTimes(aiSelectedDays);
            var iNumberOfPeriodsOfTimes = aaiPeriodsOfTimes.length;
            //TODO END IT
            for(var iIndexPeriodOfTimes = 0 ; iIndexPeriodOfTimes < iNumberOfPeriodsOfTimes; iIndexPeriodOfTimes++ )
            {
                var sNameChips="";
                if(aaiPeriodsOfTimes[iIndexPeriodOfTimes].length > 1)
                {

                    var iDayFrom = aaiPeriodsOfTimes[iIndexPeriodOfTimes][0];
                    var iDayTo = aaiPeriodsOfTimes[iIndexPeriodOfTimes][ aaiPeriodsOfTimes[iIndexPeriodOfTimes].length-1];

                    sNameChips +=   iDayFrom.toString() + "/" + asSelectedMonths[iIndexSelectedMonth].toString();
                    sNameChips +=  " - " + iDayTo.toString() + "/" + asSelectedMonths[iIndexSelectedMonth].toString();
                    sNameChips += " " + iYear;

                    var dateSensingPeriodFrom = new Date();
                    var dateSensingPeriodTo = new Date();

                    dateSensingPeriodFrom.setYear(iYear);
                    dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(asSelectedMonths[iIndexSelectedMonth]));
                    dateSensingPeriodFrom.setDate(iDayFrom);

                    // TODO CHECK LEAP YEAS (29 DAYS FEBRUARY)
                    dateSensingPeriodTo.setYear(iYear);
                    dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(asSelectedMonths[iIndexSelectedMonth]));
                    dateSensingPeriodTo.setDate(iDayTo);
                    var oData={
                        dateSensingPeriodFrom:dateSensingPeriodFrom,
                        dateSensingPeriodTo:dateSensingPeriodTo
                    };
                    this.saveDataInAdvanceFilter(sNameChips,oData,aoSaveData);

                }
                else
                {
                    var iDay = aaiPeriodsOfTimes[iIndexPeriodOfTimes][0];
                    sNameChips +=   iDay.toString() + "/" + asSelectedMonths[iIndexSelectedMonth].toString();
                    sNameChips += "/" + iYear.toString();

                    var dateSensingPeriod = new Date();
                    dateSensingPeriod.setYear(iYear);
                    dateSensingPeriod.setMonth(this.convertNameMonthInNumber(asSelectedMonths[iIndexSelectedMonth]));
                    dateSensingPeriod.setDate(iDay);

                    var oData={
                        dateSensingPeriodFrom:dateSensingPeriod,
                        dateSensingPeriodTo:dateSensingPeriod
                    };

                    this.saveDataInAdvanceFilter(sNameChips,oData,aoSaveData);
                }


            }
        }

    };

    ImportAdvanceFiltersController.prototype.savePeriodOfTime = function(aiPeriodOfTime,sMonth,iYear)
    {
        if(utilsIsObjectNullOrUndefined(aiPeriodOfTime))
        {
            return false;
        }
        var iNumberOfDays = aiPeriodOfTime.length;
        var iSelectedDayFrom = aiPeriodOfTime[0];
        var iSelectedDayTo = aiPeriodOfTime[iNumberOfDays];
         var sName="";
            sName +=  iSelectedDayFrom.toString() + " - "+ iSelectedDayTo +"/"+ this.convertNameMonthInNumber(sMonth) + "/" + iYear ;
                // sName +=  " - " +this.m_oAdvanceFilter.selectedDayTo.toString() + "/" + this.m_oAdvanceFilter.selectedMonthTo.toString();
                // sName += " " + this.m_oAdvanceFilter.selectedYears[iIndexYear].toString();

                var dateSensingPeriodFrom = new Date();
                var dateSensingPeriodTo = new Date();
                dateSensingPeriodFrom.setYear(iYear);
                dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(sMonth));
                // TODO CHECK LEAP YEAS (29 DAYS FEBRUARY)
                dateSensingPeriodFrom.setDate( iSelectedDayFrom);
                dateSensingPeriodTo.setYear(iYear);
                dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(sMonth));
                dateSensingPeriodTo.setDate(iSelectedDayTo);
                var oData={
                    dateSensingPeriodFrom:dateSensingPeriodFrom,
                    dateSensingPeriodTo:dateSensingPeriodTo
                };
                this.saveDataInAdvanceFilter(sName,oData);

    }
    /**
     *
     * @param asArray
     * @returns {*}
     */
    ImportAdvanceFiltersController.prototype.convertArrayOfStringInArrayOfInteger = function(asArray)
    {
        if(utilsIsObjectNullOrUndefined(asArray) === true)
        {
            return null;
        }
        var iNumberOfElement = asArray.length;
        var aiReturnArray = [];
        for(var iIndexArray = 0; iIndexArray < iNumberOfElement ; iIndexArray++ )
        {
            aiReturnArray.push( parseInt(asArray[iIndexArray]));
        }
        return aiReturnArray;
    };

    ImportAdvanceFiltersController.prototype.getPeriodsOfTimes = function(aiDays)
    {
        if(utilsIsObjectNullOrUndefined(aiDays))
        {
            return null;
        }
        var aaiReturnPeriodsOfTimes = [[]];
        var iIndexReturnPeriodOfTime = 0;

        aiDays.sort();
        var iNumberOfDays = aiDays.length;
        aaiReturnPeriodsOfTimes[iIndexReturnPeriodOfTime].push(aiDays[0]);
        for(var iIndexDay = 1; iIndexDay < iNumberOfDays ; iIndexDay++)
        {
            /*
            * example aiDays = [1,2,7,9]
            * iIndexDay = 1;
            * if( (aiDays[iIndexDay - 1 ] + 1 ) === aiDays[iIndexDay])
            *
            * aiDays[iIndexDay - 1 ] = 1
            *
            * (aiDays[iIndexDay - 1 ] + 1 ) = (1+1)
            *
            * aiDays[iIndexDay] = 2
            *
            * if( (1+1) === 2 ) aiDays[iIndexDay] and aiDays[iIndexDay-1] are a period of times because they are in sequence
            *
            * */
            if( (aiDays[iIndexDay-1]+1) === aiDays[iIndexDay])
            {
                aaiReturnPeriodsOfTimes[iIndexReturnPeriodOfTime].push(aiDays[iIndexDay]);
            }
            else
            {
                //Push array
                var aiNewPeriodOfTime = [ aiDays[iIndexDay] ];
                aaiReturnPeriodsOfTimes.push( aiNewPeriodOfTime );
                iIndexReturnPeriodOfTime++;
            }
        }
        return aaiReturnPeriodsOfTimes;
    };

    /**
     *
     * @param asSeasonsSelected
     * @param iYear
     * @param aoSaveData
     * @returns {boolean}
     */
    ImportAdvanceFiltersController.prototype.addSeason = function(asSeasonsSelected,iYear,aoSaveData)
    {
        if(utilsIsObjectNullOrUndefined(asSeasonsSelected) === true || utilsIsObjectNullOrUndefined(iYear) || utilsIsObjectNullOrUndefined(aoSaveData))
        {
            return false;
        }
        var iNumberOfSeasonsSelected = asSeasonsSelected.length;
        for(var iIndexSeason = 0 ; iIndexSeason < iNumberOfSeasonsSelected ; iIndexSeason++)
        {
            var oDataPeriod = null;
            switch(asSeasonsSelected[iIndexSeason].toLowerCase())
            {
                case "spring":
                    oDataPeriod = this.getPeriodSpring();
                    break;
                case "summer":
                    oDataPeriod = this.getPeriodSummer();
                    break;
                case "autumn":
                    oDataPeriod = this.getPeriodAutumn();
                    break;
                case "winter":
                    oDataPeriod = this.getPeriodWinter();
                    break;
            }

            if(asSeasonsSelected[iIndexSeason].toLowerCase() !== "winter")
            {
                if (oDataPeriod !== null) oDataPeriod.dateSensingPeriodFrom.setYear(iYear);
            }
            else
            {
                // P.Campanella 10/02/2018: the winter start in yyyy and ends in yyyy+1. Or viceversa yyyy-1 to yyyyy
                if (oDataPeriod !== null) oDataPeriod.dateSensingPeriodFrom.setYear(iYear-1);
            }
            if (oDataPeriod !== null) oDataPeriod.dateSensingPeriodTo.setYear(iYear);
            var sName = iYear.toString() + asSeasonsSelected[iIndexSeason];
            this.saveDataInAdvanceFilter(sName, oDataPeriod, aoSaveData);
        }
        // var sSeason = this.m_oAdvanceFilterOptions.selectedSeasons;
        return true;

    };

    /**
     *
     */
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

    ImportAdvanceFiltersController.prototype.isEmptyListOfFilters = function()
    {
        return (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions) === true || utilsIsObjectNullOrUndefined(this.m_oAdvanceFilterOptions.savedData) === true || this.m_oAdvanceFilterOptions.savedData.length === 0);
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
window.ImportAdvanceFiltersController = ImportAdvanceFiltersController;
