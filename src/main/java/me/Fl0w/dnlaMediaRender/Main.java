package me.Fl0w.dnlaMediaRender;

import org.apache.commons.cli.*;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.avtransport.impl.AVTransportService;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.lastchange.LastChangeAwareServiceManager;
import org.fourthline.cling.support.lastchange.LastChangeParser;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main implements Runnable {
    private static VLCControl vlc = new VLCControl();
    private ScheduledExecutorService renderingControlLastChangeExecutor = Executors.newSingleThreadScheduledExecutor();

    static VLCControl getVLCControl() {
        return vlc;
    }

    static Logger log = Logger.getLogger("VLCControlApp");

    public static String deviceName;
    public static String pathWin = "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc";
    public static String pathLinux = "/usr/bin/vlc";


    public static void main(String[] args) throws Exception {

        Thread serverThread = new Thread(new Main());
        serverThread.setDaemon(false);
        serverThread.start();
        Options options = new Options();

        options.addOption("n", "name", true, "Set the device name");
        options.addOption("v","vlcpath",true,"Set the path of vlc executable");
        options.addOption("h", "help", false, "prints the help content");
        CommandLineParser parser = new DefaultParser();
        try{
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("name")) {
                deviceName = cmd.getOptionValue("name");
                System.out.println("Device name: " + cmd.getOptionValue("name"));
            }
            if(cmd.hasOption("vlcpath")){
                pathWin = cmd.getOptionValue("vlcpath");
                pathLinux = cmd.getOptionValue("vlcpath");
                System.out.println("Vlc path: " + pathLinux);
            }
            if(cmd.hasOption("help")){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "App" , options );
                System.exit(1);
            }
        }catch (ParseException e){
            System.out.println(e.getMessage());
            System.exit(1);
        }

    }

    public void run() {
        try {
            Runtime rt = Runtime.getRuntime();

            try {
                String OS = System.getProperty("os.name").toLowerCase();
                Process process;
                if (OS.contains("win")) {
                    System.out.println("Starting mediarender for windows....");
                    process = rt.exec(new String[]{pathWin, "--intf=rc", "--rc-host=localhost:4444", "--rc-quiet", "--no-video-title-show", "--fullscreen"});
                } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
                    System.out.println("Starting mediarender for linux....");
                    process = rt.exec(new String[]{pathLinux , "--intf=rc", "--rc-host=localhost:4444", "--no-video-title-show", "--fullscreen"});
                } else {
                    new Exception("This platform is not supported").printStackTrace();
                    JOptionPane.showMessageDialog(null, "This platform is not supported", "Error", JOptionPane.ERROR_MESSAGE);
                    rt.exit(1);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            final UpnpService upnpService = new UpnpServiceImpl();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                    renderingControlLastChangeExecutor.shutdown();
                    Runtime.getRuntime().halt(0);
                }
            });

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(
                    createDevice()
            );

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {

        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("Fl0w MediaRender"));
        UDADeviceType type = new UDADeviceType("MediaRenderer", 1);
        DeviceDetails details = new DeviceDetails(
                deviceName,
                new ManufacturerDetails("Fl0w", "http://51.75.27.87/"),
                new ModelDetails("Cling MediaRenderer", deviceName , "1","http://51.75.27.87/")
        );

        Icon icon = new Icon("image/png",64,64,8,"icon64.png",this.getClass().getClassLoader().getResourceAsStream("icon.png"));
        LocalService avTransportService =
                new AnnotationLocalServiceBinder().read(AVTransportService.class);

        LastChangeParser lastChangeParser = new AVTransportLastChangeParser();

        LocalService renderingControlService = new AnnotationLocalServiceBinder().read(RenderingControlService.class);

        LastChangeParser renderingControlLastChangeParser = new RenderingControlLastChangeParser();


        LocalService connectionManagerService = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);

        connectionManagerService.setManager(
                new DefaultServiceManager<>(connectionManagerService, ConnectionManagerService.class)
        );

        renderingControlService.setManager(
                new LastChangeAwareServiceManager<RenderingControlService>(renderingControlService, renderingControlLastChangeParser){
                    @Override
                    protected RenderingControlService createServiceInstance() throws Exception {
                        return new RenderingControlService();
                    }
                }
        );

        renderingControlLastChangeExecutor.scheduleWithFixedDelay(() ->{
            LastChangeAwareServiceManager manager = (LastChangeAwareServiceManager)renderingControlService.getManager();
            manager.fireLastChange();
        },0,500, TimeUnit.MILLISECONDS);

        avTransportService.setManager(
                new LastChangeAwareServiceManager<AVTransportService>(avTransportService, lastChangeParser) {
                    @Override
                    protected AVTransportService createServiceInstance() throws Exception {
                        return new AVTransportService<>(
                                RendererStateMachine.class,   // All states
                                RendererNoMediaPresent.class  // Initial state
                        );
                    }
                });
        return new LocalDevice(identity, type, details, icon, new LocalService[]{avTransportService,renderingControlService,connectionManagerService});

    /* Several services can be bound to the same device:
    return new LocalDevice(
            identity, type, details, icon,
            new LocalService[] {switchPowerService, myOtherService}
    );
    */

    }
}
