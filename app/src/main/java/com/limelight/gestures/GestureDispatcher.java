package com.limelight.gestures;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.CustomKeyStore;

// NOTE: Connects the stream's multi-finger tap detection to the user's gesture bindings.
// NOTE: Game asks handlesTap() first, so finger counts with nothing bound pass through to the PC instead of being swallowed.
public final class GestureDispatcher {
    public interface Target {
        void showQuickMenu();

        void middleClick();

        void runAction(StreamAction action);

        void sendKeys(short[] keys);
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final Target target;
    private final MultiFingerTapDetector detector;

    public GestureDispatcher(Context context, Target target) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.target = target;
        Handler handler = new Handler(Looper.getMainLooper());
        this.detector = new MultiFingerTapDetector(new MultiFingerTapDetector.Scheduler() {
            @Override
            public void schedule(Runnable runnable, long delayMs) {
                handler.postDelayed(runnable, delayMs);
            }

            @Override
            public void cancel(Runnable runnable) {
                handler.removeCallbacks(runnable);
            }
        }, this::fire, fingers -> !GestureBindingStore.NONE.equals(binding(fingers, true)));
    }

    // True when a single or double tap with this many fingers is bound, so the tap belongs to the gesture and not the PC
    public boolean handlesTap(int fingers) {
        return !GestureBindingStore.NONE.equals(binding(fingers, false))
                || !GestureBindingStore.NONE.equals(binding(fingers, true));
    }

    public void onTap(int fingers, long eventTimeMs) {
        detector.onTap(fingers, eventTimeMs);
    }

    private String binding(int fingers, boolean doubleTap) {
        return GestureBindingStore.get(prefs, new GestureBindingStore.Slot(fingers, doubleTap));
    }

    private void fire(int fingers, boolean doubleTap) {
        String binding = binding(fingers, doubleTap);
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
        String keyId = GestureBindingStore.customKeyId(binding);
        if (keyId != null) {
            CustomKey key = CustomKeyStore.find(context, keyId);
            if (key != null) {
                target.sendKeys(key.keyCodes());
            }
            return;
        }
        StreamAction action = StreamActionRegistry.find(binding);
        if (action != null && !action.destructive) {
            target.runAction(action);
        }
    }
}
