import hiro.swiftkey.JapaneseGlideDecoder;
import java.io.FileInputStream;
import java.io.InputStream;

/** Exercise physical key coordinates: runtime scales normalized X and Y separately. */
public final class DecoderAspectTest {
    public static void main(String[] args) throws Exception {
        try (InputStream input = new FileInputStream(args[0])) {
            JapaneseGlideDecoder.load(input);
        }
        int cases = 0;
        for (float ratio : new float[]{.25f, .33f, .4f, .5f, .65f, 1f}) {
            for (int layout = 0; layout < DecoderRegressionTest.LAYOUTS.length; layout++) {
                float[] centers = DecoderRegressionTest.LAYOUTS[layout].clone();
                for (int i = 1; i < centers.length; i += 2) centers[i] *= ratio;
                for (int variant = 0; variant < 3; variant++) {
                    for (String[] word : DecoderRegressionTest.WORDS) {
                        float[] trace = DecoderRegressionTest.path(word[0], centers,
                                variant == 2 ? 2 : 9,
                                variant == 1 ? .012f : 0,
                                variant == 1 ? .007f : 0);
                        JapaneseGlideDecoder.Result[] results = JapaneseGlideDecoder.decodeCandidates(
                                trace, centers, DecoderRegressionTest.CODES);
                        if (!DecoderRegressionTest.has(results, word[1])) {
                            throw new AssertionError("height/width=" + ratio + " layout="
                                    + DecoderRegressionTest.NAMES[layout] + " variant=" + variant
                                    + " word=" + word[0] + " expected=" + word[1]
                                    + " got=" + DecoderRegressionTest.list(results));
                        }
                        cases++;
                    }
                }
            }
        }
        System.out.println("PASS " + cases + " physical-aspect decoder cases");
    }
}
