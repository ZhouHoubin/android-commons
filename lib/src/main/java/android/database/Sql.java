package android.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * 1.初始化<br/>
 * <pre>
 *      String dbPath = getApplicationContext().getDatabasePath("student.db").getPath();
 *      Sql.DataConfig dataConfig  = new Sql.DataConfig(dbPath);
 *      dataConfig.setVersion(1);
 *      Sql.init(getApplicationContext(), dataConfig);
 *  </pre>
 * <br/>2.保存<br/>
 * <pre>
 * Student student = new Student();
 * student.setId(0);
 * student.setName("aaa");
 * student.setAge(19);
 * student.setNumber(101010);
 * student.setSex("男");
 * Sql.get().bind(Student.class).save(student);
 *  </pre>
 *
 * @author z.houbin
 */
public class Sql {
    private static boolean loggable = false;

    private static Sql ORM;

    private TabInfo mTabInfo;

    private SQLiteHelper mHelper;

    private TableManager mTableManager;

    private DataConfig mDataConfig;

    private ClsParser parser = new ClsParser();

    private Sql(Context context, DataConfig dataConfig) {
        Context mContext = context;
        mDataConfig = dataConfig;
        loggable = dataConfig.isLoggable();
        mHelper = new SQLiteHelper(context, dataConfig);
    }

    public static void init(Context context, File db) {
        if (ORM == null) {
            DataConfig dataConfig = new DataConfig(db);
            ORM = new Sql(context, dataConfig);
        }
    }

    public static void init(Context context, DataConfig config) {
        if (ORM == null) {
            ORM = new Sql(context, config);
        }
    }

    public static Sql get() {
        if (ORM == null) {
            throw new IllegalStateException("init error");
        }
        return ORM;
    }

    public TableManager bind(Class<?> cls) {
        mTabInfo = parser.parseTable(cls);
        mTableManager = new TableManager(mHelper, mTabInfo);
        return mTableManager;
    }

    public SQLiteDatabase getWritableDatabase() {
        return mHelper.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        return mHelper.getReadableDatabase();
    }

    public SQLiteHelper getSQLiteHelper() {
        return mHelper;
    }

    public TableManager getTableManager() {
        return mTableManager;
    }

