import './node_modules/font-awesome/css/font-awesome.min.css'; // test with reference in node_modules
import './node_modules/jstree/dist/themes/default/style.min.css';
import './fonts/astronaut/stylesheet.css';
import './fonts/starlight/stylesheet.css';
import "./lib/Cesium/Widgets/widgets.css";
import "./lib/Cesium/Widgets/BaseLayerPicker/BaseLayerPicker.css";
import "./node_modules/angular-colorpicker-directive/color-picker.min.css";
import "./css/bootstrap-4/customized_bootstrap_v4.css";
import "./css/style.css";
import "./node_modules/angularjs-slider/dist/rzslider.min.css";

import './node_modules/vex-js/dist/css/vex.css';
import './node_modules/vex-js/dist/css/vex-theme-default.css';
import './node_modules/vex-js/dist/css/vex-theme-bottom-right-corner.css';
import './node_modules/vex-js/dist/css/vex-theme-top.css';
//import './lib/wps-js/css/wps-js.css'; commented out because in css there are references to images that are not available in node modules ??
import './lib/json-edit/css/styles.css';

import './node_modules/leaflet-draw/dist/leaflet.draw.css';
import './node_modules/angular-moment-picker/src/angular-moment-picker.css';
import './assets/slider-appdetails/css/lightslider.css';

require('./services/ConstantsService.js');
require('./services/SessionInjector.js');
require('./services/AuthService.js');
require('./services/AuthServiceFacebook.js');
require('./services/MapService.js');
require('./services/GlobeService.js');
require('./services/WorkspaceService.js');
require('./services/FileBufferService.js');
require('./services/ProductService.js');
require('./services/ConfigurationService.js');
require('./services/OpenSearchService.js');
require('./services/ProcessesLaunchedService.js');
require('./services/SearchOrbitService.js');
require('./services/RabbitStompService.js');
require('./services/ResultsOfSearchService.js');
require('./services/SnapOperationService.js');
require('./services/SatelliteService.js');
require('./services/CatalogService.js');
require('./services/PagesService.js');
require('./services/FilterService.js');
require('./services/ProcessorService.js');
require('./services/TreeService.js');
require('./services/ProcessorMediaService.js');
require('./services/NodeService.js');


/*require('./app.js');*/

/*
begin test to migrate lib



require('./lib/factories/ViewElementFactory.js');
require('./lib/enum/ProductsProviders.js');


require('./node_modules/angular-colorpicker-directive/color-picker.min.js');

require('./node_modules/showdown/dist/showdown.js');
require('./environments/secrets.js');
require('./environments/environment.js');
require('./models/RabbitConnectionState.js');
require('./models/TabType.js');
require('./models/MessageHelper.js');

require('./directives/SnakeDirective.js');
require('./directives/tree_directive/TreeDirective.js');
require('./directives/checklist-model/checklist-model.js');
require('./directives/space_invaders/SpaceInvadersDirective.js');
require('./directives/space_invaders_fixed/SpaceInvadersFixedDirective.js');
require('./directives/multiselect/MultiselectDirective.js');
require('./directives/space_invaders_fixed_small_version/SpaceInvaderFixedSmallVersionDirective.js');
require('./directives/multiselect_dropdown_menu/MultiselectDropdownMenuDirective.js');
require('./directives/DrawSquares/SquaresDirective.js');
require('./directives/multiradiobutton_dropdown_menu/MultiRadioButtonDropdownMenuDirective.js');
require('./directives/image_preview/ImagePreviewDirective.js');
require('./directives/Toggle/ToggleSwitchDirective.js');
require('./directives/image_editor/ImageEditorController.js');
require('./directives/DropDownMenu/DropDownMenuDirective.js');
require('./directives/wasdiApps/wapTextBox/wapTextBox.js');
require('./directives/wasdiApps/wapSelectArea/wapSelectArea.js');
require('./directives/wasdiApps/wapDateTimePicker/wapDateTimePicker.js');
require('./directives/wasdiApps/wapProductList/wapProductList.js');
require('./directives/wasdiApps/wapDropDown/wapDropDown.js');
require('./directives/wasdiApps/wapProductsCombo/wapProductsCombo.js');
require('./directives/wasdiApps/wapSearchEOImage/wapSearchEOImage.js');
require('./directives/wasdiApps/wapCheckBox/wapCheckBox.js');
require('./directives/wasdiApps/wapSlider/wapSlider.js');
require('./directives/chips-list/chips-list.directive.js');
require('./directives/AngularLightSlider.js');
require('./directives/insertableTextArea.js');
require('./lib/stringUtils/removeSpaces.js');


require('./dialogs/orbit_info/OrbitInfoController.js');
require('./dialogs/product_info/ProductInfoController.js');
require('./dialogs/get_capabilities_dialog/GetCapabilitiesController.js');
require('./dialogs/merge_products_dialog/MergeProductsController.js');
require('./dialogs/product_editor_info/ProductEditorInfoController.js');
require('./dialogs/attributes_metadata_info/AttributesMetadataController.js');
require('./dialogs/sftp_upload/SftpUploadController.js');
require('./dialogs/delete_process/DeleteProcessController.js');
require('./dialogs/workspace_processes_list/WorkspaceProcessesList.js');
require('./dialogs/snake_dialog/SnakeController.js');
require('./dialogs/get_info_product_catalog/GetInfoProductCatalog.js');
require('./dialogs/downloadProductInWorkspace/DownloadProductInWorkspaceController.js');
require('./dialogs/filter_band_operation/FilterBandController.js');
require('./dialogs/mask_manager/MaskManagerController.js');
require('./dialogs/import_advance_filters/ImportAdvanceFiltersController.js');
require('./dialogs/workflow_manager/WorkFlowManagerController.js');
require('./dialogs/get_list_of_workspace_selected/GetListOfWorkspacesSelectedController.js');
require('./dialogs/processor/ProcessorController.js');
require('./dialogs/workspace_details/WorkspaceDetailsController.js');
require('./dialogs/wps_dialog/WpsController.js');
require('./dialogs/wapps/WappsController.js');
require('./dialogs/edit_user/EditUserController.js');
require('./dialogs/ftp_service/FTPController.js');
require('./dialogs/image_editor/image-editor.component.js');
require('./dialogs/upload_file/UploadFileController.js');
require('./dialogs/Import/UploadController.js');
require('./dialogs/mosaic/MosaicController.js');
require('./dialogs/edit_panel/EditPanelController.js');
require('./dialogs/process_error_logs_dialog/ProcessErrorLogsDialogController.js');
require('./dialogs/share_workspace/ShareWorkspaceController.js');
require('./dialogs/manual_insert_bbox/ManualInsertBboxController.js');
require('./dialogs/payload_dialog/PayloadDialogController.js');


require('./lib/bower-sockjs-client/sockjs.js');
require('./lib/stomp-websocket/lib/stomp.js');
require('./lib/utils/FadeoutJSUtils.js');
require('./lib/utils/FadeOutVexDialogUtils.js');
require('./lib/utils/WasdiJSUtils.js');
require('./lib/leafletProvider/leaflet-providers.js');

require('./lib/Cesium/Cesium.js');
require('./node_modules/vex-js/dist/js/vex.combined.js');
require('./node_modules/angular-modal-service/dst/angular-modal-service.min.js');
require('./lib/json-edit/js/directives.js');
require('./lib/json-edit/bower_components/angular-ui-sortable/sortable.min.js');
require('./lib/wps-js/js/openlayers/OpenLayers-closure.js');
require('./lib/wps-js/js/wps-js/wps-js.0.1.1.js');
*/
//
