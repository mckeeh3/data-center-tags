package demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static demo.DataCenter.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.*;

class DemoCentersTest {
    @Test
    void tagsForWhenAllDataCentersUp1() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.up), tagCount(10));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), tagCount(10));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), tagCount(10));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags = dataCenters.tagsFor(dataCenter1.name);

        assertNotNull(tags);
        assertThat(tags.size(), is(dataCenter1.tagCount.value));
    }

    @Test
    void tagsForWhenAllDataCentersUp3() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.up), tagCount(10));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), tagCount(10));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), tagCount(10));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags = dataCenters.tagsFor(dataCenter3.name);

        assertNotNull(tags);
        assertThat(tags.size(), is(dataCenter3.tagCount.value));
    }

    @Test
    void tagsForWhenDataCenter1IsDown() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.down), tagCount(100));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), tagCount(200));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), tagCount(300));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags1 = dataCenters.tagsFor(dataCenter1.name);

        assertNotNull(tags1);
        assertTrue(tags1.isEmpty());

        List<EventTag> tags2 = dataCenters.tagsFor(dataCenter2.name);

        assertNotNull(tags2);
        assertThat(tags2.size(), greaterThan(dataCenter2.tagCount.value));
        assertThat(tags2.size(), lessThan(dataCenter1.tagCount.value + dataCenter2.tagCount.value));

        List<EventTag> tags3 = dataCenters.tagsFor(dataCenter3.name);

        assertNotNull(tags3);
        assertThat(tags3.size(), greaterThan(dataCenter3.tagCount.value));
        assertThat(tags3.size(), lessThan(dataCenter1.tagCount.value + dataCenter3.tagCount.value));

        assertEquals(tags2.size() + tags3.size(), dataCenter1.tagCount.value + dataCenter2.tagCount.value + dataCenter3.tagCount.value);

        tags2.retainAll(tags3);
        assertTrue(tags2.isEmpty());
    }
}
