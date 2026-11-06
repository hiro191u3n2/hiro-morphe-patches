package app.hiro.mapsstartup;

/** Implemented by the verified native Maps activity through the bytecode patch. */
public interface LocationHost {
    /** -1: route/navigation mode; 0: not ready; 1: native recenter requested or already tracking. */
    int hiroTryCurrentLocation();
}
