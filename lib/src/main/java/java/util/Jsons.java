package java.util;

public class Jsons {
    public static String format(String json) {
        json = json.replaceAll("(?!\\}]),", ",\r\n");
        json = json.replaceAll("\\{", "\\{\r\n");
        json = json.replaceAll("\\}", "\r\n\\}\r\n");
        String[] lines = json.split("\r\n");
        int tab = 0;
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line.contains("}")) {
                tab--;
                builder.delete(builder.length() - 4, builder.length());
            }
            if (line.trim().equalsIgnoreCase(",")) {
                builder.delete(builder.length() - 4 * tab - 2, builder.length());
            }
            if (line.contains("{")) {
                tab++;
            }
            builder.append(line);
            builder.append("\r\n");
            appendTab(builder, tab);
        }
        System.out.println("\r\n" + builder.toString());
        return builder.toString();
    }

    private static String getTab(int tab) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tab; i++) {
            builder.append("    ");
        }
        return builder.toString();
    }

    private static void appendTab(StringBuilder builder, int tab) {
        for (int i = 0; i < tab; i++) {
            builder.append("    ");
        }
    }
}
