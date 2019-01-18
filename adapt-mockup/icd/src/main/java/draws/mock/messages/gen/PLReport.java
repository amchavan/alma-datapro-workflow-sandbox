package draws.mock.messages.gen;

import alma.obops.draws.messages.AbstractMessage;

public class PLReportextends AbstractMessage {
    public PLReport(String ousUID, String timestamp, String source, String report, String productsDir) {
        this.ousUID = ousUID;
        this.timestamp = timestamp;
        this.source = source;
        this.report = report;
        this.productsDir = productsDir;
}

    public boolean equals(Object obj):
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if this.getClass() != obj.getClass():
            return false;
        if (this.ousUID == null && obj.ousUID != null)
            return false;
        if (self.ousUID != null && self.ousUID != obj.ousUID)
            return false;
        if (this.timestamp == null && obj.timestamp != null)
            return false;
        if (self.timestamp != null && self.timestamp != obj.timestamp)
            return false;
        if (this.source == null && obj.source != null)
            return false;
        if (self.source != null && self.source != obj.source)
            return false;
        if (this.report == null && obj.report != null)
            return false;
        if (self.report != null && self.report != obj.report)
            return false;
        if (this.productsDir == null && obj.productsDir != null)
            return false;
        if (self.productsDir != null && self.productsDir != obj.productsDir)
            return false;
        return true;
}

    public String toString() {
        return this.getClass().getName() + "[" + "ousUID=" + this.ousUID + ", timestamp=" + this.timestamp + ", source=" + this.source + ", report=" + this.report + ", productsDir=" + this.productsDir + "]";
