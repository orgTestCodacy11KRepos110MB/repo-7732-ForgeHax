package com.matt.forgehax.util.blocks.properties;

import com.google.common.collect.Sets;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;

/** Created on 5/23/2017 by fr1kin */
public class DimensionProperty implements IBlockProperty {
  private static final String HEADING = "dimensions";

  private Collection<DimensionType> dimensions = Sets.newHashSet();

  private boolean add(DimensionType type) {
    return type != null && dimensions.add(type);
  }

  private static DimensionType getTypeFromId(int id) {
    return DimensionManager.getRegistry().get(id);
  }

  public boolean add(int id) {
    try {
      return add(getTypeFromId(id));
    } catch (Exception e) {; // will throw exception if id does not exist
      return false;
    }
  }

  private boolean remove(DimensionType type) {
    return type != null && dimensions.remove(type);
  }

  public boolean remove(int id) {
    try {
      return remove(getTypeFromId(id));
    } catch (Exception e) {
      return false; // will throw exception if id does not exist
    }
  }

  public boolean contains(int id) {
    if (dimensions.isEmpty()) return true; // true if none other
    else
      try {
        return dimensions.contains(getTypeFromId(id));
      } catch (Exception e) {
        return false;
      }
  }

  @Override
  public void serialize(JsonWriter writer) throws IOException {
    writer.beginArray();
    for (DimensionType dimension : dimensions) {
      writer.value(dimension.getRegistryName().toString());
    }
    writer.endArray();
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    reader.beginArray();
    while (reader.hasNext() && reader.peek().equals(JsonToken.STRING)) {
      String dim = reader.nextString();
      for(Object o : DimensionManager.getRegistry()) {
        DimensionType type = (DimensionType)o;
        if(dim.equals(type.getRegistryName().toString())) {
          add(type);
          break;
        }
      }
    }
  }

  @Override
  public boolean isNecessary() {
    return !dimensions.isEmpty();
  }

  @Override
  public String helpText() {
    final StringBuilder builder = new StringBuilder("{");
    Iterator<DimensionType> it = dimensions.iterator();
    while (it.hasNext()) {
      String name = it.next().getRegistryName().toString();
      builder.append(name);
      if (it.hasNext()) builder.append(", ");
    }
    builder.append("}");
    return builder.toString();
  }

  @Override
  public IBlockProperty newImmutableInstance() {
    return new ImmutableDimension();
  }

  @Override
  public String toString() {
    return HEADING;
  }

  private static class ImmutableDimension extends DimensionProperty {
    @Override
    public boolean add(int id) {
      return false;
    }

    @Override
    public boolean remove(int id) {
      return false;
    }

    @Override
    public boolean contains(int id) {
      return true; // Allow ALL dimensions by default
    }
  }
}
