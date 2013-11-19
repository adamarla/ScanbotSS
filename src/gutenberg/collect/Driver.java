package gutenberg.collect;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;

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
            
            Options options = new Options();
            
            OptionBuilder.withDescription("simulate flag");
            options.addOption(OptionBuilder.create('s'));
            
            OptionBuilder.isRequired();
            OptionBuilder.hasArg();            
            OptionBuilder.withArgName("url");
            OptionBuilder.withDescription("url for update scanId");
            options.addOption(OptionBuilder.create('u'));
            
            OptionBuilder.isRequired();
            OptionBuilder.hasArg();
            OptionBuilder.withArgName("dir");
            OptionBuilder.withDescription("dir to look in");
            options.addOption(OptionBuilder.create('d'));
            
            CommandLine cl = (new BasicParser()).parse(options, args);
            
            boolean simulation = cl.hasOption('s');
            String directory = cl.getOptionValue('d');
            String hostport = cl.getOptionValue('u');
            if (directory == null || hostport == null) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("ScanbotSS", options);
            } else {
                Task[] tasks = Task.getTasks(simulation, directory, hostport);
                for (Task task: tasks) task.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            Files.deleteIfExists(semaphore);
        }        
    }
}
