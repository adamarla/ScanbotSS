package gutenberg.collect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class Task {
    
    public static Task[] getTasks(boolean simulate) {
        ArrayList<Task> tasks = new ArrayList<Task>();
        //backup : copy *.ad, *.ae -> backup/
        //explode: ghostscript to explode *.ue -> *.ud 
        //detect : zxing to run detection on *.ud -> [*.re or *.ur]
        //update : use http to update rails app and mv *.ud -> locker
        tasks.add(new Backup());
        tasks.add(new Explode());
        tasks.add(new Detect());
        if (!simulate) tasks.add(new Update());
        
        return tasks.toArray(new Task[0]);
    }
    
    protected Task() {
        this.bankroot = FileSystems.getDefault().getPath("/opt/gutenberg/bank");
        this.workingDir = bankroot.resolve(SCANTRAY);
    }
    
    /**
     * Any initialization code before loop goes here
     */
    protected void init() throws Exception { }
    
    public void run() throws Exception {
        init();
        DirectoryStream<Path> stream = 
            Files.newDirectoryStream(workingDir, "*" + filter + "*");
        for (Path file : stream) {
            execute(file);
        }
        cleanup();
    }
    
    protected abstract void execute(Path file) throws Exception;
    
    /**
     * Any clean up code after loop goes here
     */
    protected void cleanup() throws Exception { }
    
    protected int exec(Path workingDirPath, String command) throws Exception {        
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
    
    
    protected boolean backup;
    protected Path bankroot, workingDir;
    protected String filter;
    protected boolean simulate;
    
    protected static final String 
        SCANTRAY = "scantray",
        LOCKER = "locker", 
        BACKUP = "backup";
    protected static final String 
        AUTO_PROCESS = ".a",
        AUTO_EXPLODE = ".ae", 
        AUTO_DETECT = ".ad",
        MANUAL_DETECT = ".md",
        DETECTED = ".de",
        CROPPED = ".cr", 
        WORKING = ".wk";
}
