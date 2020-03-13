angular.module('wasdi.TableOfProductsDirective', [])
    .directive('tableofproductsdirective', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                productsList: '=',
                // deleted: '&'
            },

            template: `
        <div class="table-of-products-directive">
            <table class="table table-hover layers "  >
                <tbody ng-repeat="layer in $ctrl.productsList ">
                    <!------------------------------- SUMMARY ------------------------------->
                    <tr class="info-layer" >

                        <!-- IMAGE CELL -->
                        <td class="image-cell">
                            <span class="label label-success">{{layer.summary.Mode}}</span>&nbsp;<!--Mode-->
                            <span class="label label-success"> {{layer.summary.Instrument}}</span><!--Instrument-->
                            <div class="preview-layer-image" ><img src={{layer.preview}} alt="Image" style="height: 82px;width: 82px; "></div>
                            <span class="badge">{{layer.summary.Size}}</span><!--Size-->
                        </td>

                        <!-- INFO CELL -->
                        <td class="info-cell">

                            <div>{{layer.summary.Date|date:'medium'}}</div><!--Date-->
                            </br>

                            <div>
                                <span>Name: </span>
                                <span><b>{{layer.title}}</b></span>
                            </div>

                            <div>
                                <span>Polarisation: </span>
                                <span><b>{{layer.summary.Mode}}</b></span>
                            </div>

                            <div>
                                <span>Relative Orbit: </span>
                                <span><b>{{layer.properties.relativeorbitnumber}}</b></span>
                            </div>

                            <!--<div>{{"layer"+layer.id}}</div>-->
                            <div>
                                <span>Provider:</span>
                                <span><b>{{layer.provider}}</b></span>
                            </div>

                            <div>
                                <span>Platform name:</span>
                                <span><b>{{layer.properties.platformname}}</b></span>
                            </div>
                            <div>
                                <span>Sensor Operational mode:</span>
                                <span><b>{{layer.properties.sensoroperationalmode}}</b></span>
                            </div>
                        </td>

                    </tr>
                </tbody>
            </table>
        </div>



         `,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });


