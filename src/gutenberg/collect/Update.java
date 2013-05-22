package gutenberg.collect;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class Update extends Task {

    public Update(Path bankroot, String filter) {
        super(bankroot, filter);
        this.workingDir = bankroot.resolve(STAGING);
        this.locker = bankroot.resolve(LOCKER);
    }
    
    @Override
    protected void init() throws Exception {
        this.detectedPath = locker.resolve(todaysFolder());
        if (!Files.exists(detectedPath)) 
            Files.createDirectory(detectedPath);
        this.undetectedPath = locker.resolve("unresolved");
    }

    @Override
    protected void execute(Path file) throws Exception {
        
        String base36ScanId = file.getFileName().toString();
        boolean detected = !base36ScanId.endsWith(UNDETECTED);

        Path target = null;
        if (detected) {
            target = detectedPath.resolve(base36ScanId);
            if (Files.exists(target))
                Files.delete(file);
            else if (updateScanId(locker.relativize(target).toString()))
                Files.move(file, target);
        } else {
            target = undetectedPath.resolve(base36ScanId);
            if (Files.exists(target))
                Files.delete(file);
            else
                Files.move(file, target);                
        }        
    }
    
    private boolean updateScanId(String scanId) throws Exception {
        String hostport = System.getProperty("user.name").equals("gutenberg")?
            "www.gradians.com": "localhost:3000";
        URL updateScan = new URL(
            String.format("http://%s/update_scan_id?id=%s",hostport, scanId));
        conn = (HttpURLConnection)updateScan.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.connect();
        byte[] goodReturn = "{\"status\":\"ok\"}".getBytes();
        byte[] actualReturn = new byte[goodReturn.length];
        conn.getInputStream().read(actualReturn);
        return 200 == conn.getResponseCode() && 
            Arrays.equals(goodReturn, actualReturn);
    }    

    private String todaysFolder() {
        Calendar rightNow = Calendar.getInstance();
        return String.format("%s.%s.%s", rightNow.get(Calendar.DAY_OF_MONTH),
            rightNow.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH),
            rightNow.get(Calendar.YEAR));
    }
    
    private HttpURLConnection conn;
    private Path locker, undetectedPath, detectedPath;

}
