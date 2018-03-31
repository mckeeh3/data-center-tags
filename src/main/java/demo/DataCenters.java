package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

class DataCenters {
    private final List<DataCenter> dataCenters;

    private DataCenters() {
        dataCenters = new ArrayList<>();
    }

    static DataCenters create() {
        return new DataCenters();
    }

    DataCenters add(DataCenter dataCenter) {
        dataCenters.add(dataCenter);
        return this;
    }

    List<EventTag> tagsFor(DataCenter.Name name) {
        List<EventTag> tags = new ArrayList<>();

        for (DataCenter dataCenter : dataCenters) {
            tags.addAll(dataCenter.tags());
        }

        return filter(name, tags);
    }

    private List<EventTag> filter(DataCenter.Name name, List<EventTag> tags) {
        final TreeMap<Integer, EventTag> consistentHash = consistentHash();

        tags.removeIf(e -> isNotMyUpTag(name, e));
        tags.removeIf(e -> isMyDownTag(name, e));
        tags.removeIf(e -> isNotMyDownTag(name, e, consistentHash));

        return tags;
    }

    private boolean isNotMyUpTag(DataCenter.Name name, EventTag eventTag) {
        return eventTag.status.isUp() && !eventTag.name.equals(name);
    }

    private boolean isMyDownTag(DataCenter.Name name, EventTag eventTag) {
        return eventTag.status.isDown() && eventTag.name.equals(name);
    }

    private boolean isNotMyDownTag(DataCenter.Name name, EventTag eventTag, TreeMap<Integer, EventTag> consistentHash) {
        if (eventTag.status.isDown() && !eventTag.name.equals(name)) {
            EventTag eventTagLower = nextLower(eventTag, consistentHash);
            while (!eventTagLower.id.equals(eventTag.id) && eventTagLower.name.equals(eventTag.name)) {
                eventTagLower = nextLower(eventTagLower, consistentHash);
            }
            return eventTagLower.name.equals(name);
        } else {
            return false;
        }
    }

    private EventTag nextLower(EventTag eventTag, TreeMap<Integer, EventTag> consistentHash) {
        return (consistentHash.firstKey().equals(eventTag.hashKey()))
                ? consistentHash.lastEntry().getValue()
                : consistentHash.lowerEntry(eventTag.hashKey()).getValue();
    }

    private TreeMap<Integer, EventTag> consistentHash() {
        TreeMap<Integer, EventTag> hash = new TreeMap<>();

        for (DataCenter dataCenter : dataCenters) {
            for (EventTag eventTag : dataCenter.tags()) {
                hash.put(eventTag.hashKey(), eventTag);
            }
        }
        return hash;
    }
}
