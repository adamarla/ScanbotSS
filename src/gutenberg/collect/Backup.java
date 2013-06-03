package gutenberg.collect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Backup extends Task {
    
    protected Backup(Path bankroot, String filter) {
        super(bankroot, filter);
        this.workingDir = bankroot.resolve(SCANTRAY);
    }

    @Override
    protected void execute(Path file) throws Exception {
        Path backupPath = null;
        String name = file.getFileName().toString();
        if (name.endsWith(UNDETECTED) || name.endsWith(UNEXPLODED)) {
            backupPath = file.resolveSibling(
                file.getFileName().toString().replaceAll("\\.u.", BACKED_UP));
            Files.copy(file, backupPath, StandardCopyOption.REPLACE_EXISTING);                
        } 
    }

}
