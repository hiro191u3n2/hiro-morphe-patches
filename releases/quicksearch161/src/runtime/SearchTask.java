package app.hiro.quicksearch.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Task-scoped manual exit, dialog back handling and browser separation. */
public final class SearchTask {
    private SearchTask() {}

    public static final String PACKAGE_NAME = "jp.ddo.sugihiro.quicksearch";
    public static final int CLEAR_ID = 0x7f0900cf;
    public static final int CLOSE_ID = 0x7f090065;

    /** Implemented by a small bridge added to the original MainActivity. */
    public interface Cleanup {
        void hiroQuickSearchCleanup();
    }

    private static final List<State> STATES = new ArrayList<State>();

    private static final class State {
        final WeakReference<Activity> activity;
        final List<DialogState> dialogs = new ArrayList<DialogState>();
        boolean closing;
        boolean cleaned;
        boolean detached;
        Object activityBack;
        State(Activity value) { activity = new WeakReference<Activity>(value); }
    }

    private static final class DialogState {
        final WeakReference<Dialog> dialog;
        final State owner;
        Object back;
        boolean listening;
        DialogState(Dialog value, State state) {
            dialog = new WeakReference<Dialog>(value);
            owner = state;
        }
    }

    private static boolean owned(Activity activity) {
        if (activity == null || !PACKAGE_NAME.equals(activity.getPackageName())) return false;
        String name = activity.getClass().getName();
        return name.equals(PACKAGE_NAME + ".activity.MainActivity")
                || name.equals(PACKAGE_NAME + ".activity.WebActivity")
                || name.equals(PACKAGE_NAME + ".activity.ConfigActivity");
    }

    private static State state(Activity activity, boolean create) {
        synchronized (STATES) {
            Iterator<State> iterator = STATES.iterator();
            while (iterator.hasNext()) {
                State current = iterator.next();
                Activity value = current.activity.get();
                if (value == null) iterator.remove();
                else if (value == activity) return current;
            }
            if (!create || !owned(activity)) return null;
            State result = new State(activity);
            STATES.add(result);
            return result;
        }
    }

    public static void attach(Activity activity) {
        if (!owned(activity) || activity.isDestroyed()) return;
        State state = state(activity, true);
        if (state.closing || state.detached) return;
        if (Build.VERSION.SDK_INT >= 33 && state.activityBack == null) {
            state.activityBack = Api33.activity(activity, state);
        }
        bindButtons(activity.findViewById(CLEAR_ID), state);
        bindButtons(activity.findViewById(CLOSE_ID), state);
    }

    /** Main displays its input in a separate platform Dialog window. */
    public static void bindMain(Activity activity, Dialog dialog, View inputView) {
        attach(activity);
        State state = state(activity, false);
        if (state == null || state.closing || state.detached) return;
        if (inputView != null) {
            bindButtons(inputView.findViewById(CLEAR_ID), state);
            bindButtons(inputView.findViewById(CLOSE_ID), state);
        }
        bindDialog(dialog);
    }

    public static AlertDialog showPlatform(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.show();
        bindDialog(dialog);
        return dialog;
    }

    public static void showDialog(Dialog dialog) {
        dialog.show();
        bindDialog(dialog);
    }

    private static Activity owner(Dialog dialog) {
        if (dialog == null) return null;
        Activity owner = dialog.getOwnerActivity();
        if (owned(owner)) return owner;
        return ownerContext(dialog.getContext());
    }

