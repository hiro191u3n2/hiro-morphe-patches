package hiro.swiftkey;

/**
 * Detaches existing Latin text only after a native, successful deletion.
 * The original edit, its character count, and editor write ordering are retained.
 * Snapshots are per-call and never stored in static state or written to disk.
 */
public final class DeleteLatinBoundary {
    private DeleteLatinBoundary() {}

    /** Capture the old internal buffer without changing the editor or composition. */
    public static Object[] beforeDelete(ec0.x owner) {
        try {
            if (owner == null || owner.e == null || !owner.e.Y || !owner.C()) return null;
            ec0.f context = owner.a;
            if (context == null) return null;
            String text = context.getText();
            String composing = context.s();
            int cursor = context.P();
            int end = context.A();
            int offset = context.I();
            // Explicit range selections retain the app's original behavior.
            if (text == null || composing == null || cursor != end
                    || cursor < 0 || cursor > text.length() || offset < 0) return null;
            int composingStart = cursor - composing.length();
            if (composingStart < 0
                    || !text.regionMatches(composingStart, composing, 0, composing.length()))
                return null;

            boolean existingText = composing.isEmpty();
            if (!existingText && UnicodeLatinToken.isLatinToken(composing)
                    && context.i != null && context.i.d instanceof r40.c) {
                r40.b marker = ((r40.c) context.i.d).b;
                // Native state 1 is ORIGINAL_TEXT_IN_FIELD. A fresh input passes
                // prepareForEditing and carries state 2 (or a later editing state).
                existingText = marker == null
                        || (marker instanceof ec0.y && ((ec0.y) marker).k == 1);
            }
            if (owner.a != context) return null;
            return new Object[] { owner, context, text, Integer.valueOf(cursor),
                    Integer.valueOf(offset), Integer.valueOf(composingStart),
                    Boolean.valueOf(existingText) };
        } catch (RuntimeException unavailable) {
            return null;
        } catch (LinkageError incompatible) {
            return null;
        }
    }

    /** Run only after the original native deletion has returned successfully. */
    public static void afterDelete(ec0.x owner, Object[] snapshot, boolean nativeResult) {
        if (!nativeResult || snapshot == null || snapshot.length != 7) return;
        try {
            if (owner == null || snapshot[0] != owner || owner.e == null
                    || !owner.e.Y || !owner.C()) return;
            ec0.f context = owner.a;
            if (context == null || snapshot[1] != context) return;

            String oldText = (String) snapshot[2];
            int oldCursor = ((Integer) snapshot[3]).intValue();
            int oldOffset = ((Integer) snapshot[4]).intValue();
            int oldComposingStart = ((Integer) snapshot[5]).intValue();
            boolean existingText = ((Boolean) snapshot[6]).booleanValue();
            String text = context.getText();
            int cursor = context.P();
            if (oldText == null || text == null || cursor != context.A()
                    || cursor < 0 || cursor > text.length() || context.I() != oldOffset)
                return;
            if (oldCursor < 0 || oldCursor > oldText.length()
                    || oldComposingStart < 0 || oldComposingStart > oldCursor) return;

            // Prove that the original edit removed one contiguous interval at the
            // old cursor. Reject insertions, substitutions, buffer shifts and no-ops.
            int removed = oldText.length() - text.length();
            int removedBefore = oldCursor - cursor;
            if (removed <= 0 || removedBefore < 0 || removedBefore > removed) return;
            int removedAfter = removed - removedBefore;
            if (removedAfter > oldText.length() - oldCursor) return;
            int suffixStart = oldCursor + removedAfter;
            int suffixLength = text.length() - cursor;
            if (oldText.length() - suffixStart != suffixLength
                    || !text.regionMatches(0, oldText, 0, cursor)
                    || !text.regionMatches(cursor, oldText, suffixStart, suffixLength)) return;

            // If a live composition still remains, leave its romaji/kana editing
            // untouched. When it was completely removed, the remaining prefix
            // comes from text that preceded that composition and may be detached.
            if (!existingText && cursor > oldComposingStart) return;
            if (!UnicodeLatinToken.isLatinToken(context.s())) return;
            CursorLatinBoundary.afterSelection(owner, context);
        } catch (RuntimeException unavailable) {
            // Keep the result of the original native edit and its recovery path.
        } catch (LinkageError incompatible) {
            // A helper ABI mismatch must not turn an otherwise successful edit into a crash.
        }
    }
}
