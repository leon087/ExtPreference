package cm.android.preference.ext;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreferenceProvider extends ContentProvider {

    public static final String COLUMNS_KEY = "key";
    public static final String COLUMNS_VALUE = "value";
    public static final String COLUMNS_TYPE = "type";

    //boolean用int
    public static final int PREF_BOOLEAN = 1;
    public static final int PREF_STRING = 2;
    public static final int PREF_INT = 3;
    public static final int PREF_LONG = 4;
    public static final int PREF_FLOAT = 5;
    //set用blob
    public static final int PREF_SET = 6;

    public static final int PREF_APPLY = 100;
    public static final int PREF_ALL = 102;
    public static final int PREF_EXIST = 103;
    public static final int PREF_CLEAR = 104;
    public static final int PREF_REMOVE = 105;

    public static final int PREF_CHANGE = 106;

    /**
     * 分隔字符组合以减少误差
     */
    public static final String KEY_SPLIT = "#!&";

    public static final String KEY_PUT_STRING = "k_p_s" + KEY_SPLIT;
    public static final String KEY_PUT_LONG = "k_p_l" + KEY_SPLIT;
    public static final String KEY_PUT_INT = "k_p_i" + KEY_SPLIT;
    public static final String KEY_PUT_BOOLEAN = "k_p_b" + KEY_SPLIT;
    public static final String KEY_PUT_FLOAT = "k_p_f" + KEY_SPLIT;
    public static final String KEY_PUT_SET = "k_p_set" + KEY_SPLIT;

    public static final String KEY_REMOVE = "k_r" + KEY_SPLIT;
    public static final String KEY_CLEAR = "k_c" + KEY_SPLIT;

    private static PrefsConfig config = new PrefsConfig.Builder().authority("preference.provider").build();

    public final static void initConfig(PrefsConfig config) {
        PreferenceProvider.config = config;
    }

    static PrefsConfig config() {
        return config;
    }

    private static final Map<String, String> uris = new HashMap<>();

    private final Map<String, SharedPreferences> prefsMap = new HashMap<>();

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            synchronized (this) {
                prefsMap.clear();
            }
        }
    }

    private SharedPreferences getPrefs(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("getPrefs name is null!!!");
        }
        synchronized (this) {
            SharedPreferences prefs = prefsMap.get(name);
            if (prefs == null) {
                prefs = config.getFactory().getSharedPreferences(getContext(), name);
                prefsMap.put(name, prefs);
            }
            return prefs;
        }
    }

    public static String getKey(String key_type, String key) {
        StringBuilder sb = new StringBuilder()
                .append(key_type)
                .append(key);
        return sb.toString();
    }

    public static String parseKey(String key) {
        String rk = key.split(KEY_SPLIT)[1];
        return rk;
    }

    private static String createUri(String authority, String table) {
        StringBuilder sb = new StringBuilder()
                .append("content://")
                .append(authority)
                .append("/")
                .append(table)
                .append("/");
        return sb.toString();
    }

    private UriMatcher matcher;

    private void init(String authority) {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(authority, PREF_ALL + "/*", PREF_ALL);
        matcher.addURI(authority, PREF_EXIST + "/*/*", PREF_EXIST);
        matcher.addURI(authority, PREF_BOOLEAN + "/*/*", PREF_BOOLEAN);
        matcher.addURI(authority, PREF_STRING + "/*/*", PREF_STRING);
        matcher.addURI(authority, PREF_INT + "/*/*", PREF_INT);
        matcher.addURI(authority, PREF_LONG + "/*/*", PREF_LONG);
        matcher.addURI(authority, PREF_FLOAT + "/*/*", PREF_FLOAT);
        matcher.addURI(authority, PREF_SET + "/*/*", PREF_SET);

        matcher.addURI(authority, PREF_APPLY + "/*", PREF_APPLY);

        matcher.addURI(authority, PREF_CHANGE + "/*", PREF_CHANGE);
    }

    private static String getUriByType(String authority, int type) {
        switch (type) {
            case PreferenceProvider.PREF_BOOLEAN:
            case PreferenceProvider.PREF_INT:
            case PreferenceProvider.PREF_LONG:
            case PreferenceProvider.PREF_STRING:
            case PreferenceProvider.PREF_FLOAT:
            case PreferenceProvider.PREF_SET:

            case PreferenceProvider.PREF_ALL:
            case PreferenceProvider.PREF_EXIST:
            case PreferenceProvider.PREF_APPLY:
                return getUri(authority, type);
            default:
                throw new IllegalStateException("unsupport preftype : " + type);
        }
    }

    private static String getUri(String authority, int table) {
        String ts = String.valueOf(table);
        String uri = uris.get(ts);
        if (TextUtils.isEmpty(uri)) {
            uri = createUri(authority, ts);
            uris.put(ts, uri);
        }
        return uri;
    }

    public static Uri buildUri(String authority, String name, String key, int type) {
        StringBuilder sb = new StringBuilder()
                .append(getUriByType(authority, type))
                .append(name);
        if (!TextUtils.isEmpty(key)) {
            sb.append("/").append(key);
        }

        return Uri.parse(sb.toString());
    }

    @Override
    public boolean onCreate() {
//        config = createConfig();
        init(config.getAuthority());
        return true;
    }

