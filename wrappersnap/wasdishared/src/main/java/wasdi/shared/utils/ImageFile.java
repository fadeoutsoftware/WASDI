package wasdi.shared.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import wasdi.shared.utils.log.WasdiLog;

public class ImageFile extends File {

	/**
	 * 
	 */
	private static final long serialVersionUID = -643189587567960950L;

	public ImageFile(String paramString) {
		super(paramString);

	}
	
	public boolean saveImage (InputStream oInputStream){
		return save(oInputStream);

	}
	
	public boolean save(InputStream oInputStream){
		
		int iRead = 0;
		byte[] ayBytes = new byte[1024];

		// Try with resources. Resource declaration auto closes object(even if IOExeption occours)
		try (OutputStream oOutStream = new FileOutputStream(this)){
			while ((iRead = oInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}
			oOutStream.flush();
			oOutStream.close();
		} catch (FileNotFoundException e) {
			WasdiLog.errorLog("ImageFile.save: error", e);
			return false;
		} catch (IOException e) {
			WasdiLog.errorLog("ImageFile.save: error", e);
			return false;
		}
		return true;
	}
	
	public String getNameWithouteExtension() {
		String sName = this.getName();
		return FilenameUtils.removeExtension(sName);				
	}
	
	public String getExtension(){
		String sName = this.getName();
		return FilenameUtils.getExtension(sName);	

	}

	public boolean resizeImage(int iHeight, int iWidth ){
		
		try{
			String sExt = FilenameUtils.getExtension(this.getAbsolutePath());
	        BufferedImage oImage = ImageIO.read(this);
	        
	        if (oImage.getWidth() > iWidth || oImage.getHeight() > iHeight) {
		        BufferedImage oResized = resize(oImage , iHeight, iWidth);
		        ImageIO.write(oResized, sExt.toLowerCase(), this);	        	
	        }
		} catch (FileNotFoundException e) {
			WasdiLog.errorLog("ImageFile.resizeImage: error", e);
			return false;
		} catch (IOException e) {
			WasdiLog.errorLog("ImageFile.resizeImage: error", e);
			return false;
		}
		return true;
	}
	
	public ByteArrayInputStream getByteArrayImage(){
		byte[] abLogo = null;
		String sExt = FilenameUtils.getExtension(this.getAbsolutePath());
		try {
			BufferedImage oBufferedLogo = ImageIO.read(this);
			ByteArrayOutputStream oBaos = new ByteArrayOutputStream();
			ImageIO.write(oBufferedLogo, sExt, oBaos);
			abLogo = oBaos.toByteArray();
			
		} catch (IOException e) {
			WasdiLog.errorLog("ImageFile.getByteArrayImage: error", e);
			return null;
		}
		return new ByteArrayInputStream(abLogo);
	} 
	
    private static BufferedImage resize(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        //BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }


}
