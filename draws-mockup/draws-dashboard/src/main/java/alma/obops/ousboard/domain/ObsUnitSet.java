package alma.obops.ousboard.domain;

import java.util.Objects;

public class ObsUnitSet {
    private String _id;
    private String _rev;
    private String entityId;
    private String state;
    private String timestamp;
    private String pipelineRecipe;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_rev() {
        return _rev;
    }

    public void set_rev(String _rev) {
        this._rev = _rev;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPipelineRecipe() {
        return pipelineRecipe;
    }

    public void setPipelineRecipe(String pipelineRecipe) {
        this.pipelineRecipe = pipelineRecipe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObsUnitSet that = (ObsUnitSet) o;
        return Objects.equals(_id, that._id) &&
                Objects.equals(_rev, that._rev) &&
                Objects.equals(entityId, that.entityId) &&
                Objects.equals(state, that.state) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(pipelineRecipe, that.pipelineRecipe);
    }

    @Override
    public int hashCode() {

        return Objects.hash(_id, _rev, entityId, state, timestamp, pipelineRecipe);
    }

    @Override
    public String toString() {
        return "ObsUnitSet{" +
                "_id='" + _id + '\'' +
                ", _rev='" + _rev + '\'' +
                ", entityId='" + entityId + '\'' +
                ", state='" + state + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", pipelineRecipe='" + pipelineRecipe + '\'' +
                '}';
    }
}
