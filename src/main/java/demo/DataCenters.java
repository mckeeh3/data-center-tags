package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    DataCenters addAll(List<DataCenter> dataCenterList) {
        dataCenters.addAll(dataCenterList);
        return this;
    }

    List<EventTag> tagsFor(DataCenter.Name name) {
        final Optional<DataCenter> dataCenter = lookup(name);

        if (dataCenter.isPresent() && dataCenter.get().runner.isOn()) {
            List<EventTag> tags = new ArrayList<>();
            dataCenters.forEach(dc -> tags.addAll(dc.tags()));

            return filter(name, tags);
        } else {
            return new ArrayList<>();
        }
    }

    private Optional<DataCenter> lookup(DataCenter.Name name) {
        for (DataCenter dataCenter : dataCenters) {
            if (name.equals(dataCenter.name) && dataCenter.status.isUp()) {
                return Optional.of(dataCenter);
            }
        }
        return Optional.empty();
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
            while (!eventTagLower.id.equals(eventTag.id) && eventTagLower.status.isDown()) {
                eventTagLower = nextLower(eventTagLower, consistentHash);
            }
            return !eventTagLower.name.equals(name);
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
