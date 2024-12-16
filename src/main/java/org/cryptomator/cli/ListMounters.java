package org.cryptomator.cli;

import org.cryptomator.integrations.common.IntegrationsLoader;
import org.cryptomator.integrations.mount.MountService;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "list-mounters",
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        optionListHeading = "%nOptions:%n",
        header = "Lists available mounters",
        description = "Prints a list of available mounters to STDIN. A mounter is is the object to mount/integrate the unlocked vault into the local filesystem. In the GUI app, mounter is named \"volume type\".",
        mixinStandardHelpOptions = true)
public class ListMounters implements Callable<Integer> {

    @CommandLine.Option(names = {"--withDisplayName"}, description = "Prints also the display name of each mounter, as used in the GUI app.")
    boolean withDisplayName = false;

    @Override
    public Integer call() throws Exception {
        IntegrationsLoader.loadAll(MountService.class)
                .forEach(s -> System.out.println(s.getClass().getName() + (withDisplayName? " | " + s.displayName():"")));
        return 0;
    }
}
