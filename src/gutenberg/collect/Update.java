package gutenberg.collect;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class Update extends Task {

    public Update() {
        this.filter = DETECTED;
    }
    
    @Override
    protected void init() throws Exception {
        super.init();
        this.todaysFolder = getTodaysFolder();
    }

    @Override
    protected void execute(Path file) throws Exception {
        
        if (!Files.exists(todaysFolder)) 
            Files.createDirectory(todaysFolder);
        
        String name = getName(file);
        String[] tokens = name.split(SEP);
        String scanType = tokens[0];
        String scanId = tokens[1];
        String scanPath = todaysFolder.getFileName().resolve(tokens[2]).toString();
        Path target = todaysFolder.resolve(tokens[2]);

        if (Files.exists(target))
            Files.delete(file);
        else if (updateScanId(scanId, scanType, scanPath))
            Files.move(file, target);
        else
            Files.delete(file);
    }        
    
    @Override
    protected void cleanup() throws Exception {
        super.cleanup();
        if (conn != null) conn.disconnect();
    }

    private boolean updateScanId(String scanId, String scanType, String scanPath) throws Exception {        
        String charset = Charset.defaultCharset().name();
        
        String hostport = System.getProperty("user.name").equals("gutenberg")?
            "www.gradians.com": "localhost:3000";        
        URL updateScan = new URL(String.format("http://%s/update_scan_id", hostport));
        String params = String.format("id=%s&type=%s&path=%s",
            URLEncoder.encode(scanId, charset), scanType, scanPath);
        conn = (HttpURLConnection)updateScan.openConnection();
        conn.setDoOutput(true); // Triggers HTTP POST
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.getOutputStream().write(params.getBytes(charset));
        conn.getOutputStream().close();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            byte[] goodReturn = "{\"status\":\"ok\"}".getBytes();
            byte[] actualReturn = new byte[goodReturn.length];            
            while (conn.getInputStream().read(actualReturn) != -1) { }            
            conn.getInputStream().close();            
            return Arrays.equals(goodReturn, actualReturn);
        } else {
            return false;    
        }        
    }    

    private Path getTodaysFolder() {
        Calendar rightNow = Calendar.getInstance();
        return bankroot.resolve(LOCKER).resolve(String.format("%s.%s.%s", 
                rightNow.get(Calendar.DAY_OF_MONTH),
                rightNow.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH),
                rightNow.get(Calendar.YEAR)));
    }
    
    private HttpURLConnection conn;
    private Path todaysFolder;

}
