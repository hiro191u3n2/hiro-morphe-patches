package hiro.swiftkey;

import java.text.Normalizer;

/**
 * Recognizes existing Latin text for Japanese cursor/paste protection.
 * Normalization is used only for classification; the editor text is never changed.
 * This helper must only be called from CursorLatinBoundary.afterSelection, whose
 * existing language, real-selection, changed-cursor and native-span guards remain.
 */
public final class UnicodeLatinToken {
    private UnicodeLatinToken() {}

    public static boolean isLatinToken(String value) {
        if (value == null || value.length() == 0) return false;
        // Includes fullwidth letters, ligatures and mathematical Latin alphabets.
        String token = Normalizer.normalize(value, Normalizer.Form.NFKC);
        boolean hasLatin = false;
        boolean latinBase = false;
        for (int offset = 0; offset < token.length();) {
            int point = token.codePointAt(offset);
            offset += Character.charCount(point);
            if (point >= 0x21 && point <= 0x7e) {
                latinBase = (point >= 'A' && point <= 'Z')
                        || (point >= 'a' && point <= 'z');
                hasLatin |= latinBase;
                continue;
            }
            if (Character.isLetter(point)
                    && Character.UnicodeScript.of(point) == Character.UnicodeScript.LATIN) {
                hasLatin = true;
                latinBase = true;
                continue;
            }
            // Restrict marks to Latin combining-diacritic blocks. In particular,
            // Japanese dakuten, emoji selectors and invisible joiners stay excluded.
            if (latinBase && isLatinDiacritic(point)) continue;
            if (isWordPunctuation(point)) {
                latinBase = false;
                continue;
            }
            return false;
        }
        return hasLatin;
    }

    private static boolean isLatinDiacritic(int point) {
        if (point == 0x034f) return false; // Combining grapheme joiner is not an accent.
        int type = Character.getType(point);
        if (type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK)
            return false;
        Character.UnicodeScript script = Character.UnicodeScript.of(point);
        if (script != Character.UnicodeScript.INHERITED && script != Character.UnicodeScript.LATIN)
            return false;
        return (point >= 0x0300 && point <= 0x036f)
                || (point >= 0x1ab0 && point <= 0x1aff)
                || (point >= 0x1dc0 && point <= 0x1dff)
                || (point >= 0xfe20 && point <= 0xfe2f);
    }

    private static boolean isWordPunctuation(int point) {
        return point == 0x2018 || point == 0x2019 || point == 0x02bc
                || point == 0x2010 || point == 0x2011
                || point == 0x2013 || point == 0x2014;
    }
}
