//import './node_modules/font-awesome/css/font-awesome.min.css'; // test with reference in node_modules

import './node_modules/@fortawesome/fontawesome-free/css/all.min.css';
import './node_modules/jstree/dist/themes/default/style.min.css';
import './app/fonts/astronaut/stylesheet.css';
import './app/fonts/starlight/stylesheet.css';
import "./app/lib/Cesium/Widgets/widgets.css";
import "./app/lib/Cesium/Widgets/BaseLayerPicker/BaseLayerPicker.css";
import "./node_modules/angular-colorpicker-directive/color-picker.min.css";
import "./app/css/bootstrap-4/customized_bootstrap_v4.css";
import "./app/css/style.css";
import "./node_modules/angularjs-slider/dist/rzslider.min.css";

import './node_modules/vex-js/dist/css/vex.css';
import './node_modules/vex-js/dist/css/vex-theme-default.css';
import './node_modules/vex-js/dist/css/vex-theme-bottom-right-corner.css';
import './node_modules/vex-js/dist/css/vex-theme-top.css';
//import './app/lib/wps-js/css/wps-js.css'; commented out because in css there are references to images that are not available in node modules ??
import './app/lib/json-edit/css/styles.css';

import './node_modules/leaflet-draw/dist/leaflet.draw.css';
import './node_modules/angular-moment-picker/src/angular-moment-picker.css';
import './app/assets/slider-appdetails/css/lightslider.css';

require('./app/services/api/AuthService.js');
require('./app/services/api/CatalogService.js');
require('./app/services/api/FileBufferService.js');
require('./app/services/api/NodeService.js');
require('./app/services/api/OpenSearchService.js');
require('./app/services/api/OpportunitySearchService.js');
require('./app/services/api/OrganizationService.js');
require('./app/services/api/WorkspaceService.js');
require('./app/services/api/ProcessingService');
require('./app/services/api/ProcessorMediaService.js');
require('./app/services/api/ProcessorService.js');
require('./app/services/api/ConsoleService.js');
require('./app/services/api/ProcessWorkspaceService.js');
require('./app/services/api/ProductService.js');
require('./app/services/api/WorkflowService.js');
require('./app/services/api/StyleService.js');
require('./app/services/api/FeedbackService.js');
require('./app/services/api/AdminDashboardService.js');
require('./app/services/api/PackageManagerService.js');








require('./app/services/ConfigurationService.js');

require('./app/services/ConstantsService.js');
require('./app/services/SessionInjector.js');

//require('./app/services/api/AuthServiceFacebook.js');
require('./app/services/MapService.js');
require('./app/services/GlobeService.js');

//require('./app/services/ProcessesLaunchedService.js');
//require('./app/services/SearchOrbitService.js');
require('./app/services/RabbitStompService.js');
require('./app/services/ResultsOfSearchService.js');
//require('./app/services/SnapOperationService.js');
//require('./app/services/SatelliteService.js');

require('./app/services/PagesService.js');
//require('./app/services/api/FilterService.js');

require('./app/services/TreeService.js');




/*require('./app/app.js');*/

