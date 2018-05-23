package java.lang;

public class Strings {

    public static boolean isEmpty(String string) {
        boolean isEmpty = false;
        if (string == null) {
            isEmpty = true;
        } else if ("".equals(string)) {
            isEmpty = true;
        } else if ("null".equalsIgnoreCase(string)) {
            isEmpty = true;
        }
        return isEmpty;
    }
}
