package org.rabbitpig.durian.mybatis;

import java.util.HashMap;
import java.util.Map;

public class ColumnData {

    private String dbName;

    private String dbType;

    private String javaName;

    private String javaType;

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    // TODO: 数据库字段类型 -> Java类型
    public final static Map<String, String> TYPE_MAPPINGS = new HashMap<String, String>() {
        {
            put("int", "Integer");
            put("bigint", "Long");
            put("integer", "Integer");
            put("varchar", "String");
            put("datetime", "Date");
            put("date", "Date");
            put("text", "String");
            put("float", "Float");
            put("bigint unsigned", "Long");
            put("float unsigned", "Double");
        }
    };

    // 字符串转换
    public static String toClassName(String nameInDB) {
        StringBuilder sb = new StringBuilder(nameInDB.toLowerCase());
        toUpperChar(sb, 0);
        int i = -1;
        while ((i = sb.indexOf("_")) != -1) {
            deleteChar(sb, sb.indexOf("_"));
            toUpperChar(sb, i);
        }
        return sb.toString();
    }

    /**
     * Input:abc_de -> Output:abcDe
     *
     * @param nameInDB
     * @return
     */
    public static String toClassFormatName(String nameInDB) {
        StringBuilder sb = new StringBuilder(nameInDB.toLowerCase());
        int i = -1;
        while ((i = sb.indexOf("_")) != -1) {
            deleteChar(sb, sb.indexOf("_"));
            toUpperChar(sb, i);
        }
        return sb.toString();
    }

    /**
     * Input:abc_de -> Output:abcDe
     *
     * @param nameInDB
     * @return
     */
    public static String toPropertyName(String nameInDB) {
        StringBuilder sb = new StringBuilder(nameInDB.toLowerCase());
        int i = -1;
        while ((i = sb.indexOf("_")) != -1) {
            deleteChar(sb, sb.indexOf("_"));
            toUpperChar(sb, i);
        }
        return sb.toString();
    }

    /**
     * Input:abc_de -> Output:AbcDe
     *
     * @param nameInDB
     * @return
     */
    public static String toPropertyFormatName(String nameInDB) {
        StringBuilder sb = new StringBuilder(nameInDB.toLowerCase());
        toUpperChar(sb, 0);
        int i = -1;
        while ((i = sb.indexOf("_")) != -1) {
            deleteChar(sb, sb.indexOf("_"));
            toUpperChar(sb, i);
        }
        return sb.toString();
    }

    public static void toUpperChar(StringBuilder sb, int index) {
        if (index < sb.length()) {
            sb.replace(index, index + 1, String.valueOf(sb.charAt(index)).toUpperCase());
        }
    }

    public static void deleteChar(StringBuilder sb, int index) {
        if (index < sb.length()) {
            sb.delete(index, index + 1);
        }
    }
}
