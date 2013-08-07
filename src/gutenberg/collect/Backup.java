package gutenberg.collect;

import java.nio.file.Files;
import java.nio.file.Path;

public class Backup extends Task {
    
    public Backup() {
        this.filter = AUTO_PROCESS;
    }
    
    @Override
    protected void execute(Path file) throws Exception {
        Path backupPath = bankroot.resolve(BACKUP).resolve(file.getFileName());
        if (!Files.exists(backupPath))
            Files.copy(file, backupPath);                
    }

}
