package wasdi.shared.opensearch;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.json.JSONObject;

public class QueryExecutorSENTINEL extends QueryExecutor {

	@Override
	protected Template getTemplate() {
		return new Template("{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override	
	protected String[] getUrlPath() {
		return new String[] {"apihub","search"};
	}

	@Override
	protected String getUrlSchema() {
		return "https";
	}

	@Override
	protected String getCountUrl(String sQuery) {
		return "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=" + sQuery;
	}
	
}
