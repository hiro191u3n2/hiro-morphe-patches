package hiro.swiftkey;
final class UsageBridgeTestEnvironment {
    static boolean eligible=true, allowedBefore=true, allowedAfter=true;
    static int allowedCalls;
    static boolean allowed(UsageBridge$Session session,wb0.n0 state) {
        return allowedCalls++==0?allowedBefore:allowedAfter;
    }
}
