package hiro.swiftkey;
final class FrequencyLearning$Entry {
    final String reading, surface;
    long count, recent;
    FrequencyLearning$Entry(String reading, String surface, long count, long recent) {
        this.reading = reading; this.surface = surface; this.count = count; this.recent = recent;
    }
}