    private static Activity ownerContext(Context context) {
        for (int depth = 0; depth < 16 && context != null; depth++) {
            if (context instanceof Activity) return owned((Activity) context) ? (Activity) context : null;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }

    /** Bind shown app dialogs without replacing their positive/negative/cancel/dismiss callbacks. */
    public static void bindDialog(final Dialog dialog) {
        Activity activity = owner(dialog);
        if (activity == null || activity.isDestroyed()) return;
        attach(activity);
        final State state = state(activity, false);
        if (state == null || state.closing || state.detached) return;
        DialogState match = null;
        Iterator<DialogState> iterator = state.dialogs.iterator();
        while (iterator.hasNext()) {
            DialogState candidate = iterator.next();
            Dialog value = candidate.dialog.get();
            if (value == null) iterator.remove();
            else if (value == dialog) match = candidate;
        }
        if (match == null) {
            match = new DialogState(dialog, state);
            state.dialogs.add(match);
        }
        final DialogState binding = match;
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface ignored, int keyCode, KeyEvent event) {
                return handleBackKey(state.activity.get(), event);
            }
        });
        bindButtons(dialog.findViewById(CLEAR_ID), state);
        bindButtons(dialog.findViewById(CLOSE_ID), state);
        registerDialog(binding);
        if (!binding.listening && dialog.getWindow() != null) {
            View decor = dialog.getWindow().getDecorView();
            if (decor != null) {
                binding.listening = true;
                decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override public void onViewAttachedToWindow(View view) { registerDialog(binding); }
                    @Override public void onViewDetachedFromWindow(View view) { unregisterDialog(binding); }
                });
            }
        }
    }

    private static void registerDialog(DialogState binding) {
        Dialog dialog = binding.dialog.get();
        if (Build.VERSION.SDK_INT >= 33 && dialog != null && binding.back == null
                && !binding.owner.closing && !binding.owner.detached) {
            binding.back = Api33.dialog(dialog, binding.owner);
        }
    }

    private static void unregisterDialog(DialogState binding) {
        Object registration = binding.back;
        binding.back = null;
        if (Build.VERSION.SDK_INT >= 33 && registration != null) Api33.unregister(registration);
    }

    private static void bindButtons(View button, final State owner) {
        if (button == null) return;
        final int size = Math.max(1, (int) (48f * button.getResources().getDisplayMetrics().density + 0.5f));
        button.setMinimumHeight(size);
        ViewGroup.LayoutParams parameters = button.getLayoutParams();
        boolean weighted = parameters instanceof LinearLayout.LayoutParams
                && ((LinearLayout.LayoutParams) parameters).weight > 0f;
        if (!weighted) {
            button.setMinimumWidth(size);
            if (parameters != null && parameters.width >= 0 && parameters.width < size) parameters.width = size;
        }
        if (parameters != null) {
            if (parameters.height >= 0 && parameters.height < size) parameters.height = size;
            if (parameters instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) parameters;
                if (margins.leftMargin < 0) margins.leftMargin = 0;
                if (margins.getMarginStart() < 0) margins.setMarginStart(0);
            }
            button.setLayoutParams(parameters);
        }
        button.setContentDescription("終了");
        button.setEnabled(true);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(owner.activity.get()); }
        });
    }

    /** Legacy BACK is consumed on both phases; a cancelled gesture never exits. */
    public static boolean handleBackKey(Activity activity, KeyEvent event) {
        if (!owned(activity) || event == null || event.getKeyCode() != KeyEvent.KEYCODE_BACK) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) finish(activity);
        return true;
    }

    /** API 29-32 text fields see BACK here before the input method consumes it. */
    public static boolean handleBackKeyFromView(View view, KeyEvent event) {
        return view != null && handleBackKey(ownerContext(view.getContext()), event);
    }

    /** The history popup installs its original close listener after show(), so it has a direct hook. */
    public static void closeDialog(Dialog dialog) {
        Activity activity = owner(dialog);
        if (activity == null) {
            dialog.cancel();
            return;
        }
        finish(activity);
    }

    public static boolean isClosing(Activity activity) {
        State state = state(activity, false);
        return state != null && (state.closing || state.detached);
    }

    private static void cleanup(State state) {
        if (state.cleaned) return;
        state.cleaned = true;
        Activity activity = state.activity.get();
        if (activity instanceof Cleanup) {
            try { ((Cleanup) activity).hiroQuickSearchCleanup(); }
            catch (RuntimeException ignored) { /* Do not let a teardown race block task removal. */ }
        }
    }

    /** Detach also stops Main's periodic work during ordinary Activity destruction/recreation. */
    public static void detach(Activity activity) {
        State state = state(activity, true);
        if (state == null || state.detached) return;
        state.detached = true;
        cleanup(state);
        Object registration = state.activityBack;
        state.activityBack = null;
        if (Build.VERSION.SDK_INT >= 33 && registration != null) Api33.unregister(registration);
        for (DialogState dialog : state.dialogs) unregisterDialog(dialog);
        state.dialogs.clear();
    }

    /** Exit only our Activities in this task. Other tasks and foreign callers are retained. */
    public static void finish(Activity activity) {
        if (!owned(activity)) return;
        State current = state(activity, true);
        if (current.closing || current.detached || activity.isDestroyed()) return;
        int taskId = activity.getTaskId();
        List<State> closing = new ArrayList<State>();
        Activity root = null;
        synchronized (STATES) {
            for (State state : STATES) {
                Activity candidate = state.activity.get();
                if (candidate == null || state.detached || !owned(candidate) || candidate.isDestroyed()) continue;
                if (candidate != activity && (taskId < 0 || candidate.getTaskId() != taskId)) continue;
                state.closing = true;
                closing.add(state);
                if (candidate.isTaskRoot()) root = candidate;
            }
        }
        for (State state : closing) {
            cleanup(state);
            for (DialogState binding : new ArrayList<DialogState>(state.dialogs)) {
                unregisterDialog(binding);
                Dialog dialog = binding.dialog.get();
                if (dialog != null && dialog.isShowing()) {
                    try { dialog.dismiss(); } catch (RuntimeException ignored) { }
                }
            }
        }
        boolean removed = false;
        if (root != null) {
            try { root.finishAndRemoveTask(); removed = true; }
            catch (RuntimeException ignored) { }
        }
        if (!removed && taskId >= 0) removed = removeOwnedTask(activity, taskId);
        if (!removed) {
            for (State state : closing) {
                Activity candidate = state.activity.get();
                if (candidate != null && !candidate.isDestroyed()) candidate.finish();
            }
        }
    }

    private static boolean removeOwnedTask(Activity activity, int taskId) {
        try {
            Object service = activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (!(service instanceof ActivityManager)) return false;
            List<ActivityManager.AppTask> tasks = ((ActivityManager) service).getAppTasks();
            if (tasks == null) return false;
            for (ActivityManager.AppTask task : tasks) {
                ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                if (info != null && info.taskId == taskId && info.baseActivity != null
                        && PACKAGE_NAME.equals(info.baseActivity.getPackageName())) {
                    task.finishAndRemoveTask();
                    return true;
                }
            }
        } catch (RuntimeException ignored) { /* A vanished or foreign task falls back to our own activities. */ }
        return false;
    }

    /** Manual exit is available even with auto-exit OFF, so all external results are separated. */
    public static void startExternal(Activity activity, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    /** API 33 types are isolated so this extension still loads on the app's API 29 minimum. */
    private static final class Api33 {
        private static final class Registration {
            final android.window.OnBackInvokedDispatcher dispatcher;
            final android.window.OnBackInvokedCallback callback;
            Registration(android.window.OnBackInvokedDispatcher dispatcher,
                    android.window.OnBackInvokedCallback callback) {
                this.dispatcher = dispatcher;
                this.callback = callback;
            }
        }
        static Object activity(Activity activity, State state) {
            return register(activity.getOnBackInvokedDispatcher(), state);
        }
        static Object dialog(Dialog dialog, State state) {
            return register(dialog.getOnBackInvokedDispatcher(), state);
        }
        private static Object register(android.window.OnBackInvokedDispatcher dispatcher, final State state) {
            android.window.OnBackInvokedCallback callback = new android.window.OnBackInvokedCallback() {
                @Override public void onBackInvoked() { finish(state.activity.get()); }
            };
            dispatcher.registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback);
            return new Registration(dispatcher, callback);
        }
        static void unregister(Object value) {
            Registration registration = (Registration) value;
            try { registration.dispatcher.unregisterOnBackInvokedCallback(registration.callback); }
            catch (RuntimeException ignored) { }
        }
    }
}
