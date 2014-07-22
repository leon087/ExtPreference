package cm.android.sdk.preference;

import android.annotation.TargetApi;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import cm.android.sdk.preference.encryption.EncryptionHelper;

import java.util.Set;

/**
 * An {@link android.content.SharedPreferences.Editor} decorator.
 */
public class SecuredEditor implements Editor {
    private Editor editor;
    private EncryptionHelper helper;

    /**
     * Initializes with the {@link EncryptionHelper} an the original
     * {@link android.content.SharedPreferences.Editor}.
     *
     * @param helper The helper to use.
     * @param edit   The editor to use.
     */
    public SecuredEditor(EncryptionHelper helper, Editor edit) {
        this.helper = helper;
        this.editor = edit;
    }

    @Override
    public SecuredEditor putString(String key, String value) {
        helper.putValue(editor, key, value);
        return this;
    }

    @Override
    public SecuredEditor putStringSet(String key, Set<String> values) {
        helper.putValue(editor, key, values);
        return this;
    }

    @Override
    public SecuredEditor putInt(String key, int value) {
        helper.putValue(editor, key, value);
        return this;
    }

    @Override
    public SecuredEditor putLong(String key, long value) {
        helper.putValue(editor, key, value);
        return this;
    }

    @Override
    public SecuredEditor putFloat(String key, float value) {
        helper.putValue(editor, key, value);
        return this;
    }

    @Override
    public SecuredEditor putBoolean(String key, boolean value) {
        helper.putValue(editor, key, value);
        return this;
    }

    @Override
    public SecuredEditor remove(String key) {
        editor.remove(key);
        return this;
    }

    @Override
    public SecuredEditor clear() {
        editor.clear();
        return this;
    }

    @Override
    public boolean commit() {
        return editor.commit();
    }

    @Override
    @TargetApi(9)
    public void apply() {
        editor.apply();
    }

    /**
     * Compatibility version of original {@link android.content.SharedPreferences.Editor#apply()}
     * method that simply call {@link android.content.SharedPreferences.Editor#commit()} for pre Android Honeycomb (API 11).
     * This method is thread safe also on pre API 11.
     * Note that when two editors are modifying preferences at the same time, the last one to call apply wins. (Android Doc)
     */
    public void save() {
        compatilitySave(this);
    }

    /**
     * Saves the {@link android.content.SharedPreferences}. See save method.
     *
     * @param editor The editor to save/commit.
     */
    @TargetApi(9)
    public static void compatilitySave(Editor editor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            editor.apply();
        } else {
            synchronized (SecuredEditor.class) {
                editor.commit();
            }
        }
    }

}
