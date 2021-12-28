package io.github.wysohn.triggerreactor.core.bridge;

import io.github.wysohn.triggerreactor.core.bridge.entity.IEntity;

public interface IWorld extends IMinecraftObject {
    Iterable<? extends IEntity> getEntities();
}
