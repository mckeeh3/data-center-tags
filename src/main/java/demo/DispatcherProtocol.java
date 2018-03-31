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
