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
        //fetch  : fetch new email and copy out attachment
        //backup : copy *.ad, *.ae -> backup/
        //explode: ghostscript to explode *.ae -> *.ad 
        //detect : zxing to run detection on *.ad -> [*.de or *.md]
        //update : use http to update rails app and mv *.de -> locker
        tasks.add(new Backup());
        tasks.add(new Explode());
        tasks.add(new Detect());
        if (!simulate) tasks.add(new Update());
        
        return tasks.toArray(new Task[0]);
    }
    
    protected Task() {
        this.bankroot = FileSystems.getDefault().getPath("/opt/gutenberg/bank");
    }
    
    /**
     * Any initialization code before loop goes here
     */
    protected void init() throws Exception {
        System.out.println(this.getClass().getSimpleName() + " =========");
    }
    
    public void run() throws Exception {
        init();
        DirectoryStream<Path> stream = 
            Files.newDirectoryStream(bankroot.resolve(SCANTRAY), "*" + filter + "*");
        for (Path file : stream) {
            System.out.println(file.getFileName());
            execute(file);
        }
        cleanup();
    }
    
    protected abstract void execute(Path file) throws Exception;
    
    /**
     * Any clean up code after loop goes here
     */
    protected void cleanup() throws Exception {
        System.out.println("========================================");
    }
    
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
    
    protected String getName(Path file) {
        return file.getFileName().toString().split("\\.")[0];
    }
    
    protected boolean backup;
    protected Path bankroot;
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
        WORKING = ".wk",
        SEP = "_";
}
