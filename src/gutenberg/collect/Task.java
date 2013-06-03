package gutenberg.collect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Task {
    
    public static Task[] getTasks(Path bankroot) {
        Task[] tasks = new Task[4];
        tasks[0] = new Backup(bankroot, "");
        tasks[1] = new Explode(bankroot, UNEXPLODED);
        tasks[2] = new Detect(bankroot, UNDETECTED);
        tasks[3] = new Update(bankroot, "");
        return tasks;
    }
    
    protected Task(Path bankroot, String filter) {
        this.filter = filter;
        this.bankroot = bankroot;
    }
    
    /**
     * Any initialization code before loop goes here
     */
    protected void init() throws Exception { }
    
    public void run() throws Exception {
        System.out.println("Started Task " + this.getClass().getName() + " ...");
        init();
        DirectoryStream<Path> stream = 
            Files.newDirectoryStream(workingDir, "*" + filter);
        for (Path file : stream) {
            execute(file);
        }
        cleanup();
        System.out.println("Completed Task " + this.getClass().getName() + " ...");
    }
    
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
    
    protected abstract void execute(Path file) throws Exception;
    
    
    protected boolean backup;
    protected Path bankroot, workingDir;
    protected String filter;
    
    protected static final String SCANTRAY = "scantray",
        LOCKER = "locker", STAGING = "staging";
    protected static final String UNEXPLODED = ".ue", UNDETECTED = ".ud",
        BACKED_UP = ".bk", CROPPED = ".33", WORKING = ".wk";
}
