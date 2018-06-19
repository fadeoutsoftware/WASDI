angular.module('wasdi.ImagePreviewDirective', [])
    .directive('imagepreview', function () {
        "use strict";
        return{
            restrict : 'EAC',
            // template: "<canvas id='test' width='800' height='600'></canvas> ",
            templateUrl:"directives/image_preview/ImagePreviewView.html",
            scope :{
                urlImage : '=',
                body : '=',
                isLoaded : '=',
                heightCanvas: '=',
                widthCanvas: '='
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            link: function(scope, elem, attrs) {
                var iDefaultValueZoom = 100;
                //default value canvas
                var iDefaultHeight = 280;
                //default value canvas
                var iDefaultWidth = 560;
                var stage = new createjs.Stage("imagepreviewcanvas");
                var oBitmap = new createjs.Bitmap(scope.urlImage);

                // For mobile devices.
                createjs.Touch.enable(stage);

                // this lets our drag continue to track the mouse even when it leaves the canvas:
                // play with commenting this out to see the difference.
                stage.mouseMoveOutside = true;

                var square = new createjs.Shape();

                 var iHeight = (iDefaultHeight *iDefaultValueZoom )/100;
                 var iWidth = (iDefaultWidth *iDefaultValueZoom )/100;

                square.graphics.setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(0, 0, iWidth, iHeight);
                square.alpha = 0.5;

                // var dragger = new createjs.Container();
                //  dragger.x = dragger.y = 0;

                stage.addChild(oBitmap);
                // dragger.addChild(square); //, label
                // stage.addChild(dragger);
                stage.addChild(square)
                // dragger.on("mousedown", function (evt) {
                //     // keep a record on the offset between the mouse position and the container
                //     // position. currentTarget will be the container that the event listener was added to:
                //     evt.currentTarget.offset = {x: this.x - evt.stageX, y: this.y - evt.stageY};
                //     scope.zoom();
                // });
                //
                // dragger.on("pressmove",function(evt) {
                //     // Calculate the new X and Y based on the mouse new position plus the offset.
                //     evt.currentTarget.x = evt.stageX + evt.currentTarget.offset.x;
                //     evt.currentTarget.y = evt.stageY + evt.currentTarget.offset.y;
                //     // make sure to redraw the stage to show the change:
                //     stage.update();
                //     scope.zoom();
                //
                // });

                //create tick
                createjs.Ticker.on("tick", tick);

                function tick(event) {
                    // Other stuff
                    if(utilsIsObjectNullOrUndefined(stage) === false && utilsIsObjectNullOrUndefined(event) === false)
                    {
                        stage.update(event);
                    }
                }
                // scope.$on(
                //     "$destroy",
                //     function( event ) {
                //         console.log('Destroy Ticker');
                //         oTicker = null;
                //
                //     }
                // );

                stage.update();

                scope.Square = square;
                scope.Stage = stage;
                // scope.Dragger = dragger;
                scope.Bitmap = oBitmap;
                scope.iDefaultHeight = iDefaultHeight;
                scope.iDefaultWidth = iDefaultWidth;


                // scope.zoom = function()
                // {
                //     // Take the Preview Canvas Dimensions
                //     var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                //     var iCanvasHeight = element[0].offsetHeight;
                //     var iCanvasWidth = element[0].offsetWidth;
                //     // Take position and dimensions of the over rectangle
                //     var iHeightSquare = scope.Square.graphics.command.h;
                //     var iWidthSquare = scope.Square.graphics.command.w;
                //     var iAx = scope.Dragger.x;
                //     var iAy = scope.Dragger.y;
                //
                //     // Data check
                //     if (iCanvasHeight == 0 || iCanvasWidth == 0) return;
                //     if (iAx<0) iAx = 0;
                //     if (iAy<0) iAy = 0;
                //     if (iHeightSquare <=0) iHeightSquare = 1;
                //     if (iWidthSquare <= 0) iWidthSquare = 1;
                //
                //     // Calculate Percentage
                //     iHeightSquare = iHeightSquare/iCanvasHeight;
                //     iWidthSquare = iWidthSquare/iCanvasWidth;
                //     iAx = iAx/iCanvasWidth;
                //     iAy = iAy/iCanvasHeight;
                //
                //     // Apply to the real band dimension
                //     scope.body.viewportX = Math.round(iAx * this.body.originalBandWidth);
                //     scope.body.viewportY = Math.round(iAy *  this.body.originalBandHeight);
                //     scope.body.viewportWidth = Math.round(iWidthSquare * this.body.originalBandWidth);
                //     scope.body.viewportHeight = Math.round(iHeightSquare * this.body.originalBandHeight);
                // };


                // scope.changeZoomSquareSize = function(valueOfZoom){
                //
                //     if(utilsIsObjectNullOrUndefined(valueOfZoom) === true)  return false;
                //
                //     var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                //     var iCanvasHeight = element[0].offsetHeight;
                //     var iCanvasWidth = element[0].offsetWidth;
                //     var iNewHeight = (iCanvasHeight * valueOfZoom)/100;
                //     var iNewWidth = (iCanvasWidth  * valueOfZoom )/100;
                //
                //     scope.Square.graphics.clear().setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(0, 0, iNewWidth, iNewHeight);
                //     scope.Stage.update();
                //
                //     // this.zoom();
                //
                //     return true;
                // };
                scope.resizeRectangle = function(oBody)
                {
                    var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                    var iCanvasHeight = element[0].offsetHeight;
                    var iCanvasWidth = element[0].offsetWidth;
                    //originalBandWidth  originalBandHeight
                    var fX = oBody.viewportX / oBody.originalBandWidth;
                    var fY = oBody.viewportY /oBody.originalBandHeight;
                    fX =  fX * iCanvasWidth;
                    fY =  fY * iCanvasHeight;
                    var fNewWidth = oBody.viewportWidth / oBody.originalBandWidth;
                    var fNewHeight = oBody.viewportHeight / oBody.originalBandHeight;
                    fNewWidth = fNewWidth * iCanvasWidth;
                    fNewHeight = fNewHeight * iCanvasHeight;
                    scope.Square.graphics.clear().setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(fX, fY, fNewWidth, fNewHeight);
                };

                scope.$watchGroup(['body.viewportX','body.viewportY','body.viewportWidth','body.viewportHeight'], function (newValue, oldValue, scope)
                {
                    scope.resizeRectangle(scope.body);
                    // var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                    // var iCanvasHeight = element[0].offsetHeight;
                    // var iCanvasWidth = element[0].offsetWidth;
                    // //originalBandWidth  originalBandHeight
                    // var fX = scope.body.viewportX / scope.body.originalBandWidth;
                    // var fY = scope.body.viewportY /scope. body.originalBandHeight;
                    // fX =  fX * iCanvasWidth;
                    // fY =  fY * iCanvasHeight;
                    // var fNewWidth = scope.body.viewportWidth / scope.body.originalBandWidth;
                    // var fNewHeight = scope.body.viewportHeight / scope.body.originalBandHeight;
                    // fNewWidth = fNewWidth * iCanvasWidth;
                    // fNewHeight = fNewHeight * iCanvasHeight;
                    // scope.Square.graphics.clear().setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(fX, fY, fNewWidth, fNewHeight);
                });
                /**
                 *
                 */
                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "empty")
                    {
                        var oBitmap =  new createjs.Bitmap(newValue);
                        scope.Stage.addChild(oBitmap);
                        scope.Stage.addChild(scope.Square);
                        // scope.Stage.addChild(scope.Dragger);
                        // scope.zoom();
                    }
                    else {
                        scope.Stage.autoClear = true;
                        scope.Stage.removeAllChildren();
                        // scope.Stage.update();
                    }
                    scope.resizeRectangle(scope.body);
                    scope.Stage.update();

                });

            }
        };
    });