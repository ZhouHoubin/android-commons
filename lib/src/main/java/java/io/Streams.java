package java.io;

public class Streams {

    public static String stream2String(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();
        if (inputStream != null) {
            try {
                int len = 0;
                byte[] bytes = new byte[1024];
                while ((len = inputStream.read(bytes)) != -1) {
                    builder.append(new String(bytes, 0, len));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return builder.toString();
    }

    public static void write(OutputStream outputStream, String data) {
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void append(File file, CharSequence charSequence) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.append(charSequence);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
