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

                // Flag to know if the user is using zoom functionality
                scope.m_bIsActiveZoom = false;

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

                    if (scope.m_bIsActiveZoom==false) return;

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

                    if (scope.m_bIsActiveZoom==false) return;

                    bItIsClicked = false;
                    if( scope.isNotPointInsideDraggerSquare(evt.stageX,evt.stageY) )
                    {
                        // console.log("stagemouseup")

                        // bItIsClicked = false;
                        oMouseLastPoint.stageX = evt.stageX;
                        oMouseLastPoint.stageY = evt.stageY;
                        scope.zoom();
                    }

                    scope.m_bIsActiveZoom = false;
                    scope.clickOnGetImage();

                });


                stage.on("stagemousemove", function(evt) {

                    if (scope.m_bIsActiveZoom==false) return;

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

                scope.clickOnZoom = function () {
                    scope.m_bIsActiveZoom = true;
                };

                scope.clickOnResetZoom = function () {
                    scope.body.viewportX = 0;
                    scope.body.viewportY = 0;
                    scope.body.viewportWidth = scope.body.originalBandWidth;
                    scope.body.viewportHeight = scope.body.originalBandHeight;

                    scope.clickOnGetDefaultImage();
                };

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
                            stageY:iYSquare
                        };

                        oPointBSquare = {
                            stageX:(iXSquare + oDraggerBoundsRectangle.w),
                            stageY:(iYSquare + oDraggerBoundsRectangle.h)
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
                scope.m_bIsVisibleMouseCursorWait = false;

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
                };
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
                };

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
                    var iAx = scope.Square.graphics.command.x + Math.abs(scope.Dragger.x);
                    var iAy = scope.Square.graphics.command.y + Math.abs(scope.Dragger.y);

                    // Consider always the ViewPort with origin point on top left and positive dimensions
                    if (iHeightSquare<0) {
                        iAy = iAy + iHeightSquare;
                        iHeightSquare = Math.abs(iHeightSquare);
                    }

                    if (iWidthSquare<0) {
                        iAx = iAx + iWidthSquare;
                        iWidthSquare = Math.abs(iWidthSquare);
                    }

                    if (iAx-Math.trunc(iAx)>0) iWidthSquare = iWidthSquare+1;
                    if (iAy-Math.trunc(iAy)>0) iHeightSquare = iHeightSquare+1;

                    // Data check
                    if (iCanvasHeight == 0 || iCanvasWidth == 0) return;
                    if (iAx<0) iAx = 0;
                    if (iAy<0) iAy = 0;
                    if (iHeightSquare <=0) iHeightSquare = 1;
                    if (iWidthSquare <= 0) iWidthSquare = 1;

                    // Calculate Percentage
                    var iHeightPercentage = iHeightSquare/iCanvasHeight;
                    var iWidthPercentage = iWidthSquare/iCanvasWidth;
                    var iXPercentage = iAx/iCanvasWidth;
                    var iYPercentage = iAy/iCanvasHeight;

                    // Apply to the real band dimension
                    scope.body.viewportX = scope.body.viewportX + Math.floor(iXPercentage * this.body.viewportWidth);
                    scope.body.viewportY = scope.body.viewportY + Math.floor(iYPercentage *  this.body.viewportHeight);
                    scope.body.viewportWidth = Math.ceil(iWidthPercentage * this.body.viewportWidth);
                    scope.body.viewportHeight = Math.ceil(iHeightPercentage * this.body.viewportHeight);

                    // Fix Image Ratio
                    var dOriginalRatio = this.body.originalBandHeight / this.body.originalBandWidth;


                    if ( (scope.body.viewportWidth*dOriginalRatio) > (scope.body.viewportHeight / dOriginalRatio) )  {
                        scope.body.viewportHeight = Math.ceil(scope.body.viewportWidth * dOriginalRatio);
                    }
                    else {
                        scope.body.viewportWidth = Math.ceil(scope.body.viewportHeight / dOriginalRatio);
                    }
                    //
                    // console.log("------------------------- START: zoom -------------------------");
                    // console.log("this.body.originalBandHeight: " + this.body.originalBandHeight);
                    // console.log("this.body.originalBandWidth: " + this.body.originalBandWidth);
                    // console.log("scope.body.viewportX: " + scope.body.viewportX);
                    // console.log("scope.body.viewportY: " + scope.body.viewportY);
                    // console.log("scope.body.viewportWidth: " + scope.body.viewportWidth);
                    // console.log("scope.body.viewportHeight: " + scope.body.viewportHeight);
                    // console.log("------------------------- STOP: zoom -------------------------");

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
                    // console.log("------------------------- START: getZoomTemporaryImage -------------------------");
                    // console.log("scope.Square.graphics.command.h: " + scope.Square.graphics.command.h);
                    // console.log("scope.Square.graphics.command.w: " + scope.Square.graphics.command.w);
                    // console.log("scope.Square.graphics.command.x: " + scope.Square.graphics.command.x);
                    // console.log("scope.Square.graphics.command.y: " + scope.Square.graphics.command.y);
                    // console.log("scope.Dragger.x: " + scope.Dragger.x);
                    // console.log("scope.Dragger.y: " + scope.Dragger.y);
                    // console.log("------------------------- STOP: getZoomTemporaryImage -------------------------");
                    var iHeightSquare = scope.Square.graphics.command.h;
                    var iWidthSquare = scope.Square.graphics.command.w;
                    var iAx = scope.Square.graphics.command.x + Math.abs(scope.Dragger.x);
                    var iAy = scope.Square.graphics.command.y + Math.abs(scope.Dragger.y);

                    // Consider always the ViewPort with origin point on top left and positive dimensions
                    if (iHeightSquare<0) {
                        iAy = iAy + iHeightSquare;
                        iHeightSquare = Math.abs(iHeightSquare);
                    }

                    if (iWidthSquare<0) {
                        iAx = iAx + iWidthSquare;
                        iWidthSquare = Math.abs(iWidthSquare);
                    }

                    if (iAx-Math.trunc(iAx)>0) iWidthSquare = iWidthSquare+1;
                    if (iAy-Math.trunc(iAy)>0) iHeightSquare = iHeightSquare+1;

                    // More inclusive arrotondation
                    iAx = Math.floor(iAx);
                    iAy = Math.floor(iAy);

                    // Fix Image Ratio
                    var dOriginalRatio = this.body.originalBandHeight / this.body.originalBandWidth;
                    if (iWidthSquare * dOriginalRatio > iHeightSquare / dOriginalRatio)  {
                        iHeightSquare = Math.ceil(iWidthSquare * dOriginalRatio);
                    }
                    else {
                        iWidthSquare = Math.ceil(iHeightSquare / dOriginalRatio);
                    }

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
                    scope.m_bIsVisibleMouseCursorWait = true;
                    // console.log("---------------------------- START BEFORE Body ----------------------------");
                    // console.log("this.body.originalBandHeight: " + this.body.originalBandHeight );
                    // console.log("this.body.originalBandWidth: " +  this.body.originalBandWidth );
                    // console.log("this.body.viewportX: " + this.body.viewportX);
                    // console.log("this.body.viewportY: " + this.body.viewportY);
                    // console.log("this.body.viewportWidth: " + this.body.viewportWidth);
                    // console.log("this.body.viewportHeight: " + this.body.viewportHeight);
                    // console.log("---------------------------- END Body ----------------------------");
                    this.getZoomTemporaryImage();
                    this.applyEditorPreviewImage();
                    // this.body = this.body;
                    // console.log("---------------------------- START BEFORE AFTER Body ----------------------------");
                    // console.log("this.body.originalBandHeight: " + this.body.originalBandHeight );
                    // console.log("this.body.originalBandWidth: " +  this.body.originalBandWidth );
                    // console.log("this.body.viewportX: " + this.body.viewportX);
                    // console.log("this.body.viewportY: " + this.body.viewportY);
                    // console.log("this.body.viewportWidth: " + this.body.viewportWidth);
                    // console.log("this.body.viewportHeight: " + this.body.viewportHeight);
                    // console.log("---------------------------- END AFTER Body ----------------------------");
                };

                /**
                 *
                 */
                scope.clickOnGetDefaultImage=function()
                {
                    scope.m_bIsVisibleMouseCursorWait = true;
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
                    var fScaleValueX = Math.abs( oPositionStage.width / oPositionImageOnStage.width );
                    var fScaleValueY = Math.abs( oPositionStage.height / oPositionImageOnStage.height );

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

                /**
                 *
                 */
                scope.removeSquareAndDraggerContainer = function(){
                    scope.Stage.removeChild(scope.Square);
                    scope.Square = null;

                    scope.Stage.removeChild(scope.Dragger);
                    scope.Dragger = null;
                };

                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    scope.m_bIsVisibleMouseCursorWait = false;

                    scope.Stage.removeAllChildren();

                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "empty")
                    {
                        var oBitmap =  new createjs.Bitmap(newValue);
                        scope.Bitmap = oBitmap;
                        scope.Stage.addChild(oBitmap);
                        scope.removeSquareAndDraggerContainer();
                    }

                    scope.Stage.update();
                });
            }
        };
    });