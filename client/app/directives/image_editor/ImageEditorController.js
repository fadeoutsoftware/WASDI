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
                getDefaultImage:'&',
                applyEditorPreviewImage:'&'
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
                        // console.log("stagemousedown")
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
                        // console.log("stagemouseup")

                        // bItIsClicked = false;
                        oMouseLastPoint.stageX = evt.stageX;
                        oMouseLastPoint.stageY = evt.stageY;
                        scope.zoom();
                    }

                });


                stage.on("stagemousemove", function(evt) {
                    if( (bItIsClicked === true)  )
                    {
                        // console.log("stagemousemove")

                            //update actual position
                            oMouseLastPoint.stageX = evt.stageX;
                            oMouseLastPoint.stageY = evt.stageY;

                            if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                            {

                                var oSquare = scope.createSquare(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);


                                if(utilsIsObjectNullOrUndefined(oSquare) === false)
                                {
                                    // add new draggable square
                                    var oDragger = new createjs.Container();
                                    oDragger.x = oDragger.y = 0;
                                    oDragger.addChild(oSquare); //, label
                                    scope.Square = oSquare;
                                    scope.Dragger = oDragger;
                                    stage.addChild(oDragger);

                                    oDragger.on("mousedown", scope.draggerMouseDownCallback);
                                    oDragger.on("pressmove",scope.draggerPressMoveCallback);
                                    // oDragger.on("pressup",scope.draggerPressUpCallback);
                                    // oDragger.mouseChildren = false;
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
                    if( utilsIsObjectNullOrUndefined(scope.Square ) === true || utilsIsObjectNullOrUndefined(scope.Dragger) === true   )
                    {
                        return true;
                    }
                    var oPointASquare = null;
                    var oPointBSquare = null;

                        var oDraggerBoundsRectangle = scope.Square.graphics.command;
                        var iXSquare=0;
                        var iYSquare=0;
                        if(utilsIsObjectNullOrUndefined(scope.Dragger.offset) === false)
                        {
                            iXSquare = oDraggerBoundsRectangle.x + Math.abs(scope.Dragger.x);
                            iYSquare = oDraggerBoundsRectangle.y + Math.abs(scope.Dragger.y);
                            // iXSquare = scope.Dragger.offset.x;
                            // iYSquare = scope.Dragger.offset.y;
                        }
                        else
                        {
                            iXSquare= oDraggerBoundsRectangle.x;
                            iYSquare= oDraggerBoundsRectangle.y;
                        }

                        //SQUARE POINTS
                        oPointASquare = {
                            stageX:iXSquare,
                            stageY:iYSquare ,
                        };

                        oPointBSquare = {
                            stageX:(iXSquare + oDraggerBoundsRectangle.w),
                            stageY:(iYSquare + oDraggerBoundsRectangle.h),
                        };
                        // oPointASquare = {
                        //     stageX:scope.Dragger.x,
                        //     stageY:scope.Dragger.y,
                        // };
                        //
                        // oPointBSquare = {
                        //     stageX:(scope.Dragger.x + oDraggerBoundsRectangle.w),
                        //     stageY:(scope.Dragger.y + oDraggerBoundsRectangle.h),
                        // };
                        // console.log("-------------- START isNotPointInsideDraggerSquare --------------")
                        // console.log("square x " + iXSquare);
                        // console.log("square y " + iYSquare);
                        // console.log("-------------- END isNotPointInsideDraggerSquare --------------")



                    return (utilsIsPointInsideSquare(x, y, oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY) === false)
                };


                stage.update();
                //INIT SOME SCOPE VARIABLE
                scope.Square = null;
                scope.Dragger = null;
                scope.Stage = stage;
                scope.Bitmap = oBitmap;
                scope.Dragger = null;
                scope.IsVisibleMouseCursorWait = false;
                // scope.draggerPressUpCallback = function(evt){
                //     // scope.Square.graphics.command.x =  scope.Square.graphics.command.x + evt.currentTarget.x;
                //     // scope.Square.graphics.command.y =  scope.Square.graphics.command.y + evt.currentTarget.y;
                //     stage.update();
                // };
                /**
                 *
                 * @param evt
                 */
                scope.draggerPressMoveCallback = function(evt) {
                    // Calculate the new X and Y based on the mouse new position plus the offset.
                    evt.currentTarget.x = evt.stageX + evt.currentTarget.offset.x;
                    evt.currentTarget.y = evt.stageY + evt.currentTarget.offset.y;
                    // console.log("-------------- START draggerPressMoveCallback --------------");
                    // console.log("offsetX: " + evt.currentTarget.x);
                    // console.log("offsetY: " + evt.currentTarget.y);
                    // console.log("-------------- END draggerPressMoveCallback --------------")
                    stage.update();
                    scope.zoom();

                };
                /**
                 *
                 * @param evt
                 */
                scope.draggerMouseDownCallback = function (evt) {
                    // keep a record on the offset between the mouse position and the container
                    // position. currentTarget will be the container that the event listener was added to:
                    evt.currentTarget.offset = {x: this.x - evt.stageX, y: this.y - evt.stageY};
                    // console.log("draggerMouseDownCallback");

                    scope.zoom();
                };
                /**
                 *
                 * @param oSquare
                 * @param iMidPointX
                 * @param iMidPointY
                 * @param iWidth
                 * @param iHeight
                 * @returns {*}
                 */
                scope.updateSquare = function(oSquare,iMidPointX,iMidPointY,iWidth,iHeight){
                    oSquare.graphics.clear().setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(iMidPointX, iMidPointY, iWidth, iHeight);
                    oSquare.alpha = 0.5;
                    return oSquare;
                }
                /**
                 *
                 * @param iMidPointX
                 * @param iMidPointY
                 * @param iWidth
                 * @param iHeight
                 * @returns {createjs.Shape}
                 */
                scope.createSquare = function(iMidPointX,iMidPointY,iWidth,iHeight){
                    var square = new createjs.Shape();
                    square.graphics.setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(iMidPointX, iMidPointY, iWidth, iHeight);
                    square.alpha = 0.5;
                    return square;
                }

                /**
                 *
                 * @returns {boolean}
                 */
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
                    // var iAx = scope.Square.graphics.command.x;
                    // var iAy = scope.Square.graphics.command.y;
                    // + Math.abs(scope.Dragger.x);
                    // + Math.abs(scope.Dragger.y);
                    var iAx = scope.Square.graphics.command.x + Math.abs(scope.Dragger.x);
                    var iAy = scope.Square.graphics.command.y + Math.abs(scope.Dragger.y);
                    // var iAx = scope.Square.graphics.command.x;
                    // var iAy = scope.Square.graphics.command.y;
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
                /**
                 *
                 * @returns {boolean}
                 */
                scope.getZoomTemporaryImage = function(){
                    if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                    {
                        return false;
                    }
                    //update zoom for server request
                    scope.zoom();

                    // https://codepen.io/fabiobiondi/pen/blHoy lik useful about crop images
                    var iHeightSquare = scope.Square.graphics.command.h;
                    var iWidthSquare = scope.Square.graphics.command.w;
                    var iAx = scope.Square.graphics.command.x + Math.abs(scope.Dragger.x);
                    var iAy = scope.Square.graphics.command.y + Math.abs(scope.Dragger.y);

                    //Crop bitmap
                    // var oSquare = new createjs.Rectangle(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);
                    var oSquare = new createjs.Rectangle(iAx,iAy,iWidthSquare,iHeightSquare);
                    var oNewImage = scope.cropImageBySquare(scope.urlImage,oSquare);

                    //scale image 100% stage
                    scope.imageFillEntireStage(oNewImage,scope.Stage);
                    //Remove old Bitmap
                    scope.Stage.removeChild(scope.Bitmap);
                    scope.Bitmap = null;
                    //add new bitmap
                    stage.addChild(oNewImage);

                    // remove dragger
                    scope.Stage.removeChild(scope.Dragger);
                    scope.Dragger = null;
                    //Remove old Square
                    scope.Stage.removeChild(scope.Square);
                    scope.Square = null;
                    return true;
                };
                /**
                 *
                 */
                scope.clickOnGetImage = function()
                {
                    scope.IsVisibleMouseCursorWait = true;
                    this.getZoomTemporaryImage();
                    this.applyEditorPreviewImage();
                };

                /**
                 *
                 */
                scope.clickOnGetDefaultImage=function()
                {
                    scope.IsVisibleMouseCursorWait = true;
                    scope.getDefaultImage();
                };
                /**
                 *
                 * @param oImage
                 * @param oStage
                 * @returns {boolean}
                 */
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
                /**
                 *
                 * @param sUrlImage
                 * @param oSquare
                 * @returns {*}
                 */
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
                /**
                 *
                 */
                scope.removeSquareAndDraggerContainer = function(){
                    scope.Stage.removeChild(scope.Square);
                    scope.Square = null;

                    scope.Stage.removeChild(scope.Dragger);
                    scope.Dragger = null;
                }

                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    scope.IsVisibleMouseCursorWait = false;
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