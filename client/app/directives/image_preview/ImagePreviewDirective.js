angular.module('wasdi.ImagePreviewDirective', [])
    .directive('imagepreview', function () {
        "use strict";
        return{
            restrict : 'EAC',
            // template: "<canvas id='test' width='800' height='600'></canvas> ",
            templateUrl:"directives/image_preview/ImagePreviewView.html",
            scope :{
                // onClick: '&',
                urlImage : '=',
                body : '=',
                isLoaded : '='
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

                square.graphics.setStrokeStyle(2).beginStroke("#000").beginFill("grey").drawRect(0, 0, iWidth, iHeight);
                square.alpha = 0.5;

                var dragger = new createjs.Container();
                dragger.x = dragger.y = 0;

                stage.addChild(oBitmap);
                dragger.addChild(square); //, label
                stage.addChild(dragger);

                dragger.on("mousedown", function (evt) {
                    // keep a record on the offset between the mouse position and the container
                    // position. currentTarget will be the container that the event listener was added to:
                    evt.currentTarget.offset = {x: this.x - evt.stageX, y: this.y - evt.stageY};
                    // scope.zoom();
                });

                dragger.on("pressmove",function(evt) {
                    // Calculate the new X and Y based on the mouse new position plus the offset.
                    evt.currentTarget.x = evt.stageX + evt.currentTarget.offset.x;
                    evt.currentTarget.y = evt.stageY + evt.currentTarget.offset.y;
                    // make sure to redraw the stage to show the change:
                    stage.update();
                    scope.zoom();

                });

                //create tick
                createjs.Ticker.on("tick", tick);

                function tick(event) {
                    // Other stuff
                    stage.update(event);
                }

                stage.update();

                scope.Square = square;
                scope.Stage = stage;
                scope.Dragger = dragger;
                scope.Bitmap = oBitmap;
                scope.iDefaultHeight = iDefaultHeight;
                scope.iDefaultWidth = iDefaultWidth;


                scope.zoom = function()
                {
                    var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                    var iCanvasHeight = element[0].offsetHeight;
                    var iCanvasWidth = element[0].offsetWidth;
                    var iHeightSquare = scope.Square.graphics.command.h;
                    var iWidthSquare = scope.Square.graphics.command.w;
                    var iAx = scope.Dragger.x;
                    var iAy = scope.Dragger.y;

                    if (iCanvasHeight == 0 || iCanvasWidth == 0) return;

                    iHeightSquare = iHeightSquare/iCanvasHeight;
                    iWidthSquare = iWidthSquare/iCanvasWidth;
                    iAx = iAx/iCanvasWidth;
                    iAy = iAy/iCanvasHeight;
                    // iHeightSquare = (iHeightSquare*iCanvasHeight)/100 ;
                    // iWidthSquare = (iWidthSquare* iCanvasWidth)/100;
                    // iAx = (iAx*iCanvasWidth) /100 ;
                    // iAy = (iAy*iCanvasHeight) /100 ;

                    // this.body.vp_x = iAx * this.body.vp_x;
                    // this.body.vp_y = iAy * this.body.vp_y;
                    // this.body.vp_w = iWidthSquare * this.body.vp_w;
                    // this.body.vp_h = iHeightSquare * this.body.vp_h;

                    // scope.body.vp_x = iAx * this.body.vp_x;
                    // scope.body.vp_y = iAy * this.body.vp_y;
                    scope.body.vp_x = iAx * this.body.vp_w;
                    scope.body.vp_y = iAy *  this.body.vp_h;
                    scope.body.vp_w = iWidthSquare * this.body.vp_w;
                    scope.body.vp_h = iHeightSquare * this.body.vp_h;

                    // onClick({sSeason:body});
                };



                scope.changeZoomSquareSize = function(valueOfZoom){

                    if(utilsIsObjectNullOrUndefined(valueOfZoom) === true)  return false;

                    var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                    var iCanvasHeight = element[0].offsetHeight;
                    var iCanvasWidth = element[0].offsetWidth;
                    var iNewHeight = (iCanvasHeight * valueOfZoom)/100;
                    var iNewWidth = (iCanvasWidth  * valueOfZoom )/100;

                    scope.Square.graphics.clear().setStrokeStyle(2).beginStroke("#000").beginFill("grey").drawRect(0, 0, iNewWidth, iNewHeight);
                    scope.Stage.update();

                    this.zoom();

                    return true;
                };

                // scope.takePositionSquare = function(){
                //
                //     var element = angular.element(document.querySelector('#imagepreviewcanvas'));
                //     var iCanvasHeight = element[0].offsetHeight;
                //     var iCanvasWidth = element[0].offsetWidth;
                //     var iHeightSquare = scope.Square.graphics.command.h;
                //     var iWidthSquare = scope.Square.graphics.command.w;
                //
                //     /*
                //     *  (Ax,Ay)                                   (Bx,By)
                //     *       ---------------------------------------
                //     *       |                                     |
                //     *       |               SQUARE                |
                //     *       |                                     |
                //     *       ---------------------------------------
                //     *  (Dx,Dy)                                   (Cx,Cy)
                //     *
                //     * */
                //
                //     var iAx = scope.Dragger.x;
                //     var iAy = scope.Dragger.y;
                //     var iBx = scope.Dragger.x + iHeightSquare;
                //     var iBy = scope.Dragger.y;
                //     var iCx = scope.Dragger.x + iHeightSquare;
                //     var iCy = scope.Dragger.y - iWidthSquare;
                //     var iDx = scope.Dragger.x;
                //     var iDy = scope.Dragger.y - iWidthSquare;
                //
                //     /*
                //     *   Example X:
                //     *   (Ax,Ay) = (280,10)
                //     *   280 : 560 = x : 100
                //     *   x = 560 / (280*100)
                //     *   transform points in percents
                //     *
                //     *   Example Y:
                //     *   (Ax,Ay) = (10,140)
                //     *   140 : 280 = y : 100
                //     *   y = 280 / (140*100)
                //     *   transform points in percents
                //     * */
                //
                //     var iAyPercent = (iAy*100)/iCanvasHeight;
                //     var iAxPercent = (iAx*100)/iCanvasWidth;
                //     var iByPercent = (iBy*100)/iCanvasHeight;
                //     var iBxPercent = (iBx*100)/iCanvasWidth;
                //     var iCyPercent = (iCy*100)/iCanvasHeight;
                //     var iCxPercent = (iCx*100)/iCanvasWidth;
                //     var iDyPercent = (iDy*100)/iCanvasHeight;
                //     var iDxPercent = (iDx*100)/iCanvasWidth;
                //
                // }

                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "empty")
                    {
                        var oBitmap =  new createjs.Bitmap(newValue);
                        scope.Stage.addChild(oBitmap);
                        scope.Stage.addChild(scope.Dragger);
                        scope.zoom();
                    }
                    else {
                        scope.Stage.autoClear = true;
                        scope.Stage.removeAllChildren();
                        scope.Stage.update();
                    }

                });

            }
        };
    });