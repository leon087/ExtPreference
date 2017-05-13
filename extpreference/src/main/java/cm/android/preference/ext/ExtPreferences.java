package cm.android.preference.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExtPreferences implements SharedPreferences {
    /**
     * 初始化，确保在每一个进程中都有初始化
     */
    public static void attach(PrefsConfig config) {
//        PrefAccessor.init(config);
        PreferenceProvider.initConfig(config);
    }

    private Context context;
    private String name;

    public static SharedPreferences getSharedPreferences(Context context, String name) {
        return new ExtPreferences(context, name);
    }

    private ExtPreferences(Context context, String name) {
        this.context = context.getApplicationContext();
        this.name = name;
    }

    @Override
    public Map<String, ?> getAll() {
        return PrefAccessor.getAll(context, name);
    }

    @Override
    public String getString(String key, String defValue) {
        return PrefAccessor.getString(context, name, key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return PrefAccessor.getStringSet(context, name, key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return PrefAccessor.getInt(context, name, key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return PrefAccessor.getLong(context, name, key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return PrefAccessor.getFloat(context, name, key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return PrefAccessor.getBoolean(context, name, key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return PrefAccessor.contains(context, name, key);
    }

    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    private ContentObserver observer;

    @Override
    //TODO ggg 需要支持注册多个监听器
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
//        observer = PrefAccessor.registerContentObserver(context, name, listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
//        PrefAccessor.unregisterContentObserver(context, observer);
    }

    private class EditorImpl implements SharedPreferences.Editor {
        private final Map<String, Object> mModified = new HashMap<>();
        private boolean mClear = false;

        private EditorImpl() {
        }

        @Override
        public Editor putString(String key, String value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            synchronized (this) {
                mModified.put(key, values);
                return this;
            }
        }

        @Override
        public Editor putInt(String key, int value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putLong(String key, long value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putFloat(String key, float value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor remove(String key) {
            synchronized (this) {
                mModified.put(key, this);
                return this;
            }
        }

        @Override
        public Editor clear() {
            synchronized (this) {
                mClear = true;
                return this;
            }
        }

        @Override
        public boolean commit() {
            ContentValues cv = readyCommit();
            applyToDb(cv);
            return true;
        }

        @Override
        public void apply() {
            ContentValues cv = readyCommit();
            applyToDb(cv);
        }

        private ContentValues readyCommit() {
            synchronized (this) {
                ContentValues cv = new ContentValues();
                if (mClear) {
                    //写数据库
                    PrefAccessor.clear(cv, "");
                    mClear = false;
                }

                for (Map.Entry<String, Object> e : mModified.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    // "this" is the magic value for a removal mutation. In addition,
                    // setting a value to "null" for a given key is specified to be
                    // equivalent to calling remove on that key.
                    if (v == this || v == null) {
                        //db
                        PrefAccessor.remove(cv, k);
                    } else {
                        put(cv, k, v);
                    }
                }

                mModified.clear();
                return cv;
            }
        }

        private void applyToDb(ContentValues cv) {
            PrefAccessor.apply(context, name, cv);
        }

        private void put(ContentValues cv, String k, Object obj) {
            if (obj instanceof Boolean) {
                PrefAccessor.putBoolean(cv, k, obj);
            } else if (obj instanceof Long) {
                PrefAccessor.putLong(cv, k, obj);
            } else if (obj instanceof String) {
                PrefAccessor.putString(cv, k, obj);
            } else if (obj instanceof Integer) {
                PrefAccessor.putInt(cv, k, obj);
            } else if (obj instanceof Float) {
                PrefAccessor.putFloat(cv, k, obj);
            } else if (obj instanceof Set) {
                PrefAccessor.putStringSet(cv, k, obj);
            }
        }
    }
}
