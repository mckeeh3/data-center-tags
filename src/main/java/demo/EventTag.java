package demo;

import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

class EventTag implements Serializable {
    final Id id;
    final DataCenter.Name name;
    final DataCenter.Status status;

    private EventTag(Id id, DataCenter.Name name, DataCenter.Status status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    static EventTag create(Id id, DataCenter.Name name, DataCenter.Status status) {
        return new EventTag(id, name, status);
    }

    static Id id(String id) {
        return new Id(id);
    }

    int hashKey() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(id.value.getBytes());
            return DatatypeConverter.printHexBinary(messageDigest.digest()).hashCode();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create a MessageDigest", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventTag tag = (EventTag) o;
        return Objects.equals(id, tag.id) &&
                Objects.equals(name, tag.name) &&
                Objects.equals(status, tag.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, status);
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, %s]", getClass().getSimpleName(), id, name, status);
    }

    static class Id implements Serializable {
        final String value;

        Id(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), value);
        }
    }
}
