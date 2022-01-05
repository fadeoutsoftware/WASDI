angular.module('tooltip', []).directive('tooltip', function(){
    
    return {
        restrict: 'A',
        link: function(scope, element, attrs){
            element.on("mouseenter", function(){
                // on mouseenter
                element.tooltip('show');
            }).on("mouseleave", function(){
                // on mouseleave
                element.tooltip('hide');
            })
        }
    };
})