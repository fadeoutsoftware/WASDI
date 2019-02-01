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
                applyEditorPreviewImage:'&',
                maskManager: '&',
                filterManager: '&',
                //onEditBtnClick : '&onEditClick',
                panScaling: '=',
                heightCanvas: '=',
                widthCanvas: '='
                // isLoaded : '='
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            link: function(scope, elem, attrs) {

                // var e = elem;
                // var _this = this;
                // elem.ready(function () {
                //     debugger;
                //     console.debug(e);
                //     console.debug(_this);
                //     console.debug(scope);
                //     heie[0].parentElement.clientWidth
                // })

                var iDefaultValueZoom = 100;
                // var dimension = utilsProjectGetMapContainerSize();
                //default value canvas
                // var m_iHeightCanvas = scope.heightCanvas;
                // var m_iWidthCanvas = scope.widthCanvas;

                //default value canvas
                var stage = new createjs.Stage("imageviewcanvas");
                var oBitmap = new createjs.Bitmap(scope.urlImage);
                // var iPanScalingValue = 1.2;
                // scope.iPanScalingValue = iPanScalingValue;
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
                    if(utilsIsObjectNullOrUndefined(stage) === false && utilsIsObjectNullOrUndefined(event) === false)
                    {
                        stage.update(event);
                    }
                };
                //TODO DESTROY EVENT
                // scope.$on(
                //     "$destroy",
                //     function( event ) {
                //         console.log('Destroy Ticker');
                //         oTicker = null;
                //
                //     }
                // );

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
                        oMouseLastPoint.stageX = evt.stageX;
                        oMouseLastPoint.stageY = evt.stageY;
                        scope.zoom();
                    }

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
                    scope.m_bIsActiveZoom = !scope.m_bIsActiveZoom;
                };

                scope.clickOnResetZoom = function () {
                    scope.body.viewportX = 0;
                    scope.body.viewportY = 0;
                    scope.body.viewportWidth = scope.body.originalBandWidth;
                    scope.body.viewportHeight = scope.body.originalBandHeight;

                    scope.clickOnGetDefaultImage();
                };

                scope.clickOnEdit = function(){
                    scope.onEditBtnClick();
                }

                scope.clickOnMask = function () {
                    scope.maskManager();
                };

                scope.clickOnFilter = function () {
                    scope.filterManager();
                }

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
                    return (utilsIsPointInsideSquare(x, y, oPointASquare.stageX,oPointASquare.stageY,oPointBSquare.stageX,oPointBSquare.stageY) === false)
                };


                stage.update();

                //INIT SOME SCOPE VARIABLE
                scope.Square = null;
                scope.Stage = stage;
                scope.Bitmap = oBitmap;
                scope.Dragger = null;
                scope.Pan = null;
                scope.m_bIsVisibleMouseCursorWait = false;

                /**
                 *
                 * @param evt
                 */
                scope.draggerPressMoveCallback = function(evt) {
                    // Calculate the new X and Y based on the mouse new position plus the offset.
                    evt.currentTarget.x = evt.stageX + evt.currentTarget.offset.x;
                    evt.currentTarget.y = evt.stageY + evt.currentTarget.offset.y;

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
                    var iCanvasHeight = element[0].offsetHeight * scope.panScaling;
                    var iCanvasWidth = element[0].offsetWidth * scope.panScaling;

                    if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                    {
                        return false;
                    }
                    // Take position and dimensions of the over rectangle
                    var iHeightSquare = scope.Square.graphics.command.h;
                    var iWidthSquare = scope.Square.graphics.command.w;
                    // var iAx = scope.Square.graphics.command.x + Math.abs(scope.Dragger.x);
                    // var iAy = scope.Square.graphics.command.y + Math.abs(scope.Dragger.y);
                    var iAx,iAy;
                    //PAN
                    var oZoomSquarePoint = scope.calculateSquarePointInImage(scope.Square.graphics.command,scope.Pan);
                    if(utilsIsObjectNullOrUndefined(oZoomSquarePoint) === false)
                    {
                        iAx = oZoomSquarePoint.x;
                        iAy = oZoomSquarePoint.y;

                    }
                    
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
                    var iHeightPercentage = iHeightSquare/(iCanvasHeight);
                    var iWidthPercentage = iWidthSquare/(iCanvasWidth);
                    var iXPercentage = iAx/(iCanvasWidth);
                    var iYPercentage = iAy/(iCanvasHeight);




                    // Apply to the real band dimension
                    scope.body.viewportX = scope.body.viewportX + Math.floor(iXPercentage * this.body.viewportWidth);
                    scope.body.viewportY = scope.body.viewportY + Math.floor(iYPercentage *  this.body.viewportHeight);
                    scope.body.viewportWidth = Math.ceil(iWidthPercentage * this.body.viewportWidth);
                    scope.body.viewportHeight = Math.ceil(iHeightPercentage * this.body.viewportHeight);

                    //scaling pan
                    scope.scalePanSize(scope.body, scope.panScaling);

                    // Fix Image Ratio
                    var dOriginalRatio = this.body.originalBandHeight / this.body.originalBandWidth;


                    if ( (scope.body.viewportWidth*dOriginalRatio) > (scope.body.viewportHeight / dOriginalRatio) )  {
                        scope.body.viewportHeight = Math.ceil(scope.body.viewportWidth * dOriginalRatio);
                    }
                    else {
                        scope.body.viewportWidth = Math.ceil(scope.body.viewportHeight / dOriginalRatio);
                    }

                };

                scope.scalePanSize = function(oBody,iScaleValue){
                    // iScaleValue = 2;
                    // oBody.viewportX = oBody.viewportX * 0.98;
                    // oBody.viewportY = oBody.viewportY * 0.98;
                    oBody.viewportWidth = oBody.viewportWidth * iScaleValue;
                    oBody.viewportHeight = oBody.viewportHeight * iScaleValue;

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
                    scope.m_bIsActiveZoom = false;
                    //update zoom for server request
                    scope.zoom();

                    // https://codepen.io/fabiobiondi/pen/blHoy lik useful about crop images
                    var iHeightSquare = scope.Square.graphics.command.h;
                    var iWidthSquare = scope.Square.graphics.command.w;
                    // var iAx = scope.Square.graphics.command.x + Math.abs(scope.Dragger.x);
                    // var iAy = scope.Square.graphics.command.y + Math.abs(scope.Dragger.y);

                    var iAx,iAy;
                    //PAN
                    var oZoomSquarePoint = scope.calculateSquarePointInImage(scope.Square.graphics.command,scope.Pan);
                    if(utilsIsObjectNullOrUndefined(oZoomSquarePoint) === false)
                    {
                        iAx = oZoomSquarePoint.x;
                        iAy = oZoomSquarePoint.y;
                    }

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

                    // //Remove old Bitmap
                    // scope.Stage.removeChild(scope.Bitmap);
                    // scope.Bitmap = null;
                    scope.Stage.removeAllChildren();

                    //add new bitmap
                    stage.addChild(oNewImage);
                    // // remove dragger
                    // scope.Stage.removeChild(scope.Dragger);
                    // scope.Dragger = null;
                    // //Remove old Square
                    // scope.Stage.removeChild(scope.Square);
                    // scope.Square = null;
                    return true;
                };

                /*
                    calculateSquarePointInImage
                    this function takes in input the point of the zoom in the stage and the point of the image in the stage;
                    return the point of the zoom in the image
                * */
                scope.calculateSquarePointInImage =  function(oPointOfTheZoomInTheStage,oPointOfTheImageInTheStage )
                {
                    if( (utilsIsObjectNullOrUndefined(oPointOfTheZoomInTheStage) === true) || ( utilsIsObjectNullOrUndefined(oPointOfTheImageInTheStage) === true) )
                    {
                        return null;
                    }
                    var oZoomSquarePoint ={
                        x: ( oPointOfTheZoomInTheStage.x - oPointOfTheImageInTheStage.x ),
                        y:( oPointOfTheZoomInTheStage.y - oPointOfTheImageInTheStage.y )
                    };
                    return oZoomSquarePoint;

                }

                /**
                 *
                 */
                scope.clickOnGetImage = function()
                {
                    scope.m_bIsVisibleMouseCursorWait = true;

                    this.getZoomTemporaryImage();
                    this.applyEditorPreviewImage();

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
                 * removeSquareAndDraggerContainer
                 */
                scope.removeSquareAndDraggerContainer = function(){
                    scope.Stage.removeChild(scope.Square);
                    scope.Square = null;

                    scope.Stage.removeChild(scope.Dragger);
                    scope.Dragger = null;
                };

                /**
                 * panMouseDown
                 * @param evt
                 */
                scope.panMouseDown = function (evt)
                {
                    //if zoom is active skip drag and drop
                    if (scope.m_bIsActiveZoom === true) return;

                    // keep a record on the offset between the mouse position and the container
                    // position. currentTarget will be the container that the event listener was added to:
                    evt.currentTarget.offset = {x: this.x - evt.stageX, y: this.y - evt.stageY};
                    // scope.zoom();
                };

                /**
                 * panPressMove
                 * @param evt
                 */
                scope.panPressMove = function(evt)
                {
                    //if zoom is active skip drag and drop
                    if (scope.m_bIsActiveZoom === true) return;

                    // Calculate the new X and Y based on the mouse new position plus the offset.
                    evt.currentTarget.x = evt.stageX + evt.currentTarget.offset.x;
                    evt.currentTarget.y = evt.stageY + evt.currentTarget.offset.y;
                    // make sure to redraw the stage to show the change:
                    stage.update();
                    // scope.zoom();
                };
                /**
                 *
                 */
                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    scope.m_bIsVisibleMouseCursorWait = false;
                    scope.Stage.removeAllChildren();

                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "//:0")
                    {
                        scope.m_bIsActiveZoom = false;
                        var oBitmap =  new createjs.Bitmap(newValue);
                        //AFTER PAN
                        var oPan = new createjs.Container();
                        oPan.x = oPan.y = 0;
                        oPan.addChild(oBitmap); //, label
                        oPan.on("mousedown", scope.panMouseDown);
                        oPan.on("pressmove",scope.panPressMove);
                        scope.Pan = oPan;
                        scope.Stage.addChild(oPan);
                        scope.Bitmap = oBitmap;

                        // BEFORE PAN
                        // scope.Bitmap = oBitmap;
                        // scope.Stage.addChild(oBitmap);



                        scope.removeSquareAndDraggerContainer();
                        // scope.Stage.update();

                    }

                    scope.Stage.update();
                });
            }
        };
    });