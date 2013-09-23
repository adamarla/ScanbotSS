package gutenberg.collect;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class Detect extends Task {

    public Detect() {
        this.filter = AUTO_DETECT;
    }

    @Override
    protected void execute(Path file) throws Exception {
        
        BufferedImage img = ImageIO.read(file.toFile());
        if (img.getWidth() > img.getHeight()) {
            rotate(file, file, PI_BY_2);
        }        
        
        String targetName = null;
        String name = getName(file);
        String[] tokens = name.split(SEP);
        if (tokens[1].equals(BLANK_CODE)) {
            Result result = decode(file);
            targetName = result != null ?
                    tokens[0] + SEP + result.getText() + SEP + tokens[2] + DETECTED :
                    tokens[2] + MANUAL_DETECT;
        } else {
            targetName = name + DETECTED;
        }
        
        Path targetFile = bankroot.resolve(SCANTRAY).resolve(targetName);        
        if (!Files.exists(targetFile)) resize(file, targetFile, WIDTH, HEIGHT);        
        Files.delete(file);
    }

    private Result decode(Path scan) throws Exception {

        BufferedImage img = null, subImg = null;        
        Result result = null;
        Path subImgPath, workingImgPath;
        
        img = ImageIO.read(scan.toFile());
        if (img.getWidth() > img.getHeight()) {
            rotate(scan, scan, PI_BY_2);
            img = ImageIO.read(scan.toFile());
        }
        
        boolean flipped = false;
        while (true) {
            
            int third = (int)(img.getWidth()/3.0);            
            subImg = img.getSubimage(2*third, 0, third, (int)img.getHeight());
            
            subImgPath = scan.resolveSibling(scan.getFileName() + CROPPED);
            workingImgPath = scan.resolveSibling(scan.getFileName() + WORKING);            
            ImageIO.write(subImg, IMG_FORMAT, Files.newOutputStream(subImgPath));
            
            //{original, 150dpi, 200dpi, 125dpi, 100dpi}
            int[] widths = {third, 425, 566, 333, 283};
            for (int width : widths) {
                int height;
                if (width != third) {
                    height = (int)(subImg.getHeight()*1.0/subImg.getWidth()*width);
                    resize(subImgPath, workingImgPath, width, height);
                } else {
                    ImageIO.write(subImg, IMG_FORMAT, Files.newOutputStream(workingImgPath));
                }
                subImg = ImageIO.read(Files.newInputStream(workingImgPath));
                result = decode(subImg);
                
                if (result != null) {
                    ResultPoint[] points = result.getResultPoints();                    
                    if (points[0].getY() < points[1].getY()) rotate(scan, scan, PI);
                    break;
                }            
            }
            
            if (result == null) {
                if (flipped) {
                    break;//give up
                } else {
                    rotate(scan, scan, PI);
                    flipped = true;
                }
            } else {
                break;
            }
        }
        
        Files.delete(subImgPath);
        Files.delete(workingImgPath);
        
        return result;
    }
    
    private Result decode(BufferedImage subImg) throws Exception {
        
        ZXingConfig[] configs = new ZXingConfig[2];
        LuminanceSource source = null;
        BinaryBitmap bitmap = null;
        Binarizer binarizer = null;
        bitmap = null;
        Hashtable<DecodeHintType, Boolean> hints = null;
        Reader reader = null;
        Result result = null;
            
        hints = new Hashtable<DecodeHintType, Boolean>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                    
        source = new BufferedImageLuminanceSource(subImg);
        
        configs[0] = new ZXingConfig();
        configs[0].binarizer = new GlobalHistogramBinarizer(source);
        configs[0].reader = new QRCodeReader();

        configs[1] = new ZXingConfig();
        configs[1].binarizer = new HybridBinarizer(source);
        configs[1].reader = new QRCodeReader();
        
        for (int i = 0; i < configs.length; i++) {
            
            binarizer = configs[i].binarizer;
            bitmap = new BinaryBitmap(binarizer);
            reader = configs[i].reader;
            try {
                result = reader.decode(bitmap, hints);
                break;
            } catch (Exception e) { }
        }
        return result;
    }    
    
    private boolean resize(Path src, Path target, int width, int height) 
        throws Exception { 
        String resize = String.format(CMD_RESIZE, 
            bankroot.relativize(src), width, height, 
            bankroot.relativize(target));
        return exec(bankroot, resize) == 0;
    }
    
    private boolean rotate(Path src, Path target, String degrees) throws Exception {
        String rotate = String.format(CMD_ROTATE,
            bankroot.relativize(src), degrees,
            bankroot.relativize(target));
        return exec(bankroot, rotate) == 0;
    }    

    private final String 
        CMD_RESIZE = "convert %s -type TrueColor -resize %sx%s %s",
        CMD_ROTATE = "convert %s -type TrueColor -rotate %s %s",
        IMG_FORMAT = "JPG", PI = "180", PI_BY_2 = "90", BLANK_CODE = "0";
    private final int WIDTH = 600, HEIGHT = 800;
}

class ZXingConfig {

    public Binarizer binarizer;
    public Reader    reader;

}
