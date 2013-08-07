package gutenberg.collect;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Driver {

    /**
     * @param args - any arg will make it run in Debug mode
     * @throws IOException 
     */
    public static void main(String[] args) throws Exception {
        
        Path semaphore = FileSystems.getDefault().
                getPath("/opt/gutenberg/bank", "semaphore");
        try {
            
            if (!Files.exists(semaphore))
                Files.createFile(semaphore);
            else
                return;
            
            Task[] tasks = Task.getTasks(args[0].equals("simulate"));
            for (Task task: tasks) {
                task.run(); System.out.println(task.getClass().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Files.deleteIfExists(semaphore);
        }
        
    }
}
