package org.cryptomator.cli;

import org.cryptomator.integrations.common.IntegrationsLoader;
import org.cryptomator.integrations.mount.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.cryptomator.integrations.mount.MountCapability.*;

public class MountSetup {

    private static final Logger LOG = LoggerFactory.getLogger(MountSetup.class);

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

    @CommandLine.Option(names = {"--mountOption", "-mop"}, description = "Additional mount option. For a list of mount options, see the WinFsp, macFUSE, FUSE-T and libfuse documentation.")
    List<String> mountOptions = new ArrayList<>();

    @CommandLine.Option(names = {"--loopbackHostName"}, description = "Name of the loopback address.")
    Optional<String> loopbackHostName;

    @CommandLine.Option(names = {"--loopbackPort"}, description = "Port used at the loopback address.")
    Optional<Integer> loopbackPort;


    MountBuilder prepareMountBuilder(FileSystem fs) {
        var specifiedMOPs = listSpecifiedMountOptions();
        var builder = mountService.forFileSystem(fs.getPath("/"));
        for (var capability : mountService.capabilities()) {
            switch (capability) {
                case FILE_SYSTEM_NAME -> builder.setFileSystemName("cryptoFs");
                case LOOPBACK_PORT -> {
                    loopbackPort.ifPresent(builder::setLoopbackPort);
                    specifiedMOPs.put(LOOPBACK_PORT, false);
                }
                case LOOPBACK_HOST_NAME -> {
                    loopbackHostName.ifPresent(builder::setLoopbackHostName);
                    specifiedMOPs.put(LOOPBACK_HOST_NAME, false);
                }
                //TODO: case READ_ONLY -> builder.setReadOnly(vaultSettings.usesReadOnlyMode.get());
                case MOUNT_FLAGS -> {
                    specifiedMOPs.put(MOUNT_FLAGS, false);
                    if (mountOptions.isEmpty()) {
                        var defaultFlags = mountService.getDefaultMountFlags();
                        LOG.debug("Using default mount options {}", defaultFlags);
                        builder.setMountFlags(defaultFlags);
                    } else {
                        builder.setMountFlags(String.join(" ", mountOptions));
                    }
                }
                case VOLUME_ID -> {
                    builder.setVolumeId(volumeId);
                }
                case VOLUME_NAME -> {
                    volumeName.ifPresent(builder::setVolumeName);
                    specifiedMOPs.put(VOLUME_NAME, false);
                }
                default -> {
                    //NO-OP
                }
            }
        }

        var ignoredMOPs = specifiedMOPs.entrySet().stream().filter(Map.Entry::getValue).map(e -> e.getKey().name()).collect(Collectors.joining(","));
        if (!ignoredMOPs.isEmpty()) {
            LOG.info("Ignoring unsupported options: {}", ignoredMOPs);
        }
        return builder;
    }

    private Map<MountCapability, Boolean> listSpecifiedMountOptions() {
        var map = new HashMap<MountCapability, Boolean>();
        loopbackPort.ifPresent(_ -> map.put(LOOPBACK_PORT, true));
        loopbackHostName.ifPresent(_ -> map.put(LOOPBACK_HOST_NAME, true));
        volumeName.ifPresent(_ -> map.put(VOLUME_NAME, true));
        if (!mountOptions.isEmpty()) {
            map.put(MOUNT_FLAGS, true);
        }
        return map;
    }

    Mount mount(FileSystem fs) throws MountFailedException {
        if (!mountService.hasCapability(MOUNT_TO_SYSTEM_CHOSEN_PATH) && mountPoint.isEmpty()) {
            throw new RuntimeException("Unsupported configuration: Mounter %s requires a mount point. Use --mountPoint /path/to/mount/point to specify it.".formatted(mountService.getClass().getName()));
        }

        var builder = prepareMountBuilder(fs);

        try {
            mountPoint.ifPresent(builder::setMountpoint);
        } catch (UnsupportedOperationException e) {
            var errorMessage = String.format("Unsupported configuration: Mounter '%s' does not support flag --mountpoint", mountService.getClass().getName());
            throw new RuntimeException(errorMessage);
        }
        LOG.debug("Mounting vault using {} to {}.", mountService.displayName(), mountPoint.isPresent() ? mountPoint.get() : "system chosen location");
        return builder.mount();
    }
}
