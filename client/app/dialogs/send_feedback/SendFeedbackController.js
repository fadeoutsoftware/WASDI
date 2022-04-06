/**
 * Created by PetruPetrescu on 15/03/2022.
 */
var SendFeedbackController = (function() {

    function SendFeedbackController($scope, oClose,oExtras,oConstantsService,oFeedbackService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oFeedbackService = oFeedbackService;

        this.m_oConstantsService = oConstantsService;

        this.m_oFeedback = {
            title: null,
            message: null
        };

        this.m_oUser = this.m_oConstantsService.getUser();

        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    SendFeedbackController.prototype.sendFeedback = function() {

        if (utilsIsObjectNullOrUndefined(this.m_oFeedback)
            || utilsIsStrNullOrEmpty(this.m_oFeedback.title)
            || utilsIsStrNullOrEmpty(this.m_oFeedback.title)) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SENDING FEEDBACK");
        }

        this.m_oFeedbackService.sendFeedback(this.m_oFeedback)
            .then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                var oDialog = utilsVexDialogAlertBottomRightCorner("FEEDBACK SENT<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SENDING FEEDBACK");
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SENDING FEEDBACK");
        });

        return true;
    };

    SendFeedbackController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService',
        'FeedbackService'

    ];
    return SendFeedbackController;
})();
window.SendFeedbackController = SendFeedbackController;
