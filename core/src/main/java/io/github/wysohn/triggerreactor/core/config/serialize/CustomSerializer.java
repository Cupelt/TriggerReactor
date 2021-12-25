/*******************************************************************************
 *     Copyright (C) 2017 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package io.github.wysohn.triggerreactor.core.config.serialize;

import io.github.wysohn.gsoncopy.*;

import java.lang.reflect.Type;

public abstract class CustomSerializer<T> implements Serializer<T> {
    private final Class<?> type;

    public CustomSerializer(Class<?> type) {
        this.type = type;
    }

    @Override
    public final T deserialize(JsonElement json,
                               Type typeOfT,
                               JsonDeserializationContext context) throws JsonParseException {
        return deserialize(json, context);
    }

    @Override
    public final JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(SER_KEY, type.getName());
        jsonObject.add(SER_VALUE, serialize(src, context));
        return jsonObject;
    }

    public abstract T deserialize(JsonElement json, JsonDeserializationContext context);

    public abstract JsonElement serialize(T src, JsonSerializationContext context);


}
