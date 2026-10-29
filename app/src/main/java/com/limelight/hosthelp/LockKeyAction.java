package com.limelight.hosthelp;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.limelight.Game;
import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;

import java.util.List;
import java.util.Locale;

// NOTE: The Lock key is Win+L, and Windows ignores a Win+L that a stream injects, so the key reaches the PC and nothing locks.
// NOTE: An Apollo host can run a command on request. When it offers a server command named Lock, the Lock key runs that command and needs nothing installed on the PC.
// NOTE: Without such a command the key still sends Win+L, because a PC with an AutoHotkey script or another fix accepts it, and a one time hint says where the fix is.
public final class LockKeyAction {
    static final String PREF_HINT_SHOWN = "lock_key_hint_shown";
    // Windows virtual key code of the right Windows key and of L
    private static final short VK_RWIN = 0x5C;
    private static final short VK_L = 0x4C;

    private LockKeyAction() {
    }

    // True for exactly the shortcut Win+L, from either Windows key
    static boolean isLockCombo(short[] keys) {
        if (keys == null || keys.length != 2) {
            return false;
        }
        boolean win = keys[0] == KeyboardTranslator.VK_LWIN || keys[0] == VK_RWIN;
        return win && keys[1] == VK_L;
    }

    // The position of the first server command that locks (Lock, Lock PC, Lock screen), or -1. Unlock and similar names never match.
    static int findLockCommand(List<String> commands) {
        if (commands == null) {
            return -1;
        }
        for (int i = 0; i < commands.size(); i++) {
            String name = commands.get(i) == null ? "" : commands.get(i).trim().toLowerCase(Locale.ROOT);
            if (name.contains("lock") && !name.contains("unlock")) {
                return i;
            }
        }
        return -1;
    }

    // Returns true when the key was handled by a server command, so the caller must not send the keys
    public static boolean handle(Game game, short[] keys) {
        if (!isLockCombo(keys)) {
            return false;
        }
        int command = findLockCommand(game.getServerCmds());
        if (command >= 0) {
            game.sendExecServerCmd(command);
            Toast.makeText(game, R.string.lock_key_sent, Toast.LENGTH_SHORT).show();
            return true;
        }
        hintOnce(game);
        return false;
    }

    private static void hintOnce(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(PREF_HINT_SHOWN, false)) {
            prefs.edit().putBoolean(PREF_HINT_SHOWN, true).apply();
            Toast.makeText(context, R.string.lock_key_hint, Toast.LENGTH_LONG).show();
        }
    }
}
