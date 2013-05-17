package gutenberg.collect;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Driver {

    /**
     * @param args - any arg will make it run in Debug mode
     */
    public static void main(String[] args) {
        
        Path bank = FileSystems.getDefault().getPath("/opt/gutenberg/bank");
        Path semaphore = bank.resolve("semaphore");        
        ScanbotSS scanbot = new ScanbotSS(bank);
        try {
            Files.createFile(semaphore);
            scanbot.run(args.length > 0);
            Files.delete(semaphore);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
}
