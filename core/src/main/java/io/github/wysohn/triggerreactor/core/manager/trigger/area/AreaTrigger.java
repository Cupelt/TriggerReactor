package io.github.wysohn.triggerreactor.core.manager.trigger.area;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.github.wysohn.triggerreactor.core.manager.location.Area;
import io.github.wysohn.triggerreactor.core.manager.location.SimpleLocation;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.Trigger;
import io.github.wysohn.triggerreactor.core.manager.trigger.TriggerInfo;
import io.github.wysohn.triggerreactor.core.script.interpreter.Interpreter;
import io.github.wysohn.triggerreactor.tools.StringUtils;
import io.github.wysohn.triggerreactor.tools.ValidationUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AreaTrigger extends Trigger {
    final File folder;
    private final Map<UUID, WeakReference<Object>> trackedEntities = new ConcurrentHashMap<>();
    private EnterTrigger enterTrigger;
    private ExitTrigger exitTrigger;

    // this is a temporary variable due to the construction of Trigger class.
    // The execution takes steps: activate -> startInterpretation
    // so in activate(), we store the type (ENTER or EXIT), we call activate() of the
    // parent implementation, and the parent implementation will call the startInterpretation()
    // of this trigger (cause we override it).
    private AreaTriggerManager.EventType type = null;

    @AssistedInject
    AreaTrigger(@Assisted TriggerInfo info, @Assisted File folder) {
        super(info, null); // area trigger has scripts in its folder
        this.folder = folder;
    }

    public AreaTrigger(Trigger o) {
        super(o);
        ValidationUtil.assertTrue(o, v -> v instanceof AreaTrigger);
        AreaTrigger other = (AreaTrigger) o;

        this.folder = other.folder;
        this.enterTrigger = new EnterTrigger(other.enterTrigger);
        this.exitTrigger = new ExitTrigger(other.exitTrigger);
        this.type = other.type;
    }

    @Override
    public Trigger clone() {
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + "{area=" + getArea() + '}';
    }

    //we don't need interpreter for area trigger but enter and exit trigger
    protected Interpreter createInterpreter() {
        return null;
    }

    @Override
    protected String getTimingId() {
        return StringUtils.dottedPath(super.getTimingId(), getArea().toString());
    }

    @Override
    protected void compile() throws AbstractTriggerManager.TriggerInitFailedException {
        this.enterTrigger.compile();
        this.exitTrigger.compile();
    }

    public Area getArea(){
        SimpleLocation smallest = info.getConfig()
                .get(SMALLEST, String.class)
                .map(SimpleLocation::valueOf)
                .orElseGet(() -> new SimpleLocation(null, 0, 0, 0));
        SimpleLocation largest = info.getConfig()
                .get(LARGEST, String.class)
                .map(SimpleLocation::valueOf)
                .orElseGet(() -> new SimpleLocation(null, 0, 0, 0));

        return new Area(smallest, largest);
    }

    public void setArea(Area area){
        if(area.isDefective())
            throw new RuntimeException("Defective area detected: "+area);

        info.getConfig().put(SMALLEST, area.getSmallest().toString());
        info.getConfig().put(LARGEST, area.getLargest().toString());
    }

    public EnterTrigger getEnterTrigger() {
        return enterTrigger;
    }

    public void setEnterTrigger(String script) throws AbstractTriggerManager.TriggerInitFailedException {
        enterTrigger = new EnterTrigger(getInfo(), script, this);
    }

    public void setEnterTrigger(EnterTrigger enterTrigger) {
        this.enterTrigger = enterTrigger;
    }

    public ExitTrigger getExitTrigger() {
        return exitTrigger;
    }

    public void setExitTrigger(String script) throws AbstractTriggerManager.TriggerInitFailedException {
        exitTrigger = new ExitTrigger(getInfo(), script, this);
    }

    public void setExitTrigger(ExitTrigger exitTrigger) {
        this.exitTrigger = exitTrigger;
    }

    /**
     * Almost delegate of the activate() of Trigger class, but we need to store the type.
     * @param scriptVars
     * @param type
     */
    public void activate(Map<String, Object> scriptVars, AreaTriggerManager.EventType type) {
        this.type = type;

        // This will invoke the startInterpretation() of this trigger.
        super.activate(scriptVars);
    }

    //intercept and flow the execution to appropriate trigger (enter or exit)
    @Override
    protected void startInterpretation(Map<String, Object> scriptVars, Interpreter interpreter, boolean sync) {
        switch (type) {
            case ENTER:
                if (getEnterTrigger() != null)
                    getEnterTrigger().activate(scriptVars);
                break;
            case EXIT:
                if (getExitTrigger() != null)
                    getExitTrigger().activate(scriptVars);
                break;
            default:
                throw new RuntimeException("Unknown area event type " + type);
        }
    }

    public void addEntity(UUID entityUuid, Object entity) {
        WeakReference<Object> ref = new WeakReference<>(entity);
        this.trackedEntities.put(entityUuid, ref);
    }

    public List<Object> getEntities() {
        List<Object> entities = new ArrayList<>();

        Set<UUID> remove = new HashSet<>();
        for (Map.Entry<UUID, WeakReference<Object>> entry : this.trackedEntities.entrySet()) {
            WeakReference<Object> ref = entry.getValue();
            Object entity = ref.get();
            if (entity != null) {
                entities.add(entity);
            } else {
                remove.add(entry.getKey());
            }
        }

        for (UUID uuid : remove) {
            this.trackedEntities.remove(uuid);
        }

        return entities;
    }

    public Object getEntity(UUID uuid) {
        WeakReference<Object> ref = this.trackedEntities.get(uuid);
        if (ref == null)
            return null;

        Object entity = ref.get();
        //just remove it as it's got garbage-collected.
        if (entity == null) {
            this.trackedEntities.remove(uuid);
        }

        return entity;
    }

    public void removeEntity(UUID uuid) {
        this.trackedEntities.remove(uuid);
    }

    public void toggleSync() {
        info.setSync(!info.isSync());

        notifyObservers();
    }

    public boolean isSync() {
        return info.isSync();
    }
    protected static final String SMALLEST = "smallest";
    protected static final String LARGEST = "largest";

    public static class EnterTrigger extends Trigger {
        private final AreaTrigger areaTrigger;

        public EnterTrigger(TriggerInfo info, String script, AreaTrigger areaTrigger) {
            super(info, script);
            this.areaTrigger = areaTrigger;
        }

        public EnterTrigger(Trigger o) {
            super(o);
            ValidationUtil.assertTrue(o, v -> v instanceof EnterTrigger);
            EnterTrigger other = (EnterTrigger) o;

            this.areaTrigger = other.areaTrigger;
        }

        @Override
        protected String getTimingId() {
            return StringUtils.dottedPath(areaTrigger.getTimingId(), "Enter");
        }

        @Override
        protected void compile() throws AbstractTriggerManager.TriggerInitFailedException {
            super.compile();
        }

    }

    public static class ExitTrigger extends Trigger {
        private final AreaTrigger areaTrigger;

        public ExitTrigger(TriggerInfo info, String script, AreaTrigger areaTrigger) {
            super(info, script);
            this.areaTrigger = areaTrigger;
        }

        public ExitTrigger(Trigger o) {
            super(o);
            ValidationUtil.assertTrue(o, v -> v instanceof ExitTrigger);
            ExitTrigger other = (ExitTrigger) o;

            this.areaTrigger = other.areaTrigger;
        }

        @Override
        protected String getTimingId() {
            return StringUtils.dottedPath(areaTrigger.getTimingId(), "Exit");
        }

        @Override
        protected void compile() throws AbstractTriggerManager.TriggerInitFailedException {
            super.compile();
        }

    }
}
