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
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags = dataCenters.tagsFor(dataCenter1.name);

        assertNotNull(tags);
        assertThat(tags.size(), is(dataCenter1.tagCount.value));
    }

    @Test
    void tagsForWhenAllDataCentersUp3() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags = dataCenters.tagsFor(dataCenter3.name);

        assertNotNull(tags);
        assertThat(tags.size(), is(dataCenter3.tagCount.value));
    }

    @Test
    void tagsForWhenOneRunnerIsOffAndOneDataCenterIsDown() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.down), runner(Runner.Is.on), tagCount(100));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), runner(Runner.Is.off), tagCount(200));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(300));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags1 = dataCenters.tagsFor(dataCenter1.name);
        List<EventTag> tags2 = dataCenters.tagsFor(dataCenter2.name);
        List<EventTag> tags3 = dataCenters.tagsFor(dataCenter3.name);

        assertNotNull(tags1);
        assertTrue(tags1.isEmpty());

        assertNotNull(tags2);
        assertTrue(tags2.isEmpty());

        assertNotNull(tags3);
        assertThat(tags3.size(), greaterThan(dataCenter3.tagCount.value));
        assertThat(tags3.size(), lessThan(dataCenter3.tagCount.value + dataCenter1.tagCount.value));
    }

    @Test
    void tagsForWhenDataCenterOneIsDown() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.down), runner(Runner.Is.on), tagCount(100));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.up), runner(Runner.Is.on), tagCount(200));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(300));

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

        assertEquals(dataCenter1.tagCount.value + dataCenter2.tagCount.value + dataCenter3.tagCount.value,
                tags2.size() + tags3.size());

        tags2.retainAll(tags3);
        assertTrue(tags2.isEmpty());

        assertFalse(tags2.removeAll(tags3));
        assertFalse(tags3.removeAll(tags2));
    }

    @Test
    void tagsForWhenAllDataCentersAreDown() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.down), runner(Runner.Is.on), tagCount(100));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.down), runner(Runner.Is.on), tagCount(200));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.down), runner(Runner.Is.on), tagCount(300));

        DataCenters dataCenters = DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);

        List<EventTag> tags1 = dataCenters.tagsFor(dataCenter1.name);
        List<EventTag> tags2 = dataCenters.tagsFor(dataCenter2.name);
        List<EventTag> tags3 = dataCenters.tagsFor(dataCenter3.name);

        assertNotNull(tags1);
        assertNotNull(tags2);
        assertNotNull(tags3);
        assertTrue(tags1.isEmpty());
        assertTrue(tags2.isEmpty());
        assertTrue(tags3.isEmpty());
    }

    @Test
    void tagsForWhenTwoOfFiveDataCentersAreDown() {
        DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(Status.Is.up), runner(Runner.Is.on), tagCount(50));
        DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(Status.Is.down), runner(Runner.Is.on), tagCount(75));
        DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(25));
        DataCenter dataCenter4 = DataCenter.create(name("dc4"), status(Status.Is.down), runner(Runner.Is.on), tagCount(35));
        DataCenter dataCenter5 = DataCenter.create(name("dc5"), status(Status.Is.up), runner(Runner.Is.on), tagCount(45));

        DataCenters dataCenters = DataCenters.create()
                .add(dataCenter1)
                .add(dataCenter2)
                .add(dataCenter3)
                .add(dataCenter4)
                .add(dataCenter5);

        List<EventTag> tags2 = dataCenters.tagsFor(dataCenter2.name);

        assertNotNull(tags2);
        assertTrue(tags2.isEmpty());

        List<EventTag> tags4 = dataCenters.tagsFor(dataCenter4.name);

        assertNotNull(tags4);
        assertTrue(tags4.isEmpty());

        List<EventTag> tags1 = dataCenters.tagsFor(dataCenter1.name);
        List<EventTag> tags3 = dataCenters.tagsFor(dataCenter3.name);
        List<EventTag> tags5 = dataCenters.tagsFor(dataCenter5.name);

        assertThat(tags1.size(), greaterThan(dataCenter1.tagCount.value));
        assertThat(tags3.size(), greaterThan(dataCenter3.tagCount.value));
        assertThat(tags5.size(), greaterThan(dataCenter5.tagCount.value));

        assertEquals(dataCenter1.tagCount.value + dataCenter2.tagCount.value + dataCenter3.tagCount.value
                        + dataCenter4.tagCount.value + dataCenter5.tagCount.value,
                tags1.size() + tags3.size() + tags5.size());

        assertFalse(tags1.removeAll(tags3));
        assertFalse(tags1.removeAll(tags5));
        assertFalse(tags3.removeAll(tags1));
        assertFalse(tags3.removeAll(tags5));
        assertFalse(tags5.removeAll(tags1));
        assertFalse(tags5.removeAll(tags3));
    }
}
