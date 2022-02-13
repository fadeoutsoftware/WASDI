package wasdi.shared.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Utility class to handle String related operations.
 * 
 * @author PetruPetrescu
 *
 */
public final class StringUtils {

	private StringUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * Compress the string content. Useful when a large string should be passed as an URL.
	 * 
	 * @param srcTxt the source text
	 * @return the compressed string
	 * @throws IOException in case of any issues
	 */
	public static String compressString(String srcTxt) throws IOException {
		ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
		GZIPOutputStream zos = new GZIPOutputStream(rstBao);
		zos.write(srcTxt.getBytes());
		IOUtils.closeQuietly(zos);

		byte[] bytes = rstBao.toByteArray();
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		return encoder.encodeToString(bytes);
	}

	/**
	 * Uncompress the string content.
	 * 
	 * @param zippedBase64Str the compressed content
	 * @return the uncompressed content
	 * @throws IOException in case of any issues
	 */
	public static String uncompressString(String zippedBase64Str) throws IOException {
		String result = null;

		Base64.Decoder decoder = Base64.getUrlDecoder();
		byte[] bytes = decoder.decode(zippedBase64Str);
		GZIPInputStream zi = null;
		try {
			zi = new GZIPInputStream(new ByteArrayInputStream(bytes));
			result = IOUtils.toString(zi);
		} finally {
			IOUtils.closeQuietly(zi);
		}
		return result;
	}

}
