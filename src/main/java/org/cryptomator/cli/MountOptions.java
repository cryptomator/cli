package org.cryptomator.cli;

import org.cryptomator.integrations.common.IntegrationsLoader;
import org.cryptomator.integrations.mount.*;
import picocli.CommandLine;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MountOptions {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = {"--mounter"}, paramLabel = "fully.qualified.ClassName", description = "Name of the mounter to use", required = true)
    void setMountService(String value) {
        var services = IntegrationsLoader.loadAll(MountService.class).toList();
        var service = services.stream().filter(s -> s.getClass().getName().equals(value)).findFirst();
        if (service.isEmpty()) {
            var availableServices = services.stream().map(s -> s.getClass().getName()).collect(Collectors.joining(","));
            var errorMessage = String.format("Invalid value '%s' for option '--mounter': Available mounters are [%s].", value, availableServices);
            throw new CommandLine.ParameterException(spec.commandLine(), errorMessage);
        }
        this.mountService = service.get();
    }

    private MountService mountService;

    @CommandLine.Option(names = {"--mountPoint"}, paramLabel = "/path/to/mount/point", description = "Path to the mount point. Requirements for mount point depend on the chosen mount service")
    Optional<Path> mountPoint;

    @CommandLine.Option(names = {"--volumeName"}, description = "Name of the virtual volume.")
    Optional<String> volumeName;

    @CommandLine.Option(names = {"--volumeId"}, description = "Id of the virtual volume.")
    String volumeId = UUID.randomUUID().toString();

    @CommandLine.Option(names = {"--mountOption", "-mop"}, description = "Additional mount option. For a list of mountoptions, see the WinFsp, macFUSE, FUSE-T and libfuse documentation.")
    List<String> mountOptions = new ArrayList<>();

    @CommandLine.Option(names = {"--loopbackHostName"}, description = "Name of the loopback address.")
    Optional<String> loopbackHostName;
    @CommandLine.Option(names = {"--loopbackPort"}, description = "Port used at the loopback address.")
    Optional<Integer> loopbackPort;

    MountBuilder prepareMountBuilder(FileSystem fs) {
        var builder = mountService.forFileSystem(fs.getPath("/"));
        for (var capability : mountService.capabilities()) {
            switch (capability) {
                case FILE_SYSTEM_NAME -> builder.setFileSystemName("cryptoFs");
                case LOOPBACK_PORT -> loopbackPort.ifPresent(builder::setLoopbackPort);
                case LOOPBACK_HOST_NAME -> loopbackHostName.ifPresent(builder::setLoopbackHostName);
                //TODO: case READ_ONLY -> builder.setReadOnly(vaultSettings.usesReadOnlyMode.get());
                case MOUNT_FLAGS -> {
                    if (mountOptions.isEmpty()) {
                        builder.setMountFlags(mountService.getDefaultMountFlags());
                    } else {
                        builder.setMountFlags(String.join(" ", mountOptions));
                    }
                }
                case VOLUME_ID -> builder.setVolumeId(volumeId);
                case VOLUME_NAME -> volumeName.ifPresent(builder::setVolumeName);
            }
        }
        return builder;
    }

    Mount mount(FileSystem fs) throws MountFailedException {
        if (!mountService.hasCapability(MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH) && mountPoint.isEmpty()) {
            throw new CommandLine.ParameterException(spec.commandLine(), "The selected mounter %s requires a mount point. Use --mountPoint /path/to/mount/point to specify it.".formatted(mountService.displayName()));
        }
        var builder = prepareMountBuilder(fs);
        mountPoint.ifPresent(builder::setMountpoint);
        return builder.mount();
    }
}
