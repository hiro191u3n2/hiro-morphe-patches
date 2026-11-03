package hiro.swiftkey;

import android.os.Looper;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;
import java.util.List;
import java.util.concurrent.Future;

/** A space press selects the first current kanji candidate through the native commit path. */
public final class SpaceConversion {
    private SpaceConversion() {}

    public static boolean handle(wb0.x model, lh0.a breadcrumb, String text, String punctuation) {
        if ((!" ".equals(text) && !"\u3000".equals(text)) || breadcrumb == null
                || Looper.myLooper() != Looper.getMainLooper()) return false;
        boolean dispatched = false;
        m40.b chosen = null;
        String separator = null;
        boolean changedSeparator = false;
        try {
            if (model == null || model.c == null || model.b == null || model.g == null
                    || model.g.b == null || model.g.b.l != PredictionSearchType.JAPANESE
                    || model.g.a == null || model.g.f) return false;
            dc0.a state = model.c.C();
            if (state == null || state.k() != state.S()) return false;
            final int cursor = state.k();
            final String composition = state.s(), raw = state.G();
            final String reading = reading(composition);
            if (reading == null) return false;
            // The UI thread must never wait for a pending prediction job.
            Future<?> pending = model.g.a.h;
            if (pending == null || !pending.isDone() || pending.isCancelled()) return false;
            Object result = pending.get();
            if (!(result instanceof List)) return false;
            List<?> candidates = (List<?>) result;
            int position = -1;
            for (int i = 0; i < candidates.size(); i++) {
                Object value = candidates.get(i);
                if (!(value instanceof m40.b)) continue;
                m40.b candidate = (m40.b) value;
                l40.c request = candidate.subrequest();
                if (request == null || request.f || request.d != PredictionSearchType.JAPANESE
                        || !reading.equals(reading(request.k))) continue;
                String output = candidate.getUserFacingText();
                // Skip the literal kana/romaji, emoji, and English supplement entries.
                if (output == null || output.equals(composition) || !hasKanji(output)) continue;
                chosen = candidate;
                position = i;
                break;
            }
            if (chosen == null) return false;
            dc0.a latest = model.c.C();
            if (latest == null || latest.k() != cursor || latest.S() != cursor
                    || !same(composition, latest.s()) || !same(raw, latest.G())
                    || model.g.b.l != PredictionSearchType.JAPANESE || model.g.f || model.g.a.h != pending)
                return false;
            separator = chosen.getTrailingSeparator();
            chosen.setTrailingSeparator("");
            changedSeparator = true;
            yb0.a event = model.b.g(breadcrumb, chosen, wb0.j.a, position);
            if (event == null) return false;
            // Once native dispatch begins, never fall through to a second space write.
            dispatched = true;
            model.i0(event);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return dispatched;
        } catch (Exception unavailable) {
            return dispatched;
        } catch (LinkageError unavailable) {
            return dispatched;
        } finally {
            if (changedSeparator && !dispatched) {
                try { chosen.setTrailingSeparator(separator); }
                catch (RuntimeException ignored) { }
                catch (LinkageError ignored) { }
            }
        }
    }

    private static boolean same(String a, String b) { return a == null ? b == null : a.equals(b); }

    static String reading(String input) {
        if (input == null || input.isEmpty()) return null;
        boolean latin = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '\'' || c == '-' || c == '\u30fc')) {
                latin = false; break;
            }
        }
        String kana = latin ? com.microsoft.fluency.Japanese.romajiToHiragana(input) : input;
        if (kana == null) return null;
        String normalized = WordCandidates.normalize(kana);
        if (normalized.isEmpty()) return null;
        boolean kanaSeen = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= '\u3041' && c <= '\u3096') kanaSeen = true;
            else if (c != '\u30fc') return null;
        }
        return kanaSeen ? normalized : null;
    }

    static boolean hasKanji(String value) {
        for (int offset = 0; offset < value.length();) {
            int point = value.codePointAt(offset);
            if ((point >= 0x3400 && point <= 0x4dbf) || (point >= 0x4e00 && point <= 0x9fff)
                    || (point >= 0xf900 && point <= 0xfaff) || (point >= 0x20000 && point <= 0x2fa1f)
                    || (point >= 0x30000 && point <= 0x323af)) return true;
            offset += Character.charCount(point);
        }
        return false;
    }
}
