package fr.clashdesecoles.bingo.utils;

public final class TextUtil {
    private TextUtil() {}

    public static String stripColors(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    public static String center(String text, int width) {
        if (text == null) return "";
        String stripped = stripColors(text);
        int len = stripped.length();
        if (len >= width) return text;
        int pad = (width - len) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(' ');
        sb.append(text);
        return sb.toString();
    }
}
