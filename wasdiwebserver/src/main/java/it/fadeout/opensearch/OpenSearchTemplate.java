package it.fadeout.opensearch;

import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.i18n.templates.Template;

public class OpenSearchTemplate {
	
	
	private static final Template template =
            new Template(
                    "{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q}");

    public OpenSearchTemplate()
    {

    }

    public static String getHttpUrl(String qParams)
    {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("scheme","https");
        map.put("path", new String[] {"dhus","search"});
        map.put("q", qParams);
        
        return template.expand(map);
    }
}