/*
begin test to migrate lib



require('./app/lib/factories/ViewElementFactory.js');
require('./app/lib/enum/ProductsProviders.js');


require('./node_modules/angular-colorpicker-directive/color-picker.min.js');

require('./node_modules/showdown/dist/showdown.js');
require('./app/environments/secrets.js');
require('./app/environments/environment.js');
require('./app/models/RabbitConnectionState.js');
require('./app/models/TabType.js');
require('./app/models/MessageHelper.js');

require('./app/directives/SnakeDirective.js');
require('./app/directives/tree_directive/TreeDirective.js');
require('./app/directives/checklist-model/checklist-model.js');
require('./app/directives/space_invaders/SpaceInvadersDirective.js');
require('./app/directives/space_invaders_fixed/SpaceInvadersFixedDirective.js');
require('./app/directives/multiselect/MultiselectDirective.js');
require('./app/directives/space_invaders_fixed_small_version/SpaceInvaderFixedSmallVersionDirective.js');
require('./app/directives/multiselect_dropdown_menu/MultiselectDropdownMenuDirective.js');
require('./app/directives/DrawSquares/SquaresDirective.js');
require('./app/directives/multiradiobutton_dropdown_menu/MultiRadioButtonDropdownMenuDirective.js');
require('./app/directives/image_preview/ImagePreviewDirective.js');
require('./app/directives/Toggle/ToggleSwitchDirective.js');
require('./app/directives/image_editor/ImageEditorController.js');
require('./app/directives/DropDownMenu/DropDownMenuDirective.js');
require('./app/directives/wasdiApps/wapTextBox/wapTextBox.js');
require('./app/directives/wasdiApps/wapSelectArea/wapSelectArea.js');
require('./app/directives/wasdiApps/wapDateTimePicker/wapDateTimePicker.js');
require('./app/directives/wasdiApps/wapProductList/wapProductList.js');
require('./app/directives/wasdiApps/wapDropDown/wapDropDown.js');
require('./app/directives/wasdiApps/wapProductsCombo/wapProductsCombo.js');
require('./app/directives/wasdiApps/wapSearchEOImage/wapSearchEOImage.js');
require('./app/directives/wasdiApps/wapCheckBox/wapCheckBox.js');
require('./app/directives/wasdiApps/wapSlider/wapSlider.js');
require('./app/directives/chips-list/chips-list.directive.js');
require('./app/directives/AngularLightSlider.js');
require('./app/directives/insertableTextArea.js');
require('./app/lib/stringUtils/removeSpaces.js');


require('./app/dialogs/orbit_info/OrbitInfoController.js');
require('./app/dialogs/product_info/ProductInfoController.js');
require('./app/dialogs/get_capabilities_dialog/GetCapabilitiesController.js');
require('./app/dialogs/merge_products_dialog/MergeProductsController.js');
require('./app/dialogs/product_editor_info/ProductEditorInfoController.js');
require('./app/dialogs/attributes_metadata_info/AttributesMetadataController.js');
require('./app/dialogs/sftp_upload/SftpUploadController.js');
require('./app/dialogs/delete_process/DeleteProcessController.js');
require('./app/dialogs/workspace_processes_list/WorkspaceProcessesList.js');
require('./app/dialogs/snake_dialog/SnakeController.js');
require('./app/dialogs/get_info_product_catalog/GetInfoProductCatalog.js');
require('./app/dialogs/downloadProductInWorkspace/DownloadProductInWorkspaceController.js');
require('./app/dialogs/filter_band_operation/FilterBandController.js');
require('./app/dialogs/mask_manager/MaskManagerController.js');
require('./app/dialogs/import_advance_filters/ImportAdvanceFiltersController.js');
require('./app/dialogs/workflow_manager/WorkFlowManagerController.js');
require('./app/dialogs/get_list_of_workspace_selected/GetListOfWorkspacesSelectedController.js');
require('./app/dialogs/processor/ProcessorController.js');
require('./app/dialogs/workspace_details/WorkspaceDetailsController.js');
require('./app/dialogs/wps_dialog/WpsController.js');
require('./app/dialogs/wapps/WappsController.js');
require('./app/dialogs/edit_user/EditUserController.js');
require('./app/dialogs/ftp_service/FTPController.js');
require('./app/dialogs/image_editor/image-editor.component.js');
require('./app/dialogs/upload_file/UploadFileController.js');
require('./app/dialogs/Import/UploadController.js');
require('./app/dialogs/mosaic/MosaicController.js');
require('./app/dialogs/edit_panel/EditPanelController.js');
require('./app/dialogs/process_error_logs_dialog/ProcessErrorLogsDialogController.js');
require('./app/dialogs/share_workspace/ShareWorkspaceController.js');
require('./app/dialogs/manual_insert_bbox/ManualInsertBboxController.js');
require('./app/dialogs/payload_dialog/PayloadDialogController.js');


require('./app/lib/bower-sockjs-client/sockjs.js');
require('./app/lib/stomp-websocket/lib/stomp.js');
require('./app/lib/utils/FadeoutJSUtils.js');
require('./app/lib/utils/FadeOutVexDialogUtils.js');
require('./app/lib/utils/WasdiJSUtils.js');
require('./app/lib/leafletProvider/leaflet-providers.js');

require('./app/lib/Cesium/Cesium.js');
require('./node_modules/vex-js/dist/js/vex.combined.js');
require('./node_modules/angular-modal-service/dst/angular-modal-service.min.js');
require('./app/lib/json-edit/js/directives.js');
require('./app/lib/json-edit/bower_components/angular-ui-sortable/sortable.min.js');
require('./app/lib/wps-js/js/openlayers/OpenLayers-closure.js');
require('./app/lib/wps-js/js/wps-js/wps-js.0.1.1.js');
*/
//
