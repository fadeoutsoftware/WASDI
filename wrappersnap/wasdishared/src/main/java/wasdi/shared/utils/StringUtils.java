package wasdi.shared.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import wasdi.shared.utils.log.WasdiLog;

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
	
	/**
	 * Encode sUrl in URL Encoding
	 * @param sUrl String to encode
	 * @return Encoded String
	 */
	public static String encodeUrl(String sUrl) {
		try {
			return URLEncoder.encode(sUrl, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			WasdiLog.debugLog("StringUtils.encodeUrl: could not encode URL due to " + oE + ".");
		}

		return sUrl;
	}

	public static String generateSha224(String input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-224");
			byte[] bytes = messageDigest.digest(input.getBytes());
			String digest = bytesToHexadecimal(bytes);

			return digest;
		} catch (NoSuchAlgorithmException oEx) {
			WasdiLog.debugLog("StringUtils.generateSha224: invalid digest algorithm SHA-224: " + oEx + ".");
		} catch (Exception oEx) {
			WasdiLog.debugLog("StringUtils.generateSha224: could not generate Sha224 due to " + oEx + ".");
		}

		return input;
	}
	
	/**
	 * Convert an array of bytes in a String representation
	 * @param bytes Array of bytes
	 * @return String hex representation 
	 */
	private static String bytesToHexadecimal(byte[] bytes) {
		// Convert byte array into signum representation
		BigInteger bigInteger = new BigInteger(1, bytes);

		// Convert message digest into hex value
		String hashtext = bigInteger.toString(16);

		// Add preceding 0s to make it 32 bit
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}

		// return the HashText
		return hashtext;
	}
	
	/**
	 * Assume that the sNumber parameter contains the representation of an integer.
	 * It returns the number incremented by 1.
	 * @param sNumber Input string with the number
	 * @return String with the number incremented, empty string otherwise
	 */
	public static String incrementIntegerString(String sNumber) {
		String sReturnString = "";
		
		try {
			int iNumber = Integer.parseInt(sNumber);
			
			iNumber = iNumber+1;
			sReturnString = "" + iNumber;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StringUtils.incrementIntegerString: not valid input string - ", oEx);
		}
		
		return sReturnString;
	}

}
