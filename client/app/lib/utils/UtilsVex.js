/**
 * Created by a.corrado on 24/01/2017.
 */
/*YOU MUST DOWNLOAD VEX LIBRARY FOR USE IT */

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

//TODO TEST FUNCTIONS
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

    var oVexInstance = vex.dialog.confirm({
        message: oMessage,
        callback: oCallback,

    })
    return oVexInstance;
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

    var oVexInstance = vex.dialog.confirm({
        message: oMessage,
        callback: oCallback,
        input:['<div class="vex-custom-field-wrapper">',
            '<label for="color">Delete files on file system</label>',
            '<div class="vex-custom-input-wrapper">',
            '<input name="checkbox" type="checkbox"/>',
            '</div>',
            '</div>'].join('')

    })
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
    var oVexInstance = vex.dialog.alert({message:oMessage,
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

    var oVexInstance = vex.dialog.alert({
                        message:oMessage,
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
    var oVexInstance = vex.dialog.alert({
        message:oMessage,
        className:'vex-theme-top',// Overwrites defaultOptions
        callback:oCallback
    });
    return oVexInstance;
}
//@params: 1000 millisecond = 1
function utilsVexCloseDialogAfterFewSeconds(iSecond,oVexInstance)
{
    if(!utilsIsObjectNullOrUndefined (iSecond) && !utilsIsObjectNullOrUndefined (oVexInstance))
        setTimeout(function(){vex.close(oVexInstance) }, iSecond);
}