    //TODO ClsParser
    public final class ClsParser {
        /**
         * 解析表
         *
         * @param cls bean对象
         * @return 表信息
         */
        private TabInfo parseTable(Class<?> cls) {
            TabInfo tabInfo = new TabInfo();
            TableAnnotation clsAnnotation = cls.getAnnotation(TableAnnotation.class);
            String tabName = "";
            if (clsAnnotation == null) {
                //没有注解信息
                tabName = cls.getSimpleName();
            } else {
                tabName = clsAnnotation.value();
            }
            tabInfo.setTabName(tabName);
            tabInfo.setClsName(cls.getCanonicalName());
            try {
                tabInfo.setClsInstance(cls.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }

            Field[] declaredFields = cls.getDeclaredFields();
            List<TabInfo.ColumnInfo> columnInfoList = new ArrayList<>();
            for (Field field : declaredFields) {
                TabInfo.ColumnInfo columnInfo = new TabInfo().new ColumnInfo();
                String fieldName = field.getName();
                //可能会有特殊名字 ,$change
                if (!validFieldName(fieldName)) {
                    continue;
                }
                String fieldType = field.getType().getName();

                columnInfo.setFieldName(fieldName);
                columnInfo.setFieldType(field.getType());
                Annotation annotation = null;
                if ((annotation = field.getAnnotation(ColumnAnnotation.class)) != null) {
                    String columnName = field.getAnnotation(ColumnAnnotation.class).value();
                    columnInfo.setColumnName(columnName);
                    if (columnName.equalsIgnoreCase(Constant._ID) || columnName.equalsIgnoreCase(Constant.ID)) {
                        //continue;
                    }
                    columnInfo.setColumnType(transformType(fieldType));
                    columnInfoList.add(columnInfo);
                }
            }

            if (columnInfoList.size() == 0) {
                //没有注解信息,直接解析所有变量
                for (Field field : declaredFields) {
                    TabInfo.ColumnInfo columnInfo = new TabInfo().new ColumnInfo();

                    String fieldName = field.getName();
                    //可能会有特殊名字 ,$change
                    if (validFieldName(fieldName)) {
                        String fieldType = field.getType().getName();

                        columnInfo.setFieldName(fieldName);
                        columnInfo.setFieldType(field.getType());
                        columnInfo.setColumnName(fieldName);
                        columnInfo.setColumnType(transformType(fieldType));
                        columnInfoList.add(columnInfo);
                    }
                }
            }

            tabInfo.setColumnList(columnInfoList);
            return tabInfo;
        }

        /**
         * 判断变量名是否有效
         *
         * @param name 变量名
         * @return true 有效
         */
        private boolean validFieldName(String name) {
            return name.matches("[a-zA-Z0-9_]+");
        }

        /**
         * 获取bean对象键值数据
         *
         * @param obj   bean对象
         * @param infos 列
         * @return 键值对
         */
        private HashMap<String, String> parseTable(Object obj, List<TabInfo.ColumnInfo> infos) {
            HashMap<String, String> values = new HashMap<>();

            List<String> columns = new ArrayList<>();
            for (int i = 0; i < infos.size(); i++) {
                columns.add(infos.get(i).getColumnName());
            }
            Class cls = obj.getClass();
            Field[] declaredFields = cls.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);

                Object columnName = field.getAnnotation(ColumnAnnotation.class).value();
                if (columns.contains(columnName.toString())) {
                    columns.remove(columnName.toString());
                    try {
                        values.put(columnName + "", field.get(obj) + "");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            return values;
        }

        /**
         * 获取ben 所有对象值
         *
         * @param obj   bean对象
         * @param infos 列
         * @return 对象值列表
         */
        private String[] getFieldValueFromClass(Object obj, List<TabInfo.ColumnInfo> infos) {
            List<String> values = new ArrayList<>();

            List<String> columns = new ArrayList<>();
            for (int i = 0; i < infos.size(); i++) {
                columns.add(infos.get(i).getColumnName());
            }
            Class cls = obj.getClass();
            Field[] declaredFields = cls.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                if (field.getAnnotation(ColumnAnnotation.class) != null) {
                    Object columnName = field.getAnnotation(ColumnAnnotation.class).value();
                    if (!validFieldName(field.getName())) {
                        continue;
                    }
                    if (columns.contains(String.valueOf(columnName))) {
                        columns.remove(String.valueOf(columnName));
                        try {
                            values.add(field.get(obj) + "");
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            values.add("");
                        }
                    }
                }
            }
            if (values.size() == 0) {
                //没有任何注解变量
                for (Field field : declaredFields) {
                    field.setAccessible(true);
                    if (!validFieldName(field.getName())) {
                        continue;
                    }
                    try {
                        Object val = field.get(obj);
                        if (val != null) {
                            values.add(val + "");
                        } else {
                            values.add("");
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            return values.toArray(new String[]{});
        }

        /**
         * 获取类对应的数据库id
         *
         * @param obj   ben类
         * @param infos 列
         * @return id
         */
        String getFieldIdFromClass(Object obj, List<TabInfo.ColumnInfo> infos) {
            HashMap<String, String> map = parseTable(obj, infos);
            if (map.containsKey(Constant._ID)) {
                return map.get(Constant._ID);
            } else if (map.containsKey(Constant.ID)) {
                return map.get(Constant.ID);
            }
            return "-1";
        }

        /**
         * java 数据类型转 sqlite 数据类型
         *
         * @param type Java 数据类型
         * @return sqlite 数据类型
         */
        private String transformType(String type) {
            String value;
            if (type.contains(Constant.TYPE_STRING)) {
                value = " text ";
            } else if (type.contains(Constant.TYPE_INT)) {
                value = " integer ";
            } else if (type.contains(Constant.TYPE_BOOLEAN)) {
                value = " boolean ";
            } else if (type.contains(Constant.TYPE_FLOAT)) {
                value = " float ";
            } else if (type.contains(Constant.TYPE_DOUBLE)) {
                value = " double ";
            } else if (type.contains(Constant.TYPE_CHAR)) {
                value = " varchar ";
            } else if (type.contains(Constant.TYPE_LONG)) {
                value = " long ";
            } else {
                value = " text ";
            }
            return value;
        }
    }

    //TODO SQLiteHelper
    public final class SQLiteHelper extends SQLiteOpenHelper {

        SQLiteHelper(Context context, DataConfig dataConfig) {
            super(context, dataConfig.getFile().getPath(), null, dataConfig.getVersion());
        }

        public SQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    //TODO TableManager
    public final class TableManager {
        private SQLiteHelper mHelper;
        private TabInfo mTabInfo;

        TableManager(SQLiteHelper helper, TabInfo info) {
            mHelper = helper;
            mTabInfo = info;

            openOrCreateDatabase();
        }

        private void openOrCreateDatabase() {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            String tabName = mTabInfo.getTabName();

            if (!isTableExist(tabName)) {
                createTable();
            } else {
                //判断是否需要更新字段
                List<String> columns = getTabColumns(tabName);
                List<TabInfo.ColumnInfo> updateColumns = new ArrayList<>();
                for (int i = 0; i < mTabInfo.getColumnList().size(); i++) {
                    TabInfo.ColumnInfo columnInfo = mTabInfo.getColumnList().get(i);
                    if (columnInfo.getColumnName().contains("_id")) {
                        continue;
                    }
                    if (!columns.contains(columnInfo.getColumnName())) {
                        updateColumns.add(columnInfo);
                    }
                }
                if (updateColumns.size() != 0) {
                    updateTabColumns(updateColumns);
                }
            }
        }

        /**
         * 更新表字段
         *
         * @param updateColumns 字段
         */
        private void updateTabColumns(List<TabInfo.ColumnInfo> updateColumns) {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            StringBuilder sql = new StringBuilder("ALTER TABLE ");
            sql.append(mTabInfo.getTabName());

            for (int i = 0; i < mTabInfo.getColumnList().size(); i++) {
                TabInfo.ColumnInfo columnInfo = mTabInfo.getColumnList().get(i);
                if (columnInfo.getColumnName().contains("_id")) {
                    continue;
                }
                sql.append(" add ");
                sql.append(columnInfo.getColumnName());
                sql.append(" ");
                sql.append(columnInfo.getColumnType());
                sql.append(" ");
                if (columnInfo.isPrimary()) {
                    sql.append("PRIMARY KEY ");
                }
                if (columnInfo.isNotNull()) {
                    sql.append("NOT NULL ");
                }
                if (columnInfo.isAutoInCrement()) {
                    sql.append("AUTOINCREMENT ");
                }
                if (i + 1 != mTabInfo.getColumnList().size()) {
                    sql.append(",");
                }
            }

            if (loggable) {
                System.out.println("update column " + sql.toString());
            }
            db.execSQL(sql.toString());
        }

        /**
         * 创建表
         */
        private void createTable() {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            sql.append(mTabInfo.getTabName());
            sql.append(" ( ");

            for (int i = 0; i < mTabInfo.getColumnList().size(); i++) {
                TabInfo.ColumnInfo columnInfo = mTabInfo.getColumnList().get(i);
                if (columnInfo.getColumnName().contains("_id")) {
                    continue;
                }
                sql.append(columnInfo.getColumnName());
                sql.append(" ");
                sql.append(columnInfo.getColumnType());
                sql.append(" ");
                if (columnInfo.isPrimary()) {
                    sql.append("PRIMARY KEY ");
                }
                if (columnInfo.isNotNull()) {
                    sql.append("NOT NULL ");
                }
                if (columnInfo.isAutoInCrement()) {
                    sql.append("AUTOINCREMENT ");
                }
                if (i + 1 != mTabInfo.getColumnList().size()) {
                    sql.append(",");
                }
            }

            if (!sql.toString().contains("PRIMARY")) {
                //没有主键,创建_id
                sql.insert(sql.indexOf("(") + 1, "_id INTEGER PRIMARY KEY AUTOINCREMENT , ");
            }

            sql.append(")");
            db.execSQL(sql.toString());
        }

        /**
         * 获取表所有字段
         *
         * @param tabName 表名
         * @return TabColumns
         */
        private List<String> getTabColumns(String tabName) {
            String sql = String.format(Locale.CHINA, "select * from %s limit 0", tabName);
            Cursor cursor = mHelper.getWritableDatabase().rawQuery(sql, new String[]{});
            List<String> columns = Arrays.asList(cursor.getColumnNames());
            cursor.close();
            return columns;
        }

        /**
         * 表是否存在
         *
         * @param tabName 表名
         * @return true 存在
         */
        private boolean isTableExist(String tabName) {
            String sql = String.format(Locale.CHINA, "SELECT name FROM sqlite_sequence WHERE name like '%s'", tabName);
            Cursor cursor = mHelper.getReadableDatabase().rawQuery(sql, new String[]{});
            int count = cursor.getCount();
            cursor.close();
            return count != 0;
        }

        private SQLiteDatabase getWritableDatabase() {
            return mHelper.getWritableDatabase();
        }


        //根据自定义sql查找
        private <T> List<T> find(String sql, String[] args) {
            List<T> findList = new ArrayList<>();
            SQLiteDatabase db = mHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sql, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (cursor != null && cursor.moveToFirst()) {
                for (int j = 0; j < cursor.getCount(); j++) {
                    List<TabInfo.ColumnInfo> columnList = mTabInfo.getColumnList();

                    Object instance = null;
                    Class<?> tab = null;
                    try {
                        instance = Class.forName(mTabInfo.getClsName()).newInstance();
                        tab = instance.getClass();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (tab == null) {
                        continue;
                    }
                    for (int i = 0; i < columnList.size(); i++) {
                        TabInfo.ColumnInfo columnInfo = columnList.get(i);
                        String columnName = columnInfo.getColumnName();
                        try {

                            Object val = null;

                            if (columnInfo.getFieldType() == Integer.class || columnInfo.getFieldType() == int.class) {
                                val = cursor.getInt(cursor.getColumnIndex(columnName));
                            } else if (columnInfo.getFieldType() == String.class) {
                                val = cursor.getString(cursor.getColumnIndex(columnName));
                            }

                            Field field = tab.getDeclaredField(columnInfo.getFieldName());
                            field.setAccessible(true);
                            field.set(instance, val);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    findList.add((T) instance);
                    cursor.moveToNext();
                }
                cursor.close();
            }
            return findList;
        }

        //查找所有
        public <T> List<T> find() {
            return find("SELECT * FROM  " + mTabInfo.getTabName(), new String[]{});
        }

        //根据id查找
        public <T> T findById(long id) {
            return findById(String.valueOf(id));
        }

        //根据条件查找
        public <T> List<T> find(WhereBuilder builder) {
            return find("SELECT * FROM  " + mTabInfo.getTabName() + builder.toString(), new String[]{});
        }

        //根据id查找
        public <T> T findById(String id) {
            String sql = String.format(Locale.CHINA, "SELECT * FROM %s WHERE _id = ? ", mTabInfo.getTabName());
            List<Object> list = find(sql, new String[]{id});
            if (list.size() != 0) {
                return (T) list.get(0);
            } else {
                return null;
            }
        }

        //保存列表
        public int save(List<?> list) {
            int saveSuccess = 0;
            if (list != null) {
                String sql = getInsertSql();
                SQLiteDatabase db = mHelper.getWritableDatabase();
                SQLiteStatement statement = db.compileStatement(sql);
                db.beginTransaction();
                try {
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            String[] args = parser.getFieldValueFromClass(list.get(i), mTabInfo.getColumnList());
                            statement.bindAllArgsAsStrings(args);
                            long code = statement.executeInsert();
                            saveSuccess++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                    saveSuccess = 0;
                }
                db.endTransaction();
            }
            return saveSuccess;
        }

        //保存单个
        public int save(Object obj) {
            List<Object> saveList = new ArrayList<>();
            saveList.add(obj);
            return save(saveList);
        }

        //删除多个
        public void delete(List<?> deleteList) {
            if (writable() && deleteList != null) {
                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    String sql = getDeleteSql();
                    for (Object deleteObj : deleteList) {
                        String[] args = parser.getFieldValueFromClass(deleteObj, mTabInfo.getColumnList());
                        db.execSQL(sql, args);
                    }
                    db.setTransactionSuccessful();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                db.endTransaction();
                db.close();
            }
        }

        //删除单个
        public void delete(Object deleteObj) {
            if (writable() && deleteObj != null) {
                SQLiteDatabase db = getWritableDatabase();
                String[] args = parser.getFieldValueFromClass(deleteObj, mTabInfo.getColumnList());
                String sql = getDeleteSql();
                db.execSQL(sql, args);
            }
        }

        //删除单个
        public void deleteById(String id) {
            if (!TextUtils.isEmpty(id)) {
                SQLiteDatabase db = getWritableDatabase();
                String sql = String.format("DELETE FROM %s  WHERE _id = %s", mTabInfo.getTabName(), id);
                db.execSQL(sql);
            }
        }

        //删除单个
        public void deleteById(Object deleteObj) {
            if (deleteObj != null) {
                SQLiteDatabase db = getWritableDatabase();
                String id = parser.getFieldIdFromClass(deleteObj, mTabInfo.getColumnList());
                String sql = String.format("DELETE FROM %s  WHERE _id = %s", mTabInfo.getTabName(), id);
                db.execSQL(sql);
            }
        }


        //更新单个//TODO 待优化
        public void update(Object updateObj) {
            deleteById(updateObj);
            save(updateObj);
        }

        //更新列表//TODO 待优化
        public void update(List<?> updateList) {
            if (writable() && updateList != null) {
                getWritableDatabase().beginTransaction();
                try {
                    for (int i = 0; i < updateList.size(); i++) {
                        deleteById(updateList.get(i));
                        save(updateList.get(i));
                    }
                    getWritableDatabase().setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                getWritableDatabase().endTransaction();
            }
        }

        //删除表
        public void deleteTable() {
            if (writable()) {
                SQLiteDatabase db = getWritableDatabase();
                db.execSQL("DROP TABLE " + mTabInfo.getTabName());
            }
        }

        //清空表
        public void clearTable() {
            if (writable()) {
                SQLiteDatabase db = getWritableDatabase();
                db.execSQL("delete from " + mTabInfo.getTabName());
                db.execSQL("update sqlite_sequence SET seq = 0 where name = ? ", new String[]{" mTabInfo.getTabName()"});
            }
        }

        //获取类对应插入语句
        private String getInsertSql() {
            StringBuilder sql = new StringBuilder("INSERT INTO ");
            sql.append(mTabInfo.getTabName());
            sql.append("  ( ");

            for (int i = 0; i < mTabInfo.getColumnList().size(); i++) {
                TabInfo.ColumnInfo columnInfo = mTabInfo.getColumnList().get(i);
                sql.append(columnInfo.getColumnName());
                sql.append(" ");
                //sql.append(columnInfo.getColumnType());
                //sql.append(" ");
                if (columnInfo.isPrimary()) {
                    sql.append("PRIMARY KEY ");
                }
                if (columnInfo.isNotNull()) {
                    sql.append("NOT NULL ");
                }
                if (columnInfo.isAutoInCrement()) {
                    sql.append("AUTO_INCREMENT ");
                }
                if (i + 1 != mTabInfo.getColumnList().size()) {
                    sql.append(",");
                } else {
                    sql.append(")");
                }
            }

            sql.append("VALUES(");

            for (int i = 0; i < mTabInfo.getColumnList().size(); i++) {
                sql.append("?");
                if (i + 1 != mTabInfo.getColumnList().size()) {
                    sql.append(",");
                } else {
                    sql.append(")");
                }
            }
            return sql.toString();
        }

        //获取类对应删除语句
        private String getDeleteSql() {
            StringBuilder sql = new StringBuilder("DELETE FROM ");
            sql.append(mTabInfo.getTabName());
            sql.append("  where ");

            for (int i = 0; i < mTabInfo.getColumnList().size(); i++) {
                TabInfo.ColumnInfo columnInfo = mTabInfo.getColumnList().get(i);
                sql.append(columnInfo.getColumnName());
                sql.append(" = ? ");
                if (i + 1 != mTabInfo.getColumnList().size()) {
                    sql.append(" and ");
                }
            }
            return sql.toString();
        }

        private boolean writable() {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            return db.isOpen() && !db.isReadOnly();
        }
    }

    //TODO TabInfo
    public final class TabInfo {
        private String tabName;

        private String clsName;

        private Object clsInstance;

        private List<ColumnInfo> columnList;

        public Object getClsInstance() {
            return clsInstance;
        }

        public void setClsInstance(Object clsInstance) {
            this.clsInstance = clsInstance;
        }

        public String getClsName() {
            return clsName;
        }

        public void setClsName(String clsName) {
            this.clsName = clsName;
        }

        public String getTabName() {
            return tabName;
        }

        public void setTabName(String tabName) {
            this.tabName = tabName;
        }

        public List<ColumnInfo> getColumnList() {
            return columnList;
        }

        public void setColumnList(List<ColumnInfo> columnList) {
            this.columnList = columnList;
        }

        //列信息
        public final class ColumnInfo {
            private String columnName;
            private String columnType;
            private String fieldName;
            private Class<?> fieldType;
            private boolean autoInCrement;
            private boolean notNull;
            private boolean primary;

            public Class<?> getFieldType() {
                return fieldType;
            }

            public void setFieldType(Class<?> fieldType) {
                this.fieldType = fieldType;
            }

            public String getFieldName() {
                return fieldName;
            }

            public void setFieldName(String fieldName) {
                this.fieldName = fieldName;
            }

            public String getColumnName() {
                return columnName;
            }

            public void setColumnName(String columnName) {
                this.columnName = columnName;
            }

            public String getColumnType() {
                return columnType;
            }

            public void setColumnType(String columnType) {
                this.columnType = columnType;
            }

            public boolean isAutoInCrement() {
                return autoInCrement;
            }

            public void setAutoInCrement(boolean autoInCrement) {
                this.autoInCrement = autoInCrement;
            }

            public boolean isNotNull() {
                return notNull;
            }

            public void setNotNull(boolean notNull) {
                this.notNull = notNull;
            }

            public boolean isPrimary() {
                return primary;
            }

            public void setPrimary(boolean primary) {
                this.primary = primary;
            }
        }
    }

    //TODO DataConfig
    public static class DataConfig {
        private File mFile;
        private int version = 1;
        private boolean loggable;

        public DataConfig(File dbFile) {
            mFile = dbFile;
        }

        public DataConfig(String dbFilePath) {
            mFile = new File(dbFilePath);
        }

        public File getFile() {
            return mFile;
        }

        public boolean isLoggable() {
            return loggable;
        }

        public void setLoggable(boolean loggable) {
            this.loggable = loggable;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    //TODO Constant
    private static final class Constant {
        static final String _ID = "_id";
        static final String ID = "id";

        static final String TYPE_STRING = "String";
        static final String TYPE_INT = "int";
        static final String TYPE_BOOLEAN = "boolean";
        static final String TYPE_FLOAT = "float";
        static final String TYPE_DOUBLE = "double";
        static final String TYPE_CHAR = "char";
        static final String TYPE_LONG = "long";
    }

    //TODO WhereBuilder
    public static class WhereBuilder {
        private StringBuilder sqlBuilder = new StringBuilder(" WHERE ");

        public WhereBuilder and() {
            sqlBuilder.append(" and ");
            return this;
        }

        public WhereBuilder or() {
            sqlBuilder.append(" OR ");
            return this;
        }

        public WhereBuilder not() {
            sqlBuilder.append(" NOT ");
            return this;
        }

        public WhereBuilder whereEquals(String column, String value) {
            sqlBuilder.append(column);
            sqlBuilder.append(" = ");
            sqlBuilder.append("'");
            sqlBuilder.append(value);
            sqlBuilder.append("'");
            return this;
        }

        public WhereBuilder whereNotEquals(String column, String value) {
            sqlBuilder.append(column);
            sqlBuilder.append(" != ");
            sqlBuilder.append("'");
            sqlBuilder.append(value);
            sqlBuilder.append("'");
            sqlBuilder.append(" ");
            return this;
        }

        public WhereBuilder whereLike(String column, String like) {
            sqlBuilder.append(column);
            sqlBuilder.append(" like ");
            sqlBuilder.append("'");
            sqlBuilder.append(like);
            sqlBuilder.append("'");
            sqlBuilder.append(" ");
            return this;
        }

        public WhereBuilder whereIsNull(String column) {
            sqlBuilder.append(column);
            sqlBuilder.append(" IS NULL ");
            return this;
        }

        public WhereBuilder whereIsNotNull(String column) {
            sqlBuilder.append(column);
            sqlBuilder.append(" IS NOT NULL ");
            return this;
        }

        public WhereBuilder whereBetweenAnd(String column, String value1, String value2) {
            sqlBuilder.append(column);
            sqlBuilder.append(" BETWEEN ");
            sqlBuilder.append("'");
            sqlBuilder.append(value1);
            sqlBuilder.append("'");
            sqlBuilder.append(" ");
            and();
            sqlBuilder.append("'");
            sqlBuilder.append(value2);
            sqlBuilder.append("'");
            sqlBuilder.append(" ");
            return this;
        }

        public WhereBuilder sql(String sql) {
            sqlBuilder.append(" ");
            sqlBuilder.append(sql);
            sqlBuilder.append(" ");
            return this;
        }

        @Override
        public String toString() {
            return sqlBuilder.toString();
        }
    }

    //TODO 字段注解
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ColumnAnnotation {
        String value();
    }

    //TODO 表信息注解
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TableAnnotation {
        String value() default "nul";
    }
}