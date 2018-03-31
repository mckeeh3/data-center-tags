package demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class DataCenter {
    final Name name;
    final Status status;
    final TagCount tagCount;

    private DataCenter(Name name, Status status, TagCount tagCount) {
        this.name = name;
        this.status = status;
        this.tagCount = tagCount;
    }

    static DataCenter create(Name name, Status status, TagCount tagCount) {
        return new DataCenter(name, status, tagCount);
    }

    static Name name(String name) {
        return new Name(name);
    }

    static Status status(Status.Is status) {
        return new Status(status);
    }

    static TagCount tagCount(int tagCount) {
        return new TagCount(tagCount);
    }

    List<EventTag> tags() {
        List<EventTag> tags = new ArrayList<>();

        for (int c = 0; c < tagCount.value; c++) {
            tags.add(EventTag.create(EventTag.id(String.format("%s-%d", name.value, c)), name, status));
        }
        return tags;
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, %s]", getClass().getSimpleName(), name, status, tagCount);
    }

    static class Name implements Serializable {
        final String value;

        Name(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Name that = (Name) o;
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

    static class Status implements Serializable {
        enum Is {up, down}

        private final Is is;

        public Status(Status.Is is) {
            this.is = is;
        }

        boolean isUp() {
            return Is.up.equals(is);
        }

        boolean isDown() {
            return Is.down.equals(is);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Status status = (Status) o;
            return is == status.is;
        }

        @Override
        public int hashCode() {

            return Objects.hash(is);
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), is);
        }
    }

    static class TagCount implements Serializable {
        final int value;

        public TagCount(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TagCount tagCount = (TagCount) o;
            return value == tagCount.value;
        }

        @Override
        public int hashCode() {

            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return String.format("%s[%d]", getClass().getSimpleName(), value);
        }
    }
}
