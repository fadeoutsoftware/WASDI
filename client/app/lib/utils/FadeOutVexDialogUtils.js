/**
 * Created by a.corrado on 24/01/2017.
 */
/*YOU MUST DOWNLOAD VEX LIBRARY FOR USE UTILS VEX */

/* version (in bower)
 "vex":"3.0.0"
Import:
 <script src="bower_components/vex/dist/js/vex.combined.min.js"></script>
 <link rel="stylesheet" href="bower_components/vex/dist/css/vex.css" />
 <link rel="stylesheet" href="bower_components/vex/dist/css/vex-theme-default.css" />
 <link rel="stylesheet" href="bower_components/vex/dist/css/vex-theme-bottom-right-corner.css" />
 <link rel="stylesheet" href="bower_components/vex/dist/css/vex-theme-top.css" />
 <script>vex.defaultOptions.className = 'vex-theme-default'</script>
* */

function utilsVexDialogConfirm(oMessage,oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value)
        {
            if (value)
            {
                console.log('Successfully destroyed the planet.')
            }
            else
            {
                console.log('Chicken.')
            }
        }
    }

    oMessage+="<br>";

    var oVexInstance = vex.dialog.confirm({
        unsafeMessage: oMessage,
        callback: oCallback
    });

    return oVexInstance;
}


function utilsVexDialogAlertDefault(oMessage,oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value){return "ok"; }
    }

    oMessage += "<br>";

    var oVexInstance = vex.dialog.alert({unsafeMessage:oMessage,
                    callback:oCallback});

    return oVexInstance;
}

function utilsVexDialogAlertBottomRightCorner(oMessage,oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value){return "ok"; }
    }

    oMessage += "<br>";

    var oVexInstance = vex.dialog.alert({
        unsafeMessage:oMessage,
        showCloseButton: false,
        escapeButtonCloses: false,
        className:'vex-theme-bottom-right-corner',// Overwrites defaultOptions
        callback:oCallback
    });
    return oVexInstance;
}

function utilsVexDialogAlertTop(oMessage,oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value){return "ok"; }
    }

    oMessage += "<br>";
    var oVexInstance = vex.dialog.alert({
        unsafeMessage:oMessage,
        className:'vex-theme-top',// Overwrites defaultOptions
        callback:oCallback
    });
    return oVexInstance;
}

function utilsVexDialogBigAlertTop(oMessage,oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback = function (value){return "ok"; }
    }

    oMessage += "<br>";
    var oVexInstance = vex.dialog.alert({
        unsafeMessage:oMessage,
        className:'vex-theme-top big-vex',// Overwrites defaultOptions
        callback:oCallback,
    });
    return oVexInstance;
}

/**
 * Close a Vex dialog after a given time interval
 * @param iDelayMs The time interval in millis
 * @param oVexInstance The instance of Vex dialog to close
 */
function utilsVexCloseDialogAfter(iDelayMs,oVexInstance)
{
    if(!utilsIsObjectNullOrUndefined (iDelayMs) && !utilsIsObjectNullOrUndefined (oVexInstance))
        setTimeout(function(){vex.close(oVexInstance) }, iDelayMs);
}

function utilsVexDialogConfirmWithCheckBox(oMessage,oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value)
        {
            if (value)
            {
                console.log('Successfully destroyed the planet.')
            }
            else
            {
                console.log('Chicken.')
            }
        }
    }

    oMessage+="<br>";

    var oVexInstance = vex.dialog.confirm({
        unsafeMessage: oMessage,
        callback: oCallback,
        input:[
            '<style>',
            '.vex-custom-field-wrapper {',
            'margin: 1em 0;',
            '}',
            '.vex-custom-field-wrapper > label {',
            'display: inline-block;',
            'margin-bottom: .2em;',
            '}',
            '</style>',
            '<div class="vex-custom-field-wrapper">',
            '<input name="files" type="checkbox" checked="checked" ng-click="console.log(\'Evviva\')" /> ',
            '<label>DELETE ON FILE SYSTEM</label>',
            '</div>',
            '<div class="vex-custom-field-wrapper">',
            '<input name="geoserver" type="checkbox" checked="checked"/> ',
            '<label>DELETE LAYERS ON GEOSERVER</label>',
            '</div>'].join('')

    })
    return oVexInstance;
}

function utilsVexDialogChangeNameInTree(oMessage,oCallback,sOldName)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value)
        {
            if (value)
            {
                console.log('Successfully destroyed the planet.')
            }
            else
            {
                console.log('Chicken.')
            }
        }
    }

    var oVexInstance = vex.dialog.confirm({
        message: oMessage,
        callback: oCallback,
        input:[
                '<div class="my-input-text"><input type="text" name="renameProduct"  class="form-control " value="' + sOldName + ' "title="" placeholder="New Name..." ></div>'
                ].join('')

    })
    return oVexInstance;
}


function utilsVexPrompt(oMessage, sInitialValue, oCallback)
{
    if(utilsIsStrNullOrEmpty(oMessage))
        oMessage = "Are you absolutely sure you want to destroy the alien planet?";//Default message

    if(utilsIsObjectNullOrUndefined(oCallback))
    {
        /*Default CallBack*/
        oCallback=function (value){return "ok"; }
    }

    oMessage += "<br>";
    var oVexInstance = vex.dialog.prompt({
        unsafeMessage:oMessage,
        value: sInitialValue,
        //className:'vex-theme-top',// Overwrites defaultOptions
        callback:oCallback
    });
    return oVexInstance;
}
