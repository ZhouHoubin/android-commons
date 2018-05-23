package java.io;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Files {
    /**
     * 排序方式
     * ACS:升序
     * DESC:降序
     */
    public enum Sort {
        ACS_DATE, DESC_DATE, ACS_LENGTH, DESC_LENGTH, ACS_NAME, DESC_NAME
    }

    /**
     * 获取后缀名
     *
     * @param file 文件
     * @return 后缀名
     */
    public static String getSuffix(String file) {
        String suffix = "";
        if (isExist(file)) {
            int index = file.lastIndexOf(".");
            if (index != -1) {
                suffix = file.substring(index);
            }
        }
        return suffix;
    }

    /**
     * 获取后缀名
     *
     * @param file 文件
     * @return 后缀名, 不包含.
     */
    public static String getSuffix(File file) {
        String suffix = "";
        if (file != null) {
            suffix = getSuffix(file.getPath());
        }
        return suffix;
    }

    /**
     * 创建文件
     *
     * @param file 文件路径
     * @return 是否创建成功
     */
    public static boolean create(String file) {
        boolean result = false;
        if (!Strings.isEmpty(file)) {
            File realFile = new File(file);
            if (!realFile.getParentFile().exists()) {
                boolean mdResult = realFile.getParentFile().mkdirs();
            }
            try {
                result = realFile.createNewFile() /*&& mdResult*/;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;
    }

    /**
     * 创建文件
     *
     * @param file 文件
     * @return 是否创建成功
     */
    public static boolean create(File file) {
        boolean result = false;
        if (file != null) {
            result = create(file.getPath());
        }
        return result;
    }

    /**
     * 搜索文件
     *
     * @param file 文件夹
     * @param key  关键字
     * @return 文件列表
     */
    public static List<File> search(File file, String key) {
        return search(file, null, key);
    }

    /**
     * 列出文件
     *
     * @param file 目录
     * @param sort 排序
     * @return 文件列表
     */
    public static List<File> list(File file, Sort sort) {
        return search(file, sort, "");
    }

    /**
     * 搜索文件
     *
     * @param file 目录
     * @param sort 排序
     * @param key  关键字
     * @return 目录文件
     */
    public static List<File> search(File file, Sort sort, final String key) {
        List<File> files = new ArrayList<>();
        if (isExist(file)) {
            if (file.isDirectory()) {
                File[] searchList = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File f, String s) {
                        if (s.contains(key)) {
                            return true;
                        }
                        return false;
                    }
                });
                if (searchList != null) {
                    if (sort != null) {
                        //排序
                        switch (sort) {
                            case ACS_DATE:
                                Arrays.sort(searchList, new Comparator<File>() {
                                    @Override
                                    public int compare(File o1, File o2) {
                                        if (o1.isDirectory() && o2.isFile()) {
                                            return -1;
                                        }

                                        if (o1.isFile() && o2.isDirectory()) {
                                            return 1;
                                        }

                                        if (o1.lastModified() > o2.lastModified()) {
                                            return 1;
                                        } else {
                                            return -1;
                                        }
                                    }
                                });
                                break;
                            case DESC_DATE:
                                Arrays.sort(searchList, new Comparator<File>() {
                                    @Override
                                    public int compare(File o1, File o2) {
                                        if (o1.isDirectory() && o2.isFile()) {
                                            return -1;
                                        }

                                        if (o1.isFile() && o2.isDirectory()) {
                                            return 1;
                                        }

                                        if (o1.lastModified() < o2.lastModified()) {
                                            return 1;
                                        } else {
                                            return -1;
                                        }
                                    }
                                });
                                break;
                            case ACS_LENGTH:
                                Arrays.sort(searchList, new Comparator<File>() {
                                    @Override
                                    public int compare(File o1, File o2) {
                                        if (o1.isDirectory() && o2.isFile()) {
                                            return -1;
                                        }

                                        if (o1.isFile() && o2.isDirectory()) {
                                            return 1;
                                        }

                                        if (o1.length() > o2.length()) {
                                            return 1;
                                        } else {
                                            return -1;
                                        }
                                    }
                                });
                                break;
                            case DESC_LENGTH:
                                Arrays.sort(searchList, new Comparator<File>() {
                                    @Override
                                    public int compare(File o1, File o2) {
                                        if (o1.isDirectory() && o2.isFile()) {
                                            return -1;
                                        }

                                        if (o1.isFile() && o2.isDirectory()) {
                                            return 1;
                                        }

                                        if (o1.length() < o2.length()) {
                                            return 1;
                                        } else {
                                            return -1;
                                        }
                                    }
                                });
                                break;
                            case ACS_NAME:
                            case DESC_NAME:
                                Arrays.sort(searchList, new Comparator<File>() {
                                    @Override
                                    public int compare(File o1, File o2) {
                                        if (o1.isDirectory() && o2.isFile()) {
                                            return -1;
                                        }

                                        if (o1.isFile() && o2.isDirectory()) {
                                            return 1;
                                        }

                                        return o1.getName().compareTo(o2.getName());
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    }
                    files.addAll(Arrays.asList(searchList));
                }
            }
        }
        return files;
    }

    public static boolean isExist(String file) {
        if (Strings.isEmpty(file)) {
            return false;
        } else {
            File realFile = new File(file);
            return realFile.exists();
        }
    }

    /**
     * 是否存在
     *
     * @param file 文件
     * @return 是否存在
     */
    public static boolean isExist(File file) {
        return file != null && isExist(file.getPath());
    }

    /**
     * 获取文件大小
     *
     * @param file 文件
     * @return 长度
     */
    public static long getLength(File file) {
        long length = -1;
        if (isExist(file)) {
            length = file.length();
        }
        return length;
    }

    /**
     * 获取长度
     *
     * @param file 文件
     * @return 长度
     */
    public static long getLength(String file) {
        if (isExist(file)) {
            return getLength(new File(file));
        } else {
            return -1;
        }
    }

    /**
     * 获取文件夹大小
     *
     * @param file 文件夹
     * @return 字节单位
     */
    public static long getDirSize(String file) {
        long totalSize = -1;
        if (isExist(file)) {
            File realFile = new File(file);
            File[] listFiles = realFile.listFiles();
            assert listFiles != null;
            for (File listFile : listFiles) {
                if (listFile.isFile()) {
                    totalSize += listFile.length();
                } else {
                    totalSize += getDirSize(listFile.getPath());
                }
            }
        }
        return totalSize;
    }

    /**
     * 获取文件夹可读大小
     *
     * @param file 文件夹
     * @return 大小, 可读
     */
    public static String getReadableDirSize(String file) {
        long size = getDirSize(file);
        return getFormatSize(size);
    }

    /**
     * 格式化单位
     *
     * @param size size
     * @return size
     */
    public static String getFormatSize(long size) {
        float kiloByte = size / 1024f;

        float megaByte = kiloByte / 1024f;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Float.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }

        float gigaByte = megaByte / 1024f;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Float.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        float teraBytes = gigaByte / 1024f;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Float.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);

        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

    /**
     * 获取文件MD5
     *
     * @param file 文件
     * @return md5
     */
    public static String getMd5(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    /**
     * 写文本到文件
     *
     * @param dst  目标文件
     * @param text 内容
     */
    public static void write(File dst, String text) {
        if (!isExist(dst)) {
            create(dst);
        }
        write(getFileOutputStream(dst), text);
    }

    /**
     * 读取文件
     *
     * @param file 文件
     * @return 文件内容
     */
    public static String read(File file) {
        StringBuilder builder = new StringBuilder();
        if (isExist(file)) {
            InputStreamReader inputStreamReader = null;
            try {
                inputStreamReader = new InputStreamReader(new FileInputStream(file));
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line);
                    builder.append("\r\n");
                }
                bufferedReader.close();
                inputStreamReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return builder.toString();
    }

    /**
     * 读取文件
     *
     * @param file 文件
     * @param line 行号,最小1
     * @return 文件内容
     */
    public static String readLine(File file, int line) {
        line--;
        String text = read(file);
        if (!Strings.isEmpty(text)) {
            String[] lines = text.split("\r\n");
            if (line < lines.length) {
                return lines[line];
            }
        }
        return "";
    }

    /**
     * 获取文件输出流
     *
     * @param file 文件
     * @return 输出流
     */
    public static FileOutputStream getFileOutputStream(File file) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return stream;
    }

    /**
     * 文本写入流
     *
     * @param outputStream 输出流
     * @param text         文本
     */
    public static void write(OutputStream outputStream, String text) {
        try {
            outputStream.write(text.getBytes());
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
