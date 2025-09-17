public class RateLimitingTransformer implements Transformer<String, Notification, KeyValue<String, Notification>> {

    private ProcessorContext context;
    private KeyValueStore<String, Long> countStore;
    private static final long WINDOW_SIZE_MS = 1000;  
    private static final long MAX_PER_SECOND = 100;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.countStore = (KeyValueStore<String, Long>) context.getStateStore("rate-limit-store");
    }

    @Override
    public KeyValue<String, Notification> transform(String key, Notification value) {
        long currentWindow = context.timestamp() / WINDOW_SIZE_MS;
        String windowKey = key + "-" + currentWindow;

        Long count = countStore.get(windowKey);
        if (count == null) count = 0L;

        if (count < MAX_PER_SECOND) {
            countStore.put(windowKey, count + 1);
            return KeyValue.pair(key, value);  // Allow record to pass
        } else {
            // Drop or filter out record
            return null;
        }
    }

    @Override
    public void close() {}
}
