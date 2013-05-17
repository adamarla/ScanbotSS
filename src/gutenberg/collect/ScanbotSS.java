package gutenberg.collect;


import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
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
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

/**
 * Head less Server Side version of the Scanbot
 * 
 * @author adamarla
 */
public class ScanbotSS {
    
    protected static final String 
        UNEXPLODED = ".ue", UNDETECTED = ".ud", BACKED_UP = ".bk",
        CROPPED = ".33", WORKING = ".wk";

    public ScanbotSS(Path bankRoot) {
        this.staging = bankRoot.resolve("staging");
        this.scantray = bankRoot.resolve("scantray");
    }

    public void run(boolean backup) throws Exception {
        
        clearTempFiles();
        
        DirectoryStream<Path> stream = 
            Files.newDirectoryStream(scantray, "*" + UNEXPLODED);
        Path backupPath = null;
        for (Path file : stream) {            
            backupPath = file.resolveSibling(
                file.getFileName().toString().replace(UNEXPLODED, BACKED_UP));
            explodeGS(file);
            if (backup)
                Files.move(file, backupPath);
            else
                Files.delete(file);
        }

        String orientation;
        Result result; 
        Path target;        
        stream = Files.newDirectoryStream(scantray, "*" + UNDETECTED);
        for (Path file : stream) {            
            result = decode(file);            
            if (result != null) {
                orientation = (String)result.getResultMetadata().
                    get(ResultMetadataType.ORIENTATION);
                target = staging.resolve(String.format(TARGET, 
                    result.getText().replace(" ",""), "1", orientation));
            } else {
                target = staging.resolve(String.format(TARGET, 
                    file.getFileName().toString().split("\\.")[0], "0", "0"));
            }
            if (!Files.exists(target))
                Files.move(file, target);
            else
                Files.delete(file);
        }
    }
    
    private void clearTempFiles() throws Exception {
        DirectoryStream<Path> stream = 
            Files.newDirectoryStream(scantray, 
                String.format("*{%s,%s}", CROPPED, WORKING));
        for (Path path : stream) Files.delete(path);
    }

    private Result decode(Path scan) throws Exception {

        BufferedImage img = null, subImg = null;        
        Result result = null;
        Path subImgPath, workingImgPath;
        
        boolean flipped = false;
        while (true) {
            
            img = ImageIO.read(scan.toFile());
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
                    result.putMetadata(ResultMetadataType.ORIENTATION, 
                        flipped ? "1" : "0");
                    break;
                }            
            }
            
            if (result == null) {
                if (flipped) {
                    break;//give up
                } else {
                    flip(scan, scan);
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
        
        QRConfig[] configs = new QRConfig[2];
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
        
        configs[0] = new QRConfig();
        configs[0].binarizer = new GlobalHistogramBinarizer(source);
        configs[0].reader = new QRCodeReader();

        configs[1] = new QRConfig();
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
        
    private void explodeGS(Path file) throws Exception {
        String gs = String.format(CMD_EXPLODE,
            file.getFileName().toString().split("\\.")[0], 
            UNDETECTED, file.getFileName());
        execute(file.getParent(), gs);
    }
    
    private void explode(Path file) throws Exception {
        PdfReader pdfReader = new PdfReader(file.toString());
        PdfReaderContentParser pdfParser =
            new PdfReaderContentParser(pdfReader);
        RenderListener listener = new PDFImageRenderListener(file);
        
        for (int pg = 1; pg <= pdfReader.getNumberOfPages(); pg++) {            
            pdfParser.processContent(pg, listener);
        }
        pdfReader.close();
    }
    
    private void resize(Path src, Path target, int width, int height) 
        throws Exception { 
        String resize = String.format(CMD_RESIZE, 
            src.getFileName(), width, height, target.getFileName());
        execute(src.getParent(), resize);
    }
    
    private void flip(Path src, Path target) throws Exception {
        String rotate = String.format(CMD_ROTATE,
            src.getFileName(), target.getFileName());
        execute(src.getParent(), rotate);
    }
    
    private int execute(Path workingDirPath, String command) throws Exception {        
        String[] tokens = command.split(" ");
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(tokens);

        pb.directory(workingDirPath.toFile());
        pb.redirectErrorStream(true);

        Process build = pb.start();
        BufferedReader messages = new 
                BufferedReader(new InputStreamReader(build.getInputStream()));

        String line = null;
        while ((line = messages.readLine()) != null) {
            System.out.println(line);
        }        
        return build.waitFor();
    }
    
    private Path staging, scantray;
    
    private final String TARGET = "%s_%s_%s";
    private final String IMG_FORMAT = "JPG";
    private final String 
        CMD_RESIZE = "convert %s -type TrueColor -resize %sx%s %s",
        CMD_ROTATE = "convert %s -type TrueColor -rotate 180 %s",
        CMD_EXPLODE = "gs -dNOPAUSE -dBATCH -sDEVICE=jpeg -r300 -sOutputFile=%s-%%d%s %s";
}

class PDFImageRenderListener implements RenderListener {
    
    protected Path path;
    protected int pageNum;
    protected String filename;

    public PDFImageRenderListener(Path path) {
        this.path = path;
        this.pageNum = 1;
        this.filename = path.getFileName().toString().split("\\.")[0];
    }

    public void beginTextBlock() { }

    public void endTextBlock() { }

    public void renderImage(ImageRenderInfo renderInfo) {
        try {
            PdfImageObject image = renderInfo.getImage();
            if (image == null) return;
            BufferedImage drawingImage = image.getBufferedImage();
            Path file = path.resolveSibling(String.format("%s%02d%s", 
                filename, pageNum, ScanbotSS.UNDETECTED));
            ImageIO.write(drawingImage, "JPEG", Files.newOutputStream(file));
        } catch(Exception e) {}
        pageNum++;
    }

    public void renderText(TextRenderInfo renderInfo) { }
}


class QRConfig {

    public Binarizer binarizer;
    public Reader    reader;

}
