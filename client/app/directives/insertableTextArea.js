angular.module('wasdi.insertableTextArea', [])
    .directive('insertableTextArea', ['$rootScope', function($rootScope) {
        return {
            require: 'ngModel',
            scope: {
                ngModel: '='
            },
            link: function(scope, element, attrs, ngModelCtrl) {
                var oRemoveListener;

                oRemoveListener = $rootScope.$on('add', function(e, sVal) {
                    var oDomElement = element[0];

                    if (document.selection) {
                        oDomElement.focus();
                        var oSelRange = document.selection.createRange();
                        oSelRange.text = sVal;
                        oDomElement.focus();
                    } else if (oDomElement.selectionStart || oDomElement.selectionStart === 0) {
                        var iStartPos = oDomElement.selectionStart;
                        var iEndPos = oDomElement.selectionEnd;
                        var iScrollTop = oDomElement.scrollTop;
                        oDomElement.value = oDomElement.value.substring(0, iStartPos) + sVal + oDomElement.value.substring(iEndPos, oDomElement.value.length);
                        oDomElement.focus();
                        oDomElement.selectionStart = iStartPos + sVal.length;
                        oDomElement.selectionEnd = iStartPos + sVal.length;
                        oDomElement.scrollTop = iScrollTop;
                    } else {
                        oDomElement.value += sVal;
                        oDomElement.focus();
                    }

                    ngModelCtrl.$setViewValue(oDomElement.value);
                });

                scope.$on('$destroy', function () {
                    oRemoveListener();
                })
            }
        }
    }])
