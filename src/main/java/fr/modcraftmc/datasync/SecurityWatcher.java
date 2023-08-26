package fr.modcraftmc.datasync;

import java.util.ArrayList;
import java.util.List;

public class SecurityWatcher {
    public static SecurityIssue RABBIMQ_CONNECTION_ISSUE = new SecurityIssue("RabbitMQ connection issue", "RabbitMQ server is unreachable");
    public static SecurityIssue MONGODB_CONNECTION_ISSUE = new SecurityIssue("MongoDB connection issue", "MongoDB server is unreachable");

    private boolean isSecure;
    private String name;
    private final List<SecurityIssue> issues = new ArrayList<>();

    private final List<Runnable> onSecureEvent = new ArrayList<>();
    private final List<Runnable> onInsecureEvent = new ArrayList<>();

    public SecurityWatcher(String name) {
        this.name = name;
        this.isSecure = true;
    }

    public void addIssue(SecurityIssue issue) {
        this.issues.add(issue);

        setInsecure();
    }

    public void removeIssue(SecurityIssue issue) {
        this.issues.remove(issue);
        if(this.issues.isEmpty()) setSecure();
    }

    public void registerOnSecureEvent(Runnable runnable) {
        this.onSecureEvent.add(runnable);
    }

    public void unregisterOnSecureEvent(Runnable runnable) {
        this.onInsecureEvent.remove(runnable);
    }

    public void registerOnInsecureEvent(Runnable runnable) {
        this.onInsecureEvent.add(runnable);
    }

    public void unregisterOnInsecureEvent(Runnable runnable) {
        this.onInsecureEvent.remove(runnable);
    }

    private void setSecure(){
        if(this.isSecure) return;

        this.isSecure = true;
        onSecureEvent.forEach(Runnable::run);
    }

    private void setInsecure(){
        if(!this.isSecure) return;

        this.isSecure = false;
        onInsecureEvent.forEach(Runnable::run);
    }

    public boolean isSecure() {
        return this.isSecure;
    }

    public String getReason() {
        StringBuilder reason = new StringBuilder();
        reason.append("Security issue detected on ").append(name).append(" :\n");
        for (SecurityIssue issue : issues) {
            reason.append("\t").append(issue.issueName).append(" : ").append(issue.issueDescription).append("\n");
        }
        return reason.toString();
    }

    public record SecurityIssue(String issueName, String issueDescription) { }
}
