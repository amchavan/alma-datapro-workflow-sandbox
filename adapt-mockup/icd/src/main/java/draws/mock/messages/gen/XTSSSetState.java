package draws.mock.messages.gen;

import alma.obops.draws.messages.AbstractMessage;

public class XTSSSetStateextends AbstractMessage {
    public XTSSSetState(String ousUID, String state) {
        this.ousUID = ousUID;
        this.state = state;
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
        if (this.state == null && obj.state != null)
            return false;
        if (self.state != null && self.state != obj.state)
            return false;
        return true;
}

    public String toString() {
        return this.getClass().getName() + "[" + "ousUID=" + this.ousUID + ", state=" + this.state + "]";
