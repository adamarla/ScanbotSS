package gutenberg.collect;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    public Detect(String dir) {
        super(dir);
        this.filter = AUTO_DETECT;
    }

    @Override
    protected void execute(Path file) throws Exception {
        
        BufferedImage img = ImageIO.read(file.toFile());
        if (img.getWidth() > img.getHeight()) {
            rotate(file, file, PI_BY_2);
        }        
        
        Path targetFile = null;
        String targetName = null;
        String name = getName(file);
        String[] tokens = name.split(SEP);
        if (tokens[1].equals(BLANK_CODE)) {//PDF uploaded via website
            Result result = decode(file);
            if (result == null) {
                targetName = tokens[2] + MANUAL_DETECT;
                targetFile = file.resolveSibling(targetName);
            } else if (result.getText().compareToIgnoreCase(BLANK_CODE) != 0 &&
                       result.getText().compareToIgnoreCase("attachment") != 0) {
                targetName = tokens[0] + SEP + 
                        result.getText() + SEP + 
                        tokens[2] + DETECTED;
                targetFile = file.resolveSibling(targetName);
                
                if (Files.exists(targetFile)) {
                    targetFile = null;
                }
            }
        } else {                           //Scan uploaded via mobile app
            targetName = name + DETECTED;
        }
        
        if (targetFile != null) resize(file, targetFile, WIDTH, HEIGHT);        
        Files.delete(file);
    }

    private Result decode(Path scan) throws Exception {

        BufferedImage img = null, resampledImg = null;
        Result result = null;
        Path workingImgPath = scan.resolveSibling(scan.getFileName() + WORKING);
        
        img = ImageIO.read(scan.toFile());        
        if (img.getWidth() > img.getHeight()) {
            rotate(scan, scan, PI_BY_2);
            img = ImageIO.read(scan.toFile());
        }
        
        Files.copy(scan, workingImgPath, StandardCopyOption.REPLACE_EXISTING);
        boolean flipped = false;
        while (true) {
            //{300dpi (orig), 150dpi, 200dpi, 125dpi, 100dpi, 75dpi}
            int[] widths = {img.getWidth(), 1275, 1700, 1000, 827, 638};
            for (int width : widths) {
                
                if (width != img.getWidth()) {
                    int height = (int)(img.getHeight()*1.0/img.getWidth()*width);
                    resize(scan, workingImgPath, width, height);
                }                
                resampledImg = ImageIO.read(Files.newInputStream(workingImgPath));
                
                int third = (int)(resampledImg.getWidth()/3.0);
                int fourth = (int)(resampledImg.getWidth()/4.0);                
                int[][] subImgConfs = new int[][] {
                    {0, (int)resampledImg.getHeight()-fourth, fourth, fourth},
                    {2*third, 0, third, (int)resampledImg.getHeight()}
                };
                
                BufferedImage subImg = null;
                for (int[] subImgConf : subImgConfs) {
                    subImg = resampledImg.getSubimage(subImgConf[0], subImgConf[1], 
                            subImgConf[2], subImgConf[3]);
                    result = decode(subImg);
                    if (result != null) {
                        break;
                    }
                }
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
                    Files.copy(scan, workingImgPath, StandardCopyOption.REPLACE_EXISTING);
                    flipped = true;
                }
            } else {
                break;
            }
        }
        
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
        PI = "180", PI_BY_2 = "90", BLANK_CODE = "0";
    private final int WIDTH = 600, HEIGHT = 800;
}

class ZXingConfig {

    public Binarizer binarizer;
    public Reader    reader;

}
