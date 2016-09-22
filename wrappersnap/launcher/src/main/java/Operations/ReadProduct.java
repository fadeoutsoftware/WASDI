package Operations;

import ViewModels.AttributeViewModel;
import ViewModels.BandViewModel;
import ViewModels.MetadataViewModel;
import ViewModels.NodeGroupViewModel;
import ViewModels.ProductViewModel;
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


    private Product ReadProduct(File oFile)
    {
        //Per ora ipotizziamo solo dati Sentinel-1
        String formatName = "SENTINEL-1";
        //read product
        Product exportProduct = null;
        try {

            if (m_oCacheProducts.get(oFile) == null)
                exportProduct =  ProductIO.readProduct(oFile, formatName);

            //put in cache dictionary
            m_oCacheProducts.put(oFile.getName(), exportProduct);

        }
        catch(Exception oEx)
        {
            oEx.printStackTrace();
            System.out.println(oEx.toString());

        }

        return m_oCacheProducts.get(oFile.getName());
    }


    public ProductViewModel getProduct(File oFile) throws IOException
    {
        ProductViewModel oViewModel = new ProductViewModel();
        Product exportProduct = ReadProduct(oFile);

        if (exportProduct == null)
            return null;

        oViewModel.setMetadata(GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata")));

        this.FillBandsViewModel(oViewModel, exportProduct);

        //Gson gson = new Gson();
        //String json = gson.toJson(oViewModel);
        return  oViewModel;
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

    public String writeBigTiff(String sFileName) throws Exception {
        String sLayerId = "";

        String sTempFile = "C:\\temp\\ImagePyramidTest\\";

        Product oSentinelProduct = ReadProduct.m_oCacheProducts.get(sFileName);

        WriteProduct oWriter = new WriteProduct();
        oWriter.WriteBigTiff(oSentinelProduct, sTempFile, sFileName, null);



        return sLayerId;

    }



}
