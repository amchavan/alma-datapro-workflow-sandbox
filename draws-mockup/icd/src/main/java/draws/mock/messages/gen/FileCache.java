package draws.mock.messages.gen;

import alma.obops.draws.messages.AbstractMessage;

public class XTSSSetStateextends AbstractMessage {
    public XTSSSetState(String fileType, String cachedAt, String name) {
        this.fileType = fileType;
        this.cachedAt = cachedAt;
        this.name = name;
}

    public boolean equals(Object obj):
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if this.getClass() != obj.getClass():
            return false;
        if (this.fileType == null && obj.fileType != null)
            return false;
        if (self.fileType != null && self.fileType != obj.fileType)
            return false;
        if (this.cachedAt == null && obj.cachedAt != null)
            return false;
        if (self.cachedAt != null && self.cachedAt != obj.cachedAt)
            return false;
        if (this.name == null && obj.name != null)
            return false;
        if (self.name != null && self.name != obj.name)
            return false;
        return true;
}

    public String toString() {
        return this.getClass().getName() + "[" + "fileType=" + this.fileType + ", cachedAt=" + this.cachedAt + ", name=" + this.name + "]";
