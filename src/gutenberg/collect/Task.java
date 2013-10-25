package gutenberg.collect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class Task {
    
    public static Task[] getTasks(boolean simulation, String dir, String hostPort) {
        ArrayList<Task> tasks = new ArrayList<Task>();
        //fetch  : fetch new email and copy out attachment - TODO
        //backup : copy incoming -> backup (always)
        //explode: ghostscript to explode PDFs (if needed)
        //detect : zxing to run detection on images (if needed)
        //update : http POST to update rails app and move scans to locker
        tasks.add(new Backup(dir));
        tasks.add(new Explode(dir));
        tasks.add(new Detect(dir));
        if (!simulation) tasks.add(new Update(dir, hostPort));
        return tasks.toArray(new Task[0]);
    }
    
    protected Task(String inTray) {
        this.bankroot = FileSystems.getDefault().getPath("/opt/gutenberg/bank");
        this.inTray = inTray;
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
            Files.newDirectoryStream(bankroot.resolve(inTray), "*" + filter + "*");
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
    
    private String inTray;
    
    protected Path bankroot;
    protected String filter;
    protected boolean simulate;
    
    protected static final String 
        LOCKER = "locker", 
        BACKUP = "backup",
        AUTO_PROCESS = ".a",
        AUTO_EXPLODE = ".ae", 
        AUTO_DETECT = ".ad",
        MANUAL_DETECT = ".md",
        DETECTED = ".d",
        DETECTED_SELF = ".ds",
        DETECTED_COPY = ".dc",
        CROPPED = ".cr", 
        WORKING = ".wk",
        SEP = "_";
}
