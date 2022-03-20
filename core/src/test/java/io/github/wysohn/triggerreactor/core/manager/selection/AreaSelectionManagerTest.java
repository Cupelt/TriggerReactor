package io.github.wysohn.triggerreactor.core.manager.selection;

import io.github.wysohn.triggerreactor.components.AreaSelectionTestComponent;
import io.github.wysohn.triggerreactor.components.DaggerAreaSelectionManagerTestComponent;
import io.github.wysohn.triggerreactor.components.DaggerAreaSelectionTestComponent;
import io.github.wysohn.triggerreactor.core.manager.location.Area;
import io.github.wysohn.triggerreactor.core.manager.location.SimpleLocation;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class AreaSelectionManagerTest {
    AreaSelectionTestComponent component;
    UUID uuid = UUID.randomUUID();

    @Before
    public void init() {
        component = DaggerAreaSelectionTestComponent.builder()
                .build();
    }

    @Test
    public void testSingleton() {
        AreaSelectionTestComponent component = DaggerAreaSelectionTestComponent
                .builder()
                .build();
        assertSame(component.manager(), component.manager());
    }

    @Test
    public void toggleTest() {
        AreaSelectionManager manager = component.manager();

        assertNull(manager.getLeftPosition(uuid));
        assertNull(manager.onClick(ClickType.LEFT_CLICK, uuid, new SimpleLocation("world", 1, 2, 3)));
        assertNull(manager.getLeftPosition(uuid));

        manager.toggleSelection(uuid);
        assertNull(manager.getLeftPosition(uuid));
        assertEquals(ClickResult.LEFTSET,
                manager.onClick(ClickType.LEFT_CLICK, uuid, new SimpleLocation("world", 1, 2, 3)));
        assertEquals(new SimpleLocation("world", 1, 2, 3), manager.getLeftPosition(uuid));

        manager.toggleSelection(uuid);
        assertNull(manager.getLeftPosition(uuid));
        assertNull(manager.onClick(ClickType.RIGHT_CLICK, uuid, new SimpleLocation("world", 1, 2, 3)));
        assertNull(manager.getLeftPosition(uuid));
    }

    @Test
    public void testSelection() {
        AreaSelectionManager manager = DaggerAreaSelectionManagerTestComponent.builder()
                .build()
                .getAreaSelectionManager();

        UUID uuid = UUID.randomUUID();
        SimpleLocation loc1 = new SimpleLocation("world", 1, 2, 3);

        assertNull(manager.onClick(ClickType.LEFT_CLICK, uuid, loc1));
        assertNull(manager.getSelection(uuid));
    }

    @Test
    public void testSelectionToggle(){
        AreaSelectionManager manager = DaggerAreaSelectionManagerTestComponent.builder()
                .build()
                .getAreaSelectionManager();

        UUID uuid = UUID.randomUUID();
        SimpleLocation loc1 = new SimpleLocation("world", 1, 2, 3);
        SimpleLocation loc2 = new SimpleLocation("world", 4, 5, 6);
        Area area = new Area(loc1, loc2);

        assertTrue(manager.toggleSelection(uuid));
        assertEquals(manager.onClick(ClickType.LEFT_CLICK, uuid, loc1), ClickResult.LEFTSET);
        assertEquals(manager.onClick(ClickType.RIGHT_CLICK, uuid, loc2), ClickResult.COMPLETE);
        assertEquals(area, manager.getSelection(uuid));
        assertFalse(manager.toggleSelection(uuid));
    }

    @Test
    public void testSelectionToggle2(){
        AreaSelectionManager manager = DaggerAreaSelectionManagerTestComponent.builder()
                .build()
                .getAreaSelectionManager();

        UUID uuid = UUID.randomUUID();
        SimpleLocation loc1 = new SimpleLocation("world", 1, 2, 3);
        SimpleLocation loc2 = new SimpleLocation("world", 4, 5, 6);
        Area area = new Area(loc1, loc2);

        assertTrue(manager.toggleSelection(uuid));
        assertEquals(manager.onClick(ClickType.RIGHT_CLICK, uuid, loc1), ClickResult.RIGHTSET);
        assertEquals(manager.onClick(ClickType.LEFT_CLICK, uuid, loc2), ClickResult.COMPLETE);
        assertEquals(area, manager.getSelection(uuid));
        assertFalse(manager.toggleSelection(uuid));
    }

    @Test
    public void onClickLeft() {
        AreaSelectionManager manager = component.manager();
        manager.toggleSelection(uuid);

        assertNull(manager.getLeftPosition(uuid));
        assertEquals(ClickResult.LEFTSET,
                manager.onClick(ClickType.LEFT_CLICK, uuid, new SimpleLocation("world", 1, 2, 3)));
        assertEquals(new SimpleLocation("world", 1, 2, 3), manager.getLeftPosition(uuid));
    }

    @Test
    public void onClickRight() {
        AreaSelectionManager manager = component.manager();
        manager.toggleSelection(uuid);

        assertNull(manager.getRightPosition(uuid));
        assertEquals(ClickResult.RIGHTSET,
                manager.onClick(ClickType.RIGHT_CLICK, uuid, new SimpleLocation("world", 3, 2, -1)));
        assertEquals(new SimpleLocation("world", 3, 2, -1), manager.getRightPosition(uuid));
    }

    @Test
    public void onClickLeftRightSame() {
        AreaSelectionManager manager = component.manager();
        manager.toggleSelection(uuid);

        assertNull(manager.getLeftPosition(uuid));
        SimpleLocation leftPos = new SimpleLocation("world", 1, 2, 3);
        assertEquals(ClickResult.LEFTSET,
                manager.onClick(ClickType.LEFT_CLICK, uuid, leftPos));
        assertEquals(leftPos, manager.getLeftPosition(uuid));

        assertNull(manager.getRightPosition(uuid));
        SimpleLocation rightPos = new SimpleLocation("world", 1, 10, 3);
        assertEquals(ClickResult.COMPLETE,
                manager.onClick(ClickType.RIGHT_CLICK, uuid, rightPos));
        assertEquals(rightPos, manager.getRightPosition(uuid));

        assertEquals(new Area(leftPos, rightPos), manager.getSelection(uuid));
    }

    @Test
    public void onClickRightLeftSame() {
        AreaSelectionManager manager = component.manager();
        manager.toggleSelection(uuid);

        assertNull(manager.getRightPosition(uuid));
        SimpleLocation rightPos = new SimpleLocation("world", 1, 10, 3);
        assertEquals(ClickResult.RIGHTSET,
                manager.onClick(ClickType.RIGHT_CLICK, uuid, rightPos));
        assertEquals(rightPos, manager.getRightPosition(uuid));

        assertNull(manager.getLeftPosition(uuid));
        SimpleLocation leftPos = new SimpleLocation("world", 1, 2, 3);
        assertEquals(ClickResult.COMPLETE,
                manager.onClick(ClickType.LEFT_CLICK, uuid, leftPos));
        assertEquals(leftPos, manager.getLeftPosition(uuid));

        assertEquals(new Area(leftPos, rightPos), manager.getSelection(uuid));
    }

    @Test
    public void onClickLeftRightWorldDifferent() {
        AreaSelectionManager manager = component.manager();
        manager.toggleSelection(uuid);

        assertNull(manager.getLeftPosition(uuid));
        SimpleLocation leftPos = new SimpleLocation("world", 1, 2, 3);
        assertEquals(ClickResult.LEFTSET,
                manager.onClick(ClickType.LEFT_CLICK, uuid, leftPos));
        assertEquals(leftPos, manager.getLeftPosition(uuid));

        assertNull(manager.getRightPosition(uuid));
        SimpleLocation rightPos = new SimpleLocation("world2", 1, 10, 3);
        assertEquals(ClickResult.DIFFERENTWORLD,
                manager.onClick(ClickType.RIGHT_CLICK, uuid, rightPos));
        assertEquals(rightPos, manager.getRightPosition(uuid));
    }
}