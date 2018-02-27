angular.module('wasdi.ImageEditorDirective', [])
    .directive('imageeditor', function () {
        "use strict";
        return{
            restrict : 'EAC',
            // template: "<canvas id='test' width='800' height='600'></canvas> ",
            templateUrl:"directives/image_editor/ImageEditorView.html",
            scope :{
                // onClick: '&',
                urlImage : '=',
                body : '=',
                // isLoaded : '='
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
                var stage = new createjs.Stage("imageviewcanvas");
                var oBitmap = new createjs.Bitmap(scope.urlImage);

                // For mobile devices.
                createjs.Touch.enable(stage);

                // this lets our drag continue to track the mouse even when it leaves the canvas:
                // play with commenting this out to see the difference.
                stage.mouseMoveOutside = true;

                stage.addChild(oBitmap);

                //create tick
                createjs.Ticker.on("tick", tick);

                function tick(event) {
                    // Other stuff
                    stage.update(event);
                };

                var bItIsClicked = false;
                var oMouseDownPoint ={
                    stageX:"",
                    stageY:""
                };
                var oMouseLastPoint ={
                    stageX:"",
                    stageY:""
                };

                // var oController = this;
                stage.on("stagemousedown", function(evt) {
                    bItIsClicked = true;
                    oMouseDownPoint.stageX = evt.stageX;
                    oMouseDownPoint.stageY = evt.stageY;
                });

                stage.on("stagemouseup", function(evt) {
                    bItIsClicked = false;
                    oMouseLastPoint.stageX = evt.stageX;
                    oMouseLastPoint.stageY = evt.stageY;
                    scope.zoom();
                });

                stage.on("stagemousemove", function(evt) {
                    if( bItIsClicked === true )
                    {
                        //update actual position
                        oMouseLastPoint.stageX = evt.stageX;
                        oMouseLastPoint.stageY = evt.stageY;

                        // var oSquarePoints = scope.calculateSquarePointsByMousePoints(oMouseDownPoint,oMouseLastPoint);

                        if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                        {

                            //var oSquare = scope.createSquare(oSquarePoints.x,oSquarePoints.y,oSquarePoints.width,oSquarePoints.height);
                            var oSquare = scope.createSquare(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);

                            if(utilsIsObjectNullOrUndefined(oSquare) === false)
                            {
                                // add new square
                                scope.Square = oSquare;
                                stage.addChild(oSquare);
                            }

                        }
                        else
                        {
                            scope.updateSquare(scope.Square, oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);
                        }

                        stage.update();

                    }

                });



                stage.update();

                // scope.Square = square;
                scope.Square = null;
                scope.Stage = stage;
                scope.Bitmap = oBitmap;
                scope.iDefaultHeight = iDefaultHeight;
                scope.iDefaultWidth = iDefaultWidth;

                scope.updateSquare = function(oSquare,iMidPointX,iMidPointY,iWidth,iHeight){
                    oSquare.graphics.clear().setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(iMidPointX, iMidPointY, iWidth, iHeight);
                    oSquare.alpha = 0.5;
                    return oSquare;
                }

                scope.createSquare = function(iMidPointX,iMidPointY,iWidth,iHeight){
                    var square = new createjs.Shape();
                    square.graphics.setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(iMidPointX, iMidPointY, iWidth, iHeight);
                    square.alpha = 0.5;
                    return square;
                }

                // scope.calculateSquarePointsByMousePoints = function(oMouseDownPoint,oMouseLastPoint){
                //     var oMidPoint = utilsGetMidPoint(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX,oMouseLastPoint.stageY);
                //
                //     if(utilsIsObjectNullOrUndefined(oMidPoint))
                //     {
                //         return false;
                //     }
                //
                //     // oMouseDownPoint=(x1,y1)
                //     // oMouseLastPoint=(x2,y2)
                //     // oPointA=(x2,y1)?
                //     // oPointB=(x1,y2)?
                //     /*
                //     // RECTANGLE EXAMPLE :
                //     *   (x1,y1) ------------------------- (x2,y1)
                //     *           -------------------------
                //     *           -------------------------
                //     *           -------------------------
                //     *           -------------------------
                //     *   (x1,y2)                           (x2,y2)
                //     *
                //     * */
                //     var oPointA = {
                //             x:oMouseLastPoint.stageX,
                //             y:oMouseDownPoint.stageY
                //     };
                //     var oPointB = {
                //             x:oMouseDownPoint.stageX,
                //             y:oMouseLastPoint.stageY
                //     };
                //
                //     var fWidth = utilsCalculateDistanceBetweenTwoPoints (oMouseDownPoint.stageX,oMouseDownPoint.stageY,oPointA.x,oPointA.y);
                //     var fHeight = utilsCalculateDistanceBetweenTwoPoints (oMouseDownPoint.stageX,oMouseDownPoint.stageY,oPointB.x,oPointB.y);
                //
                //     return {
                //         x:oMidPoint.x,
                //         y:oMidPoint.y,
                //         width:fWidth,
                //         height:fHeight
                //     }
                //
                // };
                scope.zoom = function(){
                    // Take the Preview Canvas Dimensions
                    var element = angular.element(document.querySelector('#imageviewcanvas'));
                    var iCanvasHeight = element[0].offsetHeight;
                    var iCanvasWidth = element[0].offsetWidth;

                    if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                    {
                        return false;
                    }
                    // Take position and dimensions of the over rectangle
                    var iHeightSquare = scope.Square.graphics.command.h;
                    var iWidthSquare = scope.Square.graphics.command.w;
                    var iAx = scope.Square.graphics.command.x;
                    var iAy = scope.Square.graphics.command.y;
                    // var iAx = scope.Dragger.x;
                    // var iAy = scope.Dragger.y;

                    // Data check
                    if (iCanvasHeight == 0 || iCanvasWidth == 0) return;
                    if (iAx<0) iAx = 0;
                    if (iAy<0) iAy = 0;
                    if (iHeightSquare <=0) iHeightSquare = 1;
                    if (iWidthSquare <= 0) iWidthSquare = 1;

                    // Calculate Percentage
                    iHeightSquare = iHeightSquare/iCanvasHeight;
                    iWidthSquare = iWidthSquare/iCanvasWidth;
                    iAx = iAx/iCanvasWidth;
                    iAy = iAy/iCanvasHeight;

                    // Apply to the real band dimension
                    scope.body.viewportX = Math.round(iAx * this.body.originalBandWidth);
                    scope.body.viewportY = Math.round(iAy *  this.body.originalBandHeight);
                    scope.body.viewportWidth = Math.round(iWidthSquare * this.body.originalBandWidth);
                    scope.body.viewportHeight = Math.round(iHeightSquare * this.body.originalBandHeight);
                };

                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "empty")
                    {
                        var oBitmap =  new createjs.Bitmap(newValue);
                        scope.Stage.addChild(oBitmap);
                        scope.Stage.addChild(scope.Square);

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