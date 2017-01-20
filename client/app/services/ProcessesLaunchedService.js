/**
 * Created by a.corrado on 18/01/2017.
 */

'use strict';
angular.module('wasdi.ProcessesLaunchedService', ['wasdi.ProcessesLaunchedService']).
service('ProcessesLaunchedService', ['ConstantsService', function (oConstantsService) {
    this.m_aoProcessesRunning = [];
    this.COOKIE_EXPIRE_TIME_DAYS = 1;//days
    this.m_oConstantsService = oConstantsService;
    /*TODO ADD ID USER FOR COOKIE*/

    /*Cookie methods*/

    this.getProcessesRunningListByCookie = function()
    {
        var oResult = this.m_oConstantsService.getCookie("m_aoProcessesRunning");

        if(utilsIsObjectNullOrUndefined(oResult) || oResult.length == 0 || (typeof sString == 'oResult' && utilsIsStrNullOrEmpty(oResult)))
            return [];
        else
            return oResult;
    }

    this.setProcessesRunningInCookie  = function(asProcesses)
    {
        this.m_oConstantsService.setCookie("m_aoProcessesRunning",asProcesses , this.COOKIE_EXPIRE_TIME_DAYS);
    }
    /****************************************************/

    /*load process  */
    this.loadProcessesByCookie = function()
    {
        this.m_aoProcessesRunning = this.getProcessesRunningListByCookie();/*LOAD PROCESSES*/
    }



    /*SERVICE METHODS*/

    this.getProcesses = function()
    {
        return this.m_aoProcessesRunning;
    }

    this.addProcesses = function(sProcess)
    {
        if(utilsIsObjectNullOrUndefined(sProcess))
            return false;

        var asProcess = this.getProcesses();
        asProcess.push(sProcess);
        this.setProcessesRunningInCookie(asProcess);
        this.loadProcessesByCookie();
    }

    this.isEmptyProcessesRunningList = function()
    {
        if(utilsIsObjectNullOrUndefined( this.m_aoProcessesRunning) || this.m_aoProcessesRunning.length == 0)
            return true;
        return false;
    }



    this.indexProcess = function(sProcess)
    {
        if(utilsIsObjectNullOrUndefined(sProcess))
            return -1;

        var iNumberOfProcesses = this.m_aoProcessesRunning.length;
        for(var iIndex=0; iIndex < iNumberOfProcesses; iIndex++)
        {
            if(this.m_aoProcessesRunning[iIndex] == sProcess)
                return iIndex

        }

        return -1;
    }


    this.indexProcessByProperty = function(oProperty,oValue)
    {
        if(utilsIsObjectNullOrUndefined(oProperty) ||utilsIsObjectNullOrUndefined(oProperty))
            return -1;

        var iNumberOfProcesses = this.m_aoProcessesRunning.length;
        for(var iIndex=0; iIndex < iNumberOfProcesses; iIndex++)
        {
            if(this.m_aoProcessesRunning[iIndex][oProperty] == oValue)
                return iIndex

        }
        return -1;
    }

    this.removeProcess = function(sProcess)
    {
        if(utilsIsObjectNullOrUndefined(sProcess))
            return false;
        var iIndexProcess =  this.indexProcess(sProcess);

        this.m_aoProcessesRunning.splice(iIndexProcess,1);
        this.setProcessesRunningInCookie(this.m_aoProcessesRunning);
    }

    this.removeProcessByProperty = function(oProperty,oValue)
    {
        if(utilsIsObjectNullOrUndefined(oProperty) )
            return false;
        var iIndexProcess =  this.indexProcessByProperty(oProperty,oValue);
        if(iIndexProcess == -1)
            return false;
        this.m_aoProcessesRunning.splice(iIndexProcess,1);
        this.setProcessesRunningInCookie(this.m_aoProcessesRunning);
        return true;
    }


}]);
