package app.hiro.tripcom.patches;

import app.morphe.patcher.patch.ResourcePatchContext;

/** Asset writes are separately verified by the real MPP apply and JS tests. */
public final class TripPatchAssets {
    public static void replaceAiPlanner(ResourcePatchContext context) { context.assetCalls++; }
}
