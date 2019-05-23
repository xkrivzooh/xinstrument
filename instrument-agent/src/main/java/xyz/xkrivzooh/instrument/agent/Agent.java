package xyz.xkrivzooh.instrument.agent;

import java.lang.instrument.Instrumentation;


/**
 *  see：https://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html
 *  "Starting Agents After VM Startup"
 */
public class Agent {


    private static Instrumentation inst;

    //will statically load the agent using -javaagent parameter at JVM startup
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        inst = instrumentation;
    }

    //will dynamically load the agent into the JVM using the Java Attach API
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        inst = instrumentation;
    }

    public static Instrumentation instrumentation() {
        if (inst == null) {
            throw new IllegalStateException("instrument agent init error");
        }
        return inst;
    }
}
