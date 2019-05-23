package xyz.xkrivzooh.instruments;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;
import xyz.xkrivzooh.instrument.agent.Agent;

public class AgentLoader {

    private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
        @Override
        public String name() {
            return null;
        }

        @Override
        public String type() {
            return null;
        }

        @Override
        public VirtualMachine attachVirtualMachine(String id) {
            return null;
        }

        @Override
        public List<VirtualMachineDescriptor> listVirtualMachines() {
            return null;
        }
    };

    private static String jarFilePath;

    private static volatile boolean loaded = false;

    private static final Logger logger = LoggerFactory.getLogger(AgentLoader.class);

    private static Instrumentation inst;

    static {
        jarFilePath = getAgentPath();
    }

    public static void loadAgent() {
        if (loaded) {
            return;
        }

        VirtualMachine vm;
        if (AttachProvider.providers().isEmpty()) {
            String vmName = System.getProperty("java.vm.name");

            if (vmName.contains("HotSpot")) {
                vm = getVirtualMachineImplementationFromEmbeddedOnes();
            }
            else {
                String helpMessage = getHelpMessageForNonHotSpotVM(vmName);
                throw new IllegalStateException(helpMessage);
            }
        }
        else {
            vm = attachToRunningVM();
        }

        loadAgentAndDetachFromRunningVM(vm);

        inst = instrumentation();
        loaded = true;
    }


    private static String getAgentPath() {
        try {
            ProtectionDomain domain = Agent.class.getProtectionDomain();
            CodeSource source = domain.getCodeSource();
            return new File(source.getLocation().getPath()).getAbsolutePath();
        }
        catch (Exception e) {
            logger.error("get agent jar path error", e);
            throw new RuntimeException("get agent jar path error");
        }
    }

    private static String getProcessIdForRunningVM() {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }

    private static VirtualMachine attachToRunningVM() {
        String pid = getProcessIdForRunningVM();

        try {
            return VirtualMachine.attach(pid);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadAgentAndDetachFromRunningVM(VirtualMachine vm) {
        try {
            vm.loadAgent(jarFilePath, null);
            vm.detach();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes() {
        Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
        Class<?>[] parameterTypes = {AttachProvider.class, String.class};
        String pid = getProcessIdForRunningVM();

        try {
            // This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
            Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(parameterTypes);
            return vmConstructor.newInstance(ATTACH_PROVIDER, pid);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS() {
        if (File.separatorChar == '\\') {
            return WindowsVirtualMachine.class;
        }

        String osName = System.getProperty("os.name");

        if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
            return LinuxVirtualMachine.class;
        }
        else if (osName.startsWith("Mac OS X")) {
            return BsdVirtualMachine.class;
        }
        else if (osName.startsWith("Solaris")) {
            return SolarisVirtualMachine.class;
        }

        throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
    }

    private static String getHelpMessageForNonHotSpotVM(String vmName) {
        String helpMessage = "To run on " + vmName;

        if (vmName.contains("J9")) {
            helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before jmockit), or";
        }

        return helpMessage + " use -javaagent:" + jarFilePath;
    }

    private static Instrumentation instrumentation() {
        ClassLoader mainAppLoader = ClassLoader.getSystemClassLoader();
        try {
            final Class<?> javaAgentClass = mainAppLoader.loadClass(Agent.class.getCanonicalName());
            final Method method = javaAgentClass.getDeclaredMethod("instrumentation", new Class[0]);
            return (Instrumentation) method.invoke(null, new Object[0]);
        }
        catch (Throwable e) {
            logger.error("can not get agent class", e);
            throw new RuntimeException(e);
        }
    }

    public static Instrumentation getInstrumentation() {
        if (!loaded) {
            throw new IllegalStateException("should load agent first");
        }

        return inst;
    }
}
