package wasdi.snapopearations;

import org.apache.commons.net.io.Util;
import org.esa.s1tbx.io.sentinel1.Sentinel1ProductReader;
import org.esa.snap.core.util.SystemUtils;
import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AttributeViewModel;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.NodeGroupViewModel;
import wasdi.shared.viewmodels.ProductViewModel;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class ReadProduct {

    public  static HashMap<String, Product> m_oCacheProducts = new HashMap<String, Product>();


    public Product ReadProduct(File oFile)
    {
        //Per ora ipotizziamo solo dati Sentinel-1
        String formatName = "SENTINEL-1";
        //read product
        Product exportProduct = null;
        try {

            if (m_oCacheProducts.get(oFile.getName()) == null) {
                exportProduct =  ProductIO.readProduct(oFile, formatName);

                //put in cache dictionary
                m_oCacheProducts.put(oFile.getName(), exportProduct);
            }
        }
        catch(Exception oEx)
        {
            oEx.printStackTrace();
            LauncherMain.s_oLogger.debug(oEx.toString());

        }

        return m_oCacheProducts.get(oFile.getName());
    }

    private boolean RemoveFromCache(File oFile) {
        if (m_oCacheProducts.get(oFile.getName()) != null) {
            m_oCacheProducts.remove(oFile.getName());
            return  true;
        }

        return  false;
    }


    public ProductViewModel getProductViewModel(File oFile) throws IOException
    {
        ProductViewModel oViewModel = new ProductViewModel();
        Product exportProduct = ReadProduct(oFile);

        if (exportProduct == null)
            return null;

        // P.Campanella: splitted bands and metadata view models
        //oViewModel.setMetadata(GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata")));

        this.FillBandsViewModel(oViewModel, exportProduct);

        oViewModel.setName(Utils.GetFileNameWithoutExtension(oFile.getAbsolutePath()));
        oViewModel.setFileName(oFile.getName());

        return  oViewModel;
    }

    public MetadataViewModel getProductMetadataViewModel(File oFile) throws IOException
    {
        Product exportProduct = ReadProduct(oFile);

        if (exportProduct == null) return null;

        return  GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
    }

    private MetadataViewModel GetMetadataViewModel(MetadataElement oElement, MetadataViewModel oSourceViewModel) {

        for (MetadataAttribute oMetadataAttribute : oElement.getAttributes()) {
            AttributeViewModel oAttributeViewModel = new AttributeViewModel();
            oAttributeViewModel.setName(oMetadataAttribute.getName());
            oAttributeViewModel.setDescription(oMetadataAttribute.getDescription());
            if (oSourceViewModel.getAttributes() == null)
                oSourceViewModel.setAttributes(new ArrayList<AttributeViewModel>());
            oSourceViewModel.getAttributes().add(oAttributeViewModel);
        }


        for (MetadataElement oMetadataElement : oElement.getElements()) {
            MetadataViewModel oElementViewModel = new MetadataViewModel(oMetadataElement.getName());
            if (oSourceViewModel.getElements() == null)
                oSourceViewModel.setElements(new ArrayList<MetadataViewModel>());
            oSourceViewModel.getElements().add(GetMetadataViewModel(oMetadataElement, oElementViewModel));
        }

        return  oSourceViewModel;
    }

    private void FillBandsViewModel(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null)
            return;

        if (oProduct == null)
            return;

        if (oProductViewModel.getBandsGroups() == null)
            oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        ArrayList<BandViewModel> oBands = new ArrayList<BandViewModel>();

        for (Band oBand :
                oProduct.getBands()) {
            if (oProductViewModel.getBandsGroups().getBands() == null)
                oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());

            BandViewModel oViewModel = new BandViewModel(oBand.getName());
            oProductViewModel.getBandsGroups().getBands().add(oViewModel);

        }

    }

    public String writeBigTiff(String sFileName, String sWorkingPath, String sFileNameWithoutExtension) throws Exception {

        File oFile = new File (sFileName);

        LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: Read Product FILE = " + sFileName);

        Product oSentinelProduct = ReadProduct(oFile);

        if (oSentinelProduct == null) LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: Sentinel Product is null " + oFile.getAbsolutePath());

        LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: Create Writer");

        WriteProduct oWriter = new WriteProduct();

        LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: WriteTiff");

        String sBigTiff = "";

        try {
            sBigTiff = oWriter.WriteBigTiff(oSentinelProduct, sWorkingPath, sFileNameWithoutExtension, null);
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        // Close product IO
        RemoveFromCache(oFile);
        oSentinelProduct.closeIO();

        return sBigTiff;
    }



}
