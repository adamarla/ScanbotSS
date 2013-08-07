package gutenberg.collect;

import java.nio.file.Files;
import java.nio.file.Path;

public class Explode extends Task {
    
    protected Explode() {
        this.filter = AUTO_EXPLODE;
    }

    @Override
    protected void execute(Path file) throws Exception {
        explodeGS(file);
        Files.delete(file);
    }    
    
    private void explodeGS(Path file) throws Exception {        
        String gs = String.format(CMD_EXPLODE,
            file.getFileName().toString().split("\\.")[0], 
            AUTO_DETECT, file.getFileName());
        exec(file.getParent(), gs);
    }

    private final String CMD_EXPLODE = 
        "gs -dNOPAUSE -dBATCH -sDEVICE=jpeg -r300 -sOutputFile=%s-%%d%s %s";

}
