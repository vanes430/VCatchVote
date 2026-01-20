package github.vanes430.vcatchvote.manager;

public class WaitingVote {
    private final String username;
    private final String service;
    private final long timestamp;

    public WaitingVote(String username, String service) {
        this.username = username;
        this.service = service;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public String getService() {
        return service;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
