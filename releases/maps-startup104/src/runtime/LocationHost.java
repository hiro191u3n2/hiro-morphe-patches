package app.hiro.mapsstartup;

/** Implemented by the verified native Maps activity through the bytecode patch. */
public interface LocationHost {
    /**
     * -1: route/navigation mode; 0: not ready or not yet centered;
     * 1: current camera target and zoom confirm the location; 2: native command issued.
     * With issue=false this is a read-only readiness/confirmation query.
     */
    int hiroTryCurrentLocation(boolean issue);
}