//    protected abstract PrefsConfig createConfig();

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        PrefModel model = getPrefModelByUri(uri);
        boolean exist = getPrefs(model.getName()).contains(model.getKey());

        switch (matcher.match(uri)) {
            case PREF_ALL:
                return preferenceMapToCursor(getPrefs(model.getName()).getAll());
            case PREF_EXIST:
                return preferenceExistToCursor(exist);
            case PREF_SET:
                if (exist) {
                    byte[] v = PrefsCursor.Util.setToBlob(getPrefs(model.getName()).getStringSet(model.getKey(), new HashSet<String>()));
                    return preferenceToCursor(model.getKey(), v);
                }
            case PREF_BOOLEAN:
                if (exist) {
                    int v = PrefsCursor.Util.booleanToInt(getPrefs(model.getName()).getBoolean(model.getKey(), false));
                    return preferenceToCursor(model.getKey(), v);
                }
            case PREF_STRING:
                if (exist) {
                    return preferenceToCursor(model.getKey(), getPrefs(model.getName()).getString(model.getKey(), ""));
                }
            case PREF_INT:
                if (exist) {
                    return preferenceToCursor(model.getKey(), getPrefs(model.getName()).getInt(model.getKey(), -1));
                }
            case PREF_LONG:
                if (exist) {
                    return preferenceToCursor(model.getKey(), getPrefs(model.getName()).getLong(model.getKey(), -1));
                }
            case PREF_FLOAT:
                if (exist) {
                    return preferenceToCursor(model.getKey(), getPrefs(model.getName()).getFloat(model.getKey(), -1));
                }
            default:
                return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new IllegalStateException("insert unsupport!!!");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new IllegalStateException("insert unsupport!!!");
    }

    private int match(String key) {
        if (key.contains(KEY_REMOVE)) {
            return PREF_REMOVE;
        } else if (key.contains(KEY_CLEAR)) {
            return PREF_CLEAR;
        } else if (key.contains(KEY_PUT_STRING)) {
            return PREF_STRING;
        } else if (key.contains(KEY_PUT_BOOLEAN)) {
            return PREF_BOOLEAN;
        } else if (key.contains(KEY_PUT_INT)) {
            return PREF_INT;
        } else if (key.contains(KEY_PUT_LONG)) {
            return PREF_LONG;
        } else if (key.contains(KEY_PUT_FLOAT)) {
            return PREF_FLOAT;
        } else if (key.contains(KEY_PUT_SET)) {
            return PREF_SET;
        }
        throw new IllegalStateException("insert unsupport!!!");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        PrefModel model = getPrefModelByUri(uri);
        if (model == null) {
            throw new IllegalArgumentException("update prefModel is null");
        }
        if (values == null) {
            throw new IllegalArgumentException(" values is null!!!");
        }

        SharedPreferences.Editor editor = getPrefs(model.getName()).edit();
        Set<String> keys = values.keySet();
        //TODO ggg clear->remove->put
        for (String k : keys) {
            switch (match(k)) {
                case PREF_BOOLEAN:
                    editor.putBoolean(parseKey(k), values.getAsBoolean(k));
                    break;
                case PREF_FLOAT:
                    editor.putFloat(parseKey(k), values.getAsFloat(k));
                    break;
                case PREF_INT:
                    editor.putInt(parseKey(k), values.getAsInteger(k));
                    break;
                case PREF_LONG:
                    editor.putLong(parseKey(k), values.getAsLong(k));
                    break;
                case PREF_STRING:
                    editor.putString(parseKey(k), values.getAsString(k));
                    break;
                case PREF_REMOVE:
                    editor.remove(parseKey(k));
                    break;
                case PREF_CLEAR:
                    editor.clear();
                    break;
                case PREF_SET:
                    editor.putStringSet(parseKey(k), PrefsCursor.Util.getSet(values.getAsByteArray(k)));
                    break;
            }

            switch (matcher.match(uri)) {
                case PREF_APPLY:
                    SharedPreferencesCompat.apply(editor);
                    break;
                default:
                    break;
            }
        }
        return 0;
    }

    private static String[] PREFERENCE_COLUMNS_MAP = {COLUMNS_KEY, COLUMNS_VALUE};
    private static String[] PREFERENCE_COLUMNS_ALL = {COLUMNS_KEY, COLUMNS_TYPE, COLUMNS_VALUE};

    private static String[] PREFERENCE_COLUMNS_EXIST = {COLUMNS_VALUE};

    private <T> PrefsCursor preferenceToCursor(String key, T value) {
        PrefsCursor matrixCursor = new PrefsCursor(PREFERENCE_COLUMNS_MAP, 1);
        matrixCursor.addRow(new Object[]{key, value});

        return matrixCursor;
    }

    private PrefsCursor preferenceExistToCursor(boolean exist) {
        PrefsCursor matrixCursor = new PrefsCursor(PREFERENCE_COLUMNS_EXIST, 1);
        PrefsCursor.RowBuilder builder = matrixCursor.newRow();
        int vInt = exist ? 1 : 0;
        builder.add(vInt);
        return matrixCursor;
    }

    private PrefsCursor preferenceMapToCursor(Map<String, ?> values) {
        PrefsCursor matrixCursor = new PrefsCursor(PREFERENCE_COLUMNS_ALL, values.size());
        Iterator<? extends Map.Entry<String, ?>> it = values.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ?> entry = it.next();
            Object[] row = new Object[3];
            row[0] = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                row[1] = PREF_STRING;
                row[2] = value;
            } else if (value instanceof Integer) {
                row[1] = PREF_INT;
                row[2] = value;
            } else if (value instanceof Long) {
                row[1] = PREF_LONG;
                row[2] = value;
            } else if (value instanceof Float) {
                row[1] = PREF_FLOAT;
                row[2] = value;
            } else if (value instanceof Boolean) {
                row[1] = PREF_BOOLEAN;
                row[2] = PrefsCursor.Util.booleanToInt((Boolean) entry.getValue());
            } else if (value instanceof Set) {
                row[1] = PREF_SET;
                row[2] = PrefsCursor.Util.setToBlob((Set) value);
            }

            matrixCursor.addRow(row);
        }
        return matrixCursor;
    }

