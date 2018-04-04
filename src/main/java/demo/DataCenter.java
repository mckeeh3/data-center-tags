package demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class DataCenter {
    final Name name;
    final Status status;
    final Runner runner;
    final TagCount tagCount;

    private DataCenter(Name name, Status status, Runner runner, TagCount tagCount) {
        this.name = name;
        this.status = status;
        this.runner = runner;
        this.tagCount = tagCount;
    }

    static DataCenter create(Name name, Status status, Runner runner, TagCount tagCount) {
        return new DataCenter(name, status, runner, tagCount);
    }

    static Name name(String name) {
        return new Name(name);
    }

    static Status status(Status.Is status) {
        return new Status(status);
    }

    static Runner runner(Runner.Is runner) {
        return new Runner(runner);
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
        return String.format("%s[%s, %s, %s, %s]", getClass().getSimpleName(), name, status, runner, tagCount);
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

    static class Runner implements Serializable {
        enum Is {on, off}

        private final Is is;

        Runner(Is is) {
            this.is = is;
        }

        boolean isOn() {
            return Is.on.equals(is);
        }

        boolean isOff() {
            return Is.off.equals(is);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Runner runner = (Runner) o;
            return is == runner.is;
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

    static class Status implements Serializable {
        enum Is {up, down}

        private final Is is;

        Status(Status.Is is) {
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

        TagCount(int value) {
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
