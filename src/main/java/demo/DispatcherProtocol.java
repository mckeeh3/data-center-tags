package demo;

import akka.cluster.sharding.ShardRegion;

import java.io.Serializable;

class DispatcherProtocol {
    static class EventTagGo implements Serializable {
        final EventTag eventTag;

        EventTagGo(EventTag eventTag) {
            this.eventTag = eventTag;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), eventTag);
        }
    }

    static class EventTagStop implements Serializable {
        final EventTag eventTag;

        EventTagStop(EventTag eventTag) {
            this.eventTag = eventTag;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), eventTag);
        }
    }

    // todo make this real
    static class Entity implements Serializable {
        final Tag tag;
        final Id id;

        Entity(Tag tag, Id id) {
            this.tag = tag;
            this.id = id;
        }

        static Tag tag(String value) {
            return new Tag(value);
        }

        static Id id(String value) {
            return new Id(value);
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s]", getClass().getSimpleName(), tag, id);
        }

        static class Tag {
            final String value;

            Tag(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s[%s]", getClass().getSimpleName(), value);
            }
        }

        static class Id {
            final String value;

            Id(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s[%s]", getClass().getSimpleName(), value);
            }
        }
    }

    static ShardRegion.MessageExtractor messageExtractor() {
        final int numberOfShards = 100;

        return new ShardRegion.MessageExtractor() {
            @Override
            public String shardId(Object message) {
                if (message instanceof EventTagGo) {
                    return ((EventTagGo) message).eventTag.id.value.hashCode() % numberOfShards + "";
                } else if (message instanceof EventTagStop) {
                    return ((EventTagStop) message).eventTag.id.value.hashCode() % numberOfShards + "";
                } else {
                    return null;
                }
            }

            @Override
            public String entityId(Object message) {
                if (message instanceof EventTagGo) {
                    return ((EventTagGo) message).eventTag.id.value;
                } else if (message instanceof EventTagStop) {
                    return ((EventTagStop) message).eventTag.id.value;
                } else {
                    return null;
                }
            }

            @Override
            public Object entityMessage(Object message) {
                return message;
            }
        };
    }
}
