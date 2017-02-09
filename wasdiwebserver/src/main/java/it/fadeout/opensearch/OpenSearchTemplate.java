package it.fadeout.opensearch;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.apache.abdera.i18n.templates.Template;

public class OpenSearchTemplate {


	private static final Template m_sSentinelTemplate =
			//new Template(
			//        "{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q}");

			new Template(
					"{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");

	private static final Template m_sCollaborativeTemplate =
			new Template(
					"{scheme}://{-append|.|host}collaborative.mt.asi.it{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|filter}{-join|&|filter,offset,limit,sortedby,order}");

	public OpenSearchTemplate()
	{

	}

	public static String getHttpUrl(String qParams, String sStart, String sRows, String sOrderBy, String sOrder, String sProvider)
	{
		switch(sProvider)
		{
		case "SENTINEL":
			Map<String,Object> oSentinelMap = new HashMap<String, Object>();
			oSentinelMap.put("scheme","https");
			oSentinelMap.put("path", new String[] {"apihub","search"});
			oSentinelMap.put("start", sStart);
			oSentinelMap.put("rows", sRows);
			oSentinelMap.put("orderby", sOrderBy + " " + sOrder);
			oSentinelMap.put("q", qParams);
			return m_sSentinelTemplate.expand(oSentinelMap);

		case "MATERA":
			Map<String,Object> oMateraMap = new HashMap<String, Object>();
			oMateraMap.put("scheme","http");
			oMateraMap.put("path", new String[] {"api","stub", "products"});
			oMateraMap.put("offset", sStart);
			oMateraMap.put("limit", sRows);
			oMateraMap.put("sortedby", sOrderBy);
			oMateraMap.put("order", sOrder);
			oMateraMap.put("filter", qParams);
			return m_sCollaborativeTemplate.expand(oMateraMap);

		default:
			break;
		}

		return "";

	}
}
