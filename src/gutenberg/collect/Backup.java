package gutenberg.collect;

import java.nio.file.Files;
import java.nio.file.Path;

public class Backup extends Task {
    
    public Backup(String dir) {
        super(dir);
        this.filter = AUTO_PROCESS;
    }
    
    @Override
    protected void execute(Path file) throws Exception {
        Path backupPath = bankroot.resolve(BACKUP).resolve(file.getFileName());
        if (!Files.exists(backupPath)) {
            //ScanbotSS runs as gutenberg and does not have permission to
            //directly overwrite file a created by the web-server. Copying 
            //back the file from backup fixes that problem.
            Files.move(file, backupPath);
            Files.copy(backupPath, file);
        } else {
            Files.delete(file);
        }        
    }    

}
