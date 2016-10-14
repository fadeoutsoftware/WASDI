package wasdi.filebuffer;

import wasdi.ConfigReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Created by s.adamo on 06/10/2016.
 */
public class DownloadFile {



    private final int BUFFER_SIZE = 4096;

    //https://scihub.copernicus.eu/dhus/odata/v1/Products('18f7993d-eae1-4f7f-9d81-d7cf19c18378')/$value
    public String ExecuteDownloadFile(String sFileURL, String sSaveDirOnServer) throws IOException {

        // Domain check
        if (sFileURL == null) {
            System.out.println("DownloadFile.ExecuteDownloadFile: sFileURL is null");
            return "";
        }
        if (sSaveDirOnServer == null) {
            System.out.println("DownloadFile.ExecuteDownloadFile: sSaveDirOnServer is null");
            return "";
        }
        if (sFileURL.isEmpty()) {
            System.out.println("DownloadFile.ExecuteDownloadFile: sFileURL is Empty");
            return "";
        }
        if (sSaveDirOnServer.isEmpty()) {
            System.out.println("DownloadFile.ExecuteDownloadFile: sSaveDirOnServer is Empty");
            return "";
        }

        String sReturnFilePath = "";


        // TODO: Here we are assuming dhus authentication. But we have to find a general solution
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                try{
                    return new PasswordAuthentication(ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD") .toCharArray());
                }
                catch (Exception oEx){
                    System.out.println("DownloadFile.ExecuteDownloadFile: exception setting auth " + oEx.toString());
                }
                return null;
            }
        });

        URL url = new URL(sFileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1,
                        sFileURL.length());
            }

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = sSaveDirOnServer + File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            sReturnFilePath = saveFilePath;

            System.out.println("File downloaded " + sReturnFilePath);
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();

        return  sReturnFilePath;
    }
}