//    private PrefsCursor preferenceMapToCursor1(Map<String, ?> values) {
//        PrefsCursor matrixCursor = new PrefsCursor(PREFERENCE_COLUMNS_ALL, values.size());
//        Iterator<? extends Map.Entry<String, ?>> it = values.entrySet().iterator();
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Bundle bundle = new Bundle();
//            while (it.hasNext()) {
//                Map.Entry<String, ?> entry = it.next();
//                putBundle(bundle, entry.getKey(), entry.getValue());
//            }
//            matrixCursor.setExtras(bundle);
//        } else {
//            while (it.hasNext()) {
//                Map.Entry<String, ?> entry = it.next();
//                Object[] row = new Object[3];
//                row[0] = entry.getKey();
//                Object value = entry.getValue();
//                if (value instanceof String) {
//                    row[1] = PREF_STRING;
//                    row[2] = value;
//                } else if (value instanceof Integer) {
//                    row[1] = PREF_INT;
//                    row[2] = value;
//                } else if (value instanceof Long) {
//                    row[1] = PREF_LONG;
//                    row[2] = value;
//                } else if (value instanceof Float) {
//                    row[1] = PREF_FLOAT;
//                    row[2] = value;
//                } else if (value instanceof Boolean) {
//                    row[1] = PREF_BOOLEAN;
//                    row[2] = PrefsCursor.Util.booleanToInt((Boolean) entry.getValue());
//                } else if (value instanceof Set) {
//                    row[1] = PREF_SET;
//                    row[2] = PrefsCursor.Util.setToBlob((Set) value);
//                }
//
//                matrixCursor.addRow(row);
//            }
//        }
//
//        return matrixCursor;
//    }
//
//    private void putBundle(Bundle bundle, String key, Object value) {
//        if (value instanceof String) {
//            bundle.putString(key, (String) value);
//        } else if (value instanceof Boolean) {
//            bundle.putBoolean(key, (Boolean) value);
//        } else if (value instanceof Integer) {
//            bundle.putInt(key, (Integer) value);
//        } else if (value instanceof Long) {
//            bundle.putLong(key, (Long) value);
//        } else if (value instanceof Float) {
//            bundle.putFloat(key, (Float) value);
//        } else if (value instanceof Set) {
//            HashSet obj = (HashSet) value;
//            String[] array = new String[obj.size()];
//            obj.toArray(array);
//            bundle.putStringArray(key, array);
//        }
//    }

    private static PrefModel getPrefModelByUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("getPrefModelByUri uri is wrong : " + uri);
        }

        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() == 2) {
            return new PrefModel(pathSegments.get(1), "");
        } else if (pathSegments.size() == 3) {
            return new PrefModel(pathSegments.get(1), pathSegments.get(2));
        } else {
            throw new IllegalArgumentException("getPrefModelByUri uri is wrong : " + uri);
        }
    }

    private static class PrefModel {
        String name;
        String key;

        public PrefModel(String name, String key) {
            this.name = name;
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }
    }

}
