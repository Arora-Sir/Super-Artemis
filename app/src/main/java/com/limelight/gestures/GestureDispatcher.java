package com.limelight.gestures;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import androidx.preference.PreferenceManager;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.SpecialKeys;

import java.util.List;

// NOTE: Connects the stream's multi-finger gesture recognizer to the user's gesture bindings.
// NOTE: Finger counts with nothing bound are left alone, so those touches keep reaching the PC.
public final class GestureDispatcher implements MultiFingerGestureRecognizer.Callbacks {
    public interface Target {
        void showQuickMenu();

        void middleClick();

        void runAction(StreamAction action);

        void sendKeys(short[] keys);

        // Runs a special key, a single shortcut or a key with several steps
        void runKey(CustomKey key);

        // Ends what the touch already started on the PC or in pan and zoom
        void cancelTouch(MotionEvent event);
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final Target target;
    private final MultiFingerTapDetector detector;
    private final MultiFingerGestureRecognizer recognizer;

    public GestureDispatcher(Context context, Target target) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.target = target;
        Handler handler = new Handler(Looper.getMainLooper());
        MultiFingerTapDetector.Scheduler scheduler = new MultiFingerTapDetector.Scheduler() {
            @Override
            public void schedule(Runnable runnable, long delayMs) {
                handler.postDelayed(runnable, delayMs);
            }

            @Override
            public void cancel(Runnable runnable) {
                handler.removeCallbacks(runnable);
            }
        };
        this.detector = new MultiFingerTapDetector(scheduler, (fingers, doubleTap) -> fire(new GestureBindingStore.Slot(fingers, doubleTap)),
                fingers -> !GestureBindingStore.NONE.equals(binding(new GestureBindingStore.Slot(fingers, true))));
        this.recognizer = new MultiFingerGestureRecognizer(this, context.getResources().getDisplayMetrics().density, scheduler);
    }

    // Feeds every stream touch through gesture recognition first. True means the event belongs to a gesture.
    public boolean onTouchEvent(MotionEvent event) {
        return recognizer.onTouchEvent(event);
    }

    // True when a single or double tap with this many fingers is bound, so the tap belongs to the gesture and not the PC
    @Override
    public boolean handlesTap(int fingers) {
        return !GestureBindingStore.NONE.equals(binding(new GestureBindingStore.Slot(fingers, false)))
                || !GestureBindingStore.NONE.equals(binding(new GestureBindingStore.Slot(fingers, true)));
    }

    @Override
    public boolean ownsSwipes(int fingers) {
        return GestureBindingStore.hasSwipe(prefs, fingers);
    }

    @Override
    public boolean handlesHold(int fingers) {
        return !GestureBindingStore.NONE.equals(binding(new GestureBindingStore.Slot(fingers, GestureBindingStore.Kind.HOLD)));
    }

    @Override
    public long holdDurationMs() {
        return GestureBindingStore.holdDurationMs(prefs);
    }

    @Override
    public void onHold(int fingers) {
        fire(new GestureBindingStore.Slot(fingers, GestureBindingStore.Kind.HOLD));
    }

    @Override
    public void onTap(int fingers, long eventTimeMs) {
        detector.onTap(fingers, eventTimeMs);
    }

    @Override
    public void onSwipe(int fingers, MultiFingerGestureRecognizer.Direction direction) {
        GestureBindingStore.Kind kind;
        switch (direction) {
            case UP:
                kind = GestureBindingStore.Kind.SWIPE_UP;
                break;
            case DOWN:
                kind = GestureBindingStore.Kind.SWIPE_DOWN;
                break;
            case LEFT:
                kind = GestureBindingStore.Kind.SWIPE_LEFT;
                break;
            default:
                kind = GestureBindingStore.Kind.SWIPE_RIGHT;
                break;
        }
        fire(new GestureBindingStore.Slot(fingers, kind));
    }

    @Override
    public void cancelPassThrough(MotionEvent event) {
        target.cancelTouch(event);
    }

    private String binding(GestureBindingStore.Slot slot) {
        return GestureBindingStore.get(prefs, slot);
    }

    private void fire(GestureBindingStore.Slot slot) {
        com.limelight.ui.SafeRun.run(context, slot.prefKey(), () -> fireUnsafe(slot));
    }

    private void fireUnsafe(GestureBindingStore.Slot slot) {
        String binding = binding(slot);
        if (GestureBindingStore.NONE.equals(binding)) {
            return;
        }
        if (GestureBindingStore.QUICK_MENU.equals(binding)) {
            target.showQuickMenu();
            return;
        }
        if (GestureBindingStore.MIDDLE_CLICK.equals(binding)) {
            target.middleClick();
            return;
        }
        List<Integer> combo = GestureBindingStore.comboKeys(binding);
        if (combo != null) {
            short[] keys = new short[combo.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = (short) (int) combo.get(i);
            }
            target.sendKeys(keys);
            return;
        }
        String keyId = GestureBindingStore.customKeyId(binding);
        if (keyId != null) {
            CustomKey key = SpecialKeys.find(context, keyId);
            if (key != null) {
                target.runKey(key);
            }
            return;
        }
        StreamAction action = StreamActionRegistry.find(binding);
        if (action != null && !action.destructive) {
            target.runAction(action);
        }
    }
}
