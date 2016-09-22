import org.apache.abdera.i18n.templates.Template;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class SearchUtil {
    private static final Template template =
            new Template(
                    "{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q}");

    public SearchUtil()
    {

    }

    public String getHttpUrl(String qParams)
    {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("scheme","https");
        map.put("path", new String[] {"apihub","search"});
        map.put("q", qParams);

        return template.expand(map);
    }
}
