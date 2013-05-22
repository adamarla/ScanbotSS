package gutenberg.collect;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Driver {

    /**
     * @param args - any arg will make it run in Debug mode
     * @throws IOException 
     */
    public static void main(String[] args) throws Exception {
        
        Path bank = FileSystems.getDefault().getPath("/opt/gutenberg/bank");
        Path semaphore = bank.resolve("semaphore");
        try {
            if (!Files.exists(semaphore))
                Files.createFile(semaphore);
            else
                return;
            Task[] tasks = Task.getTasks(bank);
            if (args.length == 0) //skip backup step
                tasks = Arrays.copyOfRange(tasks, 1, tasks.length);            
            for (Task task: tasks) {
                task.run();    
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Files.deleteIfExists(semaphore);
        }
    }
}
