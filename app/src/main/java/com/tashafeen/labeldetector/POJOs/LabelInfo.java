package com.tashafeen.labeldetector.POJOs;

public class LabelInfo {

    String text ;
    String entityId ;
    float confidence;

    public LabelInfo(String text, String entityId, float confidence) {
        this.text = text;
        this.entityId = entityId;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "LabelInfo{" +
                "text='" + text + '\'' +
                ", entityId='" + entityId + '\'' +
                ", confidence=" + confidence +
                '}';
    }

    public LabelInfo() {
    }

    public String getText() {

        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
