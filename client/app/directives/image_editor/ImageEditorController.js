angular.module('wasdi.ImageEditorDirective', [])
    .directive('imageeditor', function () {
        "use strict";
        return{
            restrict : 'EAC',
            // template: "<canvas id='test' width='800' height='600'></canvas> ",
            templateUrl:"directives/image_editor/ImageEditorView.html",
            scope :{
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
                // var iDefaultHeight = 280;
                //default value canvas
                // var iDefaultWidth = 560;
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

                    if( scope.isNotPointInsideDraggerSquare(evt.stageX,evt.stageY) )
                    {
                        scope.removeSquareAndDraggerContainer();
                        bItIsClicked = true;
                        oMouseDownPoint.stageX = evt.stageX;
                        oMouseDownPoint.stageY = evt.stageY;
                    }

                    // }
                });

                stage.on("stagemouseup", function(evt) {
                    bItIsClicked = false;
                    if( scope.isNotPointInsideDraggerSquare(evt.stageX,evt.stageY) )
                    {
                        // bItIsClicked = false;
                        oMouseLastPoint.stageX = evt.stageX;
                        oMouseLastPoint.stageY = evt.stageY;
                        scope.zoom();
                    }

                });


                stage.on("stagemousemove", function(evt) {
                    if( (bItIsClicked === true)  )
                    {
                        // var oPointASquare = null;
                        // var oPointBSquare = null;
                        // if(utilsIsObjectNullOrUndefined(scope.Dragger)=== false)
                        // {
                        //     var oDraggerBoundsRectangle = scope.Square.graphics.command;
                        //     oPointASquare = {
                        //         stageX:oDraggerBoundsRectangle.x,
                        //         stageY:oDraggerBoundsRectangle.y,
                        //     };
                        //     oPointBSquare = {
                        //         stageX:(oDraggerBoundsRectangle.x + oDraggerBoundsRectangle.w),
                        //         stageY:(oDraggerBoundsRectangle.y + oDraggerBoundsRectangle.h),
                        //     };
                        //     //oMouseLastPoint.stageX,oMouseLastPoint.stageY
                        //     // console.log("isInside: " + scope.isPointInsideSquare(evt.stageX,evt.stageY,oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY));
                        // }

                        // if( (utilsIsObjectNullOrUndefined(oPointASquare) !== true) && (utilsIsObjectNullOrUndefined(oPointBSquare) !== true)
                        //         && scope.isPointInsideSquare(evt.stageX,evt.stageY,oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY))
                        // {
                        //     console.log("isInside: " + scope.isPointInsideSquare(evt.stageX,evt.stageY,oPointASquare.stageX,oPointBSquare.stageX,oPointASquare.stageY,oPointBSquare.stageY));
                        //     return false;
                        // }

                        // if( ( utilsIsObjectNullOrUndefined(scope.Dragger) === true ) || (scope.isPointInsideSquare(evt.stageX,evt.stageY,oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY) === false) )
                        // {
                            //update actual position
                            oMouseLastPoint.stageX = evt.stageX;
                            oMouseLastPoint.stageY = evt.stageY;

                            //
                            // console.log("----------------- Positions -----------------------");
                            // console.log("oMouseLastPoint.stageX" + oMouseLastPoint.stageX);
                            // console.log("oMouseLastPoint.stageY "+ oMouseLastPoint.stageY);
                            // console.log("oMouseDownPoint.stageX" + oMouseDownPoint.stageX);
                            // console.log("oMouseDownPoint.stageY "+ oMouseDownPoint.stageY);
                            // console.log("------------------ END -------------------------");
                            // var oSquarePoints = scope.calculateSquarePointsByMousePoints(oMouseDownPoint,oMouseLastPoint);

                            if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                            {

                                //var oSquare = scope.createSquare(oSquarePoints.x,oSquarePoints.y,oSquarePoints.width,oSquarePoints.height);
                                var oSquare = scope.createSquare(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);


                                if(utilsIsObjectNullOrUndefined(oSquare) === false)
                                {
                                    // add new draggable square
                                    var oDragger = new createjs.Container();
                                    oDragger.addChild(oSquare); //, label
                                    scope.Square = oSquare;
                                    scope.Dragger = oDragger;
                                    stage.addChild(oDragger);

                                    oDragger.on("mousedown", scope.draggerMouseDownCallback);
                                    oDragger.on("pressmove",scope.draggerPressMoveCallback);
                                }

                            }
                            else
                            {
                                scope.updateSquare(scope.Square, oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);
                            }

                            stage.update();
                        // }


                    }
                });

                scope.isNotPointInsideDraggerSquare = function(x, y) {
                    var oPointASquare = null;
                    var oPointBSquare = null;
                    if(utilsIsObjectNullOrUndefined(scope.Dragger)=== false)
                    {
                        var oDraggerBoundsRectangle = scope.Square.graphics.command;
                        oPointASquare = {
                            stageX:oDraggerBoundsRectangle.x,
                            stageY:oDraggerBoundsRectangle.y,
                        };
                        oPointBSquare = {
                            stageX:(oDraggerBoundsRectangle.x + oDraggerBoundsRectangle.w),
                            stageY:(oDraggerBoundsRectangle.y + oDraggerBoundsRectangle.h),
                        };
                        //oMouseLastPoint.stageX,oMouseLastPoint.stageY
                        // console.log("isInside: " + scope.isPointInsideSquare(evt.stageX,evt.stageY,oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY));
                    }
                    return  ( utilsIsObjectNullOrUndefined(scope.Dragger) === true ) || (utilsIsPointInsideSquare(x, y, oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY) === false)
                };


                stage.update();
                //INIT SOME SCOPE VARIABLE
                scope.Square = null;
                scope.Dragger = null;
                scope.Stage = stage;
                scope.Bitmap = oBitmap;
                scope.Dragger = null;

                scope.draggerPressMoveCallback = function(evt) {
                    // Calculate the new X and Y based on the mouse new position plus the offset.
                    evt.currentTarget.x = evt.stageX + evt.currentTarget.offset.x;
                    evt.currentTarget.y = evt.stageY + evt.currentTarget.offset.y;
                    // make sure to redraw the stage to show the change:
                    stage.update();
                    scope.zoom();

                };
                scope.draggerMouseDownCallback = function (evt) {
                    // keep a record on the offset between the mouse position and the container
                    // position. currentTarget will be the container that the event listener was added to:
                    evt.currentTarget.offset = {x: this.x - evt.stageX, y: this.y - evt.stageY};
                    scope.zoom();
                };

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

                scope.getZoomTemporaryImage = function(){
                    if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                    {
                        return false;
                    }
                    // https://codepen.io/fabiobiondi/pen/blHoy lik useful about crop images
                    //Crop bitmap
                    var oSquare = new createjs.Rectangle(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);
                    var oNewImage = scope.cropImageBySquare(scope.urlImage,oSquare);

                    //scale image 100% stage
                    scope.imageFillEntireStage(oNewImage,scope.Stage);
                    //Remove old Bitmap
                    scope.Stage.removeChild(scope.Bitmap);
                    scope.Bitmap = null;
                    //add new bitmap
                    stage.addChild(oNewImage);
                    //Remove old Square
                    scope.Stage.removeChild(scope.Square);
                    scope.Square = null;
                    return true;
                };

                scope.imageFillEntireStage = function(oImage,oStage)
                {
                    if(utilsIsObjectNullOrUndefined(oImage) || utilsIsObjectNullOrUndefined(oStage))
                    {
                        return false;
                    }

                    var oPositionStage = oStage.getTransformedBounds();
                    var oPositionImageOnStage = oImage.sourceRect;
                    if(utilsIsObjectNullOrUndefined(oPositionStage) === true || utilsIsObjectNullOrUndefined(oPositionImageOnStage) === true)
                    {
                        return false;
                    }
                    var fScaleValueX = oPositionStage.width / oPositionImageOnStage.width;
                    var fScaleValueY = oPositionStage.height / oPositionImageOnStage.height;

                    oImage.setTransform(null,null,fScaleValueX,fScaleValueY);
                };

                scope.cropImageBySquare = function(sUrlImage,oSquare){
                    if(utilsIsStrNullOrEmpty(sUrlImage) === true )
                    {
                        return null;
                    }
                    if(utilsIsObjectNullOrUndefined(oSquare) === true)
                    {
                        return null;
                    }

                    var oCrop = new createjs.Bitmap(sUrlImage);
                    oCrop.sourceRect = oSquare;

                    return oCrop;

                };

                // scope.removeElementToStage = function(oElement,oStage){
                //     if(utilsIsObjectNullOrUndefined(oElement) === true)
                //     {
                //         return false;
                //     }
                //     oStage.removeChild(oElement);
                //     oElement = null;
                //     return true;
                // };

                // scope.isPointInsideSquare = function(oPoint,oPointARectangle,oPointBRectangle)
                // {
                //     var bReturnValue = utilsIsPointInRectangle(oPoint.stageX,oPoint.stageY,oPointARectangle.stageX,oPointBRectangle.stageX,oPointARectangle.stageY,oPointBRectangle.stageY)
                //     // console.log("Point true/false:" + bReturnValue);
                //     return bReturnValue;
                // };
                scope.removeSquareAndDraggerContainer = function(){
                    scope.Stage.removeChild(scope.Square);
                    scope.Square = null;

                    scope.Stage.removeChild(scope.Dragger);
                    scope.Dragger = null;
                }
                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "empty")
                    {
                        var oBitmap =  new createjs.Bitmap(newValue);
                        scope.Bitmap = oBitmap;
                        scope.Stage.addChild(oBitmap);

                        // scope.removeElementToStage(scope.Square,scope.Stage);
                        scope.removeSquareAndDraggerContainer();
                        // scope.Stage.removeChild(scope.Square);
                        // scope.Square = null;
                        //
                        // scope.Stage.removeChild(scope.Dragger);
                        // scope.Dragger = null;
                        // scope.Stage.update();

